package com.example.presentation.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.OmniChatApp;
import com.example.R;
import com.example.domain.model.ProviderConfig;
import com.example.presentation.adapter.ProviderAdapter;
import com.example.presentation.viewmodel.ProvidersViewModel;
import com.example.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;
import java.util.UUID;

public class ProvidersFragment extends Fragment {

    private ProvidersViewModel viewModel;
    private ProviderAdapter adapter;
    private RecyclerView recycler;
    private ExtendedFloatingActionButton fabAddProvider;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OmniChatApp app = (OmniChatApp) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(this, factory).get(ProvidersViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_providers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_providers);
        fabAddProvider = view.findViewById(R.id.fab_add_provider);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ProviderAdapter(new ProviderAdapter.OnProviderActionListener() {
            @Override
            public void onToggleEnabled(ProviderConfig provider, boolean enabled) {
                provider.enabled = enabled;
                viewModel.saveProvider(provider);
            }

            @Override
            public void onTestConnection(ProviderConfig provider) {
                Toast.makeText(requireContext(), "Testing connection to " + provider.name + "...", Toast.LENGTH_SHORT).show();
                viewModel.testConnection(provider, new ProvidersViewModel.SimpleCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        Toast.makeText(requireContext(), result, Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Connection failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onSyncCatalog(ProviderConfig provider) {
                Toast.makeText(requireContext(), "Syncing model catalog...", Toast.LENGTH_SHORT).show();
                viewModel.syncCatalog(provider, new ProvidersViewModel.SimpleCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer count) {
                        Toast.makeText(requireContext(), "Synced " + count + " models successfully!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(requireContext(), "Sync failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onEdit(ProviderConfig provider) {
                showEditProviderDialog(provider);
            }
        });
        recycler.setAdapter(adapter);

        viewModel.getProviders().observe(getViewLifecycleOwner(), list -> {
            adapter.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>());
        });

        fabAddProvider.setOnClickListener(v -> {
            ProviderConfig newP = new ProviderConfig(UUID.randomUUID().toString(), "New Custom Provider", "OPENAI_COMPATIBLE", "https://api.openai.com");
            showEditProviderDialog(newP);
        });
    }

    private void showEditProviderDialog(ProviderConfig provider) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_provider_edit);

        TextView tvTitle = dialog.findViewById(R.id.tv_dialog_title);
        EditText etName = dialog.findViewById(R.id.et_provider_name);
        EditText etType = dialog.findViewById(R.id.et_provider_type);
        EditText etUrl = dialog.findViewById(R.id.et_base_url);
        EditText etKey = dialog.findViewById(R.id.et_api_key);
        View btnCancel = dialog.findViewById(R.id.btn_cancel);
        View btnSave = dialog.findViewById(R.id.btn_save);

        tvTitle.setText("Configure " + provider.name);
        etName.setText(provider.name);
        etType.setText(provider.type);
        etUrl.setText(provider.baseUrl);
        etKey.setText(provider.apiKey != null ? provider.apiKey : "");

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            provider.name = etName.getText().toString().trim();
            provider.type = etType.getText().toString().trim().toUpperCase();
            provider.baseUrl = etUrl.getText().toString().trim();
            provider.apiKey = etKey.getText().toString().trim();
            viewModel.saveProvider(provider);
            Toast.makeText(requireContext(), "Saved " + provider.name, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }
}
