package com.example.domain.provider;

import com.example.domain.model.ErrorDescriptor;
import com.example.domain.model.ModelDescriptor;
import java.util.List;

public interface LlmProvider {
    String providerId();
    List<ModelDescriptor> fetchModels() throws Exception;
    ChatStream openChatStream(ChatRequest req);
    ChatResult chatOnce(ChatRequest req) throws Exception;
    MediaTask generateImage(ImageRequest req) throws Exception;
    MediaTask generateVideo(VideoRequest req) throws Exception;
    CapabilityProbeResult probe(ModelDescriptor m);
    ErrorDescriptor normalize(Throwable t);
}
