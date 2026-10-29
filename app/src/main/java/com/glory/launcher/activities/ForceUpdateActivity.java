package com.glory.launcher.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.glory.game.BuildConfig;
import com.glory.game.R;
import com.glory.launcher.network.ManifestManager;
import com.glory.launcher.network.model.LauncherManifest;
import com.glory.launcher.util.PersianUtils;

public class ForceUpdateActivity extends AppCompatActivity {

    public static final String EXTRA_APK_URL = "extra_apk_url";
    public static final String EXTRA_REMOTE_VERSION_NAME = "extra_remote_version_name";
    public static final String EXTRA_REMOTE_VERSION_CODE = "extra_remote_version_code";

    private String apkUrl = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_force_update);

        TextView tvVersionPill = findViewById(R.id.tv_version_pill);
        Button btnDownloadApk = findViewById(R.id.btn_download_apk);

        Intent intent = getIntent();
        apkUrl = intent != null ? intent.getStringExtra(EXTRA_APK_URL) : null;
        String remoteVerName = intent != null ? intent.getStringExtra(EXTRA_REMOTE_VERSION_NAME) : null;

        if (apkUrl == null || apkUrl.isEmpty()) {
            LauncherManifest manifest = ManifestManager.getInstance().getManifest(this);
            if (manifest != null && manifest.getLauncher() != null) {
                apkUrl = manifest.getLauncher().getApkUrl();
                remoteVerName = manifest.getLauncher().getVersionName();
            }
        }

        String currentVer = BuildConfig.VERSION_NAME;
        if (remoteVerName == null || remoteVerName.isEmpty()) {
            remoteVerName = "جدید";
        }

        String pillText = "نسخه فعلی: " + PersianUtils.toPersianDigits(currentVer)
                + "  ←  نسخه جدید: " + PersianUtils.toPersianDigits(remoteVerName);
        tvVersionPill.setText(pillText);

        btnDownloadApk.setOnClickListener(v -> {
            if (apkUrl != null && !apkUrl.isEmpty()) {
                try {
                    Intent viewIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(viewIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "امکان باز کردن لینک دانلود وجود ندارد", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "لینک دانلود مشخص نشده است", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Enforce non-dismissable: do not call super
    }
}
