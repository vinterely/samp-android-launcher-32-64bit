package com.glory.launcher.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.glory.game.R;
import com.glory.launcher.network.Links;
import com.glory.launcher.util.GameLaunchHelper;

public class ChooseServerActivity extends AppCompatActivity {

    private ImageView playCrimeBtn; // Left: Forum
    private ImageView playGoldBtn;  // Middle: Play Game
    private ImageView playRublBtn;  // Right: Website
    private ConstraintLayout backBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenMode();
        setContentView(R.layout.launcher_choose_server_activity);

        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        initViews();
        setupClickListeners();
    }

    private void setFullScreenMode() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    private void initViews() {
        playCrimeBtn = findViewById(R.id.play_crime_btn);
        playGoldBtn = findViewById(R.id.play_gold_btn);
        playRublBtn = findViewById(R.id.play_rubl_btn);
        backBtn = findViewById(R.id.back_btn);
    }

    private void setupClickListeners() {
        // Left Card: Forum ("ورود به انجمن")
        playCrimeBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Links.FORUM));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(ChooseServerActivity.this, "امکان باز کردن انجمن وجود ندارد", Toast.LENGTH_SHORT).show();
            }
        });

        // Middle Card: Play Game ("ورود به بازی") - uses shared GameLaunchHelper
        playGoldBtn.setOnClickListener(v -> {
            GameLaunchHelper.executePlayFlow(ChooseServerActivity.this, 1, null);
        });

        // Right Card: Website ("ورود به سایت")
        playRublBtn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(Links.SITE));
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(ChooseServerActivity.this, "امکان باز کردن وب‌سایت وجود ندارد", Toast.LENGTH_SHORT).show();
            }
        });

        // Bottom Button: Back to Main Menu ("بازگشت به منوی اصلی")
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(ChooseServerActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ChooseServerActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
