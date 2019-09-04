package com.haodong.musicplayer.myplayer;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewParent;
import android.view.WindowManager;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.TimeBar;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Util;
import com.haodong.musicplayer.R;

import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArraySet;

import androidx.annotation.Nullable;

/**
 * created by linghaoDo on 2019-08-28
 * <p>
 * description:
 */
public class CircleTimeBar extends View implements TimeBar {
    /**
     * Default height for the time bar, in dp.
     */
    public static final int DEFAULT_BAR_HEIGHT_DP = 2;
    /**
     * Default height for the touch target, in dp.
     */
    public static final int DEFAULT_TOUCH_TARGET_HEIGHT_DP = 26;
    /**
     * Default width for ad markers, in dp.
     */
    public static final int DEFAULT_AD_MARKER_WIDTH_DP = 2;
    /**
     * Default diameter for the scrubber when enabled, in dp.
     */
    public static final int DEFAULT_SCRUBBER_ENABLED_SIZE_DP = 12;
    /**
     * Default diameter for the scrubber when disabled, in dp.
     */
    public static final int DEFAULT_SCRUBBER_DISABLED_SIZE_DP = 0;
    /**
     * Default diameter for the scrubber when dragged, in dp.
     */
    public static final int DEFAULT_SCRUBBER_DRAGGED_SIZE_DP = 16;
    /**
     * Default color for the played portion of the time bar.
     */
    public static final int DEFAULT_PLAYED_COLOR = 0xFFFFFFFF;
    /**
     * Default color for ad markers.
     */
    public static final int DEFAULT_AD_MARKER_COLOR = 0xB2FFFF00;

    public static final int default_played_color = 0xFFFF7700;

    /**
     * The threshold in dps above the bar at which touch events trigger fine scrub mode.
     */
    private static final int FINE_SCRUB_Y_THRESHOLD_DP = -50;
    /**
     * The ratio by which times are reduced in fine scrub mode.
     */
    private static final int FINE_SCRUB_RATIO = 3;
    /**
     * The time after which the scrubbing listener is notified that scrubbing has stopped after
     * performing an incremental scrub using key input.
     */
    private static final long STOP_SCRUBBING_TIMEOUT_MS = 1000;
    private static final int DEFAULT_INCREMENT_COUNT = 20;

    private final Runnable stopScrubbingRunnable;
    private final CopyOnWriteArraySet<OnScrubListener> listeners;

    private int keyCountIncrement;
    private long keyTimeIncrement;
    private int lastCoarseScrubXPosition;

    private final Paint playedPaint;
    private final Paint bufferedPaint;
    private final Paint unplayedPaint;
    private final StringBuilder formatBuilder;
    private final Formatter formatter;

    private final float density;
    private final int fineScrubYThreshold;
    private int bufferColor;
    private int unPlayedColor;
    private int playedColor;
    private int roundWidth = 2;
    private int startAngle;
    private int mMax;
    private RectF mRectF;
    private long scrubPosition;
    private long duration;
    private long position;
    private WindowManager windowManager;
    private Bitmap bitmap;
    private Paint mCoverPaint;


    private boolean scrubbing;

    public CircleTimeBar(Context context) {

        this(context, null);
    }

    public CircleTimeBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        listeners = new CopyOnWriteArraySet<>();
        windowManager = (WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        /*实例化paint*/
        playedPaint = new Paint();
        playedPaint.setAntiAlias(true);
        playedPaint.setStyle(Paint.Style.STROKE);
        playedPaint.setStrokeCap(Paint.Cap.ROUND);
        /**/
        bufferedPaint = new Paint();
        bufferedPaint.setAntiAlias(true);
        bufferedPaint.setStyle(Paint.Style.STROKE);
        bufferedPaint.setStrokeCap(Paint.Cap.ROUND);
        /**/
        unplayedPaint = new Paint();
        unplayedPaint.setAntiAlias(true);
        unplayedPaint.setStyle(Paint.Style.STROKE);
        unplayedPaint.setStrokeCap(Paint.Cap.ROUND);
        mCoverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mRectF = new RectF();
        // Calculate the dimensions and paints for drawn elements.
        Resources res = context.getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        density = displayMetrics.density;
        fineScrubYThreshold = dpToPx(density, FINE_SCRUB_Y_THRESHOLD_DP);
        int defaultBarHeight = dpToPx(density, DEFAULT_BAR_HEIGHT_DP);
        int defaultTouchTargetHeight = dpToPx(density, DEFAULT_TOUCH_TARGET_HEIGHT_DP);
        int defaultAdMarkerWidth = dpToPx(density, DEFAULT_AD_MARKER_WIDTH_DP);
        int defaultScrubberEnabledSize = dpToPx(density, DEFAULT_SCRUBBER_ENABLED_SIZE_DP);
        int defaultScrubberDisabledSize = dpToPx(density, DEFAULT_SCRUBBER_DISABLED_SIZE_DP);
        int defaultScrubberDraggedSize = dpToPx(density, DEFAULT_SCRUBBER_DRAGGED_SIZE_DP);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleTimeBar);
            roundWidth = (int) typedArray.getDimension(R.styleable.CircleTimeBar_round_width, dpToPx(density, 2));
            bufferColor = typedArray.getColor(R.styleable.CircleTimeBar_buffer_color, DEFAULT_AD_MARKER_COLOR);
            unPlayedColor = typedArray.getColor(R.styleable.CircleTimeBar_unplayed_color, DEFAULT_PLAYED_COLOR);
            playedColor = typedArray.getColor(R.styleable.CircleTimeBar_played_color, default_played_color);
            typedArray.recycle();
        }
        playedPaint.setStrokeWidth(roundWidth);
        unplayedPaint.setStrokeWidth(roundWidth);
        bufferedPaint.setStrokeWidth(roundWidth);
        playedPaint.setColor(playedColor);
        unplayedPaint.setColor(unPlayedColor);
        bufferedPaint.setColor(bufferColor);
        startAngle = 90;
        position = 0;
        mMax = 100;
        stopScrubbingRunnable = new Runnable() {
            @Override
            public void run() {
                stopScrubbing(/* canceled= */ false);
            }
        };
        formatBuilder = new StringBuilder();
        formatter = new Formatter(formatBuilder, Locale.getDefault());
    }

    private void stopScrubbing(boolean canceled) {
        removeCallbacks(stopScrubbingRunnable);
        scrubbing = false;
        setPressed(false);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
        invalidate();
        for (OnScrubListener listener : listeners) {
            listener.onScrubStop(this, scrubPosition, canceled);
        }
    }


    public synchronized void setStartAngle(int startAngle) {
        this.startAngle = startAngle;
        invalidate();

    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.AT_MOST) {
//            widthSize=SizeUtils.dip2px()
        }
        if (heightMode == MeasureSpec.AT_MOST) {

        }
        /*保证是正方形*/
        setMeasuredDimension(widthSize > heightSize ? heightSize : widthSize,
                widthSize > heightSize ? heightSize : widthSize);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        LogUtil.i("onDraw");
        /*drawCover*/
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, getWidth(), getHeight(), mCoverPaint);
            bitmap.recycle();
        }

        /*1. drawNormal*/
        int randius = getHeight() > getWidth() ? getWidth() / 2 : getHeight() / 2;
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, randius - roundWidth / 2, unplayedPaint);
        /*2. drawColor*/
        mRectF.set(roundWidth / 2, roundWidth / 2, getWidth() - roundWidth / 2, getHeight() - roundWidth / 2);
        float sweepAngle = (float) position / duration;
        canvas.drawArc(mRectF, 90, 360 * sweepAngle, false, playedPaint);


    }


    @Override
    public void addListener(OnScrubListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(OnScrubListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void setKeyTimeIncrement(long time) {
        Assertions.checkArgument(time > 0);
        keyCountIncrement = C.INDEX_UNSET;
        keyTimeIncrement = time;
    }

    @Override
    public void setKeyCountIncrement(int count) {
        Assertions.checkArgument(count > 0);
        keyCountIncrement = count;
        keyTimeIncrement = C.TIME_UNSET;
    }

    @Override
    public void setPosition(long position) {
        LogUtil.i("setPosition-->" + position);
        this.position = position;
        invalidate();
    }

    private String getProgressText() {
        return Util.getStringForTime(formatBuilder, formatter, position);
    }

    @Override
    public void setBufferedPosition(long bufferedPosition) {

    }

    @Override
    public void setDuration(long duration) {
        this.duration = duration;
        LogUtil.i("setDuration-->" + duration);
//        if (scrubbing && duration == C.TIME_UNSET) {
//            stopScrubbing(/* canceled= */ true);
//        }
        invalidate();
    }

    @Override
    public void setAdGroupTimesMs(@Nullable long[] adGroupTimesMs, @Nullable boolean[] playedAdGroups,
                                  int adGroupCount) {

    }

    public void setCoverBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }


    // View methods.

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (scrubbing && !enabled) {
            stopScrubbing(/* canceled= */ true);
        }
    }

    private static int dpToPx(float density, int dps) {
        return (int) (dps * density + 0.5f);
    }

    private static int pxToDp(float density, int px) {
        return (int) (px / density);
    }
}
