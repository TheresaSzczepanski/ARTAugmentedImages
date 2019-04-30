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

import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
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
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import android.media.MediaPlayer;
import android.widget.ToggleButton;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.os.Environment;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.media.projection.MediaProjectionManager;
import android.util.DisplayMetrics;
import android.widget.Toast;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 */
/*
Programmed by Rising Tide Augmented Reality Team (ART) - Robert Brodin, Joseph Turner, Emma Riley, and Nora Rooney - 2019
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
    public static Integer currentSongIndex = null;

    // Required ARCore ArFragment and ImageView.
    private ArFragment arFragment;
    private ImageView fitToScanView;

    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private final Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
    public boolean videoIsPlaying = false;

    // DISPLAY_WIDTH and DISPLAY_HEIGHT are set default to 480 by 640, but are changed relative to the size of the phone being used in the onCreate() method.
    private static int DISPLAY_WIDTH = 480;
    private static int DISPLAY_HEIGHT = 640;

    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjection.Callback mMediaProjectionCallback;
    private ToggleButton mToggleButton;
    private MediaRecorder mMediaRecorder;

    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        fitToScanView = findViewById(R.id.image_view_fit_to_scan);

        arFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        DISPLAY_HEIGHT = metrics.heightPixels;
        DISPLAY_WIDTH = metrics.widthPixels;

        // inits the recorder (settings for recording)
        initRecorder();
        Log.d("running", "good!");
        prepareRecorder();
        Log.d("running 2", "good! 2");

        // Finds button with ID toggle. This can be changed to any button, you will just need to cast it to (RadioButton) or (Button) instead of (ToggleButton).
        mToggleButton = (ToggleButton) findViewById(R.id.toggle);

        // Creates an event listener to see when the button is clicked.
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToggleScreenShare(v);
            }
        });

        mMediaProjectionCallback = new MediaProjectionCallback();
    }

    // onDestroy makes sure the recording stops (failsafe if .stop() doesn't work the first time).
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (augmentedImageMap.isEmpty()) {
            fitToScanView.setVisibility(View.VISIBLE);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PERMISSION_CODE) {
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            mToggleButton.setChecked(false);
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(mMediaProjectionCallback, null);
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }

    // Toggles Screen recording on and off.
    public void onToggleScreenShare(View view) {
        if (((ToggleButton)view).isChecked()) {
            mProjectionManager = (MediaProjectionManager) getSystemService
                    (Context.MEDIA_PROJECTION_SERVICE);
            mToggleButton.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            mToggleButton.setText("   ");
            shareScreen();
        } else {
            mMediaRecorder.reset();
            stopScreenSharing();
            mToggleButton.setText("Off");
            mToggleButton.setVisibility(View.VISIBLE);
            mToggleButton.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
    }


    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
            return;
        }
        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();

    }


    // Stops screen recording.
    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mMediaRecorder.reset();
        //mMediaRecorder.release();
    }

    // Creates a VirtualDisplay object using the parameters set at the start.
    private VirtualDisplay createVirtualDisplay () {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }

// Checks if mToggleButton is clicked. Will have to change mToggleButton to the new button variable if a mToggleButton is not being used.
private class MediaProjectionCallback extends MediaProjection.Callback {
    @Override
    public void onStop() {
        if (mToggleButton.isChecked()) {
            mToggleButton.setChecked(false);
            mMediaRecorder.stop();
            mMediaRecorder.reset();
        }
        mMediaProjection = null;
        stopScreenSharing();
    }
}

    // Prepares recorder (checks for exceptions)
    private void prepareRecorder () {
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    // Used to create the desired filePath that the file will be saved in. By default it goes to /sdcard/recordings, but could be changed to anything.
    public String getFilePath () {
        // Can change the folder which the video is saved into.
        final String directory = Environment.getExternalStorageDirectory() + File.separator + "Recordings";
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            // Toast is just a fancy class that helps print custom error messages.
            Toast.makeText(this, "Failed to get External Storage", Toast.LENGTH_SHORT).show();
            return null;
        }
        // Using final because the folder variable should NEVER be changed.
        final File folder = new File(directory);
        boolean success = true;

        // If the folder doesn't exist, it is created using mkdir().
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        String filePath;

        // Once the folder exists (if it didn't already), the filePath is set to the directory + capture_date.mp4. Capture can be changed quite easily.
        if (success) {
            String videoName = ("capture_" + getCurSysDate() + ".mp4");
            filePath = directory + File.separator + videoName;
        } else {
            Toast.makeText(this, "Failed to create Recordings directory", Toast.LENGTH_SHORT).show();
            return null;
        }
        return filePath;
    }

    // Gets the date (relative to the device). Will be used for the file name in getFilePath().
    public String getCurSysDate () {
        return new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
    }

    // Used for Codec and recording settings!
    private void initRecorder () {
        int YOUR_REQUEST_CODE = 200; // could be something else..
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    YOUR_REQUEST_CODE);


            if (mMediaRecorder == null) {
                mMediaRecorder = new MediaRecorder();
            }

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            // Bitrate is set relative to the screen, so it is just the width of the device * the height of the device.
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            // Framerate crashed at 60 when testing.
            mMediaRecorder.setVideoFrameRate(30);
            // Sets video size relative to the phone.
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
            // Sets file path using the getFilePath() method.
            mMediaRecorder.setOutputFile(getFilePath());
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

                        if (currentSongIndex == null) {
                            // If there is no song playing, set currentSongIndex to the song that aligns with
                            // the detected image
                            currentSongIndex = augmentedImage.getIndex();

                            audioMediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentSongIndex]);
                            audioMediaPlayer.start();
                            // Create a new mediaPlayer object with the correct song
                            // Start the song
                        } else if ((currentSongIndex != augmentedImage.getIndex())) {

                            // If the song that is playing does not match up with the song the image it detects
                            // is assigned to:
                            // Assign the correct song to currentSongIndex
                            // Stop playing the song, reset the mediaPlayer object
                            currentSongIndex = augmentedImage.getIndex();
                            audioMediaPlayer.stop();
                            audioMediaPlayer.release();
                            //audioMediaPlayer = null;

                            audioMediaPlayer = MediaPlayer.create(this, AugmentedImageFragment.Music_list[currentSongIndex]);
                            audioMediaPlayer.start();
                            // Recreate the mediaPlayer object with the correct song
                            // Start the song

                        }

                        // Create a new anchor for newly found images.

                        if (!augmentedImageMap.containsKey(augmentedImage)) {
                            AugmentedImageNode node = new AugmentedImageNode(this, AugmentedImageFragment.Image_list, augmentedImage.getIndex());
                            node.setImage(augmentedImage, augmentedImage.getIndex());
                            augmentedImageMap.put(augmentedImage, node);
                            arFragment.getArSceneView().getScene().addChild(node);
                        }
                        break;

                    case STOPPED:

                        augmentedImageMap.remove(augmentedImage);

                        break;

                    default:
                }
                // Video is playing
            } else {
                //TODO: Make it place the frame corners with the video
                if(audioMediaPlayer != null) {
                    audioMediaPlayer.release();
                    currentSongIndex = null;
                }
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

                                AugmentedImageStaticNode.node = new AugmentedImageNode(this, AugmentedImageFragment.Image_list, augmentedImage.getIndex());

                                // Used to check if a video will be played or if an audio file will be played. This, when true, creates a video.

                                if (AugmentedImageFragment.imagePlaysVideoBooleanList[augmentedImage.getIndex()]) {
                                    // In the event that there is already music playing, the mediaplayer is stopped and released.

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