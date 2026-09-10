package com.example.di;

import android.content.Context;
import androidx.annotation.NonNull;
import com.example.data.discovery.CapabilityDiscoveryEngine;
import com.example.data.local.AppDatabase;
import com.example.data.provider.ProviderRegistry;
import com.example.data.repository.ConversationRepositoryImpl;
import com.example.data.repository.MediaRepositoryImpl;
import com.example.data.repository.MessageRepositoryImpl;
import com.example.data.repository.ModelRepositoryImpl;
import com.example.data.repository.ProviderRepositoryImpl;
import com.example.data.security.SecureKeyStorage;
import com.example.domain.model.Conversation;
import com.example.domain.model.ModelDescriptor;
import com.example.domain.model.ProviderConfig;
import com.example.domain.repository.ConversationRepository;
import com.example.domain.repository.MediaRepository;
import com.example.domain.repository.MessageRepository;
import com.example.domain.repository.ModelRepository;
import com.example.domain.repository.ProviderRepository;
import com.example.util.AppExecutors;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class AppContainer {
    private final Context context;
    private final AppExecutors executors;
    private final OkHttpClient httpClient;
    private final SecureKeyStorage keyStorage;
    private final AppDatabase database;
    private final CapabilityDiscoveryEngine discoveryEngine;
    private final ProviderRegistry providerRegistry;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ProviderRepository providerRepository;
    private final ModelRepository modelRepository;
    private final MediaRepository mediaRepository;

    public AppContainer(@NonNull Context appContext) {
        this.context = appContext.getApplicationContext();
        this.executors = AppExecutors.getInstance();

        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();

        this.keyStorage = new SecureKeyStorage(context);
        this.database = AppDatabase.getInstance(context);
        this.discoveryEngine = new CapabilityDiscoveryEngine(context);
        this.providerRegistry = new ProviderRegistry(httpClient, executors);

        this.conversationRepository = new ConversationRepositoryImpl(database.conversationDao(), executors);
        this.messageRepository = new MessageRepositoryImpl(database.messageDao(), executors);
        this.providerRepository = new ProviderRepositoryImpl(database.providerDao(), keyStorage, executors);
        this.modelRepository = new ModelRepositoryImpl(database.modelDao(), executors);
        this.mediaRepository = new MediaRepositoryImpl(database.mediaJobDao(), database.mediaAssetDao(), executors);

        seedDefaultsIfNeeded();
    }

    private void seedDefaultsIfNeeded() {
        executors.diskIO().execute(() -> {
            try {
                List<ProviderConfig> existingProviders = providerRepository.getAllProvidersSync();
                if (existingProviders == null || existingProviders.isEmpty()) {
                    // Seed from provider_presets.json
                    try (InputStream is = context.getAssets().open("provider_presets.json");
                         InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        List<ProviderConfig> presets = new Gson().fromJson(reader, new TypeToken<List<ProviderConfig>>(){}.getType());
                        if (presets != null) {
                            for (ProviderConfig p : presets) {
                                providerRepository.saveProvider(p);
                            }
                        }
                    }
                }

                List<ModelDescriptor> existingModels = modelRepository.getModelsForProviderSync("openai");
                if (existingModels == null || existingModels.isEmpty()) {
                    // Seed sample models for OpenAI, Gemini, Anthropic
                    List<ModelDescriptor> defaults = new ArrayList<>();
                    defaults.add(discoveryEngine.discoverCapabilities("gpt-4o", "openai", null));
                    defaults.add(discoveryEngine.discoverCapabilities("gpt-4o-mini", "openai", null));
                    defaults.add(discoveryEngine.discoverCapabilities("o1", "openai", null));
                    defaults.add(discoveryEngine.discoverCapabilities("dall-e-3", "openai", null));
                    defaults.add(discoveryEngine.discoverCapabilities("claude-3-7-sonnet", "anthropic", null));
                    defaults.add(discoveryEngine.discoverCapabilities("claude-3-5-sonnet", "anthropic", null));
                    defaults.add(discoveryEngine.discoverCapabilities("gemini-2.0-flash", "gemini", null));
                    defaults.add(discoveryEngine.discoverCapabilities("gemini-2.0-flash-thinking", "gemini", null));
                    defaults.add(discoveryEngine.discoverCapabilities("deepseek-v3", "deepseek", null));
                    defaults.add(discoveryEngine.discoverCapabilities("deepseek-r1", "deepseek", null));
                    defaults.add(discoveryEngine.discoverCapabilities("llama-3.3-70b-instruct", "groq", null));
                    modelRepository.saveModels(defaults);
                }

                Conversation welcomeChat = conversationRepository.getConversationByIdSync("welcome_chat");
                if (welcomeChat == null) {
                    Conversation c = new Conversation("welcome_chat", "Welcome to OmniChat", "gpt-4o", "openai");
                    conversationRepository.insertConversation(c);
                }
            } catch (Exception ignored) {}
        });
    }

    public Context getContext() { return context; }
    public AppExecutors getExecutors() { return executors; }
    public OkHttpClient getHttpClient() { return httpClient; }
    public SecureKeyStorage getKeyStorage() { return keyStorage; }
    public AppDatabase getDatabase() { return database; }
    public CapabilityDiscoveryEngine getDiscoveryEngine() { return discoveryEngine; }
    public ProviderRegistry getProviderRegistry() { return providerRegistry; }

    public ConversationRepository getConversationRepository() { return conversationRepository; }
    public MessageRepository getMessageRepository() { return messageRepository; }
    public ProviderRepository getProviderRepository() { return providerRepository; }
    public ModelRepository getModelRepository() { return modelRepository; }
    public MediaRepository getMediaRepository() { return mediaRepository; }
}
