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
Also works with playing audio files instead of video files (see imagePlaysBooleanList in AugmentedImageFragment)
 */

public class AugmentedImageActivity extends AppCompatActivity {


    // Used to keep track of which video is currently playing (is changed ONLY in AugmentedImageNode)
    public static Integer augmentedImageVideoPlayerIndex = null;
    // mediaAugmentedImage is used to check if an image has been detected before, and is also used to remove specific keys from the augmented image hashmap. TODO: This will absolutely have to be changed for placing renderables and keeping renderables placed. This makes sense for videos and music, but not for renderables (ARTGallery)
    private AugmentedImage mediaAugmentedImage = null;
    // Both videoRenderable and videoPlacedRenderable are used by AugmentedImageNode, and are declared in this class to only require them to be rendered/loaded once.
    public static CompletableFuture<ModelRenderable> videoRenderable;
    @Nullable public static ModelRenderable videoPlacedRenderable;

    // audioMediaPlayer and currentSongIndex are used to play audio files (generally from musicList in AugmentedImageFragment)
    private MediaPlayer audioMediaPlayer;
    public static Integer currentIndex = null;

    // Required ARCore ArFragment and ImageView.
    private ArFragment arFragment;
    private ImageView fitToScanView;

    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
    public boolean videoIsPlaying = false;


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

            if (!(AugmentedImageFragment.imagePlaysVideoBooleanList[augmentedImage.getIndex()])) {
                if (videoIsPlaying) {
                    if ((AugmentedImageStaticNode.node != null) && (!(AugmentedImageStaticNode.node.nodeMediaPlayer == null)) && augmentedImage.getIndex() != augmentedImageVideoPlayerIndex) {
                        // Video is stopped.
                        AugmentedImageStaticNode.node.stopVideo();
                        // Node that the video is placed on is removed from the scene.
                        AugmentedImageStaticNode.node.removeChild(AugmentedImageStaticNode.node.videoNode);
                        // the image that correlates to the video that is being stopped is removed from the hashmap of detected images (makes it so if that same image is detected later, a video will be played)
                        augmentedImageMap.remove(mediaAugmentedImage);
                    }
                }
                currentIndex = augmentedImage.getIndex();
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

                        if (currentIndex == null) {
                            // If there is no song playing, set currentIndex to the song that aligns with
                            // the detected image
                            currentIndex = augmentedImage.getIndex();

                            audioMediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentIndex]);
                            audioMediaPlayer.start();
                            // Create a new mediaPlayer object with the correct song
                            // Start the song
                        } else if ((currentIndex != augmentedImage.getIndex())) {

                            // If the song that is playing does not match up with the song the image it detects
                            // is assigned to:
                            // Assign the correct song to currentIndex
                            // Stop playing the song, reset the mediaPlayer object
                            currentIndex = augmentedImage.getIndex();
                            audioMediaPlayer.stop();
                            audioMediaPlayer.release();

                            audioMediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentIndex]);
                            audioMediaPlayer.start();
                            // Recreate the mediaPlayer object with the correct song
                            // Start the song

                        }

                        // Create a new anchor for newly found images.

                        if (!augmentedImageMap.containsKey(augmentedImage)) {
                            AugmentedImageNode node = new AugmentedImageNode(this, AugmentedImageFragment.Image_list);
                            node.setImage(augmentedImage);
                            augmentedImageMap.put(augmentedImage, node);
                            arFragment.getArSceneView().getScene().addChild(node);
                        }
                        break;

                    case STOPPED:

                        augmentedImageMap.remove(augmentedImage);

                        break;

                    default:
                }
            } else {
                currentIndex = augmentedImage.getIndex();
                switch (augmentedImage.getTrackingState()) {
                    case PAUSED:
                        // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                        // but not yet tracked.
                        String text = "Detected Image " + augmentedImage.getIndex();
                        SnackbarHelper.getInstance().showMessage(this, text);
                        break;
                    // If an image is detected
                    case TRACKING:
                        // Have to switch to UI Thread to update View.
                        fitToScanView.setVisibility(View.GONE);

                        // Checks if the node that is used for the video is not null, if the node's mediaplayer is not null, and if the current image detected is not the same image as the last image detected.
                        // This section is used to stop the video from playing.
                        if(videoIsPlaying) {
                            if ((AugmentedImageStaticNode.node != null) && (!(AugmentedImageStaticNode.node.nodeMediaPlayer == null)) && augmentedImage.getIndex() != augmentedImageVideoPlayerIndex) {
                                // Video is stopped.
                                AugmentedImageStaticNode.node.stopVideo();
                                // Node that the video is placed on is removed from the scene.
                                AugmentedImageStaticNode.node.removeChild(AugmentedImageStaticNode.node.videoNode);
                                // the image that correlates to the video that is being stopped is removed from the hashmap of detected images (makes it so if that same image is detected later, a video will be played)
                                augmentedImageMap.remove(mediaAugmentedImage);
                            }
                        }
                        // If the augmentedImageMap (HASHMAP) has not seen the image before (the augmentedImage key is not in the hashmap), then a video or audio file is played.
                        if (!augmentedImageMap.containsKey(augmentedImage)) {

                            // New AugmentedImageNode class is created in THIS context.
                            AugmentedImageStaticNode.node = new AugmentedImageNode(this, AugmentedImageFragment.Image_list);

                            // Used to check if a video will be played or if an audio file will be played. This, when true, creates a video.
                            if (AugmentedImageFragment.imagePlaysVideoBooleanList[augmentedImage.getIndex()]) {
                                // In the event that there is already music playing, the mediaplayer is stopped and released.
                                if(audioMediaPlayer != null) {
                                    if (audioMediaPlayer.isPlaying()) {
                                        audioMediaPlayer.stop();
                                        audioMediaPlayer.release();
                                        currentIndex = null;
                                    }
                                }
                                // startVideo takes arguments: context, imageIndex, and texture. This method starts the video (using .create()), and places it on the surface.
                                AugmentedImageStaticNode.node.startVideo(this, augmentedImage.getIndex(), AugmentedImageStaticNode.node.texture);
                                // Set image places the video on a renderable.
                                AugmentedImageStaticNode.node.createVideo(this, augmentedImage, augmentedImage.getIndex());
                                // augmentedImage is put in a hashmap, will be used to detect if the image has already been detected (deprecated in this version)
                                augmentedImageMap.put(augmentedImage, AugmentedImageStaticNode.node);
                                arFragment.getArSceneView().getScene().addChild(AugmentedImageStaticNode.node);
                                // Fixes the issue of videos not playing with their specific image if that image has been detected before.
                                mediaAugmentedImage = augmentedImage;
                                videoIsPlaying = true;
                            }
                            // Otherwise, an audio file is created and started.
                            else {
                                videoIsPlaying = false;
                                if (currentIndex == null) {
                                    // If there is no song playing, set currentSongIndex to the song that aligns with
                                    // the detected image
                                    currentIndex = augmentedImage.getIndex();
                                    // Creates a newAugmentedImageStaticNode.node object with the correct song
                                    // Start the song
                                    //audioMediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentIndex]);
                                    //audioMediaPlayer.start();

                                } else if ((currentIndex != augmentedImage.getIndex())) {
                                    // If the song that is playing does not match up with the song the image it detects
                                    // is assigned to:
                                    // Assign the correct song to currentSongIndex
                                    // Stop playing the song, reset theAugmentedImageStaticNode.node object
                                    currentIndex = augmentedImage.getIndex();
                                    audioMediaPlayer.stop();
                                    audioMediaPlayer.release();
                                    // Recreate theAugmentedImageStaticNode.node object with the correct song
                                    // Start the song
                                    audioMediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentIndex]);
                                    audioMediaPlayer.start();
                                }
                            }
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
}
