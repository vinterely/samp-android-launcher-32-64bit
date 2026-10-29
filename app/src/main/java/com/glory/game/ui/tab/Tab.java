package com.glory.game.ui.tab;

import android.app.Activity;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.glory.game.R;
import com.glory.launcher.util.Util;
import com.nvidia.devtech.NvEventQueueActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tab {
    public ImageView mClearImg;
    public ConstraintLayout mMainLayout;
    public TextView mOnline;
    public List<PlayerData> mPlayerData;
    public RecyclerView mPlayersList;
    public EditText mSearch;
    public ImageView mSearchImg;
    public ImageView mCloseButton;
    private TabAdapter mTabAdapter;
    public Activity activity;
    private TextView mServerName;

    public Tab(Activity activity) {
        this.activity = activity;

        ConstraintLayout mainLayout = (ConstraintLayout) activity.findViewById(R.id.tab_main);
        mMainLayout = mainLayout;

        mServerName = (TextView) activity.findViewById(R.id.tab_server_name);
        EditText searchEditText = (EditText) activity.findViewById(R.id.search_view);
        mSearch = searchEditText;

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (mTabAdapter != null) {
                    mTabAdapter.getFilter().filter(editable);
                }
                setVisibleIconInSearchView(editable.toString());
            }
        });

        mSearchImg = activity.findViewById(R.id.icon_search_view);
        mClearImg = activity.findViewById(R.id.icon_clear_search_text);

        mClearImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearEditText();
            }
        });

        mSearchImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearEditText();
            }
        });

        mOnline = (TextView) activity.findViewById(R.id.players_list_value_players);
        mPlayersList = (RecyclerView) activity.findViewById(R.id.players_list);

        mPlayersList.setLayoutManager(new LinearLayoutManager(activity));

        mPlayerData = new ArrayList<>();
        mTabAdapter = new TabAdapter(mPlayerData);
        mPlayersList.setAdapter(mTabAdapter);

        mCloseButton = (ImageView) activity.findViewById(R.id.close_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(AnimationUtils.loadAnimation(activity, R.anim.click));
                close();
            }
        });

        Util.HideLayout(mainLayout, false);
    }

    public void clearEditText() {
        mSearch.setText("");
        setVisibleIconInSearchView("");
    }

    public void close() {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && mSearch != null) {
            imm.hideSoftInputFromWindow(mSearch.getWindowToken(), 0);
        }

        Util.HideLayout(mMainLayout, true);

        if (NvEventQueueActivity.getInstance() != null) {
            NvEventQueueActivity.getInstance().onTabClose();
        }

        clearData();
    }

    private void clearData() {
        if (mPlayersList != null) {
            mPlayersList.getRecycledViewPool().clear();
        }
        if (mPlayerData != null) {
            mPlayerData.clear();
        }
        if (mTabAdapter != null) {
            mTabAdapter.notifyDataSetChanged();
        }
    }

    public void clearStat() {
        if (mPlayerData != null) {
            mPlayerData.clear();
        }
        if (mTabAdapter != null) {
            mTabAdapter.notifyDataSetChanged();
        }
        updateOnlineCount();
    }

    public void setStat(Integer id, String name, Integer level, Integer ping) {
        if (mPlayerData == null) {
            mPlayerData = new ArrayList<>();
        }

        boolean playerExists = false;
        for (int i = 0; i < mPlayerData.size(); i++) {
            PlayerData player = mPlayerData.get(i);
            if (player.getId() == id) {
                player.setName(name);
                player.setLevel(level);
                player.setPing(ping);
                playerExists = true;

                if (mTabAdapter != null) {
                    mTabAdapter.notifyItemChanged(i);
                }
                break;
            }
        }

        if (!playerExists) {
            mPlayerData.add(new PlayerData(id, name, level, ping));
            if (mTabAdapter != null) {
                mTabAdapter.notifyItemInserted(mPlayerData.size() - 1);
            }
        }

        updateOnlineCount();
    }

    public void setStats(List<PlayerData> players) {
        if (mPlayerData == null) {
            mPlayerData = new ArrayList<>();
        }

        mPlayerData.clear();

        Set<Integer> uniqueIds = new HashSet<>();
        List<PlayerData> uniquePlayers = new ArrayList<>();

        for (PlayerData newPlayer : players) {
            if (!uniqueIds.contains(newPlayer.getId())) {
                uniqueIds.add(newPlayer.getId());
                uniquePlayers.add(newPlayer);
            }
        }

        mPlayerData.addAll(uniquePlayers);

        if (mTabAdapter != null) {
            mTabAdapter.updateData(mPlayerData);
        }
        updateOnlineCount();
    }

    private void updateOnlineCount() {
        if (mOnline != null && mPlayerData != null) {
            mOnline.setText(mPlayerData.size() + "/1000");
        }
    }

    public void show(boolean isAnim) {
        updateOnlineCount();

        if (mTabAdapter != null && mPlayerData != null) {
            mTabAdapter.updateData(mPlayerData);
        }

        if (mSearch != null) {
            mSearch.setText("");
        }
        setVisibleIconInSearchView("");

        if (mMainLayout != null) {
            Util.ShowLayout(mMainLayout, isAnim);
        }
    }

    public void setVisibleIconInSearchView(String text) {
        if (mSearchImg != null && mClearImg != null) {
            if (text.isEmpty()) {
                mSearchImg.setVisibility(View.VISIBLE);
                mClearImg.setVisibility(View.INVISIBLE);
            } else {
                mSearchImg.setVisibility(View.INVISIBLE);
                mClearImg.setVisibility(View.VISIBLE);
            }
        }
    }

    public void setServerName(String serverName) {
        if (mServerName != null) {
            mServerName.setText(serverName);
        }
    }

    public ConstraintLayout getMainLayout() {
        return mMainLayout;
    }

    public boolean isVisible() {
        return mMainLayout != null && mMainLayout.getVisibility() == View.VISIBLE;
    }
}