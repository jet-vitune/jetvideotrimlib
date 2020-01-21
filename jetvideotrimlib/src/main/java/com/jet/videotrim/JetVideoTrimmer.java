/*
 * MIT License
 *
 * Copyright (c) 2016 Knowledge, education for life.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.jet.videotrim;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.devbrackets.android.exomedia.listener.OnCompletionListener;
import com.devbrackets.android.exomedia.listener.OnErrorListener;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.jet.videotrim.interfaces.JetVideoListener;
import com.jet.videotrim.interfaces.OnTrimVideoListener;
import com.jet.videotrim.utils.BackgroundExecutor;
import com.jet.videotrim.utils.MyVideoTrimUtil;
import com.jet.videotrim.utils.SpacesItemDecoration;
import com.jet.videotrim.utils.TrimVideoUtil2;
import com.jet.videotrim.utils.UiThreadExecutor;
import com.jet.videotrim.view.RangeSeekBarView2;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static com.jet.videotrim.utils.MyVideoTrimUtil.VIDEO_FRAMES_WIDTH;


public class JetVideoTrimmer extends FrameLayout {

    private static final String TAG = JetVideoTrimmer.class.getSimpleName();
    private static final int MIN_TIME_FRAME = 1000;
    private RelativeLayout mLinearVideo;
    private VideoView mVideoView;
    private RecyclerView mVideoThumbRecyclerView;
    private Uri mSrc;
    private String mFinalPath;
    private OnTrimVideoListener mOnTrimVideoListener;
    private JetVideoListener mJetVideoListener;
    private ArrayList<String> mVideoMimeTypeList;

    private long mDuration = 0;
    private Context mContext;
    private VideoTrimmerAdapter mVideoThumbAdapter;
    private boolean isSeeking;
    private int lastScrollX;
    private int mScaledTouchSlop;
    private boolean isOverScaledTouchSlop;
    private long scrollPos;
    private float mAverageMsPx;
    private long mLeftProgressPos;
    private long mRightProgressPos;
    private long mRedProgressBarPos;
    private ImageView mRedProgressIcon;
    private RangeSeekBarView2 mRangeSeekBarView;
    private int mThumbsTotalCount;
    private LinearLayout mSeekBarLayout;
    private View mVideoShootTipTv;
    private boolean isFromRestore;
    private int mMaxWidth = VIDEO_FRAMES_WIDTH;
    private float averagePxMs;
    private Handler mAnimationHandle = new Handler();
    private ValueAnimator mRedProgressAnimator;
    private Handler mAnimationHandler = new Handler();
    private CustomVideoControls customVideoControls;
    private CustomVideoViewModel customVideoViewModel;
    private Handler handler = new Handler();
    private boolean forStory;

    public JetVideoTrimmer(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public JetVideoTrimmer(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;

        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true);
        mLinearVideo = ((RelativeLayout) findViewById(R.id.layout_surface_view));
        mVideoView = ((VideoView) findViewById(R.id.video_loader));
        mSeekBarLayout = findViewById(R.id.seekBarLayout);
        mVideoShootTipTv = findViewById(R.id.video_shoot_tip);
        mRedProgressIcon = findViewById(R.id.positionIcon);
        mVideoMimeTypeList = new ArrayList<>();
        initRecyclerView();
        addVideoMimeType();
        setUpListeners();
    }

    private void initRangeSeekBarView() {
        if (mRangeSeekBarView != null) return;
        int rangeWidth;
        mLeftProgressPos = 0;
        if (mDuration <= MyVideoTrimUtil.MAX_SHOOT_DURATION) {
            mThumbsTotalCount = MyVideoTrimUtil.MAX_COUNT_RANGE;
            rangeWidth = mMaxWidth;
            mRightProgressPos = mDuration;
        } else {
            mThumbsTotalCount = (int) (mDuration * 1.0f / (MyVideoTrimUtil.MAX_SHOOT_DURATION * 1.0f) * MyVideoTrimUtil.MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth / MyVideoTrimUtil.MAX_COUNT_RANGE * mThumbsTotalCount;
            mRightProgressPos = MyVideoTrimUtil.MAX_SHOOT_DURATION;
        }
        mVideoThumbRecyclerView.addItemDecoration(new SpacesItemDecoration(MyVideoTrimUtil.RECYCLER_VIEW_PADDING, mThumbsTotalCount));
        mRangeSeekBarView = new RangeSeekBarView2(mContext, mLeftProgressPos, mRightProgressPos);
        mRangeSeekBarView.setSelectedMinValue(mLeftProgressPos);
        mRangeSeekBarView.setSelectedMaxValue(mRightProgressPos);
        mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
        mRangeSeekBarView.setMinShootTime(MyVideoTrimUtil.MIN_SHOOT_DURATION);
        mRangeSeekBarView.setNotifyWhileDragging(true);
        mRangeSeekBarView.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        mSeekBarLayout.addView(mRangeSeekBarView);

        mAverageMsPx = mDuration * 1.0f / rangeWidth * 1.0f;
        averagePxMs = (mMaxWidth * 1.0f / (mRightProgressPos - mLeftProgressPos));
    }

    private final RangeSeekBarView2.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBarView2.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBarView2 bar, long minValue, long maxValue, int action, boolean isMin,
                                                RangeSeekBarView2.Thumb pressedThumb) {
            Log.d(TAG, "-----minValue----->>>>>>" + minValue);
            Log.d(TAG, "-----maxValue----->>>>>>" + maxValue);
            mLeftProgressPos = minValue + scrollPos;
            mRedProgressBarPos = mLeftProgressPos;
            mRightProgressPos = maxValue + scrollPos;
            Log.d(TAG, "-----mLeftProgressPos----->>>>>>" + mLeftProgressPos);
            Log.d(TAG, "-----mRightProgressPos----->>>>>>" + mRightProgressPos);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isSeeking = false;
                    break;
                case MotionEvent.ACTION_MOVE:
                    isSeeking = true;
//                    seekTo((int) (pressedThumb == RangeSeekBarView2.Thumb.MIN ? mLeftProgressPos : mRightProgressPos));//when drag happening
                    break;
                case MotionEvent.ACTION_UP:
                    isSeeking = false;
                    seekTo((int) mLeftProgressPos);
                    break;
                default:
                    break;
            }

            mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
        }
    };

    private void initRecyclerView() {
        mVideoThumbRecyclerView = findViewById(R.id.video_frames_recyclerView);
        mVideoThumbRecyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false));
        mVideoThumbAdapter = new VideoTrimmerAdapter(mContext);
        mVideoThumbRecyclerView.setAdapter(mVideoThumbAdapter);
        mVideoThumbRecyclerView.addOnScrollListener(mOnScrollListener);
    }

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.d(TAG, "newState = " + newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = calcScrollXDistance();
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }
            isOverScaledTouchSlop = true;
            if (scrollX == -MyVideoTrimUtil.RECYCLER_VIEW_PADDING) {
                scrollPos = 0;
            } else {
                isSeeking = true;
                scrollPos = (long) (mAverageMsPx * (MyVideoTrimUtil.RECYCLER_VIEW_PADDING + scrollX));
                mLeftProgressPos = mRangeSeekBarView.getSelectedMinValue() + scrollPos;
                mRightProgressPos = mRangeSeekBarView.getSelectedMaxValue() + scrollPos;
                Log.d(TAG, "onScrolled >>>> mLeftProgressPos = " + mLeftProgressPos);
                mRedProgressBarPos = mLeftProgressPos;
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                    setPlayPauseViewIcon(false);//here pause video
                }
                mRedProgressIcon.setVisibility(GONE);
                seekTo(mLeftProgressPos);
                mRangeSeekBarView.setStartEndTime(mLeftProgressPos, mRightProgressPos);
                mRangeSeekBarView.invalidate();
            }
            lastScrollX = scrollX;
        }
    };

    private int calcScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mVideoThumbRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private void addVideoMimeType() {
        //.MPG, .MP4, .AVI, .FLV, .WMV.
        //video/mp4
        mVideoMimeTypeList.add("video/mp4");
        mVideoMimeTypeList.add("video/mpg");
        mVideoMimeTypeList.add("video/avi");
        mVideoMimeTypeList.add("video/flv");
        mVideoMimeTypeList.add("video/wmv");
    }

    private void setUpListeners() {
        findViewById(R.id.cancelBtn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelClicked();
            }
        });

        findViewById(R.id.finishBtn).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveClicked();
            }
        });
        mVideoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion() {
                videoCompleted();
            }
        });
//        mPlayView.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                playVideoOrPause();
//            }
//        });
        mVideoView.setOnErrorListener(new OnErrorListener() {
            @Override
            public boolean onError(Exception e) {
                if (mOnTrimVideoListener != null)
                    mOnTrimVideoListener.onError("Something went wrong reason : " + e);
                return false;
            }
        });

        mVideoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                onVideoPrepared();
            }
        });
    }

    private void videoCompleted() {
        seekTo(mLeftProgressPos);
        setPlayPauseViewIcon(false);
    }

    private void playVideoOrPause() {
        if (mVideoView != null) {
            mRedProgressBarPos = mVideoView.getCurrentPosition();
            if (mVideoView.isPlaying()) {
                mVideoView.pause();
                pauseRedProgressAnimation();
            } else {
                mVideoView.start();
                playingRedProgressAnimation();
            }
            setPlayPauseViewIcon(mVideoView.isPlaying());
        }
    }

    public void pauseVideo() {
        if (mVideoView != null && mVideoView.isPlaying()) {
            mRedProgressBarPos = mVideoView.getCurrentPosition();
            mVideoView.pause();
            pauseRedProgressAnimation();
            setPlayPauseViewIcon(false);
        }
    }

    private void playingRedProgressAnimation() {
        pauseRedProgressAnimation();
        playingAnimation();
        if (mAnimationRunnable != null && mAnimationHandle != null) {
            mAnimationHandler.post(mAnimationRunnable);
        }
    }

    private void playingAnimation() {
        try {
            if (mRedProgressIcon.getVisibility() == View.GONE) {
                mRedProgressIcon.setVisibility(View.VISIBLE);
            }
            final LayoutParams params = (LayoutParams) mRedProgressIcon.getLayoutParams();
            int start = (int) (MyVideoTrimUtil.RECYCLER_VIEW_PADDING + (mRedProgressBarPos - scrollPos) * averagePxMs);
            int end = (int) (MyVideoTrimUtil.RECYCLER_VIEW_PADDING + (mRightProgressPos - scrollPos) * averagePxMs);
            mRedProgressAnimator = ValueAnimator.ofInt(start, end).setDuration((mRightProgressPos - scrollPos) - (mRedProgressBarPos - scrollPos));
            mRedProgressAnimator.setInterpolator(new LinearInterpolator());
            mRedProgressAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    params.leftMargin = (int) animation.getAnimatedValue();
                    mRedProgressIcon.setLayoutParams(params);
                    Log.d(TAG, "----onAnimationUpdate--->>>>>>>" + mRedProgressBarPos);
                }
            });
            mRedProgressAnimator.start();
        } catch (Exception e) {

        }

    }

    private void pauseRedProgressAnimation() {
        try {
            if (mRedProgressIcon != null)
                mRedProgressIcon.clearAnimation();
            if (mRedProgressAnimator != null) {
                if (mAnimationHandler != null)
                    mAnimationHandler.removeCallbacks(mAnimationRunnable);
                mRedProgressAnimator.cancel();
            }
        } catch (Exception e) {
        }

    }

    private Runnable mAnimationRunnable = new Runnable() {

        @Override
        public void run() {
            updateVideoProgress();
        }
    };

    private void onSaveClicked() {
        try {
            long videoLengthDifference = mRightProgressPos - mLeftProgressPos;
            if (mSrc == null) {
                mOnTrimVideoListener.onError("Unsupported video format.");
            }
            if (videoLengthDifference < 2000) {
                mOnTrimVideoListener.onError("The minimum length of the video should be 2 seconds!");
            } else if (videoLengthDifference > 35000) {
                mOnTrimVideoListener.onError("The maximum length of the video can be 30 seconds!");
            } else {
//            mPlayView.setVisibility(View.VISIBLE);
                playVideoOrPause();
                pauseProgressHandlerAndRemoveCallBack();
                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(getContext(), mSrc);
                long METADATA_KEY_DURATION = Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                String mimeType = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                Log.e("Saved Trim", mimeType);
                if (METADATA_KEY_DURATION < MIN_TIME_FRAME) {
                    if (mOnTrimVideoListener != null)
                        mOnTrimVideoListener.onError("Video duration too short");
                    return;
                }

                if (!mVideoMimeTypeList.contains(mimeType)) {
                    if (mOnTrimVideoListener != null)
                        mOnTrimVideoListener.onError("Unsupported video format. Accepted formats are .MPG, .MP4, .AVI, .FLV, .WMV.");
                    return;
                }

                if (!mimeType.equalsIgnoreCase("video/mp4")) {
                    if (mOnTrimVideoListener != null && forStory) {
                        mOnTrimVideoListener.getResult(mSrc, mLeftProgressPos, mRightProgressPos, true);
                        return;
                    } else if (mOnTrimVideoListener != null) {
                        mOnTrimVideoListener.getResult(mSrc, mLeftProgressPos, mRightProgressPos);
                        return;
                    }
                }

                final File file = new File(mSrc.getPath());


//            if (mTimeVideo < MIN_TIME_FRAME) {
//
//                if ((METADATA_KEY_DURATION - mRightProgressPos) > (MIN_TIME_FRAME - mTimeVideo)) {
//                    mEndPosition += (MIN_TIME_FRAME - mTimeVideo);
//                } else if (mStartPosition > (MIN_TIME_FRAME - mTimeVideo)) {
//                    mStartPosition -= (MIN_TIME_FRAME - mTimeVideo);
//                }
//            }

                //notify that video trimming started
                if (mOnTrimVideoListener != null)
                    mOnTrimVideoListener.onTrimStarted();

                BackgroundExecutor.execute(
                        new BackgroundExecutor.Task("", 0L, "") {
                            @Override
                            public void execute() {
                                try {

                                    Log.e(TAG, " mLeftProgressPos: " + mLeftProgressPos + " mRightProgressPos: " + mRightProgressPos);
                                    final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                                    String fileName = "";
                                    String deviceId = getDeviceUniqueId(mContext);
                                    if (deviceId != null && !deviceId.isEmpty()) {
                                        fileName = "MP4_" + timeStamp + "_" + deviceId + ".mp4";
                                    } else {
                                        fileName = "MP4_" + timeStamp + ".mp4";
                                    }
                                    final String filePath = getDestinationPath(forStory) + fileName;
                                    File file2 = new File(filePath);
                                    TrimVideoUtil2.startTrim(mContext, file, file2, (int) mLeftProgressPos, (int) mRightProgressPos, mOnTrimVideoListener,forStory);
                                } catch (IndexOutOfBoundsException e) {
                                    mOnTrimVideoListener.onError(mContext.getString(R.string.unsuported_media));
                                } catch (Exception e) {
                                    mOnTrimVideoListener.onError(mContext.getString(R.string.unsuported_media));
                                } catch (final Throwable e) {
                                    mOnTrimVideoListener.onError(mContext.getString(R.string.unsuported_media));
                                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                                }
                            }
                        }
                );
            }

        } catch (Exception e) {
        }
    }

    public void pauseProgressHandlerAndRemoveCallBack() {
        pauseVideo();
    }

    public static String getDeviceUniqueId(Context context) {
        String deviceId = "";

        try {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return deviceId;

        } catch (Exception e) {

            Log.e(TAG, e + "");
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                deviceId = telephonyManager.getDeviceId();
                return deviceId;
            }
        }

        return deviceId;
    }

    private void onCancelClicked() {
        if (mVideoView != null && mVideoView.isPlaying())
            mVideoView.stopPlayback();
        pauseRedProgressAnimation();
        if (mOnTrimVideoListener != null) {
            mOnTrimVideoListener.cancelAction();
        }
    }

    private String getDestinationPath(boolean forStory) {
        try {
            if (mFinalPath == null) {

                File mediaStorageDir = new File("/storage/emulated/0/Android/data/in.publicam.vitunes/data/");
                if (!mediaStorageDir.exists()) {
                    mediaStorageDir.mkdirs();
                }
                String videoStoragePath="";
                if (!forStory) {
                    videoStoragePath = mediaStorageDir + "/uploadedVideos";
                }else{
                    videoStoragePath = mediaStorageDir + "/TuneStory";
                }
                createDirectory(videoStoragePath);
                mFinalPath = videoStoragePath + File.separator;
                Log.d(TAG, "Using default path " + mFinalPath);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return mFinalPath;
    }

    public static void createDirectory(String filePath) {
        try {
            if (!new File(filePath).exists()) {
                new File(filePath).mkdirs();
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
    }

    private void onVideoPrepared() {
        // Adjust the size of the video
        // so it fits on the screen

        customVideoControls = new CustomVideoControls(mContext);
        customVideoControls.setPreviousButtonEnabled(false);
        customVideoControls.setNextButtonEnabled(false);
        customVideoControls.setPlayPauseDrawables(getResources().getDrawable(R.drawable.play), getResources().getDrawable(R.drawable.pause));
        mVideoView.setControls(customVideoControls);

        mDuration = mVideoView.getDuration();
        mVideoView.setOnTouchListener(new ActivitySwipeDetector((Activity) mContext));
        customVideoControls.playPausebtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                playVideoOrPause();
            }
        });
        seekTo(mLeftProgressPos);
        initRangeSeekBarView();
        startShootVideoThumbs(mContext, mSrc, mThumbsTotalCount, 0, mDuration);
        if (mVideoView.getDuration() < (MyVideoTrimUtil.MIN_SHOOT_DURATION + 1000)) {
            ((TextView) mVideoShootTipTv).setText(mContext.getString(R.string.drag_to_select_clips_less_30_seconds_of_posting));
        } else {
            ((TextView) mVideoShootTipTv).setText(mContext.getString(R.string.drag_to_select_clips_within_30_seconds_of_posting));
        }
    }

    private void startShootVideoThumbs(final Context context, final Uri videoUri, int totalThumbsCount, long startPosition, long endPosition) {
        MyVideoTrimUtil.shootVideoThumbInBackground(context, videoUri, totalThumbsCount, startPosition, endPosition,
                new SingleCallback<Bitmap, Integer>() {
                    @Override
                    public void onSingleCallback(final Bitmap bitmap, final Integer interval) {
                        if (bitmap != null) {
                            UiThreadExecutor.runTask("", new Runnable() {
                                @Override
                                public void run() {
                                    mVideoThumbAdapter.addBitmaps(bitmap);
                                }
                            }, 0L);
                        }
                    }
                });
    }

    private void seekTo(long msec) {
        mVideoView.seekTo((int) msec);
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            mRedProgressIcon.setVisibility(GONE);
            setPlayPauseViewIcon(false);
        }
        Log.d(TAG, "seekTo = " + msec);
    }

    private void setPlayPauseViewIcon(boolean isPlaying) {
//        mPlayView.setImageResource(isPlaying ? R.drawable.play : R.drawable.pause);
    }

    private void updateVideoProgress() {
        try {
            long currentPosition = mVideoView.getCurrentPosition();
//        Log.d(TAG, "updateVideoProgress currentPosition = " + currentPosition);
            if (currentPosition >= (mRightProgressPos)) {
                mRedProgressBarPos = mLeftProgressPos;
                pauseRedProgressAnimation();
                onVideoPause();
            } else {
                if (mAnimationHandler != null)
                    mAnimationHandler.post(mAnimationRunnable);
                mRedProgressBarPos = mLeftProgressPos;
            }
        } catch (Exception e) {
        }

    }

    public void onVideoPause() {
        try {
            if (mVideoView != null && mVideoView.isPlaying()) {
                seekTo(mLeftProgressPos);//Reset on video pause
                mVideoView.pause();
                setPlayPauseViewIcon(false);
                mRedProgressIcon.setVisibility(GONE);
            }
        } catch (Exception e) {
        }

    }

    /**
     * Listener for events such as trimming operation success and cancel
     *
     * @param onTrimVideoListener interface for events
     */
    public void setOnTrimVideoListener(OnTrimVideoListener onTrimVideoListener) {
        mOnTrimVideoListener = onTrimVideoListener;
    }

    /**
     * Listener for some {@link VideoView} events
     *
     * @param jetVideoListener interface for events
     */
    @SuppressWarnings("unused")
    public void setOnJetVideoListener(JetVideoListener jetVideoListener) {
        mJetVideoListener = jetVideoListener;
    }

    /**
     * Sets the path where the trimmed video will be saved
     * Ex: /storage/emulated/0/MyAppFolder/
     *
     * @param finalPath the full path
     */
    @SuppressWarnings("unused")
    public void setDestinationPath(final String finalPath) {
        mFinalPath = finalPath;
        Log.d(TAG, "Setting custom path " + mFinalPath);
    }

    /**
     * cancel all current operations
     */
    public void destroy() {
        BackgroundExecutor.cancelAll("", true);
        UiThreadExecutor.cancelAll("");
    }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    public void setVideoURI(final Uri videoURI) {
        mSrc = videoURI;
        mVideoView.setVideoURI(mSrc);
        mVideoView.requestFocus();
    }

    private void pauseAndDurationViewHandlingOnTouchDown() {
        if (mVideoView != null) {
            if (mVideoView.getVideoControlsCore().isVisible()) {
                mVideoView.getVideoControlsCore().hide(true);
            } else {
                mVideoView.getVideoControlsCore().show();
            }
        }
    }

    public void setForStory(boolean forStory) {
        this.forStory = forStory;

    }

    class ActivitySwipeDetector implements OnTouchListener {


        static final String logTag = "ActivitySwipeDetector";
        private Activity activity;
        static final int MIN_DISTANCE = 100;
        private float downX, downY, upX, upY;

        public ActivitySwipeDetector(Activity activity) {
            this.activity = activity;
        }

        public void onRightSwipe() {
            Log.e(logTag, "RightToLeftSwipe!");
        }

        public void onLeftSwipe() {
            Log.e(logTag, "LeftToRightSwipe!");

        }

        public void onDownSwipe() {
            Log.e(logTag, "onTopToBottomSwipe!");
        }

        public void onUpSwipe() {
            Log.e(logTag, "onBottomToTopSwipe!");

        }

        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    downX = event.getX();
                    downY = event.getY();

                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    upX = event.getX();
                    upY = event.getY();

                    float deltaX = downX - upX;
                    float deltaY = downY - upY;

                    // swipe horizontal?
                    if (Math.abs(deltaX) > Math.abs(deltaY)) {
                        if (Math.abs(deltaX) > MIN_DISTANCE) {
                            // left or right
                            if (deltaX > 0) {
                                this.onRightSwipe();
                                return true;
                            }

                            if (deltaX < 0) {
                                this.onLeftSwipe();
                                return true;
                            }
                        } else {
                            Log.e(logTag, "Horizontal Swipe was only " + Math.abs(deltaX) + " long, need at least " + MIN_DISTANCE);
                            return false; // We don't consume the event
                        }
                    }
                    // swipe vertical?
                    else {
                        pauseAndDurationViewHandlingOnTouchDown();
                    }

                    return true;
                }
            }
            return false;
        }

    }

}
