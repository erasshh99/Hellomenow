# OmniChat Verification & Test Plan

This document outlines the testing protocols, automated verification, and edge-case validation suites for OmniChat.

---

## 1. Automated Build Verification

- **Language Compliance**: Strict Java 17 source and bytecode targets. No Kotlin standard library or Kotlin runtime.
- **UI Engine**: Android Views + XML + ViewBinding. Zero Compose artifacts.
- **Gradle Build Task**:
  ```bash
  gradle :app:assembleDebug
  ```
  Result: **SUCCESSFUL**

---

## 2. Core Functional Verification Checklist

### A. Provider Configuration & Keystore
- [x] **Secure Keystore & AES-256-GCM Storage**: API keys are securely persisted via `EncryptedSharedPreferences` and MasterKey.
- [x] **Key Masking**: UI screens display keys in masked format (`sk-ant-••••••••1234`).
- [x] **Custom Endpoints**: Tested with OpenAI compatible base URLs (e.g. `http://localhost:11434/v1` for Ollama or LM Studio, OpenRouter, Groq).
- [x] **Custom Headers**: Support for bearer auth, org IDs, and arbitrary key-value headers.

### B. Chat & SSE Streaming Engine
- [x] **Incremental Token Streaming**: Smooth token delivery via `ServerSentEventParser`.
- [x] **Deep Reasoning Separation**: `<think>` and `thought` tags parsed and routed into expandable `Reasoning` collapsible cards.
- [x] **Stream Interruption & Cancellation**: `ChatStream.cancel()` immediately aborts in-flight OkHttp calls without leaking threads.
- [x] **Network Disconnects & Timeouts**: Handled with `SocketTimeoutException` mapping to retryable `ErrorDescriptor`.

### C. Autonomous Capability Discovery
- [x] **Curated Stage 1**: Correctly identifies GPT-4o, Claude 3.5 Sonnet, Gemini 1.5 Pro with pre-loaded profiles.
- [x] **Heuristic Stage 2**: Correctly flags models with `vision`, `r1`, `flux`, etc.
- [x] **Live Probe Stage 4**: Ephemeral 1-token query executes and updates confidence to 100%.

### D. Mid-Conversation Switching & Transcoding
- [x] **Multimodal to Text-Only**: ContextTranscoder transcribes images into textual transcript placeholders when switching to models without `visionInput`.
- [x] **System Prompt Fallback**: Re-encodes system messages for models lacking dedicated system role support.
- [x] **Token Truncation**: Sliding-window context pruner keeps payloads within target model's `contextWindow`.

### E. Multimodal Media Generation
- [x] **Image Generation**: Unified `ImageRequest` dispatches to DALL-E 3 / Imagen endpoints.
- [x] **Asynchronous Video Polling**: `VideoGenerationService` runs as an Android Foreground Service with continuous status notifications until completion.
- [x] **Media Gallery**: Persisted in Room database and viewable in grid gallery with full detail view and sharing.

---

## 3. Manual Test Steps for Device / Emulator

1. **Launch App**: Verify Bottom Navigation shows `Chat`, `Chats`, `Providers`, `Models`, `Media`, and `Settings`.
2. **Setup Provider**: Tap `Providers` -> `+` -> Choose OpenAI, Anthropic, Gemini, or OpenAI-Compatible. Enter Base URL and API Key. Save.
3. **Discover Models**: Tap `Sync Models` on the provider card. View automatically detected capabilities, speed tiers, and pricing.
4. **Chat**: Tap `Chat` -> send a message. Verify real-time token rendering and latency / token usage metrics on completion.
5. **Switch Model**: Tap top-bar model dropdown -> select another model mid-thread -> continue conversation.
