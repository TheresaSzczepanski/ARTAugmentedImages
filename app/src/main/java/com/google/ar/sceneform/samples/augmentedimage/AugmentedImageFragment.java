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

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.ar.core.AugmentedImageDatabase;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.sceneform.samples.common.helpers.SnackbarHelper;
import com.google.ar.sceneform.ux.ArFragment;
import java.io.IOException;
import java.io.InputStream;

/**
 * Extend the ArFragment to customize the ARCore session configuration to include Augmented Images.
 */
public class AugmentedImageFragment extends ArFragment {
  private static final String TAG = "AugmentedImageFragment";

  // Create a list of the images to be used (the order must match the order of the Music_list [indexes are the same!])
  private static final String[] Image_list = {
          "earth.jpg",
          "elephant.jpg",
          "sushi.jpg",
          "skatergirl.JPG",
          "firechicken.JPG"
  };

  // Create a list of the song file paths to be used (the order must match the order of Image_list)
  public static final int[] Music_list = {R.raw.earth, R.raw.kahoot, R.raw.sushi, R.raw.sushi, R.raw.sushi};

  // Creates a list of video files to link with image index
  public static final int[] Video_list = {R.raw.liquishaq, R.raw.skater, R.raw.liquishaq, R.raw.skater, R.raw.chicken};

  public static final boolean[] imagePlaysVideoBooleanList = {false, false, false, false, true};

  // Do a runtime check for the OpenGL level available at runtime to avoid Sceneform crashing the
  // application.
  private static final double MIN_OPENGL_VERSION = 3.0;

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);

    // Check for Sceneform being supported on this device.  This check will be integrated into
    // Sceneform eventually.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
      Log.e(TAG, "Sceneform requires Android N or later");
      SnackbarHelper.getInstance()
              .showError(getActivity(), "Sceneform requires Android N or later");
    }

    String openGlVersionString =
            ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                    .getDeviceConfigurationInfo()
                    .getGlEsVersion();
    if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
      Log.e(TAG, "Sceneform requires OpenGL ES 3.0 or later");
      SnackbarHelper.getInstance()
              .showError(getActivity(), "Sceneform requires OpenGL ES 3.0 or later");
    }
  }

  @Override
  public View onCreateView(
          LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = super.onCreateView(inflater, container, savedInstanceState);

    // Turn off the plane discovery since we're only looking for images
    getPlaneDiscoveryController().hide();
    getPlaneDiscoveryController().setInstructionView(null);
    getArSceneView().getPlaneRenderer().setEnabled(false);
    return view;
  }

  @Override
  protected Config getSessionConfiguration(Session session) {
    Config config = super.getSessionConfiguration(session);
    if (!setupAugmentedImageDatabase(config, session)) {
      SnackbarHelper.getInstance()
              .showError(getActivity(), "Could not setup augmented image database");
    }
    return config;
  }

  private boolean setupAugmentedImageDatabase(Config config, Session session) {
    AugmentedImageDatabase augmentedImageDatabase;

    AssetManager assetManager = getContext() != null ? getContext().getAssets() : null;
    if (assetManager == null) {
      Log.e(TAG, "Context is null, cannot intitialize image database.");
      return false;
    }

    // There are two ways to configure an AugmentedImageDatabase:
    // 1. Add Bitmap to DB directly
    // 2. Load a pre-built AugmentedImageDatabase
    // Option 2) has
    // * shorter setup time
    // * doesn't require images to be packaged in apk.

    augmentedImageDatabase = new AugmentedImageDatabase(session);

    for (int i = 0; i < Image_list.length; i++) {

      // For each item in the Image_list, add the image to the database

      Bitmap augmentedImageBitmap = loadAugmentedImageBitmap(assetManager, Image_list[i]);
      if (augmentedImageBitmap == null) {
        return false;
    }
      augmentedImageDatabase.addImage(Image_list[i], augmentedImageBitmap);
    }
      // If the physical size of the image is known, you can instead use:
      //     augmentedImageDatabase.addImage("image_name", augmentedImageBitmap, widthInMeters);
      // This will improve the initial detection speed. ARCore will still actively estimate the
      // physical size of the image as it is viewed from multiple viewpoints.

      config.setAugmentedImageDatabase(augmentedImageDatabase);
      return true;
    }

    private Bitmap loadAugmentedImageBitmap (AssetManager assetManager, String IMAGE_NAME){
      try (InputStream is = assetManager.open(IMAGE_NAME)) {
        return BitmapFactory.decodeStream(is);
      } catch (IOException e) {
        Log.e(TAG, "IO exception loading augmented image bitmap.", e);
      }
      return null;
    }
  }
