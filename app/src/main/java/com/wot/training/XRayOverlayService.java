package com.wot.training;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XRayOverlayService extends Service {
    
    private WindowManager windowManager;
    private OverlayDrawView overlayDrawView;
    private Button toggleButton;
    private LinearLayout controlPanel;
    private WindowManager.LayoutParams overlayParams;
    private WindowManager.LayoutParams buttonParams;
    private WindowManager.LayoutParams panelParams;
    
    private boolean isVisible = true;
    private String currentTank = "T110E4";
    private Map<String, List<HitboxRect>> tankDatabase;

    public static class HitboxRect {
        float left, top, right, bottom;
        String label;
        int color;

        public HitboxRect(float l, float t, float r, float b, String lbl, int col) {
            this.left = l;
            this.top = t;
            this.right = r;
            this.bottom = b;
            this.label = lbl;
            this.color = col;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        initializeDatabase();

        int layoutParamType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        // 1. Hitbox Çəkmə Qatı (Full Screen Transparent Overlay)
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

        overlayDrawView = new OverlayDrawView(this);

        // 2. Əsas İdarəetmə Düyməsi (Aç/Bağla)
        toggleButton = new Button(this);
        toggleButton.setText("X-RAY: ON [" + currentTank + "]");
        toggleButton.setBackgroundColor(Color.parseColor("#238636"));
        toggleButton.setTextColor(Color.WHITE);
        toggleButton.setTextSize(11f);

        buttonParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        buttonParams.gravity = Gravity.TOP | Gravity.START;
        buttonParams.x = 30;
        buttonParams.y = 100;

        toggleButton.setOnClickListener(v -> {
            isVisible = !isVisible;
            if (isVisible) {
                toggleButton.setText("X-RAY: ON [" + currentTank + "]");
                toggleButton.setBackgroundColor(Color.parseColor("#238636"));
                overlayDrawView.setVisibility(View.VISIBLE);
            } else {
                toggleButton.setText("X-RAY: OFF");
                toggleButton.setBackgroundColor(Color.parseColor("#da3633"));
                overlayDrawView.setVisibility(View.GONE);
            }
            overlayDrawView.invalidate();
        });

        // Toxunaraq düyməni səhifədə gəzdirmək (Draggable Touch Listener)
        toggleButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = buttonParams.x;
                        initialY = buttonParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false; // Click hadisəsinin işləməsinə icazə verir
                    case MotionEvent.ACTION_MOVE:
                        buttonParams.x = initialX + (int) (event.getRawX() - initialTouchX);
                        buttonParams.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(toggleButton, buttonParams);
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(overlayDrawView, overlayParams);
        windowManager.addView(toggleButton, buttonParams);
    }

    private void initializeDatabase() {
        tankDatabase = new HashMap<>();

        // T110E4 Hitbox Koordinatları (Lower Plate, Cupola, Hull)
        List<HitboxRect> e4List = new ArrayList<>();
        e4List.add(new HitboxRect(320f, 450f, 700f, 580f, "Lower Plate (Həssas)", Color.RED));
        e4List.add(new HitboxRect(420f, 280f, 600f, 360f, "Cupola (Komandir)", Color.YELLOW));
        e4List.add(new HitboxRect(250f, 380f, 770f, 650f, "Main Hull", Color.BLUE));
        tankDatabase.put("T110E4", e4List);

        // T30 Hitbox Koordinatları
        List<HitboxRect> t30List = new ArrayList<>();
        t30List.add(new HitboxRect(300f, 500f, 720f, 680f, "Hull Zəifliyi", Color.RED));
        t30List.add(new HitboxRect(380f, 300f, 640f, 420f, "Gun Mantlet (GÜCLÜ)", Color.GREEN));
        tankDatabase.put("T30", t30List);

        // ISU-152 Hitbox Koordinatları
        List<HitboxRect> isuList = new ArrayList<>();
        isuList.add(new HitboxRect(280f, 350f, 740f, 600f, "Ön Zireh (Açıq)", Color.RED));
        isuList.add(new HitboxRect(200f, 400f, 280f, 580f, "Sol Yan (HE üçün)", Color.MAGENTA));
        tankDatabase.put("ISU-152", isuList);
    }

    private class OverlayDrawView extends View {
        private Paint strokePaint;
        private Paint textPaint;

        public OverlayDrawView(android.content.Context context) {
            super(context);
            strokePaint = new Paint();
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(5f);

            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(24f);
            textPaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!isVisible) return;

            List<HitboxRect> boxes = tankDatabase.get(currentTank);
            if (boxes != null) {
                for (HitboxRect rect : boxes) {
                    strokePaint.setColor(rect.color);
                    // Hitbox çərçivəsini çək
                    canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, strokePaint);
                    // Zəif nöqtə adını yaz
                    canvas.drawText(rect.label, rect.left, rect.top - 10f, textPaint);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayDrawView != null) windowManager.removeView(overlayDrawView);
        if (toggleButton != null) windowManager.removeView(toggleButton);
    }
}
