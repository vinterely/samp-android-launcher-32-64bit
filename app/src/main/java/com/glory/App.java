package com.glory;

import android.app.Activity;
import android.net.ConnectivityManager;

import androidx.multidex.MultiDexApplication;

import com.glory.launcher.network.Server;
import com.glory.launcher.network.ServerListener;
import com.glory.launcher.network.Story;

import java.io.File;
import java.util.ArrayList;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import android.util.Log;

import es.dmoral.toasty.Toasty;

public class App extends MultiDexApplication
{
	private static App instance;
	private FirebaseAnalytics mFirebaseAnalytics;
	//
	public Integer downloadID = null;
	public static final Integer INSTALL_TYPE_CLIENT = 1;
	public static final Integer INSTALL_TYPE_GAMEFILES = 2;
	public static final Integer INSTALL_TYPE_UPDATE_GAMEFILES = 3;
	//
	private String tempNick;
	private String currentGPU;
	//
	public String errorInfo, errorText;
	//
	public static final String ADRENO_TEGRA = "dxt";
	public static final String ETC = "etc";
	public static final String MALI = "etc";//"mali";
	public static final String POWER_VR = "pvr";
	//
	public ArrayList<Server> serverList;
	public ServerListener serverListener;
	public ArrayList<Story> stories;
	// urls - links
	public String URL_CLIENT;
	public String URL_GAME_FILES;
	public String URL_GAME_FILES_UPDATE;
	public String URL_VK;
	public String URL_DISCORD;
	public String URL_YOUTUBE;
	public String URL_FORUM;
	public String URL_DONATE;
	public Integer targetClientVersion;
	public Integer targetGameFilesVersion;
	//
	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				Log.e("CRASH", "Uncaught exception in thread " + thread.getName(), throwable);
				throwable.printStackTrace();
				android.os.Process.killProcess(android.os.Process.myPid());
				System.exit(1);
			}
		});

		try {
			FirebaseApp.initializeApp(this);
			mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
			Log.d("GoldParaha", "Firebase initialized successfully");
			if (mFirebaseAnalytics != null) {
				setUserProperties();
			} else {
				Log.w("GoldParaha", "FirebaseAnalytics instance is null");
			}
		} catch (Exception e) {
			Log.e("GoldParaha", "Error initializing Firebase: " + e.getMessage(), e);
		}

		initLogger();
		serverList = new ArrayList<>();
		stories = new ArrayList<>();
	}

	private void setUserProperties() {
		try {
			mFirebaseAnalytics.setUserProperty("app_version", "0.8.2.1");
			mFirebaseAnalytics.setUserProperty("device_architecture", "ARMx64");
			mFirebaseAnalytics.setUserProperty("build_type", "release");
			Log.d("GoldParaha", "User properties set successfully");
		} catch (Exception e) {
			Log.e("GoldParaha", "Error setting user properties: " + e.getMessage(), e);
		}
	}

	private void initLogger () {
		try {
			File appDir = new File(Config.APP_PATH);
			if (!appDir.exists()) {
				appDir.mkdirs();
			}
			File logcat = new File(Config.APP_PATH + "/logcat.txt");
			if (logcat.exists()) {
				logcat.delete();
			}
			logcat.createNewFile();
			Runtime runtime = Runtime.getRuntime();
			runtime.exec("logcat -f " + logcat.getAbsolutePath());
		} catch (Exception e) {
			Utils.writeLog(getInstance(), 'e', "InitLogger error: "+e.getMessage());
		}
		try {
			((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).setNetworkPreference(ConnectivityManager.DEFAULT_NETWORK_PREFERENCE);
		}
		catch (Exception unused) {}
	}

	public static App getInstance() { return instance; } // get app context
	//
	public String getTempNickName() { return this.tempNick; } // Ð²Ñ€ÐµÐ¼ÐµÐ½Ð½Ñ‹Ð¹ Ð½Ð¸Ðº
	public void setTempNickName(String str) { this.tempNick = str; }
	//
	public ArrayList<Server> getServerList() { return this.serverList; } // ÑÐ¿Ð¸ÑÐ¾Ðº ÑÐµÑ€Ð²ÐµÑ€Ð¾Ð²
	public ArrayList<Story> getStories() { return this.stories; } // ÑÐ¿Ð¸ÑÐ¾Ðº ÑÑ‚Ð¾Ñ€Ð¸ÑÐ¾Ð²
	//
	public void setGPU(String str) {
		currentGPU = str;
	}

	public String getGPU() {
		return currentGPU;
	}
	//
	public static boolean isExternalStorageAvailable(Activity activity) {
		try {
			activity.getExternalFilesDir((String) null).getAbsolutePath();
			return true;
		} catch (Exception unused) {
			Toasty.error(activity, "Ð½ÐµÑ‚ Ð´Ð¾ÑÑ‚ÑƒÐ¿Ð° Ðº Ñ…Ñ€Ð°Ð½Ð¸Ð»Ð¸Ñ‰Ñƒ!", Toasty.LENGTH_LONG);
			return false;
		}
	}

	public static boolean isAppInstalledFromMarket(String marketNamePacket) {
		if(getInstance().getPackageManager().
				getInstallerPackageName(getInstance().getPackageName()).equals(marketNamePacket)) {
			return true;
		}
		return false;
	}

	// Yandex Metrika metodi OLIB TASHLANDI
}