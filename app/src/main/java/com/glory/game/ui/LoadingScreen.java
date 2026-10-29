package com.glory.game.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.glory.game.R;
import com.tuyenmonkey.mkloader.MKLoader;

public class LoadingScreen {

    public interface LoadingCompleteListener {
        void onLoadingComplete();
    }

    private Activity activity;
    private ConstraintLayout mainLayout;
    private MKLoader loader;
    private TextView gameStatusText;
    private Handler handler;
    private LoadingCompleteListener loadingCompleteListener;
    private boolean isVisible = false;
    private Runnable hideRunnable;

    public LoadingScreen(Activity activity) {
        this.activity = activity;
        this.handler = new Handler(Looper.getMainLooper());

        initLoadingScreen();
    }

    private void initLoadingScreen() {
        activity.runOnUiThread(() -> {
            try {
                mainLayout = (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.game_load_screen, null);

                loader = mainLayout.findViewById(R.id.loader);
                gameStatusText = mainLayout.findViewById(R.id.game_status);

                if (gameStatusText != null) {
                    gameStatusText.setText("در حال ورود به بازی...");
                }

                ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
                rootView.addView(mainLayout, new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                ));

                mainLayout.setVisibility(View.GONE);
                mainLayout.setAlpha(0f);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void setLoadingCompleteListener(LoadingCompleteListener listener) {
        this.loadingCompleteListener = listener;
    }

    public void hide() {
        handler.post(() -> {
            if (mainLayout != null && isVisible) {
                mainLayout.animate()
                        .alpha(0f)
                        .setDuration(4000)
                        .withEndAction(() -> {
                            mainLayout.setVisibility(View.GONE);
                            isVisible = false;
                            if (loadingCompleteListener != null) {
                                loadingCompleteListener.onLoadingComplete();
                            }
                        })
                        .start();

                if (hideRunnable != null) {
                    handler.removeCallbacks(hideRunnable);
                    hideRunnable = null;
                }
            }
        });
    }

    public void hideImmediately() {
        handler.post(() -> {
            if (mainLayout != null) {
                mainLayout.setVisibility(View.GONE);
                mainLayout.setAlpha(0f);
                isVisible = false;
            }

            if (hideRunnable != null) {
                handler.removeCallbacks(hideRunnable);
                hideRunnable = null;
            }
        });
    }

    public void show() {
        handler.post(() -> {
            if (mainLayout != null && !isVisible) {
                mainLayout.setAlpha(1f);
                mainLayout.setVisibility(View.VISIBLE);

                isVisible = true;

                hideRunnable = () -> {
                    if (isVisible) {
                        hide();
                    }
                };
                handler.postDelayed(hideRunnable, 50000); // 30 ÑÐµÐºÑƒÐ½Ð´
            }
        });
    }

    public void setLoadingText(String text) {
        handler.post(() -> {
            if (gameStatusText != null) {
                    gameStatusText.setText("در حال ورود به بازی...");
            }
        });
    }

    public void hideWhenHudShown() {
        handler.post(() -> {
            if (isVisible) {
                hide();
            }
        });
    }

    public void showForDuration(long durationMillis) {
        handler.post(() -> {
            if (mainLayout != null && !isVisible) {
                mainLayout.setAlpha(1f);
                mainLayout.setVisibility(View.VISIBLE);

                isVisible = true;

                hideRunnable = () -> {
                    if (isVisible) {
                        hide();
                    }
                };
                handler.postDelayed(hideRunnable, durationMillis);
            }
        });
    }

    public boolean isVisible() {
        return isVisible;
    }

    public View getView() {
        return mainLayout;
    }

    public TextView getStatusTextView() {
        return gameStatusText;
    }

    public MKLoader getLoader() {
        return loader;
    }

    public void destroy() {
        handler.post(() -> {
            if (mainLayout != null) {
                mainLayout.setVisibility(View.GONE);
                ViewGroup parent = (ViewGroup) mainLayout.getParent();
                if (parent != null) {
                    parent.removeView(mainLayout);
                }
            }

            if (hideRunnable != null) {
                handler.removeCallbacks(hideRunnable);
                hideRunnable = null;
            }
        });
    }
}