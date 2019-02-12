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
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import android.media.MediaPlayer;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
public class AugmentedImageActivity extends AppCompatActivity {

  private MediaPlayer mediaPlayer;
  public static MediaPlayer augmentedImageVideoPlayer = null;
  public static Integer augmentedImageVideoPlayerIndex = null;
  private Integer currentSongIndex = null;

  private ArFragment arFragment;
  private ImageView fitToScanView;

  private AugmentedImageNode node;

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

          //Need to check if currentNode is null or not

          // TODO: Issue is with I think how the node is defined and the variable is set equal to it, but it is not actually changing it.
          if((node != null) &&(!(AugmentedImageMediaPlayer.mediaPlayerInstance == null)) && augmentedImage.getIndex() != augmentedImageVideoPlayerIndex){

            // TODO: breaks here, augmentedImageVideoPlayer is not working correctly.

            // TODO: Actually, the solution here might be to send a signal to augmentedImageNode to stop the video.
            // TODO: using static methods is likely not a viable solution.

            // Stops the currentVideo from playing if a new image is detected and the node's video player is null.
            Log.d("if", "statement works!");
            Log.d("videoname", node.getVideoTitle());

            // It does make shaq bigger when a new image is detected!

            //TODO: ISSUE HERE!!!! - 071219

            node.videoNode.setWorldScale(new Vector3(10.0f, 10.0f, 10.0f));
            // This works!

            if(AugmentedImageMediaPlayer.mediaPlayerInstance.isPlaying()) {
              // this is running....
              //AugmentedImageMediaPlayer.mediaPlayerInstance.stop();
              Log.d("image", "stopping!!!");
              node.videoNode.setParent(null);
              node.videoNode.setRenderable(null);
              node.stopVideo();

            }
            // What if a mediaplayer is created in AugmentedImageActivity and set in node.
            //node.removeChild(node.videoNode);
            //node = null;

            //node.getScene().onRemoveChild(node.getParent());
            //node.setRenderable(null);



            //node.nodeMediaPlayer.stop();
            //node.nodeMediaPlayer.release();
            //node.nodeMediaPlayer = null;

            //node.getScene().onRemoveChild(node.getParent());
            //node.setRenderable(null);


            // Removed the node but still playing song.
            //node.removeChild(node.videoNode);
            //node = null;
            //augmentedImageVideoPlayer.stop();
            //augmentedImageVideoPlayer.release();
          }


          if (currentSongIndex == null) {
            // If there is no song playing, set currentSongIndex to the song that aligns with
            // the detected image
            currentSongIndex = augmentedImage.getIndex();

            mediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentSongIndex]);
            mediaPlayer.start();
            // Create a new mediaPlayer object with the correct song
            // Start the song
          }
          // Stopping the video player if another new image is detected.
          // Checks if the videoplayer is not null and if the currentImage index is equal to the same index as augmentedImageVideoPlayerIndex (
          else if((currentSongIndex != augmentedImage.getIndex())){

            // If the song that is playing does not match up with the song the image it detects
            // is assigned to:
            // Assign the correct song to currentSongIndex
            // Stop playing the song, reset the mediaPlayer object
            currentSongIndex = augmentedImage.getIndex();
            mediaPlayer.stop();
            mediaPlayer.release();

            mediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentSongIndex]);
            mediaPlayer.start();
            // Recreate the mediaPlayer object with the correct song
            // Start the song

          }

          // Create a new anchor for newly found images.
          if (!augmentedImageMap.containsKey(augmentedImage)) {
            //augmentedImageVideoPlayer = MediaPlayer.create(context, AugmentedImageFragment.Video_list[augmentedImageIndex]);
            node = new AugmentedImageNode(this);
            node.setImage(augmentedImage, augmentedImage.getIndex(),this);
            augmentedImageMap.put(augmentedImage, node);
            arFragment.getArSceneView().getScene().addChild(node);

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
