package com.example.presentation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.example.di.AppContainer;
import com.example.domain.model.MediaAsset;
import java.util.List;

public class MediaViewModel extends ViewModel {
    private final LiveData<List<MediaAsset>> allMedia;

    public MediaViewModel(@NonNull AppContainer container) {
        this.allMedia = container.getMediaRepository().getAllMediaAssets();
    }

    public LiveData<List<MediaAsset>> getAllMedia() {
        return allMedia;
    }
}
