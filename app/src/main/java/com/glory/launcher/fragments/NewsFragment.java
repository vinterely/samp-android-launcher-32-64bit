package com.glory.launcher.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.glory.App;
import com.glory.game.R;
import com.glory.launcher.activities.MainActivity;
import com.glory.launcher.adapters.NewsAdapter;

public class NewsFragment extends Fragment {
    public NewsFragment() {}

    private RecyclerView newsRv;
    private NewsAdapter newsAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        view = inflater.inflate(R.layout.fragment_news, container, false);
        newsRv = view.findViewById(R.id.rvNews);
        newsRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        newsAdapter = new NewsAdapter(getActivity());
        newsRv.setAdapter(newsAdapter);
        ((ImageView) view.findViewById(R.id.back2))
                .setOnClickListener(
                        new View.OnClickListener() {
                            public void onClick(View v) {

                                startActivity(new Intent(getActivity(), MainActivity.class));
                            }
                        });
        newsAdapter.addItems(App.getInstance().getStories());
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
