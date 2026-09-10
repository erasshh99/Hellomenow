# OmniChat Setup & Integration Guide

## 1. Prerequisites & Environment

- **Android Studio**: Android Studio Koala / Ladybug or newer.
- **JDK**: Java Development Kit 17 (Eclipse Temurin 17 or OpenJDK 17).
- **Android SDK**:
  - `compileSdk`: 35
  - `minSdk`: 26 (Android 8.0 Oreo)
  - `targetSdk`: 35
- **Build System**: Gradle 8.x with Kotlin DSL (`build.gradle.kts`).

---

## 2. Project Architecture & Tooling

OmniChat is written entirely in **100% Java 17** with:
- **AndroidX ViewBinding** enabled (zero Jetpack Compose, zero Kotlin).
- **Room Database** for persistence.
- **EncryptedSharedPreferences** + Android Keystore for enterprise security.
- **OkHttp 4.x** with custom SSE parser for streaming.
- **Material Design 3 Components** for native Android styling.

---

## 3. Building and Running

### Command Line
```bash
# Clean and compile the debug APK
gradle :app:assembleDebug

# Run unit tests
gradle :app:testDebugUnitTest
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 4. Configuring Providers

When launching OmniChat for the first time, navigate to the **Providers** tab:

### 1. OpenAI
- Base URL: `https://api.openai.com/v1`
- API Key: `sk-...`
- Supports: GPT-4o, o1, o3-mini, DALL-E 3, Whisper, etc.

### 2. Anthropic
- Base URL: `https://api.anthropic.com/v1`
- API Key: `sk-ant-...`
- Supports: Claude 3.5 Sonnet, Claude 3.5 Haiku, Claude 3 Opus.

### 3. Google Gemini
- Base URL: `https://generativelanguage.googleapis.com`
- API Key: `AIza...`
- Supports: Gemini 2.0 Flash, Gemini 1.5 Pro, Gemini 1.5 Flash.

### 4. Local / Self-Hosted (Ollama, LM Studio, vLLM, LocalAI)
- Base URL: `http://10.0.2.2:11434/v1` (for Android Emulator to host machine) or local LAN IP `http://192.168.1.X:11434/v1`
- API Key: Optional / any dummy string
- Toggle **Local / Offline Endpoint** to automatically enable lenient timeouts.

---

## 5. Security & Privacy

- All API keys are encrypted at rest using AES-256-GCM via AndroidX Security's `MasterKey`.
- No telemetry or third-party tracking libraries are included.
- Communication with remote endpoints occurs over TLS 1.3 direct socket connections without intermediary proxy servers.
