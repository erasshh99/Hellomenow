package com.example.presentation.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.OmniChatApp;
import com.example.R;
import com.example.presentation.adapter.MediaGalleryAdapter;
import com.example.presentation.viewmodel.MediaViewModel;
import com.example.presentation.viewmodel.ViewModelFactory;
import java.util.ArrayList;

public class MediaGalleryFragment extends Fragment {

    private MediaViewModel viewModel;
    private MediaGalleryAdapter adapter;
    private RecyclerView recycler;
    private TextView tvEmpty;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OmniChatApp app = (OmniChatApp) requireActivity().getApplication();
        ViewModelFactory factory = new ViewModelFactory(app.getAppContainer());
        viewModel = new ViewModelProvider(this, factory).get(MediaViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler_gallery);
        tvEmpty = view.findViewById(R.id.tv_empty_gallery);

        recycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        adapter = new MediaGalleryAdapter(asset -> {
            String url = asset.remoteUrl != null ? asset.remoteUrl : asset.localPath;
            if (url != null) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Prompt: " + asset.prompt, Toast.LENGTH_SHORT).show();
                }
            }
        });
        recycler.setAdapter(adapter);

        viewModel.getAllMedia().observe(getViewLifecycleOwner(), list -> {
            boolean empty = list == null || list.isEmpty();
            tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            adapter.submitList(list != null ? new ArrayList<>(list) : new ArrayList<>());
        });
    }
}
