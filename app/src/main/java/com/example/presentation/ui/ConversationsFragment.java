package com.example.presentation.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
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
import com.example.presentation.MainActivity;
import com.example.presentation.adapter.ConversationAdapter;
import com.example.presentation.viewmodel.ConversationsViewModel;
import com.example.presentation.viewmodel.ViewModelFactory;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;

public class ConversationsFragment extends Fragment {

    private ConversationsViewModel viewModel;
    private ConversationAdapter adapter;
    private RecyclerView recycler;
    private TextView tvEmpty;
    private ExtendedFloatingActionButton fabNewChat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OmniChatApp app = (OmniChatApp) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(this, factory).get(ConversationsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_conversations, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_conversations);
        tvEmpty = view.findViewById(R.id.tv_empty_state);
        fabNewChat = view.findViewById(R.id.fab_new_chat);

        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ConversationAdapter(new ConversationAdapter.OnConversationClickListener() {
            @Override
            public void onConversationClick(Conversation conversation) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToChat(conversation.id);
                }
            }

            @Override
            public void onConversationDelete(Conversation conversation) {
                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Conversation")
                        .setMessage("Are you sure you want to delete '" + conversation.title + "'?")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            viewModel.deleteConversation(conversation);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        recycler.setAdapter(adapter);

        viewModel.getConversations().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>());
        });

        fabNewChat.setOnClickListener(v -> showNewChatDialog());
    }

    private void showNewChatDialog() {
        final EditText input = new EditText(requireContext());
        input.setHint("Conversation Topic or Title");

        new AlertDialog.Builder(requireContext())
                .setTitle("New Conversation")
                .setView(input)
                .setPositiveButton("Create", (dialog, which) -> {
                    String title = input.getText().toString().trim();
                    if (title.isEmpty()) title = "New Conversation";
                    viewModel.createConversation(title, "gpt-4o", "openai", conversation -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateToChat(conversation.id);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
