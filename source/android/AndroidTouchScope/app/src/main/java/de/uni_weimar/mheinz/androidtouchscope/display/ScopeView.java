package de.uni_weimar.mheinz.androidtouchscope.display;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.Locale;

import de.uni_weimar.mheinz.androidtouchscope.display.callback.OnDataChangedInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.ScopeInterface;
import de.uni_weimar.mheinz.androidtouchscope.scope.BaseScope;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TimeData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.TriggerData;
import de.uni_weimar.mheinz.androidtouchscope.scope.wave.WaveData;

// TODO: display offset values (somehow)
// TODO: add measure cursors
public class ScopeView extends View
{
    private static final String TAG = "ScopeView";
    private static final float NUM_COLUMNS = 12f;
    private static final float NUM_ROWS = 8f;
    //scope returns 12 columns worth, this is displayed ratio
    private static final float DISPLAY_RATIO = NUM_COLUMNS / 12f;

    private int mContentWidth = 0;
    private int mContentHeight = 0;

    private int mSelectedPath = -1; // -1 if not selected
    private double mTimeScreenOffset = 0;
  /*  private WaveData mPrevChan1 = null;
    private WaveData mPrevChan2 = null;
    private TimeData mPrevTime = null;
    private TriggerData mPrevTrig = null;*/
    private OnDataChangedInterface.OnDataChanged mOnDataChanged = null;

    /****  Drawing  ****/
    private final ShapeDrawable mDrawableChan1 = new ShapeDrawable();
    private final ShapeDrawable mDrawableChan2 = new ShapeDrawable();
    private final ShapeDrawable mDrawableGridH = new ShapeDrawable();
    private final ShapeDrawable mDrawableGridV = new ShapeDrawable();

    private final Path mPathChan1 = new Path();
    private final Path mPathChan2 = new Path();
    private final Path mPathGridH = new Path();
    private final Path mPathGridV = new Path();

    private final Paint mChan1TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mChan2TextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTimeTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTimeOffsetTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTriggerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final String TIME_SCALE_TEXT = "TIME: ";
    private static final String TIME_OFFSET_TEXT = "T-> ";
    private String mChan1Text = "";
    private String mChan2Text = "";
    private String mTimeText = "";
    private String mTimeOffsetText = "";
    private String mTriggerText = "";

    private Point mTextPos;

    /****  Gestures  ****/
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;

    private PointF mFirstTouch = null;
    private boolean mInMovement = false;
    private boolean mInScaling = false;
    private int mChangeDelay = 0;

    public ScopeView(Context context)
    {
        super(context);
        initGestures(context);
        init();
    }

    public ScopeView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        initGestures(context);
        init();
    }

    public ScopeView(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        initGestures(context);
        init();
    }

    public void setOnDoCommand(OnDataChangedInterface.OnDataChanged onDataChanged)
    {
        mOnDataChanged = onDataChanged;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Init functions
    //
    //////////////////////////////////////////////////////////////////////////

    private void initGestures(Context context)
    {
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScopeScaleListener());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
        {
            mScaleGestureDetector.setQuickScaleEnabled(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            mScaleGestureDetector.setStylusScaleEnabled(false);
        }

        mGestureDetector = new GestureDetectorCompat(context, new ScopeGestureListener());
    }

    private void init()
    {
        int paddingLeft = getPaddingLeft();
        int paddingTop = getPaddingTop();
        int paddingRight = getPaddingRight();
        int paddingBottom = getPaddingBottom();

        mContentWidth = getWidth() - paddingLeft - paddingRight;
        mContentHeight = getHeight() - paddingTop - paddingBottom;
        mTextPos = new Point(5,mContentHeight - 5);

        initDrawable(mDrawableChan1, mPathChan1, Color.YELLOW, mContentWidth, mContentHeight);
        initDrawable(mDrawableChan2, mPathChan2, Color.BLUE, mContentWidth, mContentHeight);
        initGridH(mDrawableGridH, mPathGridH, Color.GRAY, mContentWidth, mContentHeight);
        initGridV(mDrawableGridV, mPathGridV, Color.GRAY, mContentWidth, mContentHeight);
        initText(mChan1TextPaint, 15, Color.YELLOW, Paint.Align.LEFT);
        initText(mChan2TextPaint, 15, Color.BLUE, Paint.Align.LEFT);
        initText(mTimeTextPaint, 15, Color.WHITE, Paint.Align.RIGHT);
        initText(mTimeOffsetTextPaint, 15, Color.rgb(255,215,0), Paint.Align.RIGHT);
        initText(mTriggerTextPaint, 15, Color.rgb(255,215,0), Paint.Align.RIGHT);
    }

    private void initDrawable(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.moveTo(0, 0);
        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.setBounds(0, 0, width, height);

        setLayerType(LAYER_TYPE_SOFTWARE, drawable.getPaint());
    }

    private void initGridV(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.rewind();
        float cellWidth = width / NUM_COLUMNS;
        for(int i = 0; i < NUM_COLUMNS; ++i)
        {
            path.moveTo(i * cellWidth,0);
            path.lineTo(i * cellWidth,height);
        }

        float offset = NUM_ROWS * 5;
        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.getPaint().setPathEffect(new DashPathEffect(new float[]{1, (height - offset) / offset}, 0));
        drawable.setBounds(0, 0, width, height);
    }

    private void initGridH(ShapeDrawable drawable, Path path, int color, int width, int height)
    {
        path.rewind();
        float cellHeight = height / NUM_ROWS;
        for(int i = 0; i < NUM_ROWS; ++i)
        {
            path.moveTo(0,i * cellHeight);
            path.lineTo(width,i * cellHeight);
        }

        float offset = NUM_COLUMNS * 5;
        drawable.setShape(new PathShape(path, width, height));
        drawable.getPaint().setStyle(Paint.Style.STROKE);
        drawable.getPaint().setColor(color);
        drawable.getPaint().setPathEffect(new DashPathEffect(new float[]{1, (width - offset) / offset}, 0));
        drawable.setBounds(0, 0, width, height);
    }

    private void initText(Paint paint, int height, int color, Paint.Align align)
    {
        paint.setColor(color);
        paint.setTextSize(height);
        paint.setTextAlign(align);
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Update View functions
    //
    //////////////////////////////////////////////////////////////////////////

    public void setInMovement(boolean moving)
    {
        mInMovement = moving;
        if(moving)
        {
            int numOn = channelOnCount();
            mChangeDelay = 4 * numOn;
        }
    }

    public void setChannelData(int channel, WaveData waveData, TimeData timeData, TriggerData trigData)
    {
        // being updated by user
        if(mInMovement || mInScaling)
            return;

        switch(channel)
        {
            case 1:
            {
              //  mPrevChan1 = waveData;
                updatePath(channel, mPathChan1, waveData);
                mChan1Text = updateVoltText(waveData, "Chan1");
                break;
            }
            case 2:
            {
              //  mPrevChan2 = waveData;
                updatePath(channel, mPathChan2, waveData);
                mChan2Text = updateVoltText(waveData, "Chan2");
                break;
            }
        }

      //  mPrevTrig = trigData;
        mTriggerText = updateTriggerText(trigData);

        if(mOnDataChanged != null && mChangeDelay <= 0)
        {
          //  mPrevTime = timeData;
            if(timeData != null)
            {
                float offset = (float) (-timeData.timeOffset / timeData.timeScale) * mContentWidth / NUM_COLUMNS;
                mTimeScreenOffset = offset + mContentWidth / 2;
                mOnDataChanged.moveTime((float) mTimeScreenOffset, false);

                mTimeText = updateTimeText(TIME_SCALE_TEXT, timeData.timeScale);
                mTimeOffsetText = updateTimeText(TIME_OFFSET_TEXT, timeData.timeOffset);
            }

            if(trigData != null && waveData != null &&
                ((trigData.source == TriggerData.TriggerSrc.CHAN1 && channel == 1) ||
                  trigData.source == TriggerData.TriggerSrc.CHAN2 && channel == 2))
            {
                float offset = (float) ( -(waveData.voltageOffset + trigData.level) /
                                           waveData.voltageScale) * mContentHeight / NUM_ROWS;
                offset = offset + mContentHeight / 2;
                mOnDataChanged.moveTrigger(offset, false);
            }
        }

        postInvalidate();
    }

    private int updatePath(int channel, Path path, WaveData waveData)
    {
        if(waveData == null || waveData.data == null || waveData.data.length == 0)
        {
            path.rewind();
            return 0;
        }

        int length = Math.min(BaseScope.SAMPLE_LENGTH, waveData.data.length) - 10;
        float widthRatio = (float)(mContentWidth) / (length * DISPLAY_RATIO);
        double vScale = waveData.voltageScale;
        if(vScale == 0)
            vScale = 1.0f;

        Path newPath = new Path();
        float point = manipulatePoint(waveData.voltageOffset, vScale, waveData.data[10]);

        float j = -(length * (1 - DISPLAY_RATIO)) / 2;
        newPath.moveTo(j++  * widthRatio, point);

        for(int i = 11; i < waveData.data.length; ++i, ++j)
        {
            point = manipulatePoint(waveData.voltageOffset, vScale, waveData.data[i]);
            newPath.lineTo(j * widthRatio, point);
        }

        if(new PathMeasure(path,false).getLength() == 0)
        {
            path.set(newPath);
            return 0;
        }

        if(mChangeDelay <= 0)
        {
            path.set(newPath);
        }
        else
        {
            mChangeDelay--;
        }

        if(mOnDataChanged != null)
        {
            RectF bounds = new RectF();
            path.computeBounds(bounds, false);
            mOnDataChanged.moveWave(channel, bounds.bottom, false);
        }
        return 0;
    }

    private String updateVoltText(WaveData waveData, String chan)
    {
        if(waveData != null && waveData.data != null)
        {
            double value;
            String end;

            if (waveData.voltageScale < 1)
            {
                value = waveData.voltageScale * 1e3;
                end = "mV";
            }
            else
            {
                value = waveData.voltageScale;
                end = "V";
            }

            return String.format(Locale.getDefault(),"%s: %.2f%s", chan, value, end);
        }
        else
        {
            return "";
        }
    }

    private String updateTimeText(String startText, double time)
    {
        double value;
        String end;
        double absTime = Math.abs(time);
        if(absTime < 1e-6)
        {
            value = (time * 1e9);
            end = "nS";
        }
        else if(absTime < 1e-3)
        {
            value = time * 1e6;
            end = "uS";
        }
        else if(absTime < 1)
        {
            value = time * 1e3;
            end = "mS";
        }
        else
        {
            value = time;
            end = "S";
        }
        return String.format(Locale.getDefault(),"%s%.2f%s",startText, value, end);
    }

    private String updateTriggerText(TriggerData trigData)
    {
        if(trigData == null)
            return "";

        double value;
        String end;

        if (trigData.level < 1)
        {
            value = trigData.level * 1e3;
            end = "mV";
        }
        else
        {
            value = trigData.level;
            end = "V";
        }

        return String.format(Locale.getDefault(),"Trig LVL: %.2f%s", value, end);
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Helper functions
    //
    //////////////////////////////////////////////////////////////////////////

    private float manipulatePoint(double voltOffset, double voltScale, int data)
    {
        float heightRatio = (mContentHeight / 8.0f) / (float)voltScale;
        float mid = (mContentHeight / 2.0f);

        float point = (float) BaseScope.actualVoltage(voltOffset, voltScale, data);
        point = mid - ((point + (float)voltOffset) * heightRatio);
        if(point < 0)
            point = 0;
        else if(point > mContentHeight)
            point = mContentHeight;

        return point;
    }

    private int channelOnCount()
    {
        int count = 0;

        PathMeasure measure = new PathMeasure(mPathChan1,false);
        count += measure.getLength() > 0 ? 1 : 0;
        measure.setPath(mPathChan2,false);
        count += measure.getLength() > 0 ? 1 : 0;

        return count;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Class overrides
    //
    //////////////////////////////////////////////////////////////////////////

    @Override
    protected void onDraw(Canvas canvas)
    {
        mDrawableChan1.draw(canvas);
        mDrawableChan2.draw(canvas);
        mDrawableGridH.draw(canvas);
        mDrawableGridV.draw(canvas);
        canvas.drawText(mChan1Text, mTextPos.x, mTextPos.y, mChan1TextPaint);
        canvas.drawText(mChan2Text, mTextPos.x + 150, mTextPos.y, mChan2TextPaint);
        canvas.drawText(mTimeText, mContentWidth - 5, mTextPos.y, mTimeTextPaint);
        canvas.drawText(mTriggerText, mContentWidth - 150, mTextPos.y, mTriggerTextPaint);
        canvas.drawText(mTimeOffsetText, mContentWidth - 5, 20, mTimeOffsetTextPaint);

        super.onDraw(canvas);
    }

    @Override
    protected void onSizeChanged (int w, int h, int oldw, int oldh)
    {
        init();
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Path Selection
    //
    //////////////////////////////////////////////////////////////////////////

    private boolean touchSelectPath(MotionEvent event)
    {
        final int pointerIndex = MotionEventCompat.getActionIndex(event);
        final float x = MotionEventCompat.getX(event, pointerIndex);
        final float y = MotionEventCompat.getY(event, pointerIndex);

        int hit = pathHitTest(x, y, 15f);
        if(hit == -1 && mSelectedPath == -1)
            return false;

        if(mSelectedPath == hit)
            mSelectedPath = -1;
        else
            mSelectedPath = hit;

        mDrawableChan1.getPaint().clearShadowLayer();
        mDrawableChan1.getPaint().setStrokeWidth(1);
        mDrawableChan2.getPaint().clearShadowLayer();
        mDrawableChan2.getPaint().setStrokeWidth(1);

        switch(mSelectedPath)
        {
            case 1:
                mDrawableChan1.getPaint().setShadowLayer(10f,0f,0f,Color.YELLOW);
                mDrawableChan1.getPaint().setStrokeWidth(1.5f);
                break;
            case 2:
                mDrawableChan2.getPaint().setShadowLayer(10f,0f,0f,Color.BLUE);
                mDrawableChan2.getPaint().setStrokeWidth(1.5f);
                break;
            default:
                break;
        }
        return true;
    }

    private float smallestDistanceToPath(Path path, float x, float y)
    {
        PathMeasure measure = new PathMeasure(path,false);
        float pos[] = {0f, 0f};
        float minDist = 1000f;
        float dist;

        for(int i = 0; i < measure.getLength(); ++i)
        {
            measure.getPosTan(i, pos, null);
            dist = (float)Math.hypot(x - pos[0], y - pos[1]);
            if(dist < minDist)
                minDist = dist;
        }
        return minDist;
    }

    private int pathHitTest(float x, float y, float threshold)
    {
        int selected = -1;
        float minDist = threshold;

        float dist = smallestDistanceToPath(mPathChan1,x,y);
        if(dist <= minDist)
        {
            selected = 1;
            minDist = dist;
        }

        dist = smallestDistanceToPath(mPathChan2,x,y);
        if(dist < minDist)
        {
            selected = 2;
        }

        return selected;
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Methods related to moving waves
    //
    //////////////////////////////////////////////////////////////////////////

    public void moveWave(int channel, float dist, boolean endMove)
    {
        if(endMove)
        {
            float move = dist / (mContentHeight / NUM_ROWS);
            mOnDataChanged.doCommand(
                    ScopeInterface.Command.SET_VOLTAGE_OFFSET,
                    channel,
                    (Float) move);
        }
        else
        {
            if(channel == 1)
            {
                mPathChan1.offset(0, dist);
            }
            else if(channel == 2)
            {
                mPathChan2.offset(0, dist);
            }
        }
        invalidate();
    }

    public void moveTime(float dist, boolean endMove)
    {
        if(endMove)
        {
            float move = dist / (mContentWidth / NUM_COLUMNS);
            mOnDataChanged.doCommand(
                    ScopeInterface.Command.SET_TIME_OFFSET,
                    0,
                    (Float) move);
        }
        else
        {
            mPathChan1.offset(dist, 0);
            mPathChan2.offset(dist, 0);

           /* if(mPrevTime != null)
            {
                float move = dist / (mContentWidth / NUM_COLUMNS);
                mPrevTime.timeOffset = (float)(move * mPrevTime.timeScale + mPrevTime.timeOffset);
                mTimeOffsetText = updateTimeText(TIME_SCALE_TEXT, mPrevTime.timeOffset);
            }*/
        }
        invalidate();
    }

    public void moveTrigger(float dist, boolean endMove)
    {
        if(endMove)
        {
            mOnDataChanged.doCommand(
                    ScopeInterface.Command.SET_TRIGGER_LEVEL,
                    0,
                    (Float) (dist / (mContentHeight / NUM_ROWS)));
        }
        invalidate();
    }

    //////////////////////////////////////////////////////////////////////////
    //
    // Methods related to gesture handling
    //
    //////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mScaleGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);

        if(MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP ||
                mScaleGestureDetector.isInProgress())
        {
            if(mInMovement && mFirstTouch != null)
            {
                int pointerIndex = MotionEventCompat.getActionIndex(event);
                float x = MotionEventCompat.getX(event, pointerIndex);
                float y = MotionEventCompat.getY(event, pointerIndex);

                if(mOnDataChanged != null)
                {
                    if(mSelectedPath > 0)
                    {
                        float distY = mFirstTouch.y - y;
                        moveWave(mSelectedPath, distY, true);
                    }
                    float distX = mFirstTouch.x - x;
                    moveTime(distX, true);
                }
                mFirstTouch = null;
            }

            setInMovement(false);
        }

        return true;
    }

    private class ScopeGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            if(mFirstTouch != null)
            {
                setInMovement(true);

                // must have a selected channel for voltage offset
                switch(mSelectedPath)
                {
                    case 1:
                        moveWave(1, -distanceY, false);
                        if(mOnDataChanged != null)
                        {
                            RectF bounds = new RectF();
                            mPathChan1.computeBounds(bounds, false);
                            mOnDataChanged.moveWave(1, bounds.bottom, true);
                        }
                        break;
                    case 2:
                        moveWave(2, -distanceY, false);
                        if(mOnDataChanged != null)
                        {
                            RectF bounds = new RectF();
                            mPathChan2.computeBounds(bounds, false);
                            mOnDataChanged.moveWave(2, bounds.bottom, true);
                        }
                        break;
                }

                moveTime(-distanceX, false);

                if(mOnDataChanged != null)
                {
                    mTimeScreenOffset = mTimeScreenOffset - distanceX;
                    mOnDataChanged.moveTime((float) mTimeScreenOffset, true);
                }

                invalidate();
            }
            return true;
        }

        @Override
        public boolean onDown(MotionEvent event)
        {
            int index = MotionEventCompat.getActionIndex(event);
            float x = MotionEventCompat.getX(event, index);
            float y = MotionEventCompat.getY(event, index);
            mFirstTouch = new PointF(x,y);

            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event)
        {
            touchSelectPath(event);
            return true;
        }
    }

    private class ScopeScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener
    {
        //TODO: continuous zooming in discrete intervals

        private float mFirstSpanX;
        private float mFirstSpanY;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector)
        {
            Log.d(TAG, "onScaleBegin");

            mInScaling = true;
            mFirstSpanX = scaleGestureDetector.getCurrentSpanX();
            mFirstSpanY = scaleGestureDetector.getCurrentSpanY();

            // stop dragging while zooming
            mFirstTouch = null;
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector)
        {
            float spanX = detector.getCurrentSpanX();
            float spanY = detector.getCurrentSpanY();
            float previousSpanX = detector.getPreviousSpanX();
            float previousSpanY = detector.getPreviousSpanY();

            float scaleX = spanX / previousSpanX;
            float scaleY = spanY / previousSpanY;//(float)Math.pow(spanY / previousSpanY, 2);

            Log.d(TAG, "onScale::x:" + scaleX + " y:" + scaleY);

            Matrix scaleMatrix = new Matrix();
            RectF rectF = new RectF();

            switch(mSelectedPath)
            {
                case 1:
                    mPathChan1.computeBounds(rectF, true);
                    scaleMatrix.setScale(1, scaleY, rectF.centerX(), rectF.bottom);
                    mPathChan1.transform(scaleMatrix);
                    break;
                case 2:
                    mPathChan2.computeBounds(rectF, true);
                    scaleMatrix.setScale(1, scaleY, rectF.centerX(), rectF.bottom);
                    mPathChan2.transform(scaleMatrix);
                    break;
            }

            scaleMatrix = new Matrix();
            Rect drawRect = new Rect();
            getDrawingRect(drawRect);
            scaleMatrix.setScale(scaleX, 1, drawRect.centerX(), drawRect.centerY());

            mPathChan1.transform(scaleMatrix);
            mPathChan2.transform(scaleMatrix);

            invalidate();

            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector)
        {
            float spanX = detector.getCurrentSpanX();
            float spanY = detector.getCurrentSpanY();

            float scaleX = spanX / mFirstSpanX;
            float scaleY = spanY / mFirstSpanY;//(float)Math.pow(spanY / mFirstSpanY, 2);

            Log.d(TAG, "onScaleEnd::x:" + scaleX + " y:" + scaleY);

            if(mOnDataChanged != null)
            {
                if(mSelectedPath > 0)
                {
                    mOnDataChanged.doCommand(ScopeInterface.Command.SET_VOLTAGE_SCALE,
                            mSelectedPath,
                            (Float) scaleY);
                }

                mOnDataChanged.doCommand(
                        ScopeInterface.Command.SET_TIME_SCALE,
                        0,
                        (Float)scaleX);
            }

            int numOn = channelOnCount();
            mChangeDelay = 4 * numOn;
            mInScaling = false;
        }
    }
}