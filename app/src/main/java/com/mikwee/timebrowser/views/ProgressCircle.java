package com.mikwee.timebrowser.views;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.mikwee.timebrowser.R;

public class ProgressCircle extends View {
    private String TAG = getClass().getSimpleName();

    private final RectF mOval = new RectF();
    private float mSweepAngle = 0;
    private int startAngle = 90;
    private int angleGap = 4;

    private int minDimension;
    private float x1, y1, x2, y2;


    float mEndAngle = 1.0f;

    Paint progressPaint = new Paint();
    TextPaint textPaint = new TextPaint();
    Paint incompletePaint = new Paint();
    Paint percentagePaint = new Paint();

    private float strokeWidth = 30.0f;

    private FinishAnimation finishAnimation;


    public ProgressCircle(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ProgressCircle, 0, 0);

        int textColor;
        float textSize;

        int progressColor;
        int incompleteColor;

        //let's try to get dimensions from xml
        try {
            //text
            textColor = a.getColor(R.styleable.ProgressCircle_android_textColor, getResources().getColor(R.color.colorPositive));
            textSize = a.getDimension(R.styleable.ProgressCircle_android_textSize, 100);

            //stroke
            strokeWidth = a.getDimension(R.styleable.ProgressCircle_strokeWidth, 50.0f);
            progressColor = a.getColor(R.styleable.ProgressCircle_progressColor, getResources().getColor(R.color.colorAccent));
            incompleteColor = a.getColor(R.styleable.ProgressCircle_incompleteProgressColor, getResources().getColor(R.color.colorDarken));

        } finally {
            a.recycle();
        }

        //text
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
        Typeface tf = Typeface.create("Roboto Condensed Light", Typeface.BOLD);
        textPaint.setTypeface(tf);

        percentagePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        percentagePaint.setColor(textColor);
        percentagePaint.setTextSize(textSize / 3);

        //stroke
        incompletePaint.setColor(incompleteColor);
        incompletePaint.setStrokeWidth(strokeWidth);
        incompletePaint.setStyle(Paint.Style.STROKE);
        incompletePaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        progressPaint.setColor(progressColor);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //get the minimum space available
        minDimension = w < h ? w : h;
        x1 = ((getWidth() - minDimension) / 2) + (strokeWidth / 2);
        y1 = ((getHeight() - minDimension) / 2) + (strokeWidth / 2);

        x2 = ((getWidth() + minDimension) / 2) - (strokeWidth / 2);
        y2 = ((getHeight() + minDimension) / 2) - (strokeWidth / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // the space between the strokes
        float currentAngleGap = mSweepAngle == 1.0f || mSweepAngle == 0 ? 0 : angleGap;

        //set the dimensions of the rectangle in which we draw x1 y1 x2 y2
        mOval.set(x1, y1, x2, y2);

        // draw the incomplete arc
        canvas.drawArc(mOval,
                0,
                360,
                false, incompletePaint);

        // draw the arc of progress
        canvas.drawArc(mOval,
                -startAngle + currentAngleGap,
                (mSweepAngle * 360) - currentAngleGap,
                false, progressPaint);


        drawText(canvas, textPaint, String.valueOf((int) (mSweepAngle * 100)), percentagePaint);

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }

    private void drawText(Canvas canvas, Paint paint, String text, Paint percentagePaint) {
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        Rect percentageBounds = new Rect();
        percentagePaint.getTextBounds("%", 0, 1, percentageBounds);

        int x = (canvas.getWidth() / 2) - (bounds.width() / 2) - (percentageBounds.width() / 2);
        int y = (canvas.getHeight() / 2) + (bounds.height() / 2);
        canvas.drawText(text, x, y, paint);

        canvas.drawText("%", x + bounds.width() + percentageBounds.width() / 2, y - bounds.height() + percentageBounds.height(), percentagePaint);
    }

    public void setTextColor(int color) {
        textPaint.setColor(color);
    }

    public void setProgressColor(int color) {
        progressPaint.setColor(color);
    }

    public void setIncompleteColor(int color) {
        incompletePaint.setColor(color);
    }

    public void setProgress(float progress) {
        if (progress > 1.0f || progress < 0) {
            throw new RuntimeException("Value must be between 0 and 1: " + progress);
        }

        mEndAngle = progress;

        this.invalidate();
    }

    public void startAnimation() {
        ValueAnimator anim = ValueAnimator.ofFloat(mSweepAngle, mEndAngle);

        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mSweepAngle = (Float) valueAnimator.getAnimatedValue();
                invalidate();
            }

        });
        anim.setDuration(900);
        if (mEndAngle == 1) {

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    finishAnimation.onAnimationFinish();
                }
            });
        }
        anim.setInterpolator(new AccelerateDecelerateInterpolator());

        anim.start();

    }

    public void setFinishListener(FinishAnimation fa) {
        this.finishAnimation = fa;
    }


    public interface FinishAnimation {
        void onAnimationFinish();
    }

}
