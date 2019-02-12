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
import com.google.ar.sceneform.lullmodel.ModelPipelineSkeletonDef;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import android.graphics.SurfaceTexture;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;


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
  private static CompletableFuture<ModelRenderable> urCorner;
  private static CompletableFuture<ModelRenderable> lrCorner;
  private static CompletableFuture<ModelRenderable> llCorner;

  // The color to filter out of the video.
  private static final Color CHROMA_KEY_COLOR = new Color(0.1843f, 1.0f, 0.098f);

  // Controls the height of the video in world space.
  private static final float VIDEO_HEIGHT_METERS = 0.2f;

  private CompletableFuture<ModelRenderable> videoRenderable;
  @Nullable private ModelRenderable videoPlacedRenderable;

  public MediaPlayer nodeMediaPlayer;
  public Node videoNode;
  private String videoString = "";
  // Todo: Will probably not need to define the scope up here but rather in the getImages method. Used to try and get the video stopping.
  public ExternalTexture texture;

  public AugmentedImageNode(Context context) {
    // Upon construction, start loading the models for the corners of the frame.
    if (ulCorner == null) {
      ulCorner =
          ModelRenderable.builder()
              .setSource(context, Uri.parse("models/frame_lower_left.sfb"))
              .build();
      urCorner =
          ModelRenderable.builder()
              .setSource(context, Uri.parse("models/Car.sfb"))
              .build();
      llCorner =
          ModelRenderable.builder()
              .setSource(context, Uri.parse("models/frame_upper_left.sfb"))
              .build();
      lrCorner =
          ModelRenderable.builder()
              .setSource(context, Uri.parse("models/frame_upper_right.sfb"))
              .build();
    }
    if(videoRenderable == null){
      videoRenderable =
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

  // TODO: Adding video to image. Working on it right now.
  // TODO: Changing context.
  public void setImage(AugmentedImage image, int augmentedImageIndex, Context context) {
    this.image = image;
    texture = new ExternalTexture();


    // If any of the models are not loaded, then recurse when all are loaded.
    if (!ulCorner.isDone() || !urCorner.isDone() || !llCorner.isDone() || !lrCorner.isDone()) {
      CompletableFuture.allOf(ulCorner, urCorner, llCorner, lrCorner)
              // Could have errors with passing augmentedImageInt here.
          .thenAccept((Void aVoid) -> setImage(image, augmentedImageIndex,  context))
          .exceptionally(
              throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
              });

      // TODO: This will probably not work.
      CompletableFuture.allOf(videoRenderable)
              .handle((notUsed, throwable) -> {
                // When you build a Renderable, Sceneform loads its resources in the background while
                // returning a CompletableFuture. Call handle(), thenAccept(), or check isDone()
                // before calling get().

                if (throwable != null) {
                  return null;
                }

                try {
                  videoPlacedRenderable = videoRenderable.get();
                  videoPlacedRenderable.getMaterial().setExternalTexture("videoTexture", texture);
                  videoPlacedRenderable.getMaterial().setFloat4("keyColor", CHROMA_KEY_COLOR);

                  // Everything finished loading successfully.
                } catch (InterruptedException | ExecutionException ex) {
                }

                return null;
                      });
    }

  // TODO: MEDIAPLAYER NOT WORKING. In the process of switching code over.


    //nodeMediaPlayer = MediaPlayer.create(context, AugmentedImageFragment.Video_list[augmentedImageIndex]);
    //nodeMediaPlayer.setSurface(texture.getSurface());
    //nodeMediaPlayer.setLooping(false);
    AugmentedImageMediaPlayer.createVideo(context, augmentedImageIndex);
    AugmentedImageMediaPlayer.editVideoSettings(texture.getSurface(), false);

    if(AugmentedImageFragment.Video_list[augmentedImageIndex] == 0){
      videoString = "shaq";
    }
    else{
      videoString = "skater";
    }




    // TODO: Setting this equal to the mediaplayer breaks it.
    //AugmentedImageActivity.augmentedImageVideoPlayer = mediaPlayer;

    // augmentedImageIndex is taken from the arguments of this method - (setImage)
    AugmentedImageActivity.augmentedImageVideoPlayerIndex = augmentedImageIndex;
    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));


    //TODO: Video nodes here.
    videoNode = new Node();
    videoNode.setParent(this);


    float videoWidth = AugmentedImageMediaPlayer.mediaPlayerInstance.getVideoWidth();
    float videoHeight = AugmentedImageMediaPlayer.mediaPlayerInstance.getVideoHeight();
    videoNode.setLocalScale(
            new Vector3(
                    VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f));

    Quaternion q1 = videoNode.getLocalRotation();
    Quaternion q2 = Quaternion.axisAngle(new Vector3(1.0f, 0f, 0f), -90f);
    videoNode.setLocalRotation(Quaternion.multiply(q1, q2));

    if (!AugmentedImageMediaPlayer.mediaPlayerInstance.isPlaying()) {
      AugmentedImageMediaPlayer.mediaPlayerInstance.start();

      // Wait to set the renderable until the first frame of the  video becomes available.
      // This prevents the renderable from briefly appearing as a black quad before the video
      // plays.
      texture
              .getSurfaceTexture()
              .setOnFrameAvailableListener(
                      (SurfaceTexture surfaceTexture) -> {
                        videoNode.setRenderable(videoPlacedRenderable);
                        texture.getSurfaceTexture().setOnFrameAvailableListener(null);

                      });
      //AugmentedImageMediaPlayer.mediaPlayerInstance.stop();
      // this stops. I AM SO CONFUSED
    } else {
      videoNode.setRenderable(videoPlacedRenderable);
    }


    /*
    // Make the 4 corner nodes.
    Vector3 localPosition = new Vector3();
    Node cornerNode;

    // Upper left corner.
    localPosition.set(-0.5f * image.getExtentX(), 0.0f, -0.5f * image.getExtentZ());
    //localPosition.set(0.0f, 0.0f, 0.0f);
    cornerNode = new Node();
    cornerNode.setParent(this);
    cornerNode.setLocalPosition(localPosition);
    cornerNode.setRenderable(ulCorner.getNow(null));

    // Upper right corner.
    //localPosition.set(0.5f * image.getExtentX(), 0.0f, -0.5f * image.getExtentZ());
    localPosition.set(0.0f, 0.0f, 0.0f);
    cornerNode = new Node();
    cornerNode.setParent(this);
    cornerNode.setLocalPosition(localPosition);
    cornerNode.setRenderable(urCorner.getNow(null));
    //cornerNode.setWorldScale(new Vector3(0, 0,0 ));
    /*currently working to scale down object to size of image. Rob suggested using
    setworldscale, but not sure how to set it, will test what these and othre values return
    then focus on changing the jpeg
     */


    /*
    // Lower right corner.
    localPosition.set(0.5f * image.getExtentX(), 0.0f, 0.5f * image.getExtentZ());
    cornerNode = new Node();
    cornerNode.setParent(this);
    cornerNode.setLocalPosition(localPosition);
    cornerNode.setRenderable(lrCorner.getNow(null));

    // Lower left corner.
    localPosition.set(-0.5f * image.getExtentX(), 0.0f, 0.5f * image.getExtentZ());
    cornerNode = new Node();
    cornerNode.setParent(this);
    cornerNode.setLocalPosition(localPosition);
    cornerNode.setRenderable(llCorner.getNow(null));
    */

    //AugmentedImageMediaPlayer.resetVideo(); -> this correctly stops the video
  }

  public AugmentedImage getImage() {
    return image;
  }

  public void stopVideo(){
    // If the video is playing.
    AugmentedImageMediaPlayer.resetVideo();
    //mediaPlayer.release();
  }

  // If the mediaPlayer is null the method returns true. Otherwise, it returns false.

  public String getVideoTitle(){
    return videoString;
  }
}
