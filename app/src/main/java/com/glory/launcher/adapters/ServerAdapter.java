package com.glory.launcher.adapters;

import static com.glory.Config.GAME_PATH;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.glory.App;
import com.glory.Utils;
import com.glory.game.R;
import com.glory.launcher.activities.ChooseServerActivity;
import com.glory.launcher.activities.DownloadActivity;
import com.glory.launcher.fragments.ServersFragment;
import com.glory.launcher.network.SampQuery;
import com.glory.launcher.network.Server;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

public class ServerAdapter extends RecyclerView.Adapter<ServerAdapter.ViewHolder> {
    public ServersFragment monitoringFragment;
    private ArrayList<Server> serverList;

    public ServerAdapter(ServersFragment monitoringFragment2, ArrayList<Server> arrayList) {
        this.monitoringFragment = monitoringFragment2;
        this.serverList = arrayList;
    }

    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_server, viewGroup, false));
    }

    public void onBindViewHolder(ViewHolder viewHolder, int i) {
        new Thread(() -> {
            InetAddress ServerAddress = null;
            SampQuery query;
            try {
                ServerAddress = InetAddress.getByName(serverList.get(i).getIP());
            } catch (UnknownHostException e) {
                Utils.writeLog(monitoringFragment.getContext(), 'e', "ÐžÑˆÐ¸Ð±ÐºÐ° ÑÐµÑ€Ð²ÐµÑ€Ð°: " + e.getMessage());
            }
            if(ServerAddress != null) {
                if (ServerAddress.getHostAddress() != null || !ServerAddress.getHostAddress().isEmpty()) {
                    query = new SampQuery(ServerAddress.getHostAddress(), serverList.get(i).getPort());
                    if (query.connect()) {
                        String[] serverInfo = query.getInfo();
                        System.out.println("SampQuery connect successful for server: " + serverList.get(i).getName());
                        System.out.println("Server Info: " + java.util.Arrays.toString(serverInfo));
                        new Handler(Looper.getMainLooper()).post(() -> {
                            String onlineText = serverInfo[1] + "/" + serverInfo[2];
                            viewHolder.online.setText(onlineText);
                            System.out.println("Setting online text to: " + onlineText + " for server: " + serverList.get(i).getName());
                        });
                        query.close();
                    } else {
                        System.out.println("SampQuery connect failed for server: " + serverList.get(i).getName());
                        new Handler(Looper.getMainLooper()).post(() -> {
                            viewHolder.online.setText("ÐÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿ÐµÐ½");
                            System.out.println("Setting online text to: ÐÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿ÐµÐ½ for server: " + serverList.get(i).getName());
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        viewHolder.online.setText("ÐÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿ÐµÐ½");
                    });
                }
            } else {
                new Handler(Looper.getMainLooper()).post(() -> {
                    viewHolder.online.setText("ÐÐµÐ´Ð¾ÑÑ‚ÑƒÐ¿ÐµÐ½");
                });
            }
        }).start();
        viewHolder.serverName.setText(serverList.get(i).getName());
        viewHolder.serverID.setText("0"+serverList.get(i).getID());
        viewHolder.btnPlay.setOnClickListener(view -> {
            view.startAnimation(
                AnimationUtils.loadAnimation(
                    monitoringFragment.getContext(),
                    R.anim.button_click
                )
            );

            Utils.handleServerPlay(
                monitoringFragment.getContext(),
                monitoringFragment.getActivity(),
                serverList.get(i).getID()
            );
        });
    }

    public int getItemCount() { return this.serverList.size(); }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView serverName;
        public TextView online;
        public TextView serverID;
        public ImageButton btnPlay;
        //public ConstraintLayout btnPlay;

        public ViewHolder(View view) {
            super(view);
            this.serverID = (TextView) view.findViewById(R.id.textView11);
            this.serverName = (TextView) view.findViewById(R.id.textView12);
            this.online = (TextView) view.findViewById(R.id.textView14);
            //this.btnPlay = (ConstraintLayout) view.findViewById(R.id.view_online);
            this.btnPlay = (ImageButton) view.findViewById(R.id.button5);
        }
    }
}
