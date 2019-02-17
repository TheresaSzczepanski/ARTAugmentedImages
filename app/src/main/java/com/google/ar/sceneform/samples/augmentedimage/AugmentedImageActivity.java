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

import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ExternalTexture;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import android.media.MediaPlayer;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
/*

Programmed by Rising Tide Augmented Reality Team (ART) - Joseph Turner, Emma Riley, Nora Rooney, and Robert Brodin - 2019

AugmentedImage - Video version, detects images and places specific videos (see AugmentedImageFragment). When a different image is detected, the video playing is stopped and a new video is started (specific to the new image).

 */

public class AugmentedImageActivity extends AppCompatActivity {
  
  public static Integer augmentedImageVideoPlayerIndex = null;
  private AugmentedImage mediaAugmentedImage = null;
  public static CompletableFuture<ModelRenderable> videoRenderable;
  @Nullable public static ModelRenderable videoPlacedRenderable;


  private ArFragment arFragment;
  private ImageView fitToScanView;

  //private AugmentedImageNode node;

  // Augmented image and its associated center pose anchor, keyed by the augmented image in
  // the database.
  private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
    fitToScanView = findViewById(R.id.image_view_fit_to_scan);

    arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (augmentedImageMap.isEmpty()) {
      fitToScanView.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Registered with the Sceneform Scene object, this method is called at the start of each frame.
   *
   * @param frameTime - time since last frame.
   */
  private void onUpdateFrame(FrameTime frameTime) {
    Frame frame = arFragment.getArSceneView().getArFrame();

    // If there is no frame or ARCore is not tracking yet, just return.
    if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      return;
    }


    Collection<AugmentedImage> updatedAugmentedImages =
        frame.getUpdatedTrackables(AugmentedImage.class);
    for (AugmentedImage augmentedImage : updatedAugmentedImages) {

      switch (augmentedImage.getTrackingState()) {
        case PAUSED:
          // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
          // but not yet tracked.
          String text = "Detected Image " + augmentedImage.getIndex();
          SnackbarHelper.getInstance().showMessage(this, text);

          break;

        case TRACKING:

          // Have to switch to UI Thread to update View.
          fitToScanView.setVisibility(View.GONE);


          // Checks if the node that is used for the video is not null, if the node's mediaplayer is not null, and if the current image detected is not the same image as the last image detected.
          // This section is used to stop the video from playing.
          if((AugmentedImageStaticNode.node != null) &&(!(AugmentedImageStaticNode.node.nodeMediaPlayer == null)) && augmentedImage.getIndex() != augmentedImageVideoPlayerIndex) {


            // Video is stopped.
            AugmentedImageStaticNode.node.stopVideo();
            // Node that the video is placed on is removed from the scene.
            AugmentedImageStaticNode.node.removeChild(AugmentedImageStaticNode.node.videoNode);
            // the image that correlates to the video that is being stopped is removed from the hashmap of detected images (makes it so if that same image is detected later, a video will be played)
            augmentedImageMap.remove(mediaAugmentedImage);

          }

          if (!augmentedImageMap.containsKey(augmentedImage)) {

            // New AugmentedImageNode class is created in THIS context.
            AugmentedImageStaticNode.node = new AugmentedImageNode(this);
            // startVideo takes arguments: context, imageIndex, and texture. This method starts the video (using .create()), and places it on the surface.
            AugmentedImageStaticNode.node.startVideo(this, augmentedImage.getIndex(), AugmentedImageStaticNode.node.texture);
            // Set image places the video on a renderable.
            AugmentedImageStaticNode.node.setImage(this, augmentedImage, augmentedImage.getIndex());
            // augmentedImage is put in a hashmap, will be used to detect if the image has already been detected (deprecated in this version)
            augmentedImageMap.put(augmentedImage, AugmentedImageStaticNode.node);
            arFragment.getArSceneView().getScene().addChild(AugmentedImageStaticNode.node);
            mediaAugmentedImage = augmentedImage; // fixes the issue of videos not playing with their specific image if that image has been detected before.


          }
          break;

        case STOPPED:

          augmentedImageMap.remove(augmentedImage);

          break;

        default:

      }
    }
  }
}
