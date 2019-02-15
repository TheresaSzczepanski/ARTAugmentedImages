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
import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;

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

  // TODO For augmented images with more than one obj file used, follow the
  // TODO for an example using the hypothetical scenario in which the sushi scene is
  // TODO created using 2 obj files, sushi, and sushi2



  private static CompletableFuture<ModelRenderable> car;
  private static CompletableFuture<ModelRenderable> sushi;
  // TODO: private static CompletableFuture<ModelRenderable> sushi2;
  private static CompletableFuture<ModelRenderable> fancyballroom;
  private static CompletableFuture<ModelRenderable> beachflag;
  private static CompletableFuture<ModelRenderable> sunsetmonorail;
  private static CompletableFuture<ModelRenderable> elephant;
  private static CompletableFuture<ModelRenderable> lavaeye;
  private static CompletableFuture<ModelRenderable> firebreathingchicken;
  private static CompletableFuture<ModelRenderable> beachcroc;
  private static CompletableFuture<ModelRenderable> skater;
  private static CompletableFuture<ModelRenderable> ufosighting;
  private static CompletableFuture<ModelRenderable> waterfall;

  public static CompletableFuture[] renderableList = {
          //beachcroc,
          beachflag,
          //elephant,
          fancyballroom,
          //firebreathingchicken,
          //lavaeye,
          //skater,
          //sunsetmonorail,
          sushi,
          //ufosighting,
          //waterfall
  };

  public AugmentedImageNode(Context context, String[] imageList, Integer currentIndex) {


      if (renderableList[currentIndex] == null) {
          renderableList[currentIndex] =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + imageList[currentIndex] + ".sfb"))
                          .build();
          /* sushi =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "sushi" + ".sfb"))
                          .build();
          // TODO sushi2 =
          // TODO   ModelRenderable.builder()
          // TODO       .setSource(context, Uri.parse("models/" + imageList[currentIndex] + ".sfb"))
          // TODO       .build();
          fancyballroom =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "fancyballroom" + ".sfb"))
                          .build();
          beachflag =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "beachflag" + ".sfb"))
                          .build();
          beachcroc =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "beachcroc" + ".sfb"))
                          .build();
          elephant =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "elephant" + ".sfb"))
                          .build();
          firebreathingchicken =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "firebreathingchicken" + ".sfb"))
                          .build();
          lavaeye =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "lavaeye" + ".sfb"))
                          .build();
          skater =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "skater" + ".sfb"))
                          .build();
          sunsetmonorail =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "sunsetmonorail" + ".sfb"))
                          .build();
          ufosighting =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "ufosighting" + ".sfb"))
                          .build();
          waterfall =
                  ModelRenderable.builder()
                          .setSource(context, Uri.parse("models/" + "waterfall" + ".sfb"))
                          .build(); */
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

    // If any of the models are not loaded, then recurse when all are loaded.
    if (!car.isDone()) {
        CompletableFuture.allOf(car)

        // TODO Replace the above line with CompletableFuture.allOf(sushi, sushi2);

          .thenAccept((Void aVoid) -> setImage(image))
          .exceptionally(
              throwable -> {
                Log.e(TAG, "Exception loading", throwable);
                return null;
              });
    }

      if (!sushi.isDone()) {
          CompletableFuture.allOf(sushi)
                  .thenAccept((Void aVoid) -> setImage(image))
                  .exceptionally(
                          throwable -> {
                              Log.e(TAG, "Exception loading", throwable);
                              return null;
                          });
      }
      if (!fancyballroom.isDone()) {
          CompletableFuture.allOf(fancyballroom)
                  .thenAccept((Void aVoid) -> setImage(image))
                  .exceptionally(
                          throwable -> {
                              Log.e(TAG, "Exception loading", throwable);
                              return null;
                          });
      }
      if (!beachflag.isDone()) {
          CompletableFuture.allOf(beachflag)
                  .thenAccept((Void aVoid) -> setImage(image))
                  .exceptionally(
                          throwable -> {
                              Log.e(TAG, "Exception loading", throwable);
                              return null;
                          });
      }


    // Set the anchor based on the center of the image.
    setAnchor(image.createAnchor(image.getCenterPose()));

    Node fullsushi;

    fullsushi = new Node();
    fullsushi.setParent(this);
    fullsushi.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullsushi.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
    fullsushi.setRenderable(sushi.getNow(null));

    // TODO Keep the above code and add

    // TODO fullsushi = new Node();
    // TODO fullsushi.setParent(this);
    // TODO fullsushi.setLocalPosition(new Vector3(x, y, z));
    // TODO fullsushi.setLocalScale(new Vector3(x, y, z));
    // TODO fullsushi.setRenderable(sushi2.getNow(null));

    Node fullfancyballroom;

    fullfancyballroom = new Node();
    fullfancyballroom.setParent(this);
    fullfancyballroom.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullfancyballroom.setLocalScale(new Vector3(0.5f, 0.5f, 0.5f));
    fullfancyballroom.setRenderable(fancyballroom.getNow(null));


    Node fullcar;

    fullcar = new Node();
    fullcar.setParent(this);
    fullcar.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullcar.setLocalScale(new Vector3(0.1f, 0.1f, 0.1f));
    fullcar.setRenderable(car.getNow(null));

    Node fullbeachflag;

    fullbeachflag = new Node();
    fullbeachflag.setParent(this);
    fullbeachflag.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullbeachflag.setLocalScale(new Vector3(1f, 1f, 1f));
    fullbeachflag.setRenderable(beachflag.getNow(null));

    Node fullelephant;

    fullelephant = new Node();
    fullelephant.setParent(this);
    fullelephant.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullelephant.setLocalScale(new Vector3(1f, 1f, 1f));
    fullelephant.setRenderable(elephant.getNow(null));

    Node fullfirebreathingchicken;

    fullfirebreathingchicken = new Node();
    fullfirebreathingchicken.setParent(this);
    fullfirebreathingchicken.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullfirebreathingchicken.setLocalScale(new Vector3(1f, 1f, 1f));
    fullfirebreathingchicken.setRenderable(firebreathingchicken.getNow(null));

    Node fulllavaeye;

    fulllavaeye = new Node();
    fulllavaeye.setParent(this);
    fulllavaeye.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fulllavaeye.setLocalScale(new Vector3(1f, 1f, 1f));
    fulllavaeye.setRenderable(lavaeye.getNow(null));

    Node fullskater;

    fullskater = new Node();
    fullskater.setParent(this);
    fullskater.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullskater.setLocalScale(new Vector3(1f, 1f, 1f));
    fullskater.setRenderable(skater.getNow(null));

    Node fullsunetmonorail;

    fullsunetmonorail = new Node();
    fullsunetmonorail.setParent(this);
    fullsunetmonorail.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullsunetmonorail.setLocalScale(new Vector3(1f, 1f, 1f));
    fullsunetmonorail.setRenderable(sunsetmonorail.getNow(null));

    Node fullufosighting;

    fullufosighting = new Node();
    fullufosighting.setParent(this);
    fullufosighting.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullufosighting.setLocalScale(new Vector3(1f, 1f, 1f));
    fullufosighting.setRenderable(ufosighting.getNow(null));

    Node fullwaterfall;

    fullwaterfall = new Node();
    fullwaterfall.setParent(this);
    fullwaterfall.setWorldPosition(new Vector3 (this.getWorldPosition().x, this.getWorldPosition().y, this.getWorldPosition().z));
    fullwaterfall.setLocalScale(new Vector3(1f, 1f, 1f));
    fullwaterfall.setRenderable(waterfall.getNow(null));

  }
}
