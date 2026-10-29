package com.glory.launcher.fragments;

import static com.glory.Config.GAME_PATH;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.glory.App;
import com.glory.Utils;
import com.glory.game.R;
import com.glory.launcher.Preferences;
import com.glory.launcher.activities.DownloadActivity;
import com.glory.launcher.activities.MainActivity;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;

import es.dmoral.toasty.Toasty;

public class SettingsFragment extends Fragment
{
	private EditText nickName;
	private EditText nickIp;

	private EditText nickport;
	private Button repairGame, btnVK, btnYT, btnDiscord;
	private ToggleButton btnNotifications;
	private TextView whoDeveloped;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view;
		view = inflater.inflate(R.layout.fragment_settings, container, false);
		nickName = view.findViewById(R.id.editTextTextNickName);
		nickIp = view.findViewById(R.id.editTextTextIP);
		nickport = view.findViewById(R.id.editTextTextPort);
		repairGame = view.findViewById(R.id.repairGame);
		btnNotifications = view.findViewById(R.id.switch1);
	//	btnFpsCounter = view.findViewById(R.id.switch2);
		btnVK = view.findViewById(R.id.button_vk);
		btnYT = view.findViewById(R.id.button_yt);
		btnDiscord = view.findViewById(R.id.button_discord);
		whoDeveloped = view.findViewById(R.id.textView18);
		((ImageView) view.findViewById(R.id.back))
				.setOnClickListener(
						new View.OnClickListener() {
							public void onClick(View v) {

								startActivity(new Intent(getActivity(), MainActivity.class));
							}
						});
		// Ð²Ð²Ð¾Ð´ Ð½Ð¸ÐºÐ°
		nickName.setText(Preferences.getString(getActivity(), Preferences.NICKNAME));
		nickName.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH ||
					actionId == EditorInfo.IME_ACTION_DONE ||
					event.getAction() == KeyEvent.ACTION_DOWN &&
							event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				String obj = nickName.getText().toString();
				if (obj.isEmpty()) {
					Toasty.warning(getActivity(), getResources().getString(R.string.enterNik), Toast.LENGTH_LONG).show();
				} else if (!obj.contains("_")) {
					Toasty.warning(getActivity(), getResources().getString(R.string.mustContains_), Toast.LENGTH_LONG).show();
				} else if (obj.length() < 4) {
					Toasty.warning(getActivity(), getResources().getString(R.string.minLengthNik), Toast.LENGTH_LONG).show();
				} else {
					Preferences.putString(getActivity(), Preferences.NICKNAME, obj);
					Toasty.success(getActivity(), "Ð’Ð°Ñˆ Ð½Ð¸ÐºÐ½ÐµÐ¹Ð¼ ÑÐ¾Ñ…Ñ€Ð°Ð½ÐµÐ½: "+obj).show();
					File settings = new File(GAME_PATH+"SAMP/settings.ini");
					if(settings.exists()) {
						try {
							Wini w = new Wini(settings);
							w.put("client", "name", obj);
							w.store();
						} catch (IOException e) {
							Utils.writeLog(getActivity(), 'e', "ÐžÑˆÐ¸Ð±ÐºÐ°: "+e.getMessage());
						}
					} else {
						Utils.writeLog(getActivity(), 'e', "Ð½ÐµÑ‚ Ñ„Ð°Ð¹Ð»Ð° settings.ini");
					}
				}
			}
			return false;
		});
		// Ð²Ð²Ð¾Ð´ Ð¸Ð¿
		nickIp.setText(Preferences.getString(getActivity(), Preferences.IP));
		nickIp.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH ||
					actionId == EditorInfo.IME_ACTION_DONE ||
					event.getAction() == KeyEvent.ACTION_DOWN &&
							event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				String obj = nickIp.getText().toString();
				if (obj.isEmpty()) {
				/*	Toasty.warning(getActivity(), getResources().getString(R.string.enterNik), Toast.LENGTH_LONG).show();
				} else if (!obj.contains("_")) {*/
				/*	Toasty.warning(getActivity(), getResources().getString(R.string.mustContains_), Toast.LENGTH_LONG).show();
				} else if (obj.length() < 4) {*/
					Toasty.warning(getActivity(), getResources().getString(R.string.minLengthNik), Toast.LENGTH_LONG).show();
				} else {
					Preferences.putString(getActivity(), Preferences.IP, obj);
					Toasty.success(getActivity(), "Ð’Ð°Ñˆ ip ÑÐ¾Ñ…Ñ€Ð°Ð½ÐµÐ½: "+obj).show();
					File settings = new File(GAME_PATH+"SAMP/settings.ini");
					if(settings.exists()) {
						try {
							Wini w = new Wini(settings);
							w.put("client", "ip", obj);
							w.store();
						} catch (IOException e) {
							Utils.writeLog(getActivity(), 'e', "ÐžÑˆÐ¸Ð±ÐºÐ°: "+e.getMessage());
						}
					} else {
						Utils.writeLog(getActivity(), 'e', "Ð½ÐµÑ‚ Ñ„Ð°Ð¹Ð»Ð° settings.ini");
					}
				}
			}
			return false;
		});
		// Ð²Ð²Ð¾Ð´ Ð¿Ð¾Ñ€Ñ‚
		nickport.setText(Preferences.getString(getActivity(), Preferences.PORT));
		nickport.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH ||
					actionId == EditorInfo.IME_ACTION_DONE ||
					event.getAction() == KeyEvent.ACTION_DOWN &&
							event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				String obj = nickport.getText().toString();
				if (obj.isEmpty()) {
					/*	Toasty.warning(getActivity(), getResources().getString(R.string.enterNik), Toast.LENGTH_LONG).show();
				} else if (!obj.contains("_")) {
					Toasty.warning(getActivity(), getResources().getString(R.string.mustContains_), Toast.LENGTH_LONG).show();
				} else if (obj.length() < 4) {*/
					Toasty.warning(getActivity(), getResources().getString(R.string.minLengthNik), Toast.LENGTH_LONG).show();
				} else {
					Preferences.putString(getActivity(), Preferences.PORT, obj);
					Toasty.success(getActivity(), "Ð’Ð°Ñˆ port ÑÐ¾Ñ…Ñ€Ð°Ð½ÐµÐ½: "+obj).show();
					File settings = new File(GAME_PATH+"SAMP/settings.ini");
					if(settings.exists()) {
						try {
							Wini w = new Wini(settings);
							w.put("client", "port", obj);
							w.store();
						} catch (IOException e) {
							Utils.writeLog(getActivity(), 'e', "ÐžÑˆÐ¸Ð±ÐºÐ°: "+e.getMessage());
						}
					} else {
						Utils.writeLog(getActivity(), 'e', "Ð½ÐµÑ‚ Ñ„Ð°Ð¹Ð»Ð° settings.ini");
					}
				}
			}
			return false;
		});
		// ÐºÐ½Ð¾Ð¿ÐºÐ° Ð¿Ð¾Ñ‡Ð¸Ð½Ð¸Ñ‚ÑŒ Ð¸Ð³Ñ€Ñƒ (Ð¿ÐµÑ€ÐµÑƒÑÑ‚Ð°Ð½Ð¾Ð²Ð¸Ñ‚ÑŒ)
		repairGame.setOnClickListener(view1 -> {
			delete(new File(GAME_PATH));
			App.getInstance().downloadID = App.INSTALL_TYPE_GAMEFILES;
			Intent intent = new Intent(getActivity(), DownloadActivity.class);
			intent.putExtras(getActivity().getIntent());
			startActivity(intent);
			getActivity().finish();
		});
		// ÑƒÐ²ÐµÐ´Ð¾Ð¼Ð»ÐµÐ½Ð¸Ñ Ð¸ ÑÑ‡ÐµÑ‚Ñ‡Ð¸Ðº Ñ„Ð¿Ñ
		btnNotifications.setChecked(Preferences.getBoolean(getActivity(), Preferences.NOTIFICATION, true));
		btnNotifications.setOnCheckedChangeListener((compoundButton, b) -> Preferences.putBoolean(getActivity(), Preferences.NOTIFICATION, b));
		//
		/*	btnFpsCounter.setChecked(false);
		File f = new File(GAME_PATH+"SAMP/settings.ini");
		if(f.exists()) {
			Wini w = null;
			try {
				w = new Wini(f);
				Integer fps = new Integer(w.get("gui", "fpscounter"));
				if(fps == 1) {
					btnFpsCounter.setChecked(true);
				}
				w.store();
			} catch (IOException e) {
				Utils.writeLog(getActivity(), 'e', "Ð¾ÑˆÐ¸Ð±ÐºÐ°: "+e.getMessage());
			}
		}

		btnFpsCounter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
				Wini w = null;
				try {
					w = new Wini(f);
					if(b) {
						w.put("gui", "fpscounter", 1);
					} else w.put("gui", "fpscounter", 0);
					w.store();
				} catch (IOException e) {
					Utils.writeLog(getActivity(), 'e', "Ð¾ÑˆÐ¸Ð±ÐºÐ°: "+e.getMessage());
				}
			}
		});*/
		// ÐºÑ‚Ð¾ Ñ€Ð°Ð·Ñ€Ð°Ð±Ð¾Ñ‚Ð°Ð»
		whoDeveloped.setOnClickListener(view15 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/9t8wgbAzM8"))));
		whoDeveloped.setText(Html.fromHtml("<font color=\"#6A7BA7\">  </font><font color=\"#6A7BA7\"><a href=\"\"></a></font><font color=\"#6A7BA7\"></font>"), TextView.BufferType.SPANNABLE);
		// ÐºÐ½Ð¾Ð¿ÐºÐ¸ ÑÐ¾Ñ† ÑÐµÑ‚ÐµÐ¹
		btnVK.setOnClickListener(view14 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(App.getInstance().URL_VK))));
		btnYT.setOnClickListener(view13 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(App.getInstance().URL_YOUTUBE))));
		btnDiscord.setOnClickListener(view12 -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(App.getInstance().URL_DISCORD))));
		return view;
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	private void delete(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				for (File delete : file.listFiles()) {
					delete(delete);
				}
				file.delete();
				return;
			}
			file.delete();
		}
	}

	private void loadFragment(Fragment fragment) {
		FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
		transaction.replace(R.id.fragment_place, fragment);
		transaction.addToBackStack(null);
		transaction.commit();
	}
}
