package com.example;

import android.app.Application;
import com.example.di.AppContainer;

public class OmniChatApp extends Application {
    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
    }

    public AppContainer getAppContainer() {
        return appContainer;
    }
}
