package com.example.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.example.di.AppContainer;

public class ViewModelFactory implements ViewModelProvider.Factory {
    private final AppContainer container;

    public ViewModelFactory(@NonNull AppContainer container) {
        this.container = container;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ChatViewModel.class)) {
            return (T) new ChatViewModel(container);
        } else if (modelClass.isAssignableFrom(ConversationsViewModel.class)) {
            return (T) new ConversationsViewModel(container);
        } else if (modelClass.isAssignableFrom(ProvidersViewModel.class)) {
            return (T) new ProvidersViewModel(container);
        } else if (modelClass.isAssignableFrom(ModelsViewModel.class)) {
            return (T) new ModelsViewModel(container);
        } else if (modelClass.isAssignableFrom(MediaViewModel.class)) {
            return (T) new MediaViewModel(container);
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
    }
}
