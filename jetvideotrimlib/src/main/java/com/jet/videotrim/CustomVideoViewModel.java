package com.jet.videotrim;

import com.devbrackets.android.exomedia.ui.widget.VideoView;

/**
 * Created by Manish Singh on 12/11/2019.
 *
 * @Jetsynthesys manish.singh@jetsynthesys.com
 */
public class CustomVideoViewModel  {

    private VideoView videoView;
    private CustomVideoControls customVideoControls;

    public VideoView getVideoView() {
        return videoView;
    }

    public void setVideoView(VideoView videoView) {
        this.videoView = videoView;
    }

    public CustomVideoControls getCustomVideoControls() {
        return customVideoControls;
    }

    public void setCustomVideoControls(CustomVideoControls customVideoControls) {
        this.customVideoControls = customVideoControls;
    }
}

