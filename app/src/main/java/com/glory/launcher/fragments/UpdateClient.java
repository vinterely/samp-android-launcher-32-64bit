package com.glory.launcher.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.glory.App;
import com.glory.game.R;
import com.glory.launcher.activities.DownloadActivity;

public class UpdateClient extends Fragment {
    private Button yes;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        view = inflater.inflate(R.layout.fragment_update, container, false);
        yes = view.findViewById(R.id.button_update);
        yes.setOnClickListener(view12 -> {
            view12.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.button_click));
            App.getInstance().downloadID = App.INSTALL_TYPE_CLIENT;
            Intent intent = new Intent(getActivity(), DownloadActivity.class);
            intent.putExtras(getActivity().getIntent());
            startActivity(intent);
            getActivity().finish();
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
}
