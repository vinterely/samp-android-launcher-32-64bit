package com.glory.launcher.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glory.App;
import com.glory.game.R;
import com.glory.launcher.adapters.NewsAdapter;
import com.glory.launcher.adapters.ServerAdapter;
import com.glory.launcher.network.Server;
import com.glory.launcher.network.ServerListener;

import java.util.ArrayList;

public class ServersFragment extends Fragment {

    private RecyclerView serversRecycler;
    private RecyclerView newsRv;

    private ServerAdapter serverAdapter;
    private NewsAdapter newsAdapter;

    private LinearLayout help_btn;

    public ServersFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_servers, container, false);

        // ðŸ”¹ Animatsiya
        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.button_click);

        help_btn = view.findViewById(R.id.help_btn);

        // =======================
        // ðŸ”¹ NEWS RecyclerView
        // =======================
        newsRv = view.findViewById(R.id.rvNews);
        if (newsRv != null) {
            newsRv.setLayoutManager(
                    new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false)
            );
            newsAdapter = new NewsAdapter(getActivity());
            newsRv.setAdapter(newsAdapter);
            newsAdapter.addItems(App.getInstance().getStories());
        }

        // =======================
        // ðŸ”¹ SERVERS RecyclerView
        // =======================
        serversRecycler = view.findViewById(R.id.rvServers);
        serversRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        serverAdapter = new ServerAdapter(this, App.getInstance().getServerList());
        serversRecycler.setAdapter(serverAdapter);

        return view;
    }
}
