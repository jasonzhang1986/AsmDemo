package com.sogou.teemo.asmdemo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class TestView extends View {
    Paint paint = new Paint();
    int width = 300;
    int left = 10;
    int top = 10;
    public TestView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.parseColor("#0000ff"));
        canvas.drawArc(left, top, left+width, top+width, -90, 270, true, paint);
    }
}
