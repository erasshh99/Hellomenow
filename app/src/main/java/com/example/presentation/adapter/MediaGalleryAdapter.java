package com.example.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.R;
import com.example.domain.model.MediaAsset;

public class MediaGalleryAdapter extends ListAdapter<MediaAsset, MediaGalleryAdapter.ViewHolder> {

    private final OnMediaClickListener listener;

    public interface OnMediaClickListener {
        void onMediaClick(MediaAsset asset);
    }

    public MediaGalleryAdapter(OnMediaClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<MediaAsset> DIFF_CALLBACK = new DiffUtil.ItemCallback<MediaAsset>() {
        @Override
        public boolean areItemsTheSame(@NonNull MediaAsset oldItem, @NonNull MediaAsset newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MediaAsset oldItem, @NonNull MediaAsset newItem) {
            return oldItem.createdAt == newItem.createdAt && oldItem.remoteUrl.equals(newItem.remoteUrl);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_gallery_media, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumbnail, ivVideoBadge;
        TextView tvCaption;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.iv_gallery_thumbnail);
            ivVideoBadge = itemView.findViewById(R.id.iv_video_badge);
            tvCaption = itemView.findViewById(R.id.tv_gallery_caption);
        }

        void bind(MediaAsset a, OnMediaClickListener listener) {
            tvCaption.setText(a.prompt);
            String url = a.remoteUrl != null ? a.remoteUrl : a.localPath;
            Glide.with(itemView.getContext()).load(url).into(ivThumbnail);

            ivVideoBadge.setVisibility("VIDEO".equalsIgnoreCase(a.type) ? View.VISIBLE : View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onMediaClick(a);
            });
        }
    }
}
