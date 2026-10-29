package com.glory.launcher.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.glory.Config;
import com.glory.Utils;
import com.glory.launcher.dialogs.DiagDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import com.glory.game.BuildConfig;
import com.glory.game.R;
import com.glory.launcher.network.Links;
import com.glory.launcher.network.ManifestManager;
import com.glory.launcher.network.model.LauncherManifest;
import com.glory.launcher.util.GameLaunchHelper;
import com.glory.launcher.util.NicknameHelper;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import es.dmoral.toasty.Toasty;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public enum GameDataStatus {
        COMPLETE,
        INCOMPLETE,
        MISSING
    }

    private TextView tvNickname;
    private View layoutNickname;
    private ImageButton btnEditNickname;
    private View btnSettings;

    // Social Rail
    private ImageButton btnSocialTelegram;
    private ImageButton btnSocialSoroush;
    private ImageButton btnSocialAparat;
    private ImageButton btnSocialWebsite;

    // Data Status Card
    private View cardGameData;
    private ImageView ivStatusIcon;
    private TextView tvStatusDesc;
    private Button btnDownloadData;

    // Primary Action
    private View btnPlay;

    private boolean doubleBackToExitPressedOnce = false;
    private final Handler handler = new Handler();
    private final ExecutorService dataCheckExecutor = Executors.newSingleThreadExecutor();
    private volatile GameDataStatus cachedStatus = GameDataStatus.MISSING;
    private volatile boolean cachedIsInterrupted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupClickListeners();

        // Request storage permission if needed
        checkStoragePermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNickname();
        refreshGameDataStatusAsync();
        checkManifestAndUpdate();
    }

    private void initViews() {
        tvNickname = findViewById(R.id.tv_nickname);
        layoutNickname = findViewById(R.id.layout_nickname);
        btnEditNickname = findViewById(R.id.btn_edit_nickname);
        btnSettings = findViewById(R.id.btn_settings);

        btnSocialTelegram = findViewById(R.id.btn_social_telegram);
        btnSocialSoroush = findViewById(R.id.btn_social_soroush);
        btnSocialAparat = findViewById(R.id.btn_social_aparat);
        btnSocialWebsite = findViewById(R.id.btn_social_website);

        cardGameData = findViewById(R.id.card_game_data);
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        tvStatusDesc = findViewById(R.id.tv_status_desc);
        btnDownloadData = findViewById(R.id.btn_download_data);

        btnPlay = findViewById(R.id.btn_play);
    }

    private void setupClickListeners() {
        // Nickname Edit
        View.OnClickListener editNickListener = v -> {
            NicknameHelper.showEditNicknameDialog(MainActivity.this, newNickname -> {
                if (tvNickname != null) {
                    tvNickname.setText(newNickname);
                }
            });
        };
        if (layoutNickname != null) layoutNickname.setOnClickListener(editNickListener);
        if (btnEditNickname != null) btnEditNickname.setOnClickListener(editNickListener);

        // 3-second long press on status card / launcher top title opens DiagDialog
        View.OnTouchListener diagTouchListener = new View.OnTouchListener() {
            private final Handler diagHandler = new Handler(Looper.getMainLooper());
            private final Runnable diagRunnable = () -> {
                DiagDialog.show(MainActivity.this);
            };

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    diagHandler.postDelayed(diagRunnable, 3000);
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    diagHandler.removeCallbacks(diagRunnable);
                }
                return false;
            }
        };
        if (cardGameData != null) cardGameData.setOnTouchListener(diagTouchListener);
        if (tvStatusDesc != null) tvStatusDesc.setOnTouchListener(diagTouchListener);

        // Settings / Menu Stub
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> openSettings());
        }

        // Social Links
        if (btnSocialTelegram != null) {
            btnSocialTelegram.setOnClickListener(v -> openUrl(Links.TELEGRAM_CHANNEL));
        }
        if (btnSocialSoroush != null) {
            btnSocialSoroush.setOnClickListener(v -> openUrl(Links.SOROUSH));
        }
        if (btnSocialAparat != null) {
            btnSocialAparat.setOnClickListener(v -> openUrl(Links.APARAT));
        }
        if (btnSocialWebsite != null) {
            btnSocialWebsite.setOnClickListener(v -> openUrl(Links.SITE));
        }

        // Game Data Download & Card Click
        View.OnClickListener downloadClickListener = v -> {
            startActivity(new Intent(MainActivity.this, DataDownloadActivity.class));
        };
        if (btnDownloadData != null) {
            btnDownloadData.setOnClickListener(downloadClickListener);
        }
        if (cardGameData != null) {
            cardGameData.setOnClickListener(v -> {
                if (cachedStatus == GameDataStatus.MISSING || cachedStatus == GameDataStatus.INCOMPLETE) {
                    startActivity(new Intent(MainActivity.this, DataDownloadActivity.class));
                }
            });
        }

        // Primary Play Button (uses shared GameLaunchHelper)
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> onPlayClicked());
        }
    }

    private void refreshNickname() {
        if (tvNickname != null) {
            String nick = NicknameHelper.getNickname(this);
            if (nick.isEmpty()) {
                tvNickname.setText(R.string.home_nickname_hint);
            } else {
                tvNickname.setText(nick);
            }
        }
    }

    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void openUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toasty.error(this, "امکان باز کردن پیوند وجود ندارد", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkManifestAndUpdate() {
        ManifestManager.getInstance().fetchManifest(this, new ManifestManager.ManifestCallback() {
            @Override
            public void onSuccess(LauncherManifest manifest) {
                checkForceUpdate(manifest);
            }

            @Override
            public void onError(Exception exception, LauncherManifest fallbackManifest) {
                if (fallbackManifest != null) {
                    checkForceUpdate(fallbackManifest);
                }
            }
        });
    }

    private void checkForceUpdate(LauncherManifest manifest) {
        if (manifest == null || manifest.getLauncher() == null) return;
        int remoteVerCode = manifest.getLauncher().getVersionCode();
        int currentVerCode = BuildConfig.VERSION_CODE;

        if (remoteVerCode > 0 && remoteVerCode != currentVerCode) {
            if (BuildConfig.DEBUG) {
                Log.i(TAG, "Launcher version mismatch (DEBUG build, not blocking): " + currentVerCode + " vs " + remoteVerCode);
            } else {
                Intent intent = new Intent(this, ForceUpdateActivity.class);
                intent.putExtra(ForceUpdateActivity.EXTRA_APK_URL, manifest.getLauncher().getApkUrl());
                intent.putExtra(ForceUpdateActivity.EXTRA_REMOTE_VERSION_NAME, manifest.getLauncher().getVersionName());
                intent.putExtra(ForceUpdateActivity.EXTRA_REMOTE_VERSION_CODE, remoteVerCode);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }
    }

    private void onPlayClicked() {
        GameLaunchHelper.executePlayFlow(this, 1, newNickname -> {
            if (tvNickname != null) tvNickname.setText(newNickname);
        });
    }

    private void refreshGameDataStatusAsync() {
        dataCheckExecutor.execute(() -> {
            GameDataStatus status = getGameDataStatus();
            cachedStatus = status;
            cachedIsInterrupted = hasInterruptedInstall(new File(Config.GAME_PATH));
            runOnUiThread(() -> updateGameDataStatusUI(status, cachedIsInterrupted));
        });
    }

    public GameDataStatus getGameDataStatus() {
        return evaluateGameDataStatus(this);
    }

    public static boolean hasInterruptedInstall(File gameDir) {
        if (gameDir == null || !gameDir.exists()) return false;
        File tmpDir = new File(gameDir, ".glory_tmp");
        if (!tmpDir.exists() || !tmpDir.isDirectory()) return false;
        File[] files = tmpDir.listFiles();
        if (files == null || files.length == 0) return false;
        for (File f : files) {
            String name = f.getName().toLowerCase(Locale.US);
            if (name.endsWith(".zip") || name.endsWith(".part") || name.endsWith(".tmp") || name.contains("extract")) {
                return true;
            }
        }
        return false;
    }

    public static GameDataStatus evaluateGameDataStatus(Context context) {
        String extRoot = "";
        try {
            File ext = Environment.getExternalStorageDirectory();
            if (ext != null) {
                extRoot = ext.getAbsolutePath();
            }
        } catch (Exception ignored) {}

        String[] candidatePaths = new String[] {
            Config.GAME_PATH,
            "/storage/emulated/0/GloryMobile/",
            "/storage/emulated/0/GloryMobile/files/",
            extRoot.isEmpty() ? "" : extRoot + "/GloryMobile/",
            extRoot.isEmpty() ? "" : extRoot + "/GloryMobile/files/",
            "/storage/emulated/0/GTA/",
            "/storage/emulated/0/GTA/files/",
            extRoot.isEmpty() ? "" : extRoot + "/Android/data/com.gloryrp.app/files/",
            extRoot.isEmpty() ? "" : extRoot + "/Android/data/com.glory.game/files/"
        };

        File selectedDir = null;
        int maxPresentCount = 0;
        boolean selectedInterrupted = false;

        String[] topFolders = new String[] {"anim", "data", "models", "texdb", "SAMP"};

        for (String p : candidatePaths) {
            if (p == null || p.isEmpty()) continue;
            if (!p.endsWith("/")) p += "/";
            File dir = new File(p);
            if (!dir.exists() || !dir.isDirectory()) continue;

            int count = 0;
            for (String folderName : topFolders) {
                File f = new File(dir, folderName);
                if (f.exists() && f.isDirectory()) {
                    String[] list = f.list();
                    if (list != null && list.length > 0) {
                        count++;
                    }
                }
            }

            boolean interrupted = hasInterruptedInstall(dir);

            if (count > maxPresentCount || (count == maxPresentCount && selectedDir == null) || interrupted) {
                maxPresentCount = count;
                selectedDir = dir;
                selectedInterrupted = interrupted;
                Config.setGamePath(p);
            }

            if (count == 5) {
                break;
            }
        }

        if (selectedDir == null) {
            File defaultDir = new File(Config.GAME_PATH);
            if (hasInterruptedInstall(defaultDir)) {
                return GameDataStatus.INCOMPLETE;
            }
            return GameDataStatus.MISSING;
        }

        Utils.prepareSampDir(selectedDir.getAbsolutePath());

        if (maxPresentCount == 5) {
            if (selectedInterrupted) {
                return GameDataStatus.INCOMPLETE;
            }
            return GameDataStatus.COMPLETE;
        } else if (maxPresentCount > 0) {
            return GameDataStatus.INCOMPLETE;
        } else {
            if (selectedInterrupted) {
                return GameDataStatus.INCOMPLETE;
            }
            return GameDataStatus.MISSING;
        }
    }

    private void updateGameDataStatusUI(GameDataStatus status, boolean isInterrupted) {
        if (ivStatusIcon == null || tvStatusDesc == null || btnDownloadData == null) return;

        switch (status) {
            case COMPLETE:
                ivStatusIcon.setImageResource(R.drawable.ic_data_complete);
                tvStatusDesc.setText(R.string.home_data_complete);
                btnDownloadData.setVisibility(View.GONE);
                break;

            case INCOMPLETE:
                ivStatusIcon.setImageResource(R.drawable.ic_data_incomplete);
                if (isInterrupted) {
                    tvStatusDesc.setText("نصب ناتمام - ادامه دانلود");
                    btnDownloadData.setText("ادامه نصب");
                    btnDownloadData.setVisibility(View.VISIBLE);
                } else {
                    tvStatusDesc.setText(R.string.home_data_incomplete);
                    btnDownloadData.setVisibility(View.GONE);
                }
                break;

            case MISSING:
            default:
                ivStatusIcon.setImageResource(R.drawable.ic_data_missing);
                tvStatusDesc.setText(R.string.home_data_missing);
                btnDownloadData.setText("دانلود دیتا");
                btnDownloadData.setVisibility(View.VISIBLE);
                break;
        }
    }

    private void checkStoragePermission() {
        if (!Utils.hasStoragePermission(this)) {
            new AlertDialog.Builder(this)
                .setTitle("دسترسی حافظه لازم است")
                .setMessage("برای خواندن دیتای بازی، لطفاً مجوز دسترسی به فایل‌ها را به برنامه بدهید.")
                .setCancelable(true)
                .setPositiveButton("باز کردن تنظیمات", (dialog, which) -> {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } else {
                            ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                        }
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                })
                .setNegativeButton("بعداً", (dialog, which) -> dialog.dismiss())
                .show();
        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toasty.info(this, "برای خروج دوباره دکمه بازگشت را بزنید", Toast.LENGTH_SHORT).show();

        handler.postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        dataCheckExecutor.shutdownNow();
    }
}
