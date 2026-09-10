package com.example.presentation.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.domain.model.CapabilitySet;
import com.example.domain.model.ModelDescriptor;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class ModelAdapter extends ListAdapter<ModelDescriptor, ModelAdapter.ViewHolder> {

    private final OnModelActionListener listener;

    public interface OnModelActionListener {
        void onModelClick(ModelDescriptor model);
        void onProbeClick(ModelDescriptor model);
        void onInspectClick(ModelDescriptor model);
    }

    public ModelAdapter(OnModelActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ModelDescriptor> DIFF_CALLBACK = new DiffUtil.ItemCallback<ModelDescriptor>() {
        @Override
        public boolean areItemsTheSame(@NonNull ModelDescriptor oldItem, @NonNull ModelDescriptor newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ModelDescriptor oldItem, @NonNull ModelDescriptor newItem) {
            return oldItem.displayName.equals(newItem.displayName)
                    && oldItem.confidence == newItem.confidence
                    && oldItem.detectedAt == newItem.detectedAt;
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_model, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvId, tvSpeedTier, tvConfidence;
        ChipGroup chipGroupCaps;
        Button btnInspect, btnProbe;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_model_name);
            tvId = itemView.findViewById(R.id.tv_model_id);
            tvSpeedTier = itemView.findViewById(R.id.tv_speed_tier);
            tvConfidence = itemView.findViewById(R.id.tv_confidence_score);
            chipGroupCaps = itemView.findViewById(R.id.chip_group_model_caps);
            btnInspect = itemView.findViewById(R.id.btn_inspect);
            btnProbe = itemView.findViewById(R.id.btn_probe);
        }

        void bind(ModelDescriptor m, OnModelActionListener listener) {
            tvName.setText(m.getFriendlyName());
            String ctx = m.contextWindow > 0 ? (m.contextWindow / 1000) + "k ctx" : "standard ctx";
            tvId.setText(m.providerId + " • " + m.id + " • " + ctx);
            tvSpeedTier.setText(m.tier != null ? m.tier.label : "⚖ Balanced");
            tvConfidence.setText("Confidence: " + m.confidence + "% (" + (m.source != null ? m.source.name() : "HEURISTIC") + ")");

            chipGroupCaps.removeAllViews();
            CapabilitySet caps = m.capabilities;
            if (caps != null) {
                if (caps.visionInput.isTrue()) addCapChip("👁 Vision");
                if (caps.imageGeneration.isTrue()) addCapChip("🎨 Image Gen");
                if (caps.videoGeneration.isTrue()) addCapChip("🎬 Video Gen");
                if (caps.reasoningTokens.isTrue()) addCapChip("🧠 Reasoning");
                if (caps.functionCalling.isTrue()) addCapChip("🛠 Tools");
                if (caps.documentInputPdf.isTrue()) addCapChip("📄 PDF");
                if (caps.streaming.isTrue()) addCapChip("⚡ Stream");
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onModelClick(m);
            });

            btnProbe.setOnClickListener(v -> {
                if (listener != null) listener.onProbeClick(m);
            });

            btnInspect.setOnClickListener(v -> {
                if (listener != null) listener.onInspectClick(m);
            });
        }

        private void addCapChip(String text) {
            Chip chip = new Chip(itemView.getContext());
            chip.setText(text);
            chip.setTextSize(10f);
            chip.setCheckable(false);
            chipGroupCaps.addView(chip);
        }
    }
}
