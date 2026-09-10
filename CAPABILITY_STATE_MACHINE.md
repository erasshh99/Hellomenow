# Capability State Machine & Autonomous Discovery

## 1. Overview & Pipeline Philosophy

AI model naming and feature sets evolve rapidly. Hardcoded lists quickly fall out of date, while blind assumptions cause silent API errors (e.g., sending image payloads to models that only accept text, or expecting reasoning tokens from standard completion endpoints).

OmniChat implements a **4-Stage Capability Discovery Pipeline** governed by a strict state machine.

---

## 2. The 4 Discovery Stages

| Stage | Name | Confidence | Cost / Latency | Mechanism |
|---|---|---|---|---|
| **Stage 1** | Curated Seed Registry | 95% | 0 ms (Local) | Pre-bundled profiles for known industry staples (`gpt-4o`, `claude-3-5-sonnet`, `gemini-1.5-pro`, `o1`, `deepseek-r1`). |
| **Stage 2** | Heuristic Name Parsing | 60% | <1 ms (Regex) | Regular expressions matching token keywords: `.*vision.*`, `.*r1.*`, `.*instruct.*`, `.*embed.*`, `.*dall-e.*`, `.*sora.*`, `.*veo.*`. |
| **Stage 3** | Response / JSON Schema Parsing | 80% | 50–300 ms (Catalog Sync) | Inspects metadata from `/v1/models` (e.g. OpenAI `owned_by`, Gemini `supportedGenerationMethods`, Ollama parameter keys). |
| **Stage 4** | Active 1-Token Live Probe | 100% | 200–800 ms (1 Token API Call) | Sends an ephemeral test payload (1-token completion or tool test) to definitively prove endpoint support. |

---

## 3. Capability State Machine Diagram

```mermaid
stateDiagram-v2
    [*] --> Stage1_Registry: Model ID Introduced

    state Stage1_Registry {
        [*] --> CheckCuratedProfiles
        CheckCuratedProfiles --> CuratedMatch: ID exists in Seed Database
        CheckCuratedProfiles --> UnknownProfile: ID not in Seed Database
    }

    CuratedMatch --> FinalCapabilitySet: Confidence = 95%\nSource = REGISTRY
    UnknownProfile --> Stage2_Heuristics: Progress to Stage 2

    state Stage2_Heuristics {
        [*] --> EvaluateRegexPatterns
        EvaluateRegexPatterns --> VisionPattern: Matches "(vision|vl|4v|multimodal)"
        EvaluateRegexPatterns --> ReasoningPattern: Matches "(o1|o3|r1|reasoning)"
        EvaluateRegexPatterns --> MediaPattern: Matches "(dall-e|imagen|flux|sora|veo)"
        EvaluateRegexPatterns --> TextPattern: Default Text Completion
    }

    Stage2_Heuristics --> HeuristicConfidence: Confidence = 60%\nSource = HEURISTIC
    HeuristicConfidence --> Stage3_SchemaAnalysis: Provider Catalog Sync

    state Stage3_SchemaAnalysis {
        [*] --> ParseRawModelJson
        ParseRawModelJson --> GeminiMethods: Contains "supportedGenerationMethods"
        ParseRawModelJson --> OpenAiSchema: Inspect "permission" & "context_window"
    }

    Stage3_SchemaAnalysis --> InferredConfidence: Confidence = 80%\nSource = INFERRED

    InferredConfidence --> Stage4_LiveProbe: User Requests Verification or Error Encountered

    state Stage4_LiveProbe {
        [*] --> Send1TokenPing
        Send1TokenPing --> ProbeSuccess: HTTP 200 + Valid Tokens
        Send1TokenPing --> ProbeFailModal: HTTP 400 Unsupported Modality
        Send1TokenPing --> ProbeAuthFail: HTTP 401/403 Invalid API Key
    }

    ProbeSuccess --> FinalCapabilitySet: Confidence = 100%\nSource = PROBED
    ProbeFailModal --> DowngradeCapability: Mark Specific Feature FALSE (e.g. visionInput=FALSE)
    DowngradeCapability --> FinalCapabilitySet: Confidence = 90%\nSource = PROBED
```

---

## 4. Mid-Conversation Model Switching & Modality Degradation

When switching models mid-conversation, conversations frequently contain assets that the new target model cannot natively process. The `ContextTranscoder` performs deterministic transformations:

```mermaid
flowchart TD
    Start[Mid-Conversation Model Switch Triggered] --> CheckModel[Inspect Target Model CapabilitySet]
    
    CheckModel --> CheckVision{Vision Supported?}
    CheckVision -- Yes --> KeepImages[Preserve Image ContentParts]
    CheckVision -- No --> DegradeImages[Transcode Images: Extract Alt-Text or Transcript]
    
    CheckModel --> CheckSystemPrompt{System Prompt Supported?}
    CheckSystemPrompt -- Yes --> SendSysPrompt[Deliver in standard system parameter]
    CheckSystemPrompt -- No --> PrependSysPrompt[Prepend System Prompt as First User Turn]
    
    CheckModel --> CheckContextBudget{Context Tokens Exceeded?}
    CheckContextBudget -- No --> BuildFinalPayload[Dispatch ChatRequest to Provider]
    CheckContextBudget -- Yes --> SummarizeOrPrune[Prune Oldest Messages, preserving System & Most Recent Turns]
    
    KeepImages --> CheckSystemPrompt
    DegradeImages --> Warning[Surface Non-Blocking Warning Banner in UI] --> CheckSystemPrompt
    SendSysPrompt --> CheckContextBudget
    PrependSysPrompt --> CheckContextBudget
    SummarizeOrPrune --> BuildFinalPayload
```

### Degradation Transformation Rules

1. **Vision Loss (`visionInput: TRUE -> FALSE`)**:
   - High-resolution image payloads are removed from the request array.
   - Replaced by a synthetic system message: `[Image omitted: Model lacks vision capability. Context preserved via previous assistant descriptions.]`
   - Non-blocking banner displayed to user: *"Active model lacks vision. Images in context were transcoded."*

2. **System Prompt Rejection (`systemPromptSupport: FALSE`)**:
   - Model providers that forbid `system` role objects (e.g., legacy Anthropic formats or certain local completion checkpoints) have their system instructions prepended to message 0 as: `[System Instructions: <instructions>]\n\nUser: <first message>`

3. **Tool Call Incompatibility (`functionCalling: FALSE`)**:
   - JSON structured tool definitions are transcoded into system-prompt ReAct-style text instructions: `Answer using the format: Action: <tool_name> Action Input: <json_args>`.
