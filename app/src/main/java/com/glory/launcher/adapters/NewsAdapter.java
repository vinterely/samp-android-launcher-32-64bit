package com.glory.launcher.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.glory.game.R;
import com.glory.launcher.network.Story;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.ViewHolder> {

    private Context context;
    private List<Story> stories = new ArrayList<>();

    public NewsAdapter(Context context) {
        this.context = context;
    }

    public void addItems(List<Story> list) {
        stories = list;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_news, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Story story = stories.get(position);

        holder.title.setText(story.getTitle());

        Glide.with(context)
                .load(story.getImage())
                .into(holder.image);

        // Agar link boâ€˜lmasa â€” "ÐŸÐ¾Ð´Ñ€Ð¾Ð±Ð½ÐµÐµ" ni yashiramiz
        holder.moreText.setVisibility(
                story.getLink() == null || story.getLink().isEmpty()
                        ? View.GONE
                        : View.VISIBLE
        );

        // BUTUN CARD BOSILADI
        holder.clickArea.setOnClickListener(v -> {
            if (story.getLink() == null || story.getLink().isEmpty()) return;

            v.startAnimation(AnimationUtils.loadAnimation(context, R.anim.button_click));

            new Handler().postDelayed(() -> {
                context.startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(story.getLink()))
                );
            }, 180);
        });
    }

    @Override
    public int getItemCount() {
        return stories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        ImageView arrow;
        TextView title;
        TextView moreText;
        View clickArea; // MaterialRippleLayout

        ViewHolder(View view) {
            super(view);

            image = view.findViewById(R.id.ivNewsImage);
            arrow = view.findViewById(R.id.imageView19);
            title = view.findViewById(R.id.tvNewsText);
            moreText = view.findViewById(R.id.tvMore);

            // MUHIM: bu ImageView EMAS!
            clickArea = view.findViewById(R.id.button_update);
        }
    }
}
