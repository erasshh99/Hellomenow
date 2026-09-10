package com.example.presentation.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import com.example.domain.model.ModelDescriptor;
import com.example.presentation.adapter.ModelAdapter;
import com.example.presentation.viewmodel.ModelsViewModel;
import com.example.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ModelsFragment extends Fragment {

    private ModelsViewModel viewModel;
    private ModelAdapter adapter;
    private RecyclerView recycler;
    private EditText etSearch;
    private ChipGroup chipGroupFilters;

    private List<ModelDescriptor> fullList = new ArrayList<>();
    private String currentQuery = "";
    private int currentFilterId = R.id.chip_filter_all;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OmniChatApp app = (OmniChatApp) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(this, factory).get(ModelsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_models, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_models);
        etSearch = view.findViewById(R.id.et_model_search);
        chipGroupFilters = view.findViewById(R.id.chip_group_filters);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ModelAdapter(new ModelAdapter.OnModelActionListener() {
            @Override
            public void onModelClick(ModelDescriptor model) {
                showCapabilityInspector(model);
            }

            @Override
            public void onProbeClick(ModelDescriptor model) {
                Toast.makeText(requireContext(), "Probing " + model.getFriendlyName() + "...", Toast.LENGTH_SHORT).show();
                viewModel.probeModel(model, result -> {
                    Toast.makeText(requireContext(), (result.success ? "Probe Success: " : "Probe Failed: ") + result.notes, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onInspectClick(ModelDescriptor model) {
                showCapabilityInspector(model);
            }
        });
        recycler.setAdapter(adapter);

        viewModel.getAllModels().observe(getViewLifecycleOwner(), list -> {
            fullList = list != null ? new ArrayList<>(list) : new ArrayList<>();
            applyFilter();
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {
                currentQuery = s.toString().toLowerCase(Locale.US);
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentFilterId = checkedIds.get(0);
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        List<ModelDescriptor> filtered = new ArrayList<>();
        for (ModelDescriptor m : fullList) {
            boolean matchesSearch = currentQuery.isEmpty()
                    || m.id.toLowerCase(Locale.US).contains(currentQuery)
                    || m.getFriendlyName().toLowerCase(Locale.US).contains(currentQuery)
                    || m.providerId.toLowerCase(Locale.US).contains(currentQuery);

            if (!matchesSearch) continue;

            boolean matchesChip = true;
            if (currentFilterId == R.id.chip_filter_vision) {
                matchesChip = m.capabilities != null && m.capabilities.visionInput.isTrue();
            } else if (currentFilterId == R.id.chip_filter_image_gen) {
                matchesChip = m.capabilities != null && m.capabilities.imageGeneration.isTrue();
            } else if (currentFilterId == R.id.chip_filter_video_gen) {
                matchesChip = m.capabilities != null && m.capabilities.videoGeneration.isTrue();
            } else if (currentFilterId == R.id.chip_filter_reasoning) {
                matchesChip = m.capabilities != null && m.capabilities.reasoningTokens.isTrue();
            }

            if (matchesChip) {
                filtered.add(m);
            }
        }
        adapter.submitList(filtered);
    }

    private void showCapabilityInspector(ModelDescriptor model) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_capability_inspector);

        TextView tvTitle = dialog.findViewById(R.id.tv_inspector_title);
        TextView tvSub = dialog.findViewById(R.id.tv_inspector_subtitle);
        TextView tvMatrix = dialog.findViewById(R.id.tv_caps_matrix);
        TextView tvRaw = dialog.findViewById(R.id.tv_raw_json);
        View btnClose = dialog.findViewById(R.id.btn_close_inspector);
        View btnProbe = dialog.findViewById(R.id.btn_run_probe);

        tvTitle.setText(model.getFriendlyName());
        tvSub.setText("ID: " + model.id + " • Provider: " + model.providerId);

        StringBuilder matrix = new StringBuilder();
        if (model.capabilities != null) {
            matrix.append("Vision Input: ").append(model.capabilities.visionInput.label).append("\n");
            matrix.append("Image Gen:    ").append(model.capabilities.imageGeneration.label).append("\n");
            matrix.append("Video Gen:    ").append(model.capabilities.videoGeneration.label).append("\n");
            matrix.append("Reasoning:    ").append(model.capabilities.reasoningTokens.label).append("\n");
            matrix.append("Tools/Func:   ").append(model.capabilities.functionCalling.label).append("\n");
            matrix.append("Streaming:    ").append(model.capabilities.streaming.label).append("\n");
            matrix.append("System Role:  ").append(model.capabilities.systemPromptSupport.label);
        }
        tvMatrix.setText(matrix.toString());

        tvRaw.setText(model.rawJson != null && !model.rawJson.isEmpty() ? model.rawJson : "{ \"modelId\": \"" + model.id + "\" }");

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnProbe.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Probing...", Toast.LENGTH_SHORT).show();
            viewModel.probeModel(model, result -> {
                Toast.makeText(requireContext(), "Probe: " + result.notes, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
