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
import com.example.domain.model.ProviderConfig;
import com.google.android.material.materialswitch.MaterialSwitch;

public class ProviderAdapter extends ListAdapter<ProviderConfig, ProviderAdapter.ViewHolder> {

    private final OnProviderActionListener listener;

    public interface OnProviderActionListener {
        void onToggleEnabled(ProviderConfig provider, boolean enabled);
        void onTestConnection(ProviderConfig provider);
        void onSyncCatalog(ProviderConfig provider);
        void onEdit(ProviderConfig provider);
    }

    public ProviderAdapter(OnProviderActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ProviderConfig> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProviderConfig>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProviderConfig oldItem, @NonNull ProviderConfig newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProviderConfig oldItem, @NonNull ProviderConfig newItem) {
            return oldItem.name.equals(newItem.name)
                    && oldItem.enabled == newItem.enabled
                    && oldItem.baseUrl.equals(newItem.baseUrl);
        }
    };

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_provider, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View statusDot;
        TextView tvName, tvTypeUrl, tvMaskedKey;
        MaterialSwitch switchEnabled;
        Button btnTest, btnSync, btnEdit;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            statusDot = itemView.findViewById(R.id.view_status_dot);
            tvName = itemView.findViewById(R.id.tv_provider_name);
            tvTypeUrl = itemView.findViewById(R.id.tv_provider_type_url);
            tvMaskedKey = itemView.findViewById(R.id.tv_masked_key);
            switchEnabled = itemView.findViewById(R.id.switch_provider_enabled);
            btnTest = itemView.findViewById(R.id.btn_test_conn);
            btnSync = itemView.findViewById(R.id.btn_sync_catalog);
            btnEdit = itemView.findViewById(R.id.btn_edit_provider);
        }

        void bind(ProviderConfig p, OnProviderActionListener listener) {
            tvName.setText(p.name);
            tvTypeUrl.setText(p.type + " • " + p.baseUrl);
            tvMaskedKey.setText("API Key: " + p.getMaskedKey());
            switchEnabled.setChecked(p.enabled);

            switchEnabled.setOnCheckedChangeListener((btn, isChecked) -> {
                if (listener != null) listener.onToggleEnabled(p, isChecked);
            });

            btnTest.setOnClickListener(v -> {
                if (listener != null) listener.onTestConnection(p);
            });

            btnSync.setOnClickListener(v -> {
                if (listener != null) listener.onSyncCatalog(p);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(p);
            });
        }
    }
}
