package com.example.presentation.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.OmniChatApp;
import com.example.R;
import com.example.domain.model.Conversation;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.UnifiedMessage;
import com.example.presentation.adapter.MessageAdapter;
import com.example.presentation.viewmodel.ChatViewModel;
import com.example.presentation.viewmodel.ModelsViewModel;
import com.example.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private static final String ARG_CONV_ID = "conv_id";

    private ChatViewModel chatViewModel;
    private ModelsViewModel modelsViewModel;

    private MessageAdapter messageAdapter;
    private RecyclerView recyclerMessages;
    private EditText etMessage;
    private ImageButton btnSendStop, btnInspector;
    private Chip chipCurrentModel;
    private TextView tvChatTitle, tvModelStatus, tvDegradationBanner;
    private LinearProgressIndicator progressStreaming;

    private String conversationId = "welcome_chat";

    public static ChatFragment newInstance(String conversationId) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CONV_ID, conversationId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(ARG_CONV_ID)) {
            conversationId = getArguments().getString(ARG_CONV_ID);
        }
        OmniChatApp app = (OmniChatApp) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        chatViewModel = new ViewModelProvider(this, factory).get(ChatViewModel.class);
        modelsViewModel = new ViewModelProvider(this, factory).get(ModelsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerMessages = view.findViewById(R.id.recycler_messages);
        etMessage = view.findViewById(R.id.et_message);
        btnSendStop = view.findViewById(R.id.btn_send_stop);
        btnInspector = view.findViewById(R.id.btn_inspector);
        chipCurrentModel = view.findViewById(R.id.chip_current_model);
        tvChatTitle = view.findViewById(R.id.tv_chat_title);
        tvModelStatus = view.findViewById(R.id.tv_model_status);
        tvDegradationBanner = view.findViewById(R.id.tv_degradation_banner);
        progressStreaming = view.findViewById(R.id.progress_streaming);

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        layoutManager.setStackFromEnd(true);
        recyclerMessages.setLayoutManager(layoutManager);

        messageAdapter = new MessageAdapter(message -> {
            // Regenerate
            chatViewModel.sendMessage("Please retry: " + message.getTextContent(), null);
        });
        recyclerMessages.setAdapter(messageAdapter);

        // Load conversation
        chatViewModel.loadConversation(conversationId).observe(getViewLifecycleOwner(), messages -> {
            messageAdapter.submitList(messages != null ? new ArrayList<>(messages) : new ArrayList<>());
            if (messages != null && !messages.isEmpty()) {
                recyclerMessages.scrollToPosition(messages.size() - 1);
            }
        });

        chatViewModel.getActiveConversation().observe(getViewLifecycleOwner(), conversation -> {
            if (conversation != null) {
                tvChatTitle.setText(conversation.title);
            }
        });

        chatViewModel.getActiveModel().observe(getViewLifecycleOwner(), model -> {
            if (model != null) {
                chipCurrentModel.setText(model.getFriendlyName());
                tvModelStatus.setText(model.providerId + " • " + (model.tier != null ? model.tier.label : ""));
            }
        });

        chatViewModel.getIsStreaming().observe(getViewLifecycleOwner(), streaming -> {
            if (Boolean.TRUE.equals(streaming)) {
                btnSendStop.setImageResource(R.drawable.ic_stop);
                progressStreaming.setVisibility(View.VISIBLE);
            } else {
                btnSendStop.setImageResource(R.drawable.ic_send);
                progressStreaming.setVisibility(View.GONE);
            }
        });

        chatViewModel.getDegradationWarnings().observe(getViewLifecycleOwner(), warnings -> {
            if (warnings != null && !warnings.isEmpty()) {
                tvDegradationBanner.setVisibility(View.VISIBLE);
                tvDegradationBanner.setText(warnings.get(0));
            } else {
                tvDegradationBanner.setVisibility(View.GONE);
            }
        });

        chatViewModel.getErrorEvent().observe(getViewLifecycleOwner(), err -> {
            if (err != null) {
                Toast.makeText(requireContext(), err.userFacingMessage, Toast.LENGTH_LONG).show();
            }
        });

        btnSendStop.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(chatViewModel.getIsStreaming().getValue())) {
                chatViewModel.stopStreaming();
            } else {
                String text = etMessage.getText().toString().trim();
                if (!text.isEmpty()) {
                    chatViewModel.sendMessage(text, null);
                    etMessage.setText("");
                }
            }
        });

        chipCurrentModel.setOnClickListener(v -> showModelSwitchDialog());
        btnInspector.setOnClickListener(v -> showCapabilityInspectorDialog());
    }

    private void showModelSwitchDialog() {
        modelsViewModel.getAllModels().observe(getViewLifecycleOwner(), models -> {
            if (models == null || models.isEmpty()) {
                Toast.makeText(requireContext(), "No models available in catalog.", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[models.size()];
            for (int i = 0; i < models.size(); i++) {
                ModelDescriptor m = models.get(i);
                names[i] = m.getFriendlyName() + " (" + m.providerId + ")";
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Switch Model (Mid-Conversation)")
                    .setItems(names, (dialog, which) -> {
                        ModelDescriptor selected = models.get(which);
                        chatViewModel.switchModel(selected);
                        Toast.makeText(requireContext(), "Switched to " + selected.getFriendlyName(), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void showCapabilityInspectorDialog() {
        ModelDescriptor active = chatViewModel.getActiveModel().getValue();
        if (active == null) return;

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_capability_inspector);

        TextView tvTitle = dialog.findViewById(R.id.tv_inspector_title);
        TextView tvSub = dialog.findViewById(R.id.tv_inspector_subtitle);
        TextView tvMatrix = dialog.findViewById(R.id.tv_caps_matrix);
        TextView tvRaw = dialog.findViewById(R.id.tv_raw_json);
        View btnClose = dialog.findViewById(R.id.btn_close_inspector);
        View btnProbe = dialog.findViewById(R.id.btn_run_probe);

        tvTitle.setText(active.getFriendlyName());
        tvSub.setText("ID: " + active.id + " • Provider: " + active.providerId);

        StringBuilder matrix = new StringBuilder();
        if (active.capabilities != null) {
            matrix.append("Vision Input: ").append(active.capabilities.visionInput.label).append("\n");
            matrix.append("Image Gen:    ").append(active.capabilities.imageGeneration.label).append("\n");
            matrix.append("Video Gen:    ").append(active.capabilities.videoGeneration.label).append("\n");
            matrix.append("Reasoning:    ").append(active.capabilities.reasoningTokens.label).append("\n");
            matrix.append("Tools/Func:   ").append(active.capabilities.functionCalling.label).append("\n");
            matrix.append("Streaming:    ").append(active.capabilities.streaming.label).append("\n");
            matrix.append("System Role:  ").append(active.capabilities.systemPromptSupport.label);
        }
        tvMatrix.setText(matrix.toString());

        tvRaw.setText(active.rawJson != null && !active.rawJson.isEmpty() ? active.rawJson : "{ \"modelId\": \"" + active.id + "\" }");

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnProbe.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Probing model capabilities...", Toast.LENGTH_SHORT).show();
            modelsViewModel.probeModel(active, result -> {
                Toast.makeText(requireContext(), "Probe " + (result.success ? "Passed" : "Failed") + ": " + result.notes, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }
}
