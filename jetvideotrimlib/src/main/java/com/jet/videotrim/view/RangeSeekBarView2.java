package com.jet.videotrim.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.jet.videotrim.R;
import com.jet.videotrim.utils.DateUtil;
import com.jet.videotrim.utils.MyVideoTrimUtil;

import java.text.DecimalFormat;

/**
 * Created by Manish Singh on 12/9/2019.
 *
 * @Jetsynthesys manish.singh@jetsynthesys.com
 */
public class RangeSeekBarView2 extends View {
    private static final String TAG = RangeSeekBarView2.class.getSimpleName();
    public static final int INVALID_POINTER_ID = 255;
    public static final int ACTION_POINTER_INDEX_MASK = 0x0000ff00, ACTION_POINTER_INDEX_SHIFT = 8;
    private static final int TextPositionY = UnitConverter.dpToPx(7);
    private static final int paddingTop = UnitConverter.dpToPx(10);
    private int mActivePointerId = INVALID_POINTER_ID;

    private long mMinShootTime = MyVideoTrimUtil.MIN_SHOOT_DURATION;
    private double absoluteMinValuePrim, absoluteMaxValuePrim;
    private double normalizedMinValue = 0d;//value varies between0-1
    private double normalizedMaxValue = 1d;//value varies between 0-1
    private double normalizedMinValueTime = 0d;
    private double normalizedMaxValueTime = 1d;// normalized：value varies 0-1
    private int mScaledTouchSlop;
    private Bitmap thumbImageLeft;
    private Bitmap thumbImageRight;
    private Bitmap thumbPressedImage;
    private Paint paint;
    private Paint rectPaint;
    private final Paint mVideoTrimTimePaintL = new Paint();
    private final Paint mVideoTrimTimePaintR = new Paint();
    private final Paint mShadow = new Paint();
    private int thumbWidth;
    private float thumbHalfWidth;
    private final float padding = 0;
    private long mStartPosition = 0;
    private long mEndPosition = 0;
    private float thumbPaddingTop = 0;
    private boolean isTouchDown;
    private float mDownMotionX;
    private boolean mIsDragging;
    private Thumb pressedThumb;
    private boolean isMin;
    private double min_width = 1;
    private boolean notifyWhileDragging = false;
    private OnRangeSeekBarChangeListener mRangeSeekBarChangeListener;
    private int whiteColorRes = getContext().getResources().getColor(R.color.jet_trim_white_color);

    public enum Thumb {
        MIN, MAX
    }

    public RangeSeekBarView2(Context context) {
        super(context);
    }

    public RangeSeekBarView2(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RangeSeekBarView2(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public RangeSeekBarView2(Context context, long absoluteMinValuePrim, long absoluteMaxValuePrim) {
        super(context);
        this.absoluteMinValuePrim = absoluteMinValuePrim;
        this.absoluteMaxValuePrim = absoluteMaxValuePrim;
        setFocusable(true);
        setFocusableInTouchMode(true);
        init(context);
    }

    private void init(Context context) {
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        thumbImageLeft = getBitmapFromDrawable(context, R.drawable.jet_trim_ic_video_thumb_handle_right);
        thumbImageRight = getBitmapFromDrawable(context, R.drawable.jet_trim_ic_video_thumb_handle_left);

        int width = thumbImageLeft.getWidth();
        int height = thumbImageLeft.getHeight();
        int newWidth = UnitConverter.dpToPx(12);
        int newHeight = UnitConverter.dpToPx(55);
        float scaleWidth = newWidth * 1.0f / width;
        float scaleHeight = newHeight * 1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        thumbImageLeft = Bitmap.createBitmap(thumbImageLeft, 0, 0, width, height, matrix, true);
        thumbImageRight = Bitmap.createBitmap(thumbImageRight, 0, 0, width, height, matrix, true);
        thumbPressedImage = thumbImageLeft;
        thumbWidth = newWidth;
        thumbHalfWidth = thumbWidth / 2;
        int shadowColor = getContext().getResources().getColor(R.color.jet_trim_shadow_color);
        mShadow.setAntiAlias(true);
        mShadow.setColor(shadowColor);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rectPaint.setStyle(Paint.Style.FILL);
        rectPaint.setColor(whiteColorRes);

        mVideoTrimTimePaintL.setStrokeWidth(3);
        mVideoTrimTimePaintL.setARGB(255, 51, 51, 51);
        mVideoTrimTimePaintL.setTextSize(20);
        mVideoTrimTimePaintL.setAntiAlias(true);
        mVideoTrimTimePaintL.setColor(whiteColorRes);
        mVideoTrimTimePaintL.setTextAlign(Paint.Align.LEFT);

        mVideoTrimTimePaintR.setStrokeWidth(3);
        mVideoTrimTimePaintR.setARGB(255, 51, 51, 51);
        mVideoTrimTimePaintR.setTextSize(20);
        mVideoTrimTimePaintR.setAntiAlias(true);
        mVideoTrimTimePaintR.setColor(whiteColorRes);
        mVideoTrimTimePaintR.setTextAlign(Paint.Align.RIGHT);
    }

    public static Bitmap getBitmapFromDrawable(Context context, @DrawableRes int drawableId) {
        Drawable drawable = AppCompatResources.getDrawable(context, drawableId);

        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof VectorDrawableCompat) {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (drawable instanceof VectorDrawable) {
                Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            } else {
                throw new IllegalArgumentException("unsupported drawable type");
            }
        } else {
            throw new IllegalArgumentException("unsupported drawable type");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = 300;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(widthMeasureSpec)) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }
        int height = 120;
        if (MeasureSpec.UNSPECIFIED != MeasureSpec.getMode(heightMeasureSpec)) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(width, height);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float bg_middle_left = 0;
        float bg_middle_right = getWidth() - getPaddingRight();
        float rangeL = normalizedToScreen(normalizedMinValue);
        float rangeR = normalizedToScreen(normalizedMaxValue);
        Rect leftRect = new Rect((int) bg_middle_left, getHeight(), (int) rangeL, 0);
        Rect rightRect = new Rect((int) rangeR, getHeight(), (int) bg_middle_right, 0);
        canvas.drawRect(leftRect, mShadow);
        canvas.drawRect(rightRect, mShadow);

        canvas.drawRect(rangeL, thumbPaddingTop + paddingTop, rangeR, thumbPaddingTop + UnitConverter.dpToPx(2) + paddingTop, rectPaint);
        canvas.drawRect(rangeL, getHeight() - UnitConverter.dpToPx(2), rangeR, getHeight(), rectPaint);

        drawThumb(normalizedToScreen(normalizedMinValue), false, canvas, true);
        drawThumb(normalizedToScreen(normalizedMaxValue), false, canvas, false);
        drawVideoTrimTimeText(canvas);
    }

    private void drawThumb(float screenCoord, boolean pressed, Canvas canvas, boolean isLeft) {
        canvas.drawBitmap(pressed ? thumbPressedImage : (isLeft ? thumbImageLeft : thumbImageRight), screenCoord - (isLeft ? 0 : thumbWidth), paddingTop,
                paint);
    }

    private void drawVideoTrimTimeText(Canvas canvas) {
        String leftThumbsTime = DateUtil.convertSecondsToTime(mStartPosition);
        String rightThumbsTime = DateUtil.convertSecondsToTime(mEndPosition);
        canvas.drawText(leftThumbsTime, normalizedToScreen(normalizedMinValue), TextPositionY, mVideoTrimTimePaintL);
        canvas.drawText(rightThumbsTime, normalizedToScreen(normalizedMaxValue), TextPositionY, mVideoTrimTimePaintR);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isTouchDown) {
            return super.onTouchEvent(event);
        }
        if (event.getPointerCount() > 1) {
            return super.onTouchEvent(event);
        }

        if (!isEnabled()) return false;
        if (absoluteMaxValuePrim <= mMinShootTime) {
            return super.onTouchEvent(event);
        }
        int pointerIndex;// Record clicks index
        final int action = event.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                //Remember the coordinates of the point where the last finger tapped on the screen x，mDownMotionX
                mActivePointerId = event.getPointerId(event.getPointerCount() - 1);
                pointerIndex = event.findPointerIndex(mActivePointerId);
                mDownMotionX = event.getX(pointerIndex);
                // Determine whether the touch has the maximum thumb or minimum thumb
                pressedThumb = evalPressedThumb(mDownMotionX);
                if (pressedThumb == null) return super.onTouchEvent(event);
                setPressed(true);// Set the control to be pressed
                onStartTrackingTouch();// Set mIsDragging to true to start tracking touch events
                trackTouchEvent(event);
                attemptClaimDrag();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_DOWN, isMin,
                            pressedThumb);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (pressedThumb != null) {
                    if (mIsDragging) {
                        trackTouchEvent(event);
                    } else {
                        // Scroll to follow the motion event
                        pointerIndex = event.findPointerIndex(mActivePointerId);
                        final float x = event.getX(pointerIndex);// X coordinate of the finger point on the control
                        // The finger is not on the maximum and minimum, and there is a swipe event on the control
                        if (Math.abs(x - mDownMotionX) > mScaledTouchSlop) {
                            setPressed(true);
                            Log.e(TAG, "No drag");//Never executed?
                            invalidate();
                            onStartTrackingTouch();
                            trackTouchEvent(event);
                            attemptClaimDrag();
                        }
                    }
                    if (notifyWhileDragging && mRangeSeekBarChangeListener != null) {
                        mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_MOVE,
                                isMin, pressedThumb);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                invalidate();
                if (mRangeSeekBarChangeListener != null) {
                    mRangeSeekBarChangeListener.onRangeSeekBarValuesChanged(this, getSelectedMinValue(), getSelectedMaxValue(), MotionEvent.ACTION_UP, isMin,
                            pressedThumb);
                }
                pressedThumb = null;// Lift your finger, and set the touched thumb to empty
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = event.getPointerCount() - 1;
                // final int index = ev.getActionIndex();
                mDownMotionX = event.getX(index);
                mActivePointerId = event.getPointerId(index);
                invalidate();
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(event);
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate(); // see above explanation
                break;
            default:
                break;
        }
        return true;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mDownMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    private void trackTouchEvent(MotionEvent event) {
        if (event.getPointerCount() > 1) return;
        Log.e(TAG, "trackTouchEvent: " + event.getAction() + " x: " + event.getX());
        final int pointerIndex = event.findPointerIndex(mActivePointerId);// Get the index of the pressed point
        float x = 0;
        try {
            x = event.getX(pointerIndex);
        } catch (Exception e) {
            return;
        }
        if (Thumb.MIN.equals(pressedThumb)) {
            // screenToNormalized(x)-->Get a normalized value of 0-1
            setNormalizedMinValue(screenToNormalized(x, 0));
        } else if (Thumb.MAX.equals(pressedThumb)) {
            setNormalizedMaxValue(screenToNormalized(x, 1));
        }
    }

    private double screenToNormalized(float screenCoord, int position) {
        int width = getWidth();
        if (width <= 2 * padding) {
            // prevent division by zero, simply return 0.
            return 0d;
        } else {
            isMin = false;
            double current_width = screenCoord;
            float rangeL = normalizedToScreen(normalizedMinValue);
            float rangeR = normalizedToScreen(normalizedMaxValue);
            double min = mMinShootTime / (absoluteMaxValuePrim - absoluteMinValuePrim) * (width - thumbWidth * 2);

            if (absoluteMaxValuePrim > 5 * 60 * 1000) {//Four decimal places greater than 5 minutes
                DecimalFormat df = new DecimalFormat("0.0000");
                min_width = Double.parseDouble(df.format(min));
            } else {
                min_width = Math.round(min + 0.5d);
            }
            if (position == 0) {
                if (isInThumbRangeLeft(screenCoord, normalizedMinValue, 0.5)) {
                    return normalizedMinValue;
                }

                float rightPosition = (getWidth() - rangeR) >= 0 ? (getWidth() - rangeR) : 0;
                double left_length = getValueLength() - (rightPosition + min_width);

                if (current_width > rangeL) {
                    current_width = rangeL + (current_width - rangeL);
                } else if (current_width <= rangeL) {
                    current_width = rangeL - (rangeL - current_width);
                }

                if (current_width > left_length) {
                    isMin = true;
                    current_width = left_length;
                }

                if (current_width < thumbWidth * 2 / 3) {
                    current_width = 0;
                }

                double resultTime = (current_width - padding) / (width - 2 * thumbWidth);
                normalizedMinValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                return Math.min(1d, Math.max(0d, result));// The value is guaranteed to be between 0-1, but when is this judgment useful?
            } else {
                if (isInThumbRange(screenCoord, normalizedMaxValue, 0.5)) {
                    return normalizedMaxValue;
                }

                double right_length = getValueLength() - (rangeL + min_width);
                if (current_width > rangeR) {
                    current_width = rangeR + (current_width - rangeR);
                } else if (current_width <= rangeR) {
                    current_width = rangeR - (rangeR - current_width);
                }

                double paddingRight = getWidth() - current_width;

                if (paddingRight > right_length) {
                    isMin = true;
                    current_width = getWidth() - right_length;
                    paddingRight = right_length;
                }

                if (paddingRight < thumbWidth * 2 / 3) {
                    current_width = getWidth();
                    paddingRight = 0;
                }

                double resultTime = (paddingRight - padding) / (width - 2 * thumbWidth);
                resultTime = 1 - resultTime;
                normalizedMaxValueTime = Math.min(1d, Math.max(0d, resultTime));
                double result = (current_width - padding) / (width - 2 * padding);
                return Math.min(1d, Math.max(0d, result));// The value is guaranteed to be between 0-1, but when is this judgment useful?
            }
        }
    }

    private int getValueLength() {
        return (getWidth() - 2 * thumbWidth);
    }

    /**
     * Calculate which Thumb is in
     *
     * @param touchX touchX
     * @return Is touched empty or maximum or minimum
     */
    private Thumb evalPressedThumb(float touchX) {
        Thumb result = null;
        boolean minThumbPressed = isInThumbRange(touchX, normalizedMinValue, 2);// Whether the touch point is within the minimum picture range
        boolean maxThumbPressed = isInThumbRange(touchX, normalizedMaxValue, 2);
        if (minThumbPressed && maxThumbPressed) {
            // If two thumbs overlap, you cannot determine which one to drag, do the following
            // If the touch point is on the right side of the screen, it is judged that the touch has
            // reached the minimum thumb, otherwise it is judged that the touch has reached the maximum thumb.
            result = (touchX / getWidth() > 0.5f) ? Thumb.MIN : Thumb.MAX;
        } else if (minThumbPressed) {
            result = Thumb.MIN;
        } else if (maxThumbPressed) {
            result = Thumb.MAX;
        }
        return result;
    }

    private boolean isInThumbRange(float touchX, double normalizedThumbValue, double scale) {
        // X coordinate of the current touch point-the difference between the X coordinate of the center point of the
        // minimum picture and the screen <= the width of the minimum point of the picture
        //        // That is, determine whether the touch point is in a circle with the center
        //        of the minimum picture as the origin and half the width as the radius.
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue)) <= thumbHalfWidth * scale;
    }

    private boolean isInThumbRangeLeft(float touchX, double normalizedThumbValue, double scale) {
        // X coordinate of the current touch point-the difference between the X coordinate of the center
        // point of the minimum picture and the screen <= the width of the minimum point of the picture
        //        // That is, determine whether the touch point is in a circle with
        //        the center of the minimum picture as the origin and half the width as the radius.
        return Math.abs(touchX - normalizedToScreen(normalizedThumbValue) - thumbWidth) <= thumbHalfWidth * scale;
    }

    /**
     * Trying to tell the parent view not to intercept child controls' drags
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    void onStopTrackingTouch() {
        mIsDragging = false;
    }

    public void setMinShootTime(long min_cut_time) {
        this.mMinShootTime = min_cut_time;
    }

    private float normalizedToScreen(double normalizedCoord) {
        return (float) (getPaddingLeft() + normalizedCoord * (getWidth() - getPaddingLeft() - getPaddingRight()));
    }

    private double valueToNormalized(long value) {
        if (0 == absoluteMaxValuePrim - absoluteMinValuePrim) {
            return 0d;
        }
        return (value - absoluteMinValuePrim) / (absoluteMaxValuePrim - absoluteMinValuePrim);
    }

    public void setStartEndTime(long start, long end) {
        this.mStartPosition = start / 1000;
        this.mEndPosition = end / 1000;
    }

    public void setSelectedMinValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMinValue(0d);
        } else {
            setNormalizedMinValue(valueToNormalized(value));
        }
    }

    public void setSelectedMaxValue(long value) {
        if (0 == (absoluteMaxValuePrim - absoluteMinValuePrim)) {
            setNormalizedMaxValue(1d);
        } else {
            setNormalizedMaxValue(valueToNormalized(value));
        }
    }

    public void setNormalizedMinValue(double value) {
        normalizedMinValue = Math.max(0d, Math.min(1d, Math.min(value, normalizedMaxValue)));
        invalidate();// Redraw this view
    }

    public void setNormalizedMaxValue(double value) {
        normalizedMaxValue = Math.max(0d, Math.min(1d, Math.max(value, normalizedMinValue)));
        invalidate();//Redraw this view
    }

    public long getSelectedMinValue() {
        return normalizedToValue(normalizedMinValueTime);
    }

    public long getSelectedMaxValue() {
        return normalizedToValue(normalizedMaxValueTime);
    }

    private long normalizedToValue(double normalized) {
        return (long) (absoluteMinValuePrim + normalized * (absoluteMaxValuePrim - absoluteMinValuePrim));
    }

    /**
     * For external activity calls, the control is to print
     * log information when dragging, the default is false not to print
     */
    public boolean isNotifyWhileDragging() {
        return notifyWhileDragging;
    }

    public void setNotifyWhileDragging(boolean flag) {
        this.notifyWhileDragging = flag;
    }

    public void setTouchDown(boolean touchDown) {
        isTouchDown = touchDown;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();
        bundle.putParcelable("SUPER", super.onSaveInstanceState());
        bundle.putDouble("MIN", normalizedMinValue);
        bundle.putDouble("MAX", normalizedMaxValue);
        bundle.putDouble("MIN_TIME", normalizedMinValueTime);
        bundle.putDouble("MAX_TIME", normalizedMaxValueTime);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable parcel) {
        final Bundle bundle = (Bundle) parcel;
        super.onRestoreInstanceState(bundle.getParcelable("SUPER"));
        normalizedMinValue = bundle.getDouble("MIN");
        normalizedMaxValue = bundle.getDouble("MAX");
        normalizedMinValueTime = bundle.getDouble("MIN_TIME");
        normalizedMaxValueTime = bundle.getDouble("MAX_TIME");
    }

    public interface OnRangeSeekBarChangeListener {
        void onRangeSeekBarValuesChanged(RangeSeekBarView2 bar, long minValue, long maxValue, int action, boolean isMin, Thumb pressedThumb);
    }

    public void setOnRangeSeekBarChangeListener(OnRangeSeekBarChangeListener listener) {
        this.mRangeSeekBarChangeListener = listener;
    }
}

