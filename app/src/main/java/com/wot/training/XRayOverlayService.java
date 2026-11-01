package com.wot.training;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

public class XRayOverlayService extends Service {
    private WindowManager windowManager;
    private View overlayView;
    private Button toggleButton;
    private WindowManager.LayoutParams overlayParams;
    private WindowManager.LayoutParams buttonParams;
    private boolean isVisible = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        int layoutParamType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        // 1. Qırmızı Hitbox Çərçivə Katmanı
        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP | Gravity.START;

        overlayView = new View(this) {
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                if (!isVisible) return;

                android.graphics.Paint paint = new android.graphics.Paint();
                paint.setColor(android.graphics.Color.RED);
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeWidth(6f);

                // Tankın qarajda durduğu mərkəzi hissəyə uyğunlaşdırılmış dinamik qutu
                // Buradaki rəqəmləri ekran ölçünə görə dəyişə bilərsən
                canvas.drawRect(250f, 220f, 750f, 650f, paint);
            }
        };

        // 2. Açıb-Bağlamaq üçün Sürüklənən Düymə (Floating Toggle Button)
        toggleButton = new Button(this);
        toggleButton.setText("X-Ray: ON");
        toggleButton.setBackgroundColor(android.graphics.Color.parseColor("#238636"));
        toggleButton.setTextColor(android.graphics.Color.WHITE);
        toggleButton.setTextSize(12f);

        buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = 50;
        buttonParams.y = 150;

        // Düyməyə basanda çərçivəni açıb-bağlasın
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isVisible = !isVisible;
                if (isVisible) {
                    toggleButton.setText("X-Ray: ON");
                    toggleButton.setBackgroundColor(android.graphics.Color.parseColor("#238636"));
                    overlayView.setVisibility(View.VISIBLE);
                } else {
                    toggleButton.setText("X-Ray: OFF");
                    toggleButton.setBackgroundColor(android.graphics.Color.parseColor("#da3633"));
                    overlayView.setVisibility(View.GONE);
                }
            }
        });

        // Düyməni ekranda barmaqla sürükləmək üçün toxunma mexanizmi
        toggleButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = buttonParams.x;
                        initialY = buttonParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        buttonParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        buttonParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(toggleButton, buttonParams);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(overlayView, overlayParams);
        windowManager.addView(toggleButton, buttonParams);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) windowManager.removeView(overlayView);
        if (toggleButton != null) windowManager.removeView(toggleButton);
    }
}
