package com.glory.launcher.fragments;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.ValueCallback;
import android.widget.EditText;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class DonateFragment extends Fragment {
	private EditText email;
	private EditText nick;
	private TextView serverName, deposit;
	private EditText sum;

    public DonateFragment(){}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = null;
		/*view = inflater.inflate(R.layout.rustate_launcher_donate_fragment, container, false);
		nick = (EditText) view.findViewById(R.id.nik);
		email = (EditText) view.findViewById(R.id.email);
		sum = (EditText) view.findViewById(R.id.sum);
		//serverName = (TextView) view.findViewById(R.id.serverName);
		deposit = (TextView) view.findViewById(R.id.deposit);*/
		return view;
	}
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		/*String string = Preferences.getString(getActivity(), Preferences.NICKNAME);
		if (!string.isEmpty()) { nick.setText(string); }
		String string2 = Preferences.getString(getActivity(), Preferences.EMAIL);
		if (!string2.isEmpty()) { email.setText(string2); }
		deposit.setOnClickListener(view1 -> {
			view1.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.button_click));
			new Handler().postDelayed(() -> onClickDeposit(), 200);
		});
		sum.addTextChangedListener(new TextWatcher() {
			public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
			public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {}
			public void afterTextChanged(Editable editable) {
				String obj = editable.toString();
				if (obj.length() > 0 && obj.charAt(0) == '0') {
					DonateFragment.this.sum.setText(obj.substring(1));
				}
			}
		});
    }
	public void onClickDeposit() {
		if (this.nick.getText().toString().isEmpty()) {
			Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.enterNikForDonate), Toast.LENGTH_LONG).show();
		} else if (!this.nick.getText().toString().contains("_")) {
			Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.mustContains_), Toast.LENGTH_LONG).show();
		} else if (this.nick.getText().toString().length() < 4) {
			Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.minLengthNik), Toast.LENGTH_LONG).show();
		} else if (this.email.getText().toString().isEmpty()) {
			Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.enterYourEmail), Toast.LENGTH_LONG).show();
		} else if (this.sum.getText().toString().isEmpty()) {
			Toast.makeText(getActivity(), getActivity().getResources().getString(R.string.enterSumForDonate), Toast.LENGTH_LONG).show();
		}  else {
			Dialog dialog = new Dialog(getActivity());
			dialog.setContentView(R.layout.rustate_launcher_fragment_webview);
			dialog.setCancelable(true);
			if (dialog.getWindow() != null) {
				dialog.getWindow().setLayout(-1, -1);
				dialog.getWindow().setBackgroundDrawableResource(R.drawable.background_dialog_full);
				dialog.getWindow().getAttributes().dimAmount = 0.0f;
			}
			((ImageView) dialog.findViewById(R.id.close)).setOnClickListener(view -> {
				view.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.button_click));
				dialog.dismiss();
			});
			clearCookies();
			WebView webView = (WebView) dialog.findViewById(R.id.webView);
			webView.setSoundEffectsEnabled(false);
			webView.getSettings().setJavaScriptEnabled(true);
			webView.getSettings().setLoadWithOverviewMode(false);
			webView.getSettings().setUseWideViewPort(false);
			webView.getSettings().setAppCacheEnabled(true);
			webView.getSettings().setAllowContentAccess(true);
			webView.getSettings().setBuiltInZoomControls(true);
			webView.getSettings().setDisplayZoomControls(true);
			webView.getSettings().setSupportZoom(true);
			webView.getSettings().setDomStorageEnabled(true);
			webView.getSettings().setGeolocationEnabled(true);
			webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
			webView.getSettings().setAllowFileAccessFromFileURLs(true);
			webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
			webView.setLayerType(View.LAYER_TYPE_HARDWARE, (Paint) null);
			webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
			webView.getSettings().setDatabaseEnabled(true);
			webView.getSettings().setDomStorageEnabled(true);
			webView.setWebViewClient(new WebViewClient());
			CookieSyncManager.createInstance(getActivity());
			CookieManager instance = CookieManager.getInstance();
			if (Build.VERSION.SDK_INT >= 21) {
				instance.setAcceptThirdPartyCookies(webView, true);
			} else {
				instance.setAcceptCookie(true);
			}
			webView.clearCache(true);
			webView.clearHistory();
			Preferences.putString(getActivity(), Preferences.EMAIL, email.getText().toString());
			Preferences.putString(getActivity(), Preferences.NICKNAME, nick.getText().toString());
			String.format(App.getInstance().URL_DONATE, new Object[] { sum.getText(), nick.getText(), email.getText() });
			try {
				webView.postUrl
						(App.getInstance().URL_DONATE, ("&summa=" + URLEncoder.encode(
								sum.getText().toString(),
								Key.STRING_CHARSET_NAME) + "&account=" +
								URLEncoder.encode(nick.getText().toString(), Key.STRING_CHARSET_NAME) +
								"&mail=" +
								URLEncoder.encode(email.getText().toString(), Key.STRING_CHARSET_NAME) +
								"&desc=" + URLEncoder.encode(String.format
								("ÐŸÐ¾Ð¿Ð¾Ð»Ð½ÐµÐ½Ð¸Ðµ Ð¸Ð³Ñ€Ð¾Ð²Ð¾Ð³Ð¾ ÑÑ‡Ñ‘Ñ‚Ð° Ð½Ð° ÑÐµÑ€Ð²ÐµÑ€Ðµ LEGENDARY"), Key.STRING_CHARSET_NAME) +
								"&server=1").getBytes()
						);
			} catch (Exception e) {
				FirebaseCrashlytics.getInstance().recordException(e);
			}
			dialog.show();
		}*/
	}
	private void clearCookies() {
		if (Build.VERSION.SDK_INT >= 22) {
			CookieManager.getInstance().removeAllCookies((ValueCallback) null);
			CookieManager.getInstance().flush();
			return;
		}
		CookieSyncManager createInstance = CookieSyncManager.createInstance(getActivity());
		createInstance.startSync();
		CookieManager instance = CookieManager.getInstance();
		instance.removeAllCookie();
		instance.removeSessionCookie();
		createInstance.stopSync();
		createInstance.sync();
	}
}
