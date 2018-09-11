package com.android.systemui.recent;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class TextProgressBar extends ProgressBar {
    String mText = "";
    int mProgress;
    Paint mPaint;

    public TextProgressBar(Context context) {
        super(context);
        initText();
    }

    public TextProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initText();
    }

    public TextProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initText();
    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
    }

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mPaint == null) {
            initText();
        }
        Rect rect = new Rect();
        mPaint.getTextBounds(mText, 0, mText.length(), rect);
        int x = (getWidth() / 2) - rect.centerX();
        int y = (getHeight() / 2) - rect.centerY();
        canvas.drawText(mText, x, y, mPaint);
    }

    private void initText() {
        mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
    }

    public void setText(String str) {
        mText = str;
    }
}