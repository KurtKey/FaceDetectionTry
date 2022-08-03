package com.directxhoop.tryfacedetection;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class Draw extends View {
    private Rect rect;
    private String text;
    private float left;
    private float top;
    private float right;
    private float bottom;

    public Draw(Context context,float left, float top, float right, float bottom, Rect rect, String text) {
        super(context);
        this.rect=rect;
        this.text=text;
        this.left=left;
        this.top=top;
        this.right=right;
        this.bottom=bottom;
        init();
    }
    Paint boundryPaint;
    Paint textPaint;

    // instantiate paints and giving characteristics
    private void init() {
        boundryPaint= new Paint();
        boundryPaint.setColor(Color.RED);
        boundryPaint.setStrokeWidth(10f);
        boundryPaint.setStyle(Paint.Style.STROKE);

        textPaint= new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(50f);
        textPaint.setStyle(Paint.Style.FILL);
    }

    //draw rect and text
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawText(text,rect.centerX(),rect.centerY(),textPaint);
        canvas.drawRect(left,top,right,bottom,boundryPaint);
    }

}
