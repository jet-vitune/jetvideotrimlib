package com.jet.videotrim.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

import com.jet.videotrim.SingleCallback;
import com.jet.videotrim.view.UnitConverter;

/**
 * Created by Manish Singh on 12/5/2019.
 *
 * @Jetsynthesys manish.singh@jetsynthesys.com
 */
public class MyVideoTrimUtil {

    private static final String TAG = MyVideoTrimUtil.class.getSimpleName();
    public static final long MIN_SHOOT_DURATION = 15000L;//Video Minmum duration frame
    public static final int VIDEO_MAX_TIME = 10;
    public static final long MAX_SHOOT_DURATION = VIDEO_MAX_TIME * 3000L;//Video max duration

    public static final int MAX_COUNT_RANGE = 10;  //seekBar
    private static final int SCREEN_WIDTH_FULL = DeviceUtil.getDeviceWidth();
    public static final int RECYCLER_VIEW_PADDING = UnitConverter.dpToPx(35);
    public static final int VIDEO_FRAMES_WIDTH = SCREEN_WIDTH_FULL - RECYCLER_VIEW_PADDING * 2;
    private static final int THUMB_WIDTH = (SCREEN_WIDTH_FULL - RECYCLER_VIEW_PADDING * 2) / VIDEO_MAX_TIME;
    private static final int THUMB_HEIGHT = UnitConverter.dpToPx(50);

    public static void shootVideoThumbInBackground(final Context context, final Uri videoUri, final int totalThumbsCount, final long startPosition,
                                                   final long endPosition, final SingleCallback<Bitmap, Integer> callback) {
        BackgroundExecutor.execute(new BackgroundExecutor.Task("", 0L, "") {
            @Override public void execute() {
                try {
                    MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                    mediaMetadataRetriever.setDataSource(context, videoUri);
                    // Retrieve media data use microsecond
                    long interval = (endPosition - startPosition) / (totalThumbsCount - 1);
                    for (long i = 0; i < totalThumbsCount; ++i) {
                        long frameTime = startPosition + interval * i;
                        Bitmap bitmap = mediaMetadataRetriever.getFrameAtTime(frameTime * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                        if(bitmap == null) continue;
                        try {
                            bitmap = Bitmap.createScaledBitmap(bitmap, THUMB_WIDTH, THUMB_HEIGHT, false);
                        } catch (final Throwable t) {
                            t.printStackTrace();
                        }
                        callback.onSingleCallback(bitmap, (int) interval);
                    }
                    mediaMetadataRetriever.release();
                } catch (final Throwable e) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        });
    }
}
