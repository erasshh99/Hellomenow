package com.example.presentation.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.R;
import com.example.domain.model.ContentPart;
import com.example.domain.model.ContentPartType;
import com.example.domain.model.Role;
import com.example.domain.model.UnifiedMessage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends ListAdapter<UnifiedMessage, RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_ASSISTANT = 2;
    private static final int VIEW_TYPE_SYSTEM = 3;
    private static final int VIEW_TYPE_MEDIA = 4;

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final OnMessageActionListener actionListener;

    public interface OnMessageActionListener {
        void onRetry(UnifiedMessage message);
    }

    public MessageAdapter(OnMessageActionListener actionListener) {
        super(DIFF_CALLBACK);
        this.actionListener = actionListener;
    }

    private static final DiffUtil.ItemCallback<UnifiedMessage> DIFF_CALLBACK = new DiffUtil.ItemCallback<UnifiedMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull UnifiedMessage oldItem, @NonNull UnifiedMessage newItem) {
            return oldItem.id == newItem.id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull UnifiedMessage oldItem, @NonNull UnifiedMessage newItem) {
            return oldItem.getTextContent().equals(newItem.getTextContent())
                    && oldItem.status == newItem.status
                    && oldItem.parts.size() == newItem.parts.size();
        }
    };

    @Override
    public int getItemViewType(int position) {
        UnifiedMessage msg = getItem(position);
        if (msg.role == Role.SYSTEM) {
            return VIEW_TYPE_SYSTEM;
        } else if (msg.role == Role.USER) {
            return VIEW_TYPE_USER;
        } else {
            // Check if it is primarily an image/video generated result
            for (ContentPart p : msg.parts) {
                if (p.type == ContentPartType.IMAGE || p.type == ContentPartType.VIDEO) {
                    return VIEW_TYPE_MEDIA;
                }
            }
            return VIEW_TYPE_ASSISTANT;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_USER) {
            View v = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserViewHolder(v);
        } else if (viewType == VIEW_TYPE_SYSTEM) {
            View v = inflater.inflate(R.layout.item_message_system, parent, false);
            return new SystemViewHolder(v);
        } else if (viewType == VIEW_TYPE_MEDIA) {
            View v = inflater.inflate(R.layout.item_message_media, parent, false);
            return new MediaViewHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_assistant, parent, false);
            return new AssistantViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        UnifiedMessage msg = getItem(position);
        if (holder instanceof UserViewHolder) {
            ((UserViewHolder) holder).bind(msg);
        } else if (holder instanceof AssistantViewHolder) {
            ((AssistantViewHolder) holder).bind(msg, actionListener);
        } else if (holder instanceof SystemViewHolder) {
            ((SystemViewHolder) holder).bind(msg);
        } else if (holder instanceof MediaViewHolder) {
            ((MediaViewHolder) holder).bind(msg);
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvText, tvTime;
        ImageView ivAttachment;

        UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_message_text);
            tvTime = itemView.findViewById(R.id.tv_timestamp);
            ivAttachment = itemView.findViewById(R.id.iv_attachment);
        }

        void bind(UnifiedMessage msg) {
            tvText.setText(msg.getTextContent());
            tvTime.setText(TIME_FMT.format(new Date(msg.createdAt)));

            boolean hasImage = false;
            for (ContentPart p : msg.parts) {
                if (p.type == ContentPartType.IMAGE && (p.mediaUrl != null || p.localPath != null)) {
                    hasImage = true;
                    ivAttachment.setVisibility(View.VISIBLE);
                    Glide.with(itemView.getContext())
                            .load(p.mediaUrl != null ? p.mediaUrl : p.localPath)
                            .into(ivAttachment);
                    break;
                }
            }
            if (!hasImage) {
                ivAttachment.setVisibility(View.GONE);
            }
        }
    }

    static class AssistantViewHolder extends RecyclerView.ViewHolder {
        TextView tvModel, tvText, tvMetrics, tvReasoningHeader, tvReasoningBody;
        LinearLayout layoutReasoning;
        ImageButton btnCopy, btnShare, btnRetry;

        AssistantViewHolder(@NonNull View itemView) {
            super(itemView);
            tvModel = itemView.findViewById(R.id.tv_model_attribution);
            tvText = itemView.findViewById(R.id.tv_message_text);
            tvMetrics = itemView.findViewById(R.id.tv_footer_metrics);
            layoutReasoning = itemView.findViewById(R.id.layout_reasoning);
            tvReasoningHeader = itemView.findViewById(R.id.tv_reasoning_header);
            tvReasoningBody = itemView.findViewById(R.id.tv_reasoning_body);
            btnCopy = itemView.findViewById(R.id.btn_copy);
            btnShare = itemView.findViewById(R.id.btn_share);
            btnRetry = itemView.findViewById(R.id.btn_retry);
        }

        void bind(UnifiedMessage msg, OnMessageActionListener listener) {
            tvModel.setText(msg.sourceModelId != null ? msg.sourceModelId : "Assistant");
            tvText.setText(msg.getTextContent());

            // Reasoning block toggle
            String reasoning = msg.getReasoningContent();
            if (reasoning != null && !reasoning.isEmpty()) {
                layoutReasoning.setVisibility(View.VISIBLE);
                tvReasoningBody.setText(reasoning);
                tvReasoningHeader.setOnClickListener(v -> {
                    boolean isCollapsed = tvReasoningBody.getVisibility() == View.GONE;
                    tvReasoningBody.setVisibility(isCollapsed ? View.VISIBLE : View.GONE);
                    tvReasoningHeader.setText(isCollapsed ? "▲ Thought Process (Reasoning Tokens)" : "▼ Thought Process (Reasoning Tokens)");
                });
            } else {
                layoutReasoning.setVisibility(View.GONE);
            }

            // Metrics footer
            StringBuilder metrics = new StringBuilder();
            if (msg.tokenUsage != null && msg.tokenUsage.totalTokens > 0) {
                metrics.append(msg.tokenUsage.totalTokens).append(" tokens");
            }
            if (msg.latencyMs > 0) {
                if (metrics.length() > 0) metrics.append(" • ");
                metrics.append(String.format(Locale.US, "%.2fs", msg.latencyMs / 1000.0));
            }
            tvMetrics.setText(metrics.toString());
            tvMetrics.setVisibility(metrics.length() > 0 ? View.VISIBLE : View.GONE);

            // Action Buttons
            btnCopy.setOnClickListener(v -> {
                Context ctx = itemView.getContext();
                ClipboardManager cm = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("OmniChat Message", msg.getTextContent());
                if (cm != null) {
                    cm.setPrimaryClip(clip);
                    Toast.makeText(ctx, "Copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });

            btnShare.setOnClickListener(v -> {
                Context ctx = itemView.getContext();
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, msg.getTextContent());
                sendIntent.setType("text/plain");
                ctx.startActivity(Intent.createChooser(sendIntent, "Share Message"));
            });

            btnRetry.setOnClickListener(v -> {
                if (listener != null) listener.onRetry(msg);
            });
        }
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        TextView tvModel, tvPrompt;
        ImageView ivMedia, ivPlay;
        View btnSave, btnShare;

        MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvModel = itemView.findViewById(R.id.tv_media_model);
            tvPrompt = itemView.findViewById(R.id.tv_media_prompt);
            ivMedia = itemView.findViewById(R.id.iv_media);
            ivPlay = itemView.findViewById(R.id.iv_play_overlay);
            btnSave = itemView.findViewById(R.id.btn_save_gallery);
            btnShare = itemView.findViewById(R.id.btn_share_media);
        }

        void bind(UnifiedMessage msg) {
            tvModel.setText(msg.sourceModelId != null ? msg.sourceModelId : "Media");
            tvPrompt.setText(msg.getTextContent());

            for (ContentPart p : msg.parts) {
                if (p.type == ContentPartType.IMAGE || p.type == ContentPartType.VIDEO) {
                    String url = p.mediaUrl != null ? p.mediaUrl : p.localPath;
                    Glide.with(itemView.getContext()).load(url).into(ivMedia);
                    ivPlay.setVisibility(p.type == ContentPartType.VIDEO ? View.VISIBLE : View.GONE);
                    break;
                }
            }

            btnSave.setOnClickListener(v -> {
                Toast.makeText(itemView.getContext(), "Saved to gallery", Toast.LENGTH_SHORT).show();
            });

            btnShare.setOnClickListener(v -> {
                Context ctx = itemView.getContext();
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, msg.getTextContent());
                sendIntent.setType("text/plain");
                ctx.startActivity(Intent.createChooser(sendIntent, "Share Media"));
            });
        }
    }

    static class SystemViewHolder extends RecyclerView.ViewHolder {
        TextView tvText;

        SystemViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tv_system_text);
        }

        void bind(UnifiedMessage msg) {
            tvText.setText(msg.getTextContent());
        }
    }
}
