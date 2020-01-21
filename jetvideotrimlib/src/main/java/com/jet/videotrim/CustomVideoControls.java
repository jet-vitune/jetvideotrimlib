package com.jet.videotrim;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.devbrackets.android.exomedia.ui.animation.BottomViewHideShowAnimation;
import com.devbrackets.android.exomedia.ui.animation.TopViewHideShowAnimation;
import com.devbrackets.android.exomedia.ui.widget.VideoControls;
import com.devbrackets.android.exomedia.util.TimeFormatUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Manish Singh on 12/11/2019.
 *
 * @Jetsynthesys manish.singh@jetsynthesys.com
 */
public class CustomVideoControls extends VideoControls {


    protected SeekBar seekBar;
    protected ImageView playPausebtn;
    protected LinearLayout extraViewsContainer;
    protected ImageView btnControlPrevious,btnControlNext;
    //exomedia_controls_previous_btn exomedia_controls_next_btn
    protected boolean userInteracting = false;
    //protected ViewGroup layout_exomedia_controls;





    public CustomVideoControls(Context context) {
        super(context);
    }

    public CustomVideoControls(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomVideoControls(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CustomVideoControls(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected int getLayoutResource() {
        return R.layout.exomedia_custom_video_control1;
    }

    @Override
    public void setPosition(@IntRange(from = 0) long position) {
        currentTimeTextView.setText(TimeFormatUtil.formatMs(position));
        seekBar.setProgress((int) position);
    }

    @Override
    public void setDuration(@IntRange(from = 0) long duration) {
        if (duration != seekBar.getMax()) {
            endTimeTextView.setText(TimeFormatUtil.formatMs(duration));
            seekBar.setMax((int) duration);
        }
    }

    @Override
    public void updateProgress(@IntRange(from = 0) long position, @IntRange(from = 0) long duration, @IntRange(from = 0, to = 100) int bufferPercent) {
        if (!userInteracting) {
            seekBar.setSecondaryProgress((int) (seekBar.getMax() * ((float)bufferPercent / 100)));
            seekBar.setProgress((int) position);
            currentTimeTextView.setText(TimeFormatUtil.formatMs(position));
        }
    }

    @Override
    protected void retrieveViews() {
        super.retrieveViews();
        seekBar = findViewById(R.id.exomedia_controls_video_seek);
        playPausebtn = findViewById(R.id.exomedia_controls_play_pause_btn);
        extraViewsContainer = findViewById(R.id.exomedia_controls_extra_container);
        ////exomedia_controls_previous_btn exomedia_controls_next_btn
        btnControlPrevious=findViewById(R.id.exomedia_controls_previous_btn);
        btnControlNext=findViewById(R.id.exomedia_controls_next_btn);
        //layout_exomedia_controls=findViewById(R.id.layout_exomedia_controls);
    }

    @Override
    protected void registerListeners() {
        super.registerListeners();
        seekBar.setOnSeekBarChangeListener(new SeekBarChanged());


       /* btnControlPrevious.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mVideoListEventInterface!=null){
                 mVideoListEventInterface.clickedPrevious(0);
                }
            }
        });

        btnControlNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mVideoListEventInterface!=null){
                    mVideoListEventInterface.clickedNext(0);
                }
            }
        });*/

    }

    @Override
    public void addExtraView(@NonNull View view) {
        extraViewsContainer.addView(view);
    }

    @Override
    public void removeExtraView(@NonNull View view) {
        extraViewsContainer.removeView(view);
    }

    @NonNull
    @Override
    public List<View> getExtraViews() {
        int childCount = extraViewsContainer.getChildCount();
        if (childCount <= 0) {
            return super.getExtraViews();
        }

        //Retrieves the layouts children
        List<View> children = new LinkedList<>();
        for (int i = 0; i < childCount; i++) {
            children.add(extraViewsContainer.getChildAt(i));
        }

        return children;
    }

    @Override
    public void hideDelayed(long delay) {
        hideDelay = delay;

        if (delay < 0 || !canViewHide || isLoading) {
            return;
        }

        //If the user is interacting with controls we don't want to start the delayed hide yet
        if (!userInteracting) {
            visibilityHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    animateVisibility(false);
                }
            }, delay);
        }
    }

    @Override
    protected void animateVisibility(boolean toVisible) {
        if (isVisible == toVisible) {
            return;
        }

        if (!hideEmptyTextContainer || !isTextContainerEmpty()) {
            textContainer.startAnimation(new TopViewHideShowAnimation(textContainer, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        }

        if (!isLoading) {
            controlsContainer.startAnimation(new BottomViewHideShowAnimation(controlsContainer, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));
            //layout_exomedia_controls.startAnimation(new BottomViewHideShowAnimation(controlsContainer, toVisible, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        }

        isVisible = toVisible;
        onVisibilityChanged();
    }

    @Override
    protected void updateTextContainerVisibility() {
        if (!isVisible) {
            return;
        }

        boolean emptyText = isTextContainerEmpty();
        if (hideEmptyTextContainer && emptyText && textContainer.getVisibility() == VISIBLE) {
            textContainer.clearAnimation();
            textContainer.startAnimation(new TopViewHideShowAnimation(textContainer, false, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        } else if ((!hideEmptyTextContainer || !emptyText) && textContainer.getVisibility() != VISIBLE) {
            textContainer.clearAnimation();
            textContainer.startAnimation(new TopViewHideShowAnimation(textContainer, true, CONTROL_VISIBILITY_ANIMATION_LENGTH));
        }
    }

    @Override
    public void showLoading(boolean initialLoad) {
        if (isLoading) {
            return;
        }

        isLoading = true;
        playPauseButton.setVisibility(View.VISIBLE);
        previousButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        if (initialLoad) {
            controlsContainer.setVisibility(View.GONE);
            //layout_exomedia_controls.setVisibility(View.GONE);
        } else {
            //playPauseButton.setEnabled(false);
            //previousButton.setEnabled(false);
            //nextButton.setEnabled(false);
        }

        show();
    }

    @Override
    public void finishLoading() {
        if (!isLoading) {
            return;
        }

        isLoading = false;
        playPauseButton.setVisibility(View.VISIBLE);
        previousButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);


        controlsContainer.setVisibility(View.VISIBLE);
        //layout_exomedia_controls.setVisibility(View.VISIBLE);

        playPauseButton.setEnabled(true);
        previousButton.setEnabled(enabledViews.get(R.id.exomedia_controls_previous_btn, true));
        nextButton.setEnabled(enabledViews.get(R.id.exomedia_controls_next_btn, true));

        updatePlaybackState(videoView != null && videoView.isPlaying());
    }

    @Override
    public void hide() {
        super.hide();
        playPauseButton.setVisibility(GONE);
        previousButton.setVisibility(GONE);
        nextButton.setVisibility(GONE);

    }


    @Override
    public void show() {
        super.show();
        playPauseButton.setVisibility(VISIBLE);
        previousButton.setVisibility(VISIBLE);
        nextButton.setVisibility(VISIBLE);
    }

    /**
     * Listens to the seek bar change events and correctly handles the changes
     */
    protected class SeekBarChanged implements SeekBar.OnSeekBarChangeListener {
        private long seekToTime;

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            seekToTime = progress;
            if (currentTimeTextView != null) {
                currentTimeTextView.setText(TimeFormatUtil.formatMs(seekToTime));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            userInteracting = true;
            if (seekListener == null || !seekListener.onSeekStarted()) {
                internalListener.onSeekStarted();

                playPauseButton.setVisibility(VISIBLE);
                previousButton.setVisibility(VISIBLE);
                nextButton.setVisibility(VISIBLE);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            userInteracting = false;
            if (seekListener == null || !seekListener.onSeekEnded(seekToTime)) {
                internalListener.onSeekEnded(seekToTime);
                playPauseButton.setVisibility(GONE);
                previousButton.setVisibility(GONE);
                nextButton.setVisibility(GONE);
            }
        }
    }


}

