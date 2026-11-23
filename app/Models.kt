package com.robloxai.studio.data.model

data class ModelOption(
    val id: String,
    val label: String,
    val isFree: Boolean = false
)

enum class AiProvider(
    val displayName: String,
    val baseUrl: String,
    val defaultModels: List<ModelOption>
) {
    OPENROUTER(
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        defaultModels = listOf(
            ModelOption("openrouter/free", "Free Models Router (Auto :free)", isFree = true),
            ModelOption("meta-llama/llama-3.3-70b-instruct:free", "Llama 3.3 70B Instruct (Free)", isFree = true),
            ModelOption("deepseek/deepseek-chat-v3-0324:free", "DeepSeek V3 (Free)", isFree = true),
            ModelOption("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet")
        )
    ),
    OPENAI(
        displayName = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        defaultModels = listOf(
            ModelOption("gpt-4o", "GPT-4o"),
            ModelOption("gpt-4o-mini", "GPT-4o mini"),
            ModelOption("o3-mini", "o3-mini")
        )
    ),
    ANTHROPIC(
        displayName = "Anthropic",
        baseUrl = "https://api.anthropic.com/v1",
        defaultModels = listOf(
            ModelOption("claude-3-7-sonnet-latest", "Claude 3.7 Sonnet"),
            ModelOption("claude-3-5-haiku-latest", "Claude 3.5 Haiku")
        )
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        baseUrl = "https://api.deepseek.com/v1",
        defaultModels = listOf(
            ModelOption("deepseek-chat", "DeepSeek Chat (V3)"),
            ModelOption("deepseek-reasoner", "DeepSeek Reasoner (R1)")
        )
    )
}

enum class ConnectionStatus {
    UNCONFIGURED,
    CHECKING,
    CONNECTED,
    AUTH_FAILED,
    RATE_LIMITED,
    ERROR
}

data class ProviderStatusInfo(
    val status: ConnectionStatus = ConnectionStatus.UNCONFIGURED,
    val message: String = "Not configured"
)

enum class RobloxScriptType(
    val label: String,
    val recommendedParent: String,
    val extension: String
) {
    SCRIPT(
        label = "Server Script",
        recommendedParent = "ServerScriptService",
        extension = ".server.lua"
    ),
    LOCAL_SCRIPT(
        label = "LocalScript",
        recommendedParent = "StarterPlayerScripts / StarterGui",
        extension = ".client.lua"
    ),
    MODULE_SCRIPT(
        label = "ModuleScript",
        recommendedParent = "ReplicatedStorage",
        extension = ".lua"
    )
}

data class ChatTurn(
    val id: Long = 0L,
    val role: String,
    val content: String,
    val providerName: String = "",
    val modelId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
