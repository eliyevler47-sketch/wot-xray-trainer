package com.wot.training;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XRayOverlayService extends Service {
    
    private WindowManager windowManager;
    private OverlayDrawView overlayDrawView;
    private Button toggleButton;
    private Button switchTankButton;
    private LinearLayout buttonLayout;
    
    private WindowManager.LayoutParams overlayParams;
    private WindowManager.LayoutParams layoutParams;
    
    private boolean isVisible = true;
    private int currentTankIndex = 0;
    private String[] tankNames = {"T110E4", "T30", "ISU-152"};
    private Map<String, List<HitboxRect>> tankDatabase;

    public static class HitboxRect {
        float left, top, right, bottom;
        String label;
        int color;

        public HitboxRect(float l, float t, float r, float b, String lbl, int col) {
            this.left = l; this.top = t; this.right = r; this.bottom = b;
            this.label = lbl; this.color = col;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        createNotificationChannel();
        Notification notification = new Notification.Builder(this, "wot_xray_channel")
                .setContentTitle("WoT X-Ray Trainer")
                .setContentText("Aktivdir")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
        startForeground(1, notification);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        initializeDatabase();

        int layoutParamType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP | Gravity.START;
        overlayDrawView = new OverlayDrawView(this);

        buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

        toggleButton = new Button(this);
        toggleButton.setText("ON");
        toggleButton.setBackgroundColor(Color.parseColor("#238636"));
        toggleButton.setTextColor(Color.WHITE);
        toggleButton.setTextSize(11f);

        switchTankButton = new Button(this);
        switchTankButton.setText("Tank: " + tankNames[currentTankIndex]);
        switchTankButton.setBackgroundColor(Color.parseColor("#1f6feb"));
        switchTankButton.setTextColor(Color.WHITE);
        switchTankButton.setTextSize(11f);

        buttonLayout.addView(toggleButton);
        buttonLayout.addView(switchTankButton);

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                layoutParamType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = 30;
        layoutParams.y = 100;

        toggleButton.setOnClickListener(v -> {
            isVisible = !isVisible;
            if (isVisible) {
                toggleButton.setText("ON");
                toggleButton.setBackgroundColor(Color.parseColor("#238636"));
                overlayDrawView.setVisibility(View.VISIBLE);
            } else {
                toggleButton.setText("OFF");
                toggleButton.setBackgroundColor(Color.parseColor("#da3633"));
                overlayDrawView.setVisibility(View.GONE);
            }
            overlayDrawView.invalidate();
        });

        switchTankButton.setOnClickListener(v -> {
            currentTankIndex = (currentTankIndex + 1) % tankNames.length;
            String newTank = tankNames[currentTankIndex];
            switchTankButton.setText("Tank: " + newTank);
            overlayDrawView.invalidate();
        });

        windowManager.addView(overlayDrawView, overlayParams);
        windowManager.addView(buttonLayout, layoutParams);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "wot_xray_channel",
                    "WoT X-Ray Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initializeDatabase() {
        tankDatabase = new HashMap<>();

        List<HitboxRect> e4List = new ArrayList<>();
        e4List.add(new HitboxRect(250f, 400f, 800f, 650f, "T110E4: Lower Plate", Color.RED));
        e4List.add(new HitboxRect(380f, 250f, 650f, 380f, "T110E4: Cupola", Color.YELLOW));
        tankDatabase.put("T110E4", e4List);

        List<HitboxRect> t30List = new ArrayList<>();
        t30List.add(new HitboxRect(250f, 420f, 800f, 680f, "T30: Hull Zəifliyi", Color.RED));
        t30List.add(new HitboxRect(350f, 280f, 680f, 400f, "T30: Gun Mantlet", Color.GREEN));
        tankDatabase.put("T30", t30List);

        List<HitboxRect> isuList = new ArrayList<>();
        isuList.add(new HitboxRect(220f, 350f, 820f, 650f, "ISU-152: Ön Korpus", Color.RED));
        tankDatabase.put("ISU-152", isuList);
    }

    private class OverlayDrawView extends View {
        private Paint strokePaint, textPaint;

        public OverlayDrawView(android.content.Context context) {
            super(context);
            strokePaint = new Paint();
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(6f);

            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(26f);
            textPaint.setAntiAlias(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!isVisible) return;

            String activeTank = tankNames[currentTankIndex];
            List<HitboxRect> boxes = tankDatabase.get(activeTank);
            if (boxes != null) {
                for (HitboxRect rect : boxes) {
                    strokePaint.setColor(rect.color);
                    canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, strokePaint);
                    canvas.drawText(rect.label, rect.left, rect.top - 15f, textPaint);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayDrawView != null) windowManager.removeView(overlayDrawView);
        if (buttonLayout != null) windowManager.removeView(buttonLayout);
    }
}
