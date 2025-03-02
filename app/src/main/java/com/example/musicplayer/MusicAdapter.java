package com.example.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.ViewHolder> {
    private final List<MusicItem> musicList;
    private final OnItemClickListener listener;
    private int selectedPosition = -1;
    private final Context context;

    public interface OnItemClickListener {
        void onItemClick(MusicItem item);
    }

    public MusicAdapter(Context context, List<MusicItem> musicList, OnItemClickListener listener) {
        this.context = context;
        this.musicList = musicList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.music_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final MusicItem item = musicList.get(position);
        
        holder.titleTextView.setText(item.getTitle());
        holder.artistTextView.setText(item.getArtist());
        holder.ivSelected.setVisibility(position == selectedPosition ? View.VISIBLE : View.INVISIBLE);

        // 点击动画
        if (position == selectedPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.item_select);
            holder.itemView.startAnimation(animation);
        } else {
            holder.itemView.clearAnimation();
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int previous = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                notifyItemChanged(previous);
                notifyItemChanged(selectedPosition);
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView titleTextView;
        final TextView artistTextView;
        final ImageView ivSelected;

        ViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            artistTextView = itemView.findViewById(R.id.artistTextView);
            ivSelected = itemView.findViewById(R.id.ivSelected);
        }
    }
}