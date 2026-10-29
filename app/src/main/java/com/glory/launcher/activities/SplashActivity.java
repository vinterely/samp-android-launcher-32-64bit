package com.glory.launcher.activities;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.glory.App;
import com.glory.Utils;
import com.glory.game.BuildConfig;
import com.glory.game.R;
import com.glory.launcher.Preferences;
import com.glory.launcher.fragments.UpdateClient;
import com.glory.launcher.network.ApiService;
import com.glory.launcher.network.Links;
import com.glory.launcher.network.Server;
import com.glory.launcher.network.Story;
import com.google.firebase.messaging.FirebaseMessaging;
import com.liulishuo.filedownloader.FileDownloader;

import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {
    // permissions
    private static final int MAIN_PERMISSIONS_REQUEST_CODE = 300;
    private Handler handler;
    String[] main_permissions = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler = new Handler();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                Utils.writeLog(this, 'i', "app sucessfully started!");
                Init();
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, MAIN_PERMISSIONS_REQUEST_CODE);
            }
        } else {
            if (EasyPermissions.hasPermissions(this, main_permissions)) {
                Utils.writeLog(this, 'i', "app sucessfully started!");
                Init();
            } else {
                Utils.writeLog(this, 'i', "app not have full permissions!!!");
                EasyPermissions.requestPermissions(this,
                        "Ð¿Ñ€Ð¸Ð»Ð¾Ð¶ÐµÐ½Ð¸ÑŽ Ñ‚Ñ€ÐµÐ±ÑƒÐµÑ‚ÑÑ Ñ€Ð°Ð·Ñ€ÐµÑˆÐµÐ½Ð¸Ðµ Ð½Ð° Ð·Ð°Ð¿Ð¸ÑÑŒ Ð² Ð¿Ð°Ð¼ÑÑ‚ÑŒ",
                        MAIN_PERMISSIONS_REQUEST_CODE, main_permissions);
            }
        }
    }
    private void Init()
    {
        FileDownloader.setup(this);
        if (!Preferences.getBoolean(this, Preferences.FIRST_START)) {
            Preferences.putBoolean(SplashActivity.this, Preferences.FIRST_START, true);
        }
        startLauncher();
    }

    // Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ° Ð°Ð¿Ð¸, Ð½Ð¾Ð²Ð¾ÑÑ‚ÐµÐ¹, ÑÐµÑ€Ð²ÐµÑ€Ð¾Ð², Ð¸Ñ‚Ð´
    private void loadAPI() {
        ApiService.getInstance().getApiService().getLinks().enqueue(new Callback<Links>() {
            public void onResponse(Call<Links> call, Response<Links> response) {
                if(response.isSuccessful())
                {
                    if(response.body() != null) {
                        // Ð²ÐµÑ€ÑÐ¸Ð¸
                        App.getInstance().targetClientVersion = response.body().getTargetClientVersion();
                        App.getInstance().targetGameFilesVersion = response.body().getTargetGameFilesVersion();
                        // ÑÑÑ‹Ð»ÐºÐ¸ Ð½Ð° Ñ„Ð°Ð¹Ð»Ñ‹
                        App.getInstance().URL_CLIENT = response.body().getUrlClient();
                        App.getInstance().URL_GAME_FILES = response.body().getUrlFiles();
                        App.getInstance().URL_GAME_FILES_UPDATE = response.body().getUrlFilesUpdate();
                        // Ñ€ÐµÑÑƒÑ€ÑÑ‹ Ð¿Ñ€Ð¾ÐµÐºÑ‚Ð°
                        App.getInstance().URL_FORUM = response.body().getUrlForum();
                        App.getInstance().URL_DONATE = response.body().getUrlDonate();
                        // ÑÐ¾Ñ† ÑÐµÑ‚Ð¸
                        App.getInstance().URL_VK = response.body().getUrlVKontakte();
                        App.getInstance().URL_DISCORD = response.body().getUrlDiscord();
                        App.getInstance().URL_YOUTUBE = response.body().getUrlYoutube();
                        // Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ° Ð¾ÑÑ‚Ð°Ð»ÑŒÐ½Ð¾Ð³Ð¾
                        loadServers();
                    }
                } else {
                    Toasty.error(SplashActivity.this, "ÐÐµ Ð¿Ð¾Ð»ÑƒÑ‡Ð°ÐµÑ‚ÑÑ ÑƒÑÑ‚Ð°Ð½Ð¾Ð²Ð¸Ñ‚ÑŒ ÑÐ¾ÐµÐ´Ð¸Ð½ÐµÐ½Ð¸Ðµ Ñ ÑÐµÑ€Ð²ÐµÑ€Ð¾Ð¼ BRILLIANT GAMES. ÐŸÐ¾Ð¿Ñ€Ð¾Ð±ÑƒÐ¹Ñ‚Ðµ Ð¿Ð¾Ð·Ð´Ð½ÐµÐµ.", Toasty.LENGTH_LONG).show();
                    Utils.writeLog(SplashActivity.this, 'e', "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐ¿Ð¸ÑÐºÐ° API: "+response.errorBody());
                }
            }
            public void onFailure(Call<Links> call, Throwable th) {
                Utils.writeLog(SplashActivity.this, 'e', "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐ¿Ð¸ÑÐ¾ÐºÐ° API: "+th.getMessage());
                if(!Utils.isInternetConnected(SplashActivity.this))
                {
                    Toasty.error(SplashActivity.this, "Ð£ Ð²Ð°Ñ Ð½ÐµÑ‚ Ð¿Ð¾Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ Ðº Ð¸Ð½Ñ‚ÐµÑ€Ð½ÐµÑ‚Ñƒ.", Toasty.LENGTH_LONG).show();
                    return;
                }
                Toasty.error(SplashActivity.this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð¿Ð¾Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ Ðº ÑÐµÑ€Ð²ÐµÑ€Ñƒ. Ð¡ÐµÑ€Ð²ÐµÑ€ Ð½ÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿ÐµÐ½.", Toasty.LENGTH_LONG).show();
            }
        });
    }
    private void loadServers() {
        ApiService.getInstance().getApiService().getServers().enqueue(new Callback<ArrayList<Server>>() {
            public void onResponse(Call<ArrayList<Server>> call, Response<ArrayList<Server>> response) {
                if(response.isSuccessful())
                {
                    if (response.body() != null) {
                        App.getInstance().serverList.clear();
                        App.getInstance().serverList.addAll(response.body());
                        if (App.getInstance().serverListener != null) {
                            App.getInstance().serverListener.onChange();
                        }
                        loadStories();
                    }
                } else {
                    Utils.writeLog(SplashActivity.this, 'e', "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐ¿Ð¸ÑÐºÐ° ÑÐµÑ€Ð²ÐµÑ€Ð¾Ð²: "+response.errorBody());
                }
            }
            public void onFailure(Call<ArrayList<Server>> call, Throwable th) {
                Utils.writeLog(SplashActivity.this, 'e', "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐ¿Ð¸ÑÐ¾ÐºÐ° ÑÐµÑ€Ð²ÐµÑ€Ð¾Ð²: "+th.getMessage());
                if(!Utils.isInternetConnected(SplashActivity.this))
                {
                    Toasty.error(SplashActivity.this, "Ð£ Ð²Ð°Ñ Ð½ÐµÑ‚ Ð¿Ð¾Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ Ðº Ð¸Ð½Ñ‚ÐµÑ€Ð½ÐµÑ‚Ñƒ.", Toasty.LENGTH_LONG).show();
                    return;
                }
                Toasty.error(SplashActivity.this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð¿Ð¾Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ Ðº ÑÐµÑ€Ð²ÐµÑ€Ñƒ. Ð¡ÐµÑ€Ð²ÐµÑ€ Ð½ÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿ÐµÐ½.", Toasty.LENGTH_LONG).show();
            }
        });
    }

    private void loadStories() { // Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ° Ð½Ð¾Ð²Ð¾ÑÑ‚ÐµÐ¹
        ApiService.getInstance().getApiService().getStories().enqueue(new Callback<ArrayList<Story>>() {
            public void onResponse(Call<ArrayList<Story>> call, Response<ArrayList<Story>> response) {
                if(response.isSuccessful()) {
                    if (response.body() != null) {
                        App.getInstance().stories.clear();
                        App.getInstance().stories.addAll(response.body());
                        runOnUiThread(() -> startLauncher());
                    }
                } else {
                    Utils.writeLog(SplashActivity.this, 'e', "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐ¿Ð¸ÑÐºÐ° Ð½Ð¾Ð²Ð¾ÑÑ‚ÐµÐ¹: "+response.errorBody());
                }
            }
            public void onFailure(Call<ArrayList<Story>> call, Throwable th) {
                Utils.writeLog(SplashActivity.this, 'e', "ÐžÑˆÐ¸Ð±ÐºÐ° Ð·Ð°Ð³Ñ€ÑƒÐ·ÐºÐ¸ ÑÐ¿Ð¸ÑÐ¾ÐºÐ° Ð½Ð¾Ð²Ð¾ÑÑ‚ÐµÐ¹: "+th.getMessage());
                if(!Utils.isInternetConnected(SplashActivity.this))
                {
                    Toasty.error(SplashActivity.this, "Ð£ Ð²Ð°Ñ Ð½ÐµÑ‚ Ð¿Ð¾Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ Ðº Ð¸Ð½Ñ‚ÐµÑ€Ð½ÐµÑ‚Ñƒ.", Toasty.LENGTH_LONG).show();
                    return;
                }
                Toasty.error(SplashActivity.this, "ÐžÑˆÐ¸Ð±ÐºÐ° Ð¿Ð¾Ð´ÐºÐ»ÑŽÑ‡ÐµÐ½Ð¸Ñ Ðº ÑÐµÑ€Ð²ÐµÑ€Ñƒ. Ð¡ÐµÑ€Ð²ÐµÑ€ Ð½ÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿ÐµÐ½.", Toasty.LENGTH_LONG).show();
            }
        });
    }

    //-------------------------------------------------------------------

    private void startLauncher() {
        handler.postDelayed((Runnable) () -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
            finish();
        }, 1000);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MAIN_PERMISSIONS_REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Init();
                } else {
                    finish();
                }
            }
        }
    }

    public void onPermissionsGranted(int requestCode, List<String> list) {
        if(requestCode == MAIN_PERMISSIONS_REQUEST_CODE) {
            Init();
        }
    }
    public void onPermissionsDenied(int requestCode, List<String> list) {
        if(requestCode == MAIN_PERMISSIONS_REQUEST_CODE) {
            Utils.writeLog(SplashActivity.this, 'i', "Ð¾ÑˆÐ¸Ð±ÐºÐ° Ð²Ñ‹Ð´Ð°Ñ‡Ð¸ Ñ€Ð°Ð·Ñ€ÐµÑˆÐµÐ½Ð¸Ð¹");
            //finish();
        }
    }
}