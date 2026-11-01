package com.wot.training.overlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

public class TankOverlayView extends View {
    private Paint ammoPaint, enginePaint, textPaint, borderPaint;

    public TankOverlayView(Context context) {
        super(context);
        init();
    }

    private void init() {
        ammoPaint = new Paint();
        ammoPaint.setColor(Color.parseColor("#99FF3333"));
        ammoPaint.setStyle(Paint.Style.FILL);

        enginePaint = new Paint();
        enginePaint.setColor(Color.parseColor("#99FFAA00"));
        enginePaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3f);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(28f);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(350, 400, 500, 480, ammoPaint);
        canvas.drawRect(350, 400, 500, 480, borderPaint);
        canvas.drawText("AMMO RACK", 350, 390, textPaint);

        canvas.drawRect(520, 420, 680, 520, enginePaint);
        canvas.drawRect(520, 420, 680, 520, borderPaint);
        canvas.drawText("MOTOR", 520, 410, textPaint);
    }
}
