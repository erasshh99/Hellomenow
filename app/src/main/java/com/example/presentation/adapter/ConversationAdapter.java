package com.example.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.domain.model.Conversation;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ConversationAdapter extends ListAdapter<Conversation, ConversationAdapter.ViewHolder> {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("MMM d, HH:mm", Locale.getDefault());
    private final OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
        void onConversationDelete(Conversation conversation);
    }

    public ConversationAdapter(OnConversationClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Conversation> DIFF_CALLBACK = new DiffUtil.ItemCallback<Conversation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull Conversation oldItem, @NonNull Conversation newItem) {
            return oldItem.title.equals(newItem.title)
                    && oldItem.currentModelId.equals(newItem.currentModelId)
                    && oldItem.updatedAt == newItem.updatedAt;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvModel, tvDate;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_conv_title);
            tvModel = itemView.findViewById(R.id.tv_conv_model);
            tvDate = itemView.findViewById(R.id.tv_conv_date);
            btnDelete = itemView.findViewById(R.id.btn_delete_conv);
        }

        void bind(Conversation c, OnConversationClickListener listener) {
            tvTitle.setText(c.title);
            tvModel.setText(c.currentModelId);
            tvDate.setText(DATE_FMT.format(new Date(c.updatedAt)));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onConversationClick(c);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onConversationDelete(c);
            });
        }
    }
}
