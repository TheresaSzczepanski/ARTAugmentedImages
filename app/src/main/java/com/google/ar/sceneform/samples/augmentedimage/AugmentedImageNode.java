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
import android.net.Uri;
import android.util.Log;
import android.view.Display;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Node for rendering an augmented image. The image is framed by placing the virtual picture frame
 * at the corners of the augmented image trackable.
 */
@SuppressWarnings({"AndroidApiChecker"})

public class AugmentedImageNode extends AnchorNode {

  private static final String TAG = "AugmentedImageNode";

  // The augmented image represented by this node.
  private AugmentedImage augmentedImage;

  // We use completable futures here to simplify
  // the error handling and asynchronous loading.  The loading is started with the
  // first construction of an instance, and then used when the image is set.

  // Each obj file needs its own CompletableFuture, regardless of whether or not
  // the obj files are being used on the same augmented image or not (ex: 4 obj files for each of the
  // corners of a frame and each needs its own CompletableFuture)

  private static CompletableFuture<ModelRenderable> car;
  private static CompletableFuture<ModelRenderable> sushi;
  private static CompletableFuture<ModelRenderable> fancyballroom;
  private static CompletableFuture<ModelRenderable> beachflag;
  private static CompletableFuture<ModelRenderable> sunsetmonorail;
  private static CompletableFuture<ModelRenderable> bigger_elephant;
  private static CompletableFuture<ModelRenderable> lavaeye;
  private static CompletableFuture<ModelRenderable> firebreathingchicken;
  private static CompletableFuture<ModelRenderable> beachcroc;
  private static CompletableFuture<ModelRenderable> skater;
  private static CompletableFuture<ModelRenderable> ufosighting;
  private static CompletableFuture<ModelRenderable> waterfall;

  public ArrayList<CompletableFuture<ModelRenderable>> renderableList = new ArrayList<CompletableFuture<ModelRenderable>>(Arrays.asList(
          beachcroc,
          beachflag,
          bigger_elephant,
          fancyballroom,
          firebreathingchicken,
          lavaeye,
          skater,
          sunsetmonorail,
          sushi,
          ufosighting,
          waterfall));

  public static Vector3[] nodePosition = {
          new Vector3 (0.0f, 0.0f, 0.0f), //beachcroc
          new Vector3 (0.0f, 0.0f, 0.0f), //beachflag
          new Vector3 (0.0f, 0.0f, 0.0f), //bigger_elephant
          new Vector3 (0.0f, 0.0f, 0.0f), //fancyballroom
          new Vector3 (0.0f, 0.0f, 0.0f), //firebreathingchicken
          new Vector3 (0.0f, 0.0f, 0.0f), //lavaeye
          new Vector3 (0.0f, 0.0f, 0.0f), //skater
          new Vector3 (0.0f, 0.0f, 0.0f), //sunsetmonorail
          new Vector3 (0.0f, 0.0f, 0.0f), //sushi
          new Vector3 (0.0f, 0.0f, 0.0f), //ufosighting
          new Vector3 (0.0f, 0.0f, 0.0f)  //waterfall
  };

  public CompletableFuture<ModelRenderable> currentRenderable;

  public AugmentedImageNode(Context context, String[] imageList) {

      if (sushi == null) {
         currentRenderable = renderableList.get(AugmentedImageActivity.currentIndex);
          currentRenderable =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + imageList[AugmentedImageActivity.currentIndex] + ".sfb"))
                          .build();
          Log.d("modelrenderable", ("conditional is running! filepath: " + Uri.parse("models/" + imageList[AugmentedImageActivity.currentIndex] + ".sfb")));


      }
    }

  /**
   * Called when the AugmentedImage is detected and should be rendered. A Sceneform node tree is
   * created based on an Anchor created from the image. The corners are then positioned based on the
   * extents of the image. There is no need to worry about world coordinates since everything is
   * relative to the center of the image, which is the parent node of the corners.
   */

  @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
  public void setImage(AugmentedImage image) {
    this.augmentedImage = image;

      if (!currentRenderable.isDone()) {
          CompletableFuture.allOf(currentRenderable)
                  .thenAccept((Void aVoid) -> setImage(image))
                  .exceptionally(
                          throwable -> {
                              Log.e(TAG, "Exception loading", throwable);
                              return null;
                          });
      }

    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    Node fullnode;

    fullnode = new Node();
    fullnode.setParent(this);
    fullnode.setWorldPosition(new Vector3 ((this.getWorldPosition().x + (nodePosition[AugmentedImageActivity.currentIndex].x)), (this.getWorldPosition().y + (nodePosition[AugmentedImageActivity.currentIndex].y)), (this.getWorldPosition().z + (nodePosition[AugmentedImageActivity.currentIndex].z))));
    fullnode.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
    fullnode.setRenderable(currentRenderable.getNow(null));

  }
}
