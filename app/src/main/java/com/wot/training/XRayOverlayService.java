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
        boolean isFill;

        public HitboxRect(float l, float t, float r, float b, String lbl, int col, boolean fill) {
            this.left = l; this.top = t; this.right = r; this.bottom = b;
            this.label = lbl; this.color = col; this.isFill = fill;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        createNotificationChannel();
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, "wot_xray_channel")
                    .setContentTitle("WoT X-Ray Trainer")
                    .setContentText("Şəffaf Analiz Qatmanı Aktivdir")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("WoT X-Ray Trainer")
                    .setContentText("Şəffaf Analiz Qatmanı Aktivdir")
                    .setSmallIcon(android.R.drawable.ic_menu_compass)
                    .build();
        }
        startForeground(1, notification);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        initializeDatabase();

        int layoutParamType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        // BURASI ÇOX VACİBDİR: PixelFormat.TRANSLUCENT ekranı tam şəffaf edir,
        // FLAG_NOT_TOUCHABLE isə toxunmaları birbaşa arxaya ötürür.
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

        // İdarəetmə Paneli (Düymələr üçün toxunula bilən hissə)
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
                if (overlayDrawView != null) overlayDrawView.setVisibility(View.VISIBLE);
            } else {
                toggleButton.setText("OFF");
                toggleButton.setBackgroundColor(Color.parseColor("#da3633"));
                if (overlayDrawView != null) overlayDrawView.setVisibility(View.GONE);
            }
            if (overlayDrawView != null) overlayDrawView.invalidate();
        });

        switchTankButton.setOnClickListener(v -> {
            currentTankIndex = (currentTankIndex + 1) % tankNames.length;
            String newTank = tankNames[currentTankIndex];
            switchTankButton.setText("Tank: " + newTank);
            if (overlayDrawView != null) overlayDrawView.invalidate();
        });

        try {
            windowManager.addView(overlayDrawView, overlayParams);
            windowManager.addView(buttonLayout, layoutParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        // Konumlar və Rənglər: Bənövşəyi (Mühimmat/Yanacaq), Qırmızı (Zəif nöqtə), Sarı (Cupola)
        List<HitboxRect> e4List = new ArrayList<>();
        e4List.add(new HitboxRect(150f, 300f, 700f, 550f, "T110E4: Lower Plate", Color.RED, false));
        e4List.add(new HitboxRect(300f, 180f, 500f, 280f, "Cupola", Color.YELLOW, false));
        e4List.add(new HitboxRect(400f, 350f, 620f, 480f, "MÜHİMMAT: Bənövşəyi", Color.parseColor("#9b59b6"), true));
        tankDatabase.put("T110E4", e4List);

        List<HitboxRect> t30List = new ArrayList<>();
        t30List.add(new HitboxRect(200f, 320f, 750f, 580f, "T30: Gövdə Zəifliyi", Color.RED, false));
        t30List.add(new HitboxRect(350f, 200f, 550f, 300f, "Gun Mantlet", Color.GREEN, false));
        t30List.add(new HitboxRect(450f, 380f, 680f, 500f, "YANACAQ: Bənövşəyi", Color.parseColor("#9b59b6"), true));
        tankDatabase.put("T30", t30List);

        List<HitboxRect> isuList = new ArrayList<>();
        isuList.add(new HitboxRect(180f, 280f, 780f, 550f, "ISU-152: Ön Zireh", Color.RED, false));
        isuList.add(new HitboxRect(500f, 350f, 700f, 480f, "MOTOR: Bənövşəyi", Color.parseColor("#9b59b6"), true));
        tankDatabase.put("ISU-152", isuList);
    }

    // Tamamilə şəffaf arxa fon üzərində səliqəli çərçivələr çəkən sinif
    private class OverlayDrawView extends View {
        private final Paint strokePaint;
        private final Paint fillPaint;
        private final Paint textPaint;
        private final Paint bgTextPaint;

        public OverlayDrawView(android.content.Context context) {
            super(context);
            
            // Xətt qalınlığı
            strokePaint = new Paint();
            strokePaint.setStyle(Paint.Style.STROKE);
            strokePaint.setStrokeWidth(5f);
            strokePaint.setAntiAlias(true);

            // Modulların içi üçün yarı-şəffaf dolğu
            fillPaint = new Paint();
            fillPaint.setStyle(Paint.Style.FILL);
            fillPaint.setAntiAlias(true);

            // Yazı tərzi
            textPaint = new Paint();
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(24f);
            textPaint.setAntiAlias(true);
            textPaint.setFakeBoldText(true);

            // Yazının oxunması üçün qara fon qutusu
            bgTextPaint = new Paint();
            bgTextPaint.setColor(Color.parseColor("#B3000000")); // Tünd qara fon
            bgTextPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (!isVisible) return;

            // DİQQƏT: Burada canvas-ın ümumi arxa fonunu qətiyyən rəngləmirik (`canvas.drawColor(...)` yoxdur).
            // Bu səbəbdən ekran tamamilə şəffaf qalır və arxadakı hər şey görünür!

            String activeTank = tankNames[currentTankIndex];
            List<HitboxRect> boxes = tankDatabase.get(activeTank);
            if (boxes != null) {
                for (HitboxRect rect : boxes) {
                    
                    if (rect.isFill) {
                        // Bənövşəyi modullar üçün yarı-şəffaf iç dolğu
                        fillPaint.setColor(rect.color);
                        fillPaint.setAlpha(120); // Şəffaflıq dərəcəsi
                        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, fillPaint);
                    }

                    // Sərhəd xətti
                    strokePaint.setColor(rect.color);
                    canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, strokePaint);

                    // Etiket arxası qara fon
                    float textWidth = textPaint.measureText(rect.label);
                    canvas.drawRect(rect.left, rect.top - 35f, rect.left + textWidth + 16f, rect.top, bgTextPaint);

                    // Etiket yazısı
                    canvas.drawText(rect.label, rect.left + 8f, rect.top - 10f, textPaint);
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (overlayDrawView != null) windowManager.removeView(overlayDrawView);
            if (buttonLayout != null) windowManager.removeView(buttonLayout);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
