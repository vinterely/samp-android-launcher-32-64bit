package com.glory.game.ui.tab;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.glory.game.R;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.glory.launcher.util.Util;

public class TabAdapter extends RecyclerView.Adapter<TabAdapter.ViewHolder> implements Filterable {

    private List<PlayerData> mPlayerData;
    private List<PlayerData> mPlayerDataCopy;

    public TabAdapter(List<PlayerData> playerData) {
        this.mPlayerData = playerData;
        this.mPlayerDataCopy = new ArrayList<>(playerData);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.tab_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PlayerData data = this.mPlayerData.get(position);
        holder.id.setText(String.valueOf(data.getId()));
        holder.name.setText(Util.transfromColors(data.getName()));
        holder.level.setText(String.valueOf(data.getLevel()));
        holder.ping.setText(String.valueOf(data.getPing()));
    }

    @Override
    public int getItemCount() {
        return this.mPlayerData.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String input = charSequence.toString().toLowerCase();
                List<PlayerData> filteredList = new ArrayList<>();

                if (input.isEmpty()) {
                    filteredList.addAll(mPlayerDataCopy);
                } else {
                    for (PlayerData playerData : mPlayerDataCopy) {
                        if (playerData.getName().toLowerCase().contains(input)) {
                            filteredList.add(playerData);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filteredList;
                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mPlayerData.clear();
                mPlayerData.addAll((List<PlayerData>) filterResults.values);
                notifyDataSetChanged();
            }
        };
    }

    public void updateData(List<PlayerData> newData) {
        Set<Integer> uniqueIds = new HashSet<>();
        List<PlayerData> uniqueData = new ArrayList<>();

        for (PlayerData player : newData) {
            if (!uniqueIds.contains(player.getId())) {
                uniqueIds.add(player.getId());
                uniqueData.add(player);
            }
        }

        mPlayerData.clear();
        mPlayerData.addAll(uniqueData);
        mPlayerDataCopy.clear();
        mPlayerDataCopy.addAll(uniqueData);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView id;
        public TextView name;
        public TextView level;
        public TextView ping;
        public ConstraintLayout itemLayout;

        public ViewHolder(View itemView) {
            super(itemView);
            itemLayout = (ConstraintLayout) itemView.findViewById(R.id.hassle_tab_item);
            id = (TextView) itemView.findViewById(R.id.player_id);
            name = (TextView) itemView.findViewById(R.id.player_name);
            level = (TextView) itemView.findViewById(R.id.player_level);
            ping = (TextView) itemView.findViewById(R.id.player_ping);
        }
    }
}