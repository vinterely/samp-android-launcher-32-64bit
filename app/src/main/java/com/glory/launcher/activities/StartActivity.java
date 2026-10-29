package com.glory.launcher.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.glory.App;
import com.glory.Utils;
import com.glory.game.R;
import com.glory.launcher.Preferences;

import es.dmoral.toasty.Toasty;

public class StartActivity extends AppCompatActivity
{
    Button download;
    EditText nickName;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_start);
        download = findViewById(R.id.button_update);
        nickName = findViewById(R.id.editTextTextPersonName);

        // username enter listener
        nickName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    actionId == EditorInfo.IME_ACTION_DONE ||
                    event.getAction() == KeyEvent.ACTION_DOWN &&
                            event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                String obj = nickName.getText().toString();
                if (obj.isEmpty()) {
                    Toasty.warning(this, getResources().getString(R.string.enterNik), Toast.LENGTH_LONG).show();
                } else if (!obj.contains("_")) {
                    Toasty.warning(this, getResources().getString(R.string.mustContains_), Toast.LENGTH_LONG).show();
                } else if (obj.length() < 4) {
                    Toasty.warning(this, getResources().getString(R.string.minLengthNik), Toast.LENGTH_LONG).show();
                } else {
                    Preferences.putString(this, Preferences.NICKNAME, obj);
                    if(!com.glory.Utils.isGameInstalled()) {
                        new android.app.AlertDialog.Builder(StartActivity.this)
                            .setTitle(getString(R.string.game_files_not_found_title))
                            .setMessage(getString(R.string.game_files_not_found_desc))
                            .setCancelable(false)
                            .setPositiveButton(getString(R.string.dialog_ok), (d, w) -> d.dismiss())
                            .show();
                    } else {
                        startActivity(new android.content.Intent(StartActivity.this, MainActivity.class));
                        finish();
                    }
                }
            }
            return false;
        });

        // continue button
        download.setOnClickListener(view -> {
            String obj = nickName.getText().toString();
            if (obj.isEmpty()) {
                Toasty.warning(StartActivity.this, getResources().getString(R.string.enterNik), Toast.LENGTH_LONG).show();
            } else if (!obj.contains("_")) {
                Toasty.warning(StartActivity.this, getResources().getString(R.string.mustContains_), Toast.LENGTH_LONG).show();
            } else if (obj.length() < 4) {
                Toasty.warning(StartActivity.this, getResources().getString(R.string.minLengthNik), Toast.LENGTH_LONG).show();
            } else {
                Preferences.putString(StartActivity.this, Preferences.NICKNAME, obj);
                if(!Utils.isGameInstalled()) {
                    new android.app.AlertDialog.Builder(StartActivity.this)
                        .setTitle(getString(R.string.game_files_not_found_title))
                        .setMessage(getString(R.string.game_files_not_found_desc))
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.dialog_ok), (d, w) -> d.dismiss())
                        .show();
                } else {
                    startActivity(new android.content.Intent(StartActivity.this, MainActivity.class));
                    finish();
                }
            }
        });
    }
}
