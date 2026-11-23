package com.robloxai.studio.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.robloxai.studio.data.local.*
import com.robloxai.studio.data.model.*
import com.robloxai.studio.data.remote.AiApiService
import com.robloxai.studio.data.remote.AiResult
import com.robloxai.studio.data.security.ApiKeyVault
import com.robloxai.studio.domain.RobloxPrompts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val vault = ApiKeyVault(app)
    private val dao = AppDatabase.getInstance(app).studioDao()
    private val api = AiApiService()

    val projects: StateFlow<List<ProjectEntity>> = dao.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeProjectId = MutableStateFlow<Long?>(null)
    val activeProjectId: StateFlow<Long?> = _activeProjectId.asStateFlow()

    val activeProject: StateFlow<ProjectEntity?> = combine(projects, activeProjectId) { list, id ->
        list.firstOrNull { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val projectScripts: StateFlow<List<ScriptEntity>> = activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList()) else dao.observeScripts(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectChat: StateFlow<List<ChatMessageEntity>> = activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList()) else dao.observeChat(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val projectConcepts3D: StateFlow<List<Concept3DEntity>> = activeProject
        .flatMapLatest { proj ->
            if (proj == null) flowOf(emptyList()) else dao.observeConcepts3D(proj.id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProvider = MutableStateFlow(vault.getSelectedProvider())
    val selectedProvider: StateFlow<AiProvider> = _selectedProvider.asStateFlow()

    private val _selectedModel = MutableStateFlow(vault.getSelectedModel(vault.getSelectedProvider()))
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _availableModels = MutableStateFlow<Map<AiProvider, List<ModelOption>>>(
        AiProvider.entries.associateWith { it.defaultModels }
    )
    val availableModels: StateFlow<Map<AiProvider, List<ModelOption>>> = _availableModels.asStateFlow()

    private val _providerStatuses = MutableStateFlow<Map<AiProvider, ProviderStatusInfo>>(
        AiProvider.entries.associateWith { ProviderStatusInfo() }
    )
    val providerStatuses: StateFlow<Map<AiProvider, ProviderStatusInfo>> = _providerStatuses.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    // Editor state
    private val _editorScriptId = MutableStateFlow<Long?>(null)
    val editorScriptId: StateFlow<Long?> = _editorScriptId.asStateFlow()

    private val _editorTitle = MutableStateFlow("NewSystem")
    val editorTitle: StateFlow<String> = _editorTitle.asStateFlow()

    private val _editorScriptType = MutableStateFlow(RobloxScriptType.SCRIPT)
    val editorScriptType: StateFlow<RobloxScriptType> = _editorScriptType.asStateFlow()

    private val _editorCode = MutableStateFlow(
        "--!strict\nlocal ReplicatedStorage = game:GetService(\"ReplicatedStorage\")\n\nprint(\"Roblox AI Studio Assistant Ready\")\n"
    )
    val editorCode: StateFlow<String> = _editorCode.asStateFlow()

    private val _editorNotes = MutableStateFlow("")
    val editorNotes: StateFlow<String> = _editorNotes.asStateFlow()

    // Error fixer state
    private val _errorAnalysisResult = MutableStateFlow("")
    val errorAnalysisResult: StateFlow<String> = _errorAnalysisResult.asStateFlow()

    private val _errorFixedCode = MutableStateFlow("")
    val errorFixedCode: StateFlow<String> = _errorFixedCode.asStateFlow()

    // 3D Builder state
    private val _latest3DOutput = MutableStateFlow("")
    val latest3DOutput: StateFlow<String> = _latest3DOutput.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = dao.observeProjects().first()
            if (existing.isEmpty()) {
                val defaultId = dao.insertProject(
                    ProjectEntity(
                        name = "My First Obby / RPG",
                        genre = "Action RPG",
                        instructions = "Use modular ReplicatedStorage scripts, server-authoritative combat, and low-poly assets.",
                        progressNotes = "1. Setup DataStore\n2. Add Shop & Currency\n3. Build NPC Quest Giver"
                    )
                )
                _activeProjectId.value = defaultId
            } else if (_activeProjectId.value == null) {
                _activeProjectId.value = existing.first().id
            }
        }
        refreshAllConnectionStates()
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun selectProject(projectId: Long) {
        if (_activeProjectId.value != projectId) {
            _activeProjectId.value = projectId
            _editorScriptId.value = null
            _editorNotes.value = ""
        }
    }

    fun createProject(name: String, genre: String, instructions: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val id = dao.insertProject(
                ProjectEntity(
                    name = trimmed,
                    genre = genre.trim().ifBlank { "General" },
                    instructions = instructions.trim(),
                    progressNotes = "Project initialized"
                )
            )
            _activeProjectId.value = id
            _editorScriptId.value = null
            _editorNotes.value = ""
            _bannerMessage.value = "Created project \"$trimmed\""
        }
    }

    fun updateProjectNotes(project: ProjectEntity, instructions: String, progress: String) {
        viewModelScope.launch {
            dao.updateProject(
                project.copy(
                    instructions = instructions,
                    progressNotes = progress,
                    updatedAt = System.currentTimeMillis()
                )
            )
            _bannerMessage.value = "Project notes saved"
        }
    }

    fun deleteProject(project: ProjectEntity) {
        viewModelScope.launch {
            dao.deleteProject(project)
            if (_activeProjectId.value == project.id) {
                _activeProjectId.value = projects.value.firstOrNull { it.id != project.id }?.id
                _editorScriptId.value = null
                _editorNotes.value = ""
            }
        }
    }

    fun selectProvider(provider: AiProvider) {
        _selectedProvider.value = provider
        vault.saveSelectedProvider(provider)
        val model = vault.getSelectedModel(provider)
        _selectedModel.value = model
        verifyProvider(provider)
    }

    fun selectModel(modelId: String) {
        _selectedModel.value = modelId
        vault.saveSelectedModel(_selectedProvider.value, modelId)
    }

    fun getStoredApiKey(provider: AiProvider): String = vault.getApiKey(provider)
    fun isRelayEnabled(): Boolean = vault.isRelayEnabled()
    fun getRelayUrl(): String = vault.getRelayUrl()
    fun getRelayToken(): String = vault.getRelayToken()

    fun saveCredentialsAndVerify(provider: AiProvider, key: String) {
        if (key.isBlank()) vault.clearApiKey(provider) else vault.saveApiKey(provider, key)
        verifyProvider(provider)
    }

    fun saveRelaySettings(enabled: Boolean, url: String, token: String) {
        vault.setRelayEnabled(enabled)
        vault.saveRelayUrl(url)
        vault.saveRelayToken(token)
        refreshAllConnectionStates()
    }

    fun refreshAllConnectionStates() {
        AiProvider.entries.forEach { verifyProvider(it) }
    }

    fun verifyProvider(provider: AiProvider) {
        verifyProviderWithKey(provider, vault.getApiKey(provider))
    }

    fun verifyProviderWithKey(provider: AiProvider, apiKey: String) {
        viewModelScope.launch {
            _providerStatuses.update {
                it + (provider to ProviderStatusInfo(ConnectionStatus.CHECKING, "Verifying credentials..."))
            }
            val result = api.verifyConnection(
                provider = provider,
                apiKey = apiKey,
                relayEnabled = vault.isRelayEnabled(),
                relayUrl = vault.getRelayUrl(),
                relayToken = vault.getRelayToken()
            )
            _providerStatuses.update { it + (provider to result) }
        }
    }

    fun refreshOpenRouterFreeModels() {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                when (val res = api.fetchOpenRouterFreeModels(vault.getApiKey(AiProvider.OPENROUTER))) {
                    is AiResult.Success -> {
                        _availableModels.update { it + (AiProvider.OPENROUTER to res.data) }
                        _bannerMessage.value = "Loaded ${res.data.size} free OpenRouter models"
                    }
                    is AiResult.Failure -> {
                        _bannerMessage.value = res.errorMessage
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun sendChatMessage(promptText: String) {
        val project = activeProject.value ?: return
        val trimmedPrompt = promptText.trim()
        if (trimmedPrompt.isBlank()) return
        val provider = _selectedProvider.value
        val model = _selectedModel.value

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val priorHistory = projectChat.value
                    .filterNot { it.role == "assistant" && it.content.startsWith("⚠️ [") }
                    .map { ChatTurn(it.id, it.role, it.content, it.providerName, it.modelId, it.createdAt) }

                dao.insertChat(
                    ChatMessageEntity(
                        projectId = project.id,
                        role = "user",
                        content = trimmedPrompt,
                        providerName = provider.displayName,
                        modelId = model
                    )
                )

                val history = (priorHistory + ChatTurn(
                    role = "user",
                    content = trimmedPrompt,
                    providerName = provider.displayName,
                    modelId = model
                )).takeLast(14)

                val res = api.sendChat(
                    provider = provider,
                    modelId = model,
                    apiKey = vault.getApiKey(provider),
                    systemPrompt = RobloxPrompts.unifiedAssistantSystem(project.name, project.instructions),
                    history = history,
                    relayEnabled = vault.isRelayEnabled(),
                    relayUrl = vault.getRelayUrl(),
                    relayToken = vault.getRelayToken()
                )

                when (res) {
                    is AiResult.Success -> {
                        dao.insertChat(
                            ChatMessageEntity(
                                projectId = project.id,
                                role = "assistant",
                                content = res.data,
                                providerName = provider.displayName,
                                modelId = model
                            )
                        )
                        _providerStatuses.update {
                            it + (provider to ProviderStatusInfo(ConnectionStatus.CONNECTED, "Connected"))
                        }
                    }
                    is AiResult.Failure -> {
                        _providerStatuses.update {
                            it + (provider to ProviderStatusInfo(res.status, res.errorMessage))
                        }
                        dao.insertChat(
                            ChatMessageEntity(
                                projectId = project.id,
                                role = "assistant",
                                content = "⚠️ [${provider.displayName} Error]\n${res.errorMessage}",
                                providerName = provider.displayName,
                                modelId = model
                            )
                        )
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun clearProjectChat() {
        val project = activeProject.value ?: return
        viewModelScope.launch { dao.clearChat(project.id) }
    }

    fun generateScriptToEditor(
        title: String,
        scriptType: RobloxScriptType,
        specification: String,
        includeComments: Boolean
    ) {
        val project = activeProject.value ?: return
        if (specification.isBlank()) return
        val provider = _selectedProvider.value
        val model = _selectedModel.value

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val prompt = RobloxPrompts.scriptGeneratorPrompt(scriptType, specification, includeComments)
                val res = api.sendChat(
                    provider = provider,
                    modelId = model,
                    apiKey = vault.getApiKey(provider),
                    systemPrompt = RobloxPrompts.unifiedAssistantSystem(project.name, project.instructions),
                    history = listOf(ChatTurn(role = "user", content = prompt, providerName = provider.displayName, modelId = model)),
                    relayEnabled = vault.isRelayEnabled(),
                    relayUrl = vault.getRelayUrl(),
                    relayToken = vault.getRelayToken()
                )

                when (res) {
                    is AiResult.Success -> {
                        val code = RobloxPrompts.extractFirstLuaBlock(res.data) ?: res.data
                        _editorScriptId.value = null
                        _editorTitle.value = title.trim().ifBlank { "GeneratedScript" }
                        _editorScriptType.value = scriptType
                        _editorCode.value = code
                        _editorNotes.value = "Where it belongs: ${scriptType.recommendedParent}\n\n${res.data}"
                        saveCurrentScriptToProject()
                    }
                    is AiResult.Failure -> {
                        _providerStatuses.update { it + (provider to ProviderStatusInfo(res.status, res.errorMessage)) }
                        _bannerMessage.value = res.errorMessage
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun startNewScriptInEditor() {
        _editorScriptId.value = null
        _editorTitle.value = "NewScript"
        _editorScriptType.value = RobloxScriptType.SCRIPT
        _editorCode.value = "--!strict\nlocal ReplicatedStorage = game:GetService(\"ReplicatedStorage\")\n\n"
        _editorNotes.value = ""
    }

    fun loadScriptIntoEditor(entity: ScriptEntity) {
        _editorScriptId.value = entity.id
        _editorTitle.value = entity.title
        _editorScriptType.value = RobloxScriptType.entries.firstOrNull { it.name == entity.scriptType }
            ?: RobloxScriptType.SCRIPT
        _editorCode.value = entity.code
        _editorNotes.value = entity.notes
    }

    fun setEditorFields(title: String, type: RobloxScriptType, code: String) {
        _editorTitle.value = title
        _editorScriptType.value = type
        _editorCode.value = code
    }

    fun saveCodeSnippetToEditor(title: String, code: String) {
        _editorScriptId.value = null
        _editorTitle.value = title.trim().ifBlank { "GeneratedScript" }
        _editorCode.value = code
        _editorNotes.value = ""
        saveCurrentScriptToProject()
    }

    fun saveCurrentScriptToProject() {
        val project = activeProject.value ?: return
        val scriptTitle = _editorTitle.value.trim().ifBlank { "UntitledScript" }
        val scriptType = _editorScriptType.value
        val scriptCode = _editorCode.value
        val scriptNotes = _editorNotes.value
        val currentId = _editorScriptId.value ?: 0L

        viewModelScope.launch {
            val id = dao.upsertScript(
                ScriptEntity(
                    id = currentId,
                    projectId = project.id,
                    title = scriptTitle,
                    scriptType = scriptType.name,
                    studioLocation = scriptType.recommendedParent,
                    code = scriptCode,
                    notes = scriptNotes
                )
            )
            _editorScriptId.value = id
            _bannerMessage.value = "Saved $scriptTitle to ${project.name}"
        }
    }

    fun deleteScript(entity: ScriptEntity) {
        viewModelScope.launch {
            dao.deleteScript(entity)
            if (_editorScriptId.value == entity.id) {
                _editorScriptId.value = null
            }
        }
    }

    fun analyzeAndFixError(studioError: String, brokenCode: String) {
        val project = activeProject.value ?: return
        if (studioError.isBlank()) return
        val provider = _selectedProvider.value
        val model = _selectedModel.value

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val prompt = RobloxPrompts.errorFixerPrompt(studioError, brokenCode)
                val res = api.sendChat(
                    provider = provider,
                    modelId = model,
                    apiKey = vault.getApiKey(provider),
                    systemPrompt = RobloxPrompts.unifiedAssistantSystem(project.name, project.instructions),
                    history = listOf(ChatTurn(role = "user", content = prompt, providerName = provider.displayName, modelId = model)),
                    relayEnabled = vault.isRelayEnabled(),
                    relayUrl = vault.getRelayUrl(),
                    relayToken = vault.getRelayToken()
                )
                when (res) {
                    is AiResult.Success -> {
                        _errorAnalysisResult.value = res.data
                        _errorFixedCode.value = RobloxPrompts.extractFirstLuaBlock(res.data) ?: ""
                    }
                    is AiResult.Failure -> {
                        _providerStatuses.update { it + (provider to ProviderStatusInfo(res.status, res.errorMessage)) }
                        _bannerMessage.value = res.errorMessage
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun generate3DConcept(assetName: String, category: String, style: String, polyBudget: String) {
        val project = activeProject.value ?: return
        if (assetName.isBlank()) return
        val provider = _selectedProvider.value
        val model = _selectedModel.value

        viewModelScope.launch {
            _isBusy.value = true
            try {
                val prompt = RobloxPrompts.model3DPrompt(assetName, category, style, polyBudget)
                val res = api.sendChat(
                    provider = provider,
                    modelId = model,
                    apiKey = vault.getApiKey(provider),
                    systemPrompt = RobloxPrompts.unifiedAssistantSystem(project.name, project.instructions),
                    history = listOf(ChatTurn(role = "user", content = prompt, providerName = provider.displayName, modelId = model)),
                    relayEnabled = vault.isRelayEnabled(),
                    relayUrl = vault.getRelayUrl(),
                    relayToken = vault.getRelayToken()
                )
                when (res) {
                    is AiResult.Success -> {
                        _latest3DOutput.value = res.data
                        val extractedPrompt = RobloxPrompts.extractPromptBlock(res.data)
                            ?: "$style low-poly $assetName ($category) game-ready asset, clean topology, $polyBudget"
                        dao.insertConcept3D(
                            Concept3DEntity(
                                projectId = project.id,
                                title = assetName.trim(),
                                category = category,
                                generatorPrompt = extractedPrompt,
                                conceptDetails = res.data
                            )
                        )
                    }
                    is AiResult.Failure -> {
                        _providerStatuses.update { it + (provider to ProviderStatusInfo(res.status, res.errorMessage)) }
                        _bannerMessage.value = res.errorMessage
                    }
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun deleteConcept3D(concept: Concept3DEntity) {
        viewModelScope.launch { dao.deleteConcept3D(concept) }
    }

    fun createLuaShareIntent(context: Context, title: String, scriptType: RobloxScriptType, code: String): Intent {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = title.replace(Regex("[^A-Za-z0-9_-]"), "_").ifBlank { "RobloxScript" }
        val file = File(exportDir, "$safeName${scriptType.extension}")
        file.writeText(code)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
