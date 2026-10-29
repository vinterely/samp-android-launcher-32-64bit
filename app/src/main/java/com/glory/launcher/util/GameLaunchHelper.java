package com.glory.launcher.util;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.glory.Utils;
import com.glory.game.BuildConfig;
import com.glory.launcher.activities.DataDownloadActivity;
import com.glory.launcher.activities.ForceUpdateActivity;
import com.glory.launcher.activities.MainActivity;
import com.glory.launcher.network.ManifestManager;
import com.glory.launcher.network.model.LauncherManifest;

import es.dmoral.toasty.Toasty;

public class GameLaunchHelper {

    private static final String TAG = "GameLaunchHelper";

    public interface OnNicknameUpdatedListener {
        void onNicknameUpdated(String newNickname);
    }

    public static void executePlayFlow(Activity activity, int serverId, OnNicknameUpdatedListener nickListener) {
        if (activity == null || activity.isFinishing()) return;

        // 1. Data Gate
        MainActivity.GameDataStatus status = MainActivity.evaluateGameDataStatus(activity);
        if (status == MainActivity.GameDataStatus.MISSING) {
            new AlertDialog.Builder(activity)
                .setTitle("دیتای بازی یافت نشد")
                .setMessage("برای ورود به بازی ابتدا باید دیتای بازی نصب شود.")
                .setPositiveButton("دانلود دیتای بازی", (dialog, which) -> {
                    activity.startActivity(new Intent(activity, DataDownloadActivity.class));
                })
                .setNegativeButton("انصراف", null)
                .show();
            return;
        }

        // 2. Forced Update Gate
        LauncherManifest manifest = ManifestManager.getInstance().getManifest(activity);
        if (manifest != null && manifest.getLauncher() != null) {
            int remoteVerCode = manifest.getLauncher().getVersionCode();
            int currentVerCode = BuildConfig.VERSION_CODE;
            if (remoteVerCode > 0 && remoteVerCode != currentVerCode) {
                if (BuildConfig.DEBUG) {
                    Log.i(TAG, "Forced update check (DEBUG build, not blocking): " + remoteVerCode + " vs " + currentVerCode);
                } else {
                    Intent updateIntent = new Intent(activity, ForceUpdateActivity.class);
                    updateIntent.putExtra(ForceUpdateActivity.EXTRA_APK_URL, manifest.getLauncher().getApkUrl());
                    updateIntent.putExtra(ForceUpdateActivity.EXTRA_REMOTE_VERSION_NAME, manifest.getLauncher().getVersionName());
                    updateIntent.putExtra(ForceUpdateActivity.EXTRA_REMOTE_VERSION_CODE, remoteVerCode);
                    updateIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.startActivity(updateIntent);
                    activity.finish();
                    return;
                }
            }
        }

        // 3. Nickname Validation Gate
        String nickname = NicknameHelper.getNickname(activity);
        NicknameHelper.ValidationResult val = NicknameHelper.validateNickname(activity, nickname);
        if (!val.isValid) {
            Toasty.warning(activity, val.errorMessage, Toast.LENGTH_LONG).show();
            NicknameHelper.showEditNicknameDialog(activity, newNickname -> {
                if (nickListener != null) {
                    nickListener.onNicknameUpdated(newNickname);
                }
            });
            return;
        }

        // 4. Handle Server Play (writes server & nickname to settings.ini and launches)
        Utils.handleServerPlay(activity, activity, serverId);
    }
}
