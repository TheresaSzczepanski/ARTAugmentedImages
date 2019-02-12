package com.google.ar.sceneform.samples.augmentedimage;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;

public class AugmentedImageMediaPlayer extends MediaPlayer {

    // Could be an issue here with initializing it as this.
    // Now, I am trying to remove the declaration as a MediaPlayer ( = new MediaPlayer();)
    public static MediaPlayer mediaPlayerInstance;

    public static MediaPlayer getMediaInstance(){
        return mediaPlayerInstance;
    }

    public static void createVideo(Context context, int augmentedImageIndex){
        mediaPlayerInstance = MediaPlayer.create(context, AugmentedImageFragment.Video_list[augmentedImageIndex]);
        // This works to stop it, which is very strange.
        //mediaPlayerInstance.stop();
    }

    public static void editVideoSettings(Surface surface, boolean isLooping){
        mediaPlayerInstance.setSurface(surface);
        mediaPlayerInstance.setLooping(isLooping);
    }

    public static void resetVideo(){
        if(getMediaInstance().isPlaying()) {
            mediaPlayerInstance.stop();
        }
        mediaPlayerInstance.release();
        mediaPlayerInstance = null;
        Log.d("imagemediaplayer", "video reset");
    }

}
