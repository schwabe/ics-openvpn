package de.blinkt.openvpn.fragments;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ViewConfiguration;
import android.widget.SeekBar;

public class SeekBarTicks extends SeekBar {
    private Paint mTickPaint;
    private float mTickHeight;


    public SeekBarTicks(Context context, AttributeSet attrs) {
        super (context, attrs);

        initTicks (context, attrs, android.R.attr.seekBarStyle);
    }


    public SeekBarTicks(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initTicks (context, attrs, defStyle);

        /*mTickHeight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                tickHeightDP,
                ctx.getResources().getDisplayMetrics()); */
    }

    private void initTicks(Context context, AttributeSet attrs, int defStyle) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                new int[] { android.R.attr.secondaryProgress }, defStyle, 0);


        int tickColor = a.getColor(0, android.R.color.white);
        mTickPaint = new Paint();
        mTickPaint.setColor( context.getResources().getColor(tickColor));
        a.recycle();
    }


    @Override
    protected synchronized void onDraw(Canvas canvas) {
        drawTicks(canvas);
        super.onDraw(canvas);
    }

    private void drawTicks(Canvas canvas) {

        final int available = getWidth() - getPaddingLeft() - getPaddingRight();
        int tickSpacing = available / (getMax() );

        for (int i = 1; i < getMax(); i++) {
            final float x = getPaddingLeft() + i * tickSpacing;
            canvas.drawLine(x, getPaddingTop(), x, getHeight()-getPaddingBottom(), mTickPaint);
        }
    }
}
