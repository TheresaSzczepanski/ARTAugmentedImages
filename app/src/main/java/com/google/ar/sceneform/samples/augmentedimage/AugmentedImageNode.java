/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import android.graphics.SurfaceTexture;



/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})
public class AugmentedImageNode extends AnchorNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage image;

  // Models of the 4 corners.  We use completable futures here to simplify
  // the error handling and asynchronous loading.  The loading is started with the
  // first construction of an instance, and then used when the image is set.
  private static CompletableFuture<ModelRenderable> ulCorner;

  // The color to filter out of the video.
  private static final Color CHROMA_KEY_COLOR = new Color(0.1843f, 1.0f, 0.098f);

  // Controls the height of the video in world space.
  private static final float VIDEO_HEIGHT_METERS = 0.2f;


  public MediaPlayer nodeMediaPlayer;
  public Node videoNode;
  public ExternalTexture texture = new ExternalTexture();


  // Used to create the video. If nodeMediaPlayer.start() is uncommented, then it will appear immediately when an image is detected but a black screen will briefly appear. Starting the nodeMediaPlayer inside of getImage() removes that issue.
  public void startVideo(Context context, int augmentedImageIndex, ExternalTexture texture) {
    nodeMediaPlayer = MediaPlayer.create(context, AugmentedImageFragment.Video_list[augmentedImageIndex]);
    nodeMediaPlayer.setSurface(texture.getSurface());
    nodeMediaPlayer.setLooping(false);
    //nodeMediaPlayer.start();
  }

  // Used to stop the video.
  public void stopVideo() {
    try {
      texture.getSurfaceTexture().release();
      nodeMediaPlayer.reset();
      nodeMediaPlayer.prepare();
      nodeMediaPlayer.stop();
      nodeMediaPlayer.release();
      nodeMediaPlayer = null;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // Used to build the renderables. Should only run the first time an image is detected.
  public AugmentedImageNode(Context context) {
    // Upon construction, start loading the models for the corners of the frame.
    if (ulCorner == null) {
      ulCorner =
              ModelRenderable.builder()
                      .setSource(context, Uri.parse("models/frame_lower_left.sfb"))
                      .build();
    }
    if (AugmentedImageActivity.videoRenderable == null) {
      AugmentedImageActivity.videoRenderable =
              ModelRenderable.builder()
                      .setSource(context, Uri.parse("models/chroma_key_video.sfb"))
                      .build();
    }
  }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */
  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})

  // Used to place the renderable on the image (video is also placed on the renderable)
  public void setImage(Context context, AugmentedImage image, int augmentedImageIndex) {
    this.image = image;


    CompletableFuture.allOf(AugmentedImageActivity.videoRenderable)
            .handle((notUsed, throwable) -> {
              // When you build a Renderable, Sceneform loads its resources in the background while
              // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
              // before calling get().

              if (throwable != null) {
                return null;
              }

              try {
                // Sets the texture to the texture defined using (new ExternalTexture()) -> the video's surface is on that texture.
                AugmentedImageActivity.videoPlacedRenderable = AugmentedImageActivity.videoRenderable.get();
                AugmentedImageActivity.videoPlacedRenderable.getMaterial().setExternalTexture("videoTexture", texture);
                AugmentedImageActivity.videoPlacedRenderable.getMaterial().setFloat4("keyColor", CHROMA_KEY_COLOR);

                // Everything finished loading successfully.
              } catch (InterruptedException | ExecutionException ex) {
              }

              return null;
            });




    // augmentedImageIndex is taken from the arguments of this method - (setImage)
    AugmentedImageActivity.augmentedImageVideoPlayerIndex = augmentedImageIndex;
    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));


    // videoNode is created and will be placed on the image, directly parallel to the image.
    videoNode = new Node();
    videoNode.setParent(this);


    float videoWidth = nodeMediaPlayer.getVideoWidth();
    float videoHeight = nodeMediaPlayer.getVideoHeight();
    videoNode.setLocalScale(
            new Vector3(
                    VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f));

    // Sets the video's position from perpendicular to the image to parallel to the detected image.
    Quaternion q1 = videoNode.getLocalRotation();
    Quaternion q2 = Quaternion.axisAngle(new Vector3(1.0f, 0f, 0f), -90f);
    videoNode.setLocalRotation(Quaternion.multiply(q1, q2));

    // If the video is not playing, it will be started.
    if (!nodeMediaPlayer.isPlaying()) {
      nodeMediaPlayer.start();
      // Wait to set the renderable until the first frame of the  video becomes available.
      // This prevents the renderable from briefly appearing as a black quad before the video
      // plays.
      texture
              .getSurfaceTexture()
              .setOnFrameAvailableListener(
                      (SurfaceTexture surfaceTexture) -> {
                        videoNode.setRenderable(AugmentedImageActivity.videoPlacedRenderable);
                        texture.getSurfaceTexture().setOnFrameAvailableListener(null);

                      });
    } else {
      videoNode.setRenderable(AugmentedImageActivity.videoPlacedRenderable);
    }

  }
}
