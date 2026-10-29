package com.glory.launcher.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.glory.game.R;
import com.glory.launcher.network.ManifestManager;
import com.glory.launcher.network.model.LauncherManifest;
import com.glory.launcher.services.DataDownloadService;
import com.glory.launcher.util.PersianUtils;

public class DataDownloadActivity extends AppCompatActivity implements DataDownloadService.DownloadListener {

    private TextView tvProgressPercent;
    private TextView tvProgressBytes;
    private ProgressBar pbDownload;
    private TextView tvDownloadSpeed;
    private TextView tvDownloadEta;
    private TextView tvStatusMessage;

    private TextView step1Badge;
    private TextView step1Text;
    private TextView step2Badge;
    private TextView step2Text;
    private TextView step3Badge;
    private TextView step3Text;
    private TextView step4Badge;
    private TextView step4Text;

    private LinearLayout layoutActions;
    private Button btnPauseResume;
    private Button btnCancel;
    private Button btnRetry;
    private Button btnFinish;

    private boolean isPaused = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_data_download);

        initViews();
        setupListeners();

        LauncherManifest manifest = ManifestManager.getInstance().getManifest(this);
        DataDownloadService.start(this, manifest);
    }

    private void initViews() {
        tvProgressPercent = findViewById(R.id.tv_progress_percent);
        tvProgressBytes = findViewById(R.id.tv_progress_bytes);
        pbDownload = findViewById(R.id.pb_download);
        tvDownloadSpeed = findViewById(R.id.tv_download_speed);
        tvDownloadEta = findViewById(R.id.tv_download_eta);
        tvStatusMessage = findViewById(R.id.tv_status_message);

        step1Badge = findViewById(R.id.step_1_badge);
        step1Text = findViewById(R.id.step_1_text);
        step2Badge = findViewById(R.id.step_2_badge);
        step2Text = findViewById(R.id.step_2_text);
        step3Badge = findViewById(R.id.step_3_badge);
        step3Text = findViewById(R.id.step_3_text);
        step4Badge = findViewById(R.id.step_4_badge);
        step4Text = findViewById(R.id.step_4_text);

        layoutActions = findViewById(R.id.layout_actions);
        btnPauseResume = findViewById(R.id.btn_pause_resume);
        btnCancel = findViewById(R.id.btn_cancel);
        btnRetry = findViewById(R.id.btn_retry);
        btnFinish = findViewById(R.id.btn_finish);
    }

    private void setupListeners() {
        btnPauseResume.setOnClickListener(v -> {
            if (isPaused) {
                isPaused = false;
                btnPauseResume.setText("توقف");
                DataDownloadService.resume(this);
            } else {
                isPaused = true;
                btnPauseResume.setText("ادامه");
                DataDownloadService.pause(this);
            }
        });

        btnCancel.setOnClickListener(v -> {
            DataDownloadService.cancel(this);
            finish();
        });

        btnRetry.setOnClickListener(v -> {
            layoutActions.setVisibility(View.VISIBLE);
            btnRetry.setVisibility(View.GONE);
            tvStatusMessage.setTextColor(0xFFDFD7EE);
            LauncherManifest manifest = ManifestManager.getInstance().getManifest(this);
            DataDownloadService.start(this, manifest);
        });

        btnFinish.setOnClickListener(v -> {
            setResult(RESULT_OK);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        DataDownloadService.addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        DataDownloadService.removeListener(this);
    }

    @Override
    public void onProgress(DataDownloadService.ProgressInfo info) {
        if (info == null) return;

        runOnUiThread(() -> {
            updateSteps(info.step);

            tvProgressPercent.setText(PersianUtils.toPersianDigits(info.percent) + "٪");
            pbDownload.setProgress(info.percent);

            String formattedDownloaded = PersianUtils.formatBytes(info.bytesDownloaded);
            String formattedTotal = PersianUtils.formatBytes(info.totalBytes);
            tvProgressBytes.setText(formattedDownloaded + " از " + formattedTotal);

            tvDownloadSpeed.setText(PersianUtils.formatSpeed(info.speedBytesPerSec));
            if (info.etaSeconds > 0) {
                tvDownloadEta.setText(PersianUtils.formatDuration(info.etaSeconds) + " باقی‌مانده");
            } else {
                tvDownloadEta.setText("--:--");
            }

            if (info.statusText != null && !info.statusText.isEmpty()) {
                tvStatusMessage.setText(info.statusText);
            }

            if (info.step == DataDownloadService.DownloadStep.PAUSED) {
                isPaused = true;
                btnPauseResume.setText("ادامه");
            } else if (info.step == DataDownloadService.DownloadStep.DOWNLOADING) {
                isPaused = false;
                btnPauseResume.setText("توقف");
            }
        });
    }

    @Override
    public void onComplete() {
        runOnUiThread(() -> {
            setStepDone(step1Badge, step1Text);
            setStepDone(step2Badge, step2Text);
            setStepDone(step3Badge, step3Text);
            setStepDone(step4Badge, step4Text);

            tvProgressPercent.setText(PersianUtils.toPersianDigits("100") + "٪");
            pbDownload.setProgress(100);
            tvDownloadSpeed.setText("تکمیل شد");
            tvDownloadEta.setText("۰:۰۰");
            tvStatusMessage.setText("دیتای بازی با موفقیت نصب شد. آماده اجرای بازی!");
            tvStatusMessage.setTextColor(0xFF2ECC71);

            layoutActions.setVisibility(View.GONE);
            btnRetry.setVisibility(View.GONE);
            btnFinish.setVisibility(View.VISIBLE);
        });
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> {
            tvStatusMessage.setText(message != null ? message : "خطایی در دانلود یا نصب دیتا رخ داد");
            tvStatusMessage.setTextColor(0xFFEF4444);
            layoutActions.setVisibility(View.GONE);
            btnRetry.setVisibility(View.VISIBLE);
            btnFinish.setVisibility(View.GONE);
        });
    }

    private void updateSteps(DataDownloadService.DownloadStep step) {
        switch (step) {
            case CHECKING_SPACE:
            case DOWNLOADING:
                setStepActive(step1Badge, step1Text);
                setStepPending(step2Badge, step2Text);
                setStepPending(step3Badge, step3Text);
                setStepPending(step4Badge, step4Text);
                break;

            case VERIFYING:
                setStepDone(step1Badge, step1Text);
                setStepActive(step2Badge, step2Text);
                setStepPending(step3Badge, step3Text);
                setStepPending(step4Badge, step4Text);
                break;

            case EXTRACTING:
                setStepDone(step1Badge, step1Text);
                setStepDone(step2Badge, step2Text);
                setStepActive(step3Badge, step3Text);
                setStepPending(step4Badge, step4Text);
                break;

            case COMPLETED:
                setStepDone(step1Badge, step1Text);
                setStepDone(step2Badge, step2Text);
                setStepDone(step3Badge, step3Text);
                setStepDone(step4Badge, step4Text);
                break;

            default:
                break;
        }
    }

    private void setStepActive(TextView badge, TextView text) {
        badge.setBackgroundResource(R.drawable.glory_bg_step_circle_active);
        badge.setTextColor(0xFFFFFFFF);
        text.setTextColor(0xFFF5A30A);
    }

    private void setStepDone(TextView badge, TextView text) {
        badge.setBackgroundResource(R.drawable.glory_bg_step_circle_done);
        badge.setTextColor(0xFFFFFFFF);
        text.setTextColor(0xFF2ECC71);
    }

    private void setStepPending(TextView badge, TextView text) {
        badge.setBackgroundResource(R.drawable.glory_bg_step_circle_pending);
        badge.setTextColor(0xFFA69BBB);
        text.setTextColor(0xFF7A6899);
    }

    @Override
    public void onBackPressed() {
        // Safe exit
        super.onBackPressed();
    }
}
