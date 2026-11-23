package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HazardAiService
import com.example.data.HazardRepository
import com.example.model.ChatMessage
import com.example.model.CreationCategory
import com.example.model.CreationItem
import com.example.model.MemoryPatch
import com.example.model.MemoryProfile
import com.example.model.MissionTopic
import com.example.model.SpeakerMode
import com.example.model.UiState
import com.example.security.MemoryPrivacyGuard
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class HazardTab(val route: String) {
    CHAT("chat"),
    MARKETS("markets"),
    MONEY("money"),
    PROJECTS("projects"),
    MEMORY("memory")
}

class HazardViewModel(
    private val repository: HazardRepository,
    private val aiService: HazardAiService,
    private val currentUserId: String,
    private val lockedWyattMode: Boolean = false
) : ViewModel() {

    val memoryState: StateFlow<UiState<MemoryProfile>> = repository.observeMemory(currentUserId)
        .map<MemoryProfile, UiState<MemoryProfile>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing memory", rootError)
            emit(UiState.Error(rootError.message ?: "Impossible de charger la Fiche Mémoire."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val wyattMemoryState: StateFlow<UiState<MemoryProfile>> = repository.observeWyattMemory(currentUserId)
        .map<MemoryProfile, UiState<MemoryProfile>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing wyatt memory", rootError)
            emit(UiState.Error(rootError.message ?: "Impossible de charger la mémoire de Wyatt."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val messagesState: StateFlow<UiState<List<ChatMessage>>> = repository.observeMessages(currentUserId)
        .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing messages", rootError)
            emit(UiState.Error(rootError.message ?: "Impossible de charger les messages."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val creationsState: StateFlow<UiState<List<CreationItem>>> = repository.observeCreations(currentUserId)
        .map<List<CreationItem>, UiState<List<CreationItem>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing creations", rootError)
            emit(UiState.Error(rootError.message ?: "Impossible de charger le Coffre."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedTab = MutableStateFlow(HazardTab.CHAT)
    val selectedTab: StateFlow<HazardTab> = _selectedTab.asStateFlow()

    private val _speakerMode = MutableStateFlow(SpeakerMode.UNKNOWN)
    val speakerMode: StateFlow<SpeakerMode> = _speakerMode.asStateFlow()

    private val _activeTopic = MutableStateFlow(MissionTopic.GENERAL)
    val activeTopic: StateFlow<MissionTopic> = _activeTopic.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _streamingText = MutableStateFlow<String?>(null)
    val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private val _isExtractingMemory = MutableStateFlow(false)
    val isExtractingMemory: StateFlow<Boolean> = _isExtractingMemory.asStateFlow()

    private val _pendingMemoryPatch = MutableStateFlow<MemoryPatch?>(null)
    val pendingMemoryPatch: StateFlow<MemoryPatch?> = _pendingMemoryPatch.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _preferFastGroq = MutableStateFlow(false)
    val preferFastGroq: StateFlow<Boolean> = _preferFastGroq.asStateFlow()

    init {
        if (lockedWyattMode) {
            _speakerMode.value = SpeakerMode.WYATT
            _activeTopic.value = MissionTopic.WYATT_ADVENTURE
        }
        viewModelScope.launch {
            repository.ensureInitialMemoryExists(currentUserId)
            repository.ensureInitialWyattMemoryExists(currentUserId)
        }
    }

    fun selectTab(tab: HazardTab) {
        if ((lockedWyattMode || _speakerMode.value == SpeakerMode.WYATT) &&
            (tab == HazardTab.MARKETS || tab == HazardTab.MONEY || tab == HazardTab.MEMORY)
        ) {
            return
        }
        _selectedTab.value = tab
    }

    fun setSpeakerMode(mode: SpeakerMode) {
        val effectiveMode = if (lockedWyattMode) SpeakerMode.WYATT else mode
        _speakerMode.value = effectiveMode
        if (effectiveMode == SpeakerMode.WYATT) {
            _activeTopic.value = MissionTopic.WYATT_ADVENTURE
            if (_selectedTab.value == HazardTab.MARKETS ||
                _selectedTab.value == HazardTab.MONEY ||
                _selectedTab.value == HazardTab.MEMORY
            ) {
                _selectedTab.value = HazardTab.CHAT
            }
        } else if (effectiveMode == SpeakerMode.RAPHAEL && _activeTopic.value == MissionTopic.WYATT_ADVENTURE) {
            _activeTopic.value = MissionTopic.GENERAL
        }
    }

    fun toggleFastGroq() {
        _preferFastGroq.value = !_preferFastGroq.value
    }

    fun setActiveTopic(topic: MissionTopic) {
        _activeTopic.value = topic
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showStatusBanner(msg: String) {
        _statusBanner.value = msg
    }

    fun clearAiError() {
        _aiError.value = null
    }

    fun launchPromptInChat(prompt: String, topic: MissionTopic) {
        if (_speakerMode.value == SpeakerMode.UNKNOWN) {
            _speakerMode.value = SpeakerMode.RAPHAEL
        }
        _activeTopic.value = topic
        _selectedTab.value = HazardTab.CHAT
        sendMessage(prompt, topicOverride = topic)
    }

    fun sendMessage(rawInput: String, topicOverride: MissionTopic? = null) {
        val text = rawInput.trim()
        if (text.isBlank() || _isGenerating.value) return

        val lower = text.lowercase(Locale.FRANCE)
        val detectedSpeaker = if (lockedWyattMode) {
            SpeakerMode.WYATT
        } else {
            when {
                lower.contains("c'est wyatt") || lower == "wyatt" -> SpeakerMode.WYATT
                lower.contains("c'est moi") || lower.contains("c'est raph") -> SpeakerMode.RAPHAEL
                else -> _speakerMode.value
            }
        }
        if (detectedSpeaker != _speakerMode.value) {
            setSpeakerMode(detectedSpeaker)
        }

        val topic = if (detectedSpeaker == SpeakerMode.WYATT) {
            MissionTopic.WYATT_ADVENTURE
        } else {
            topicOverride ?: _activeTopic.value
        }
        val currentMemory = (memoryState.value as? UiState.Success)?.data
            ?: MemoryProfile(userId = currentUserId)
        val currentWyattMemory = (wyattMemoryState.value as? UiState.Success)?.data
            ?: MemoryProfile(id = "wyatt", userId = currentUserId, firstName = "Wyatt")
        val promptMemory = if (detectedSpeaker == SpeakerMode.WYATT) {
            currentWyattMemory.copy(
                wyattLikes = currentWyattMemory.wyattLikes.ifBlank { currentMemory.wyattLikes },
                wyattWantsToLearn = currentWyattMemory.wyattWantsToLearn.ifBlank { currentMemory.wyattWantsToLearn },
                wyattMemories = currentWyattMemory.wyattMemories.ifBlank { currentMemory.wyattMemories }
            )
        } else {
            currentMemory
        }
        val rawHistory = (messagesState.value as? UiState.Success)?.data.orEmpty()
        val history = if (detectedSpeaker == SpeakerMode.WYATT) {
            rawHistory.filter { it.speakerMode == SpeakerMode.WYATT.wireValue }
        } else {
            rawHistory
        }

        viewModelScope.launch {
            _aiError.value = null
            _isGenerating.value = true
            _streamingText.value = ""

            val saveUserResult = repository.addMessage(
                userId = currentUserId,
                role = "user",
                speakerMode = detectedSpeaker,
                topic = topic,
                text = text,
                requiresConfirmation = false
            )
            if (saveUserResult.isFailure) {
                _isGenerating.value = false
                _streamingText.value = null
                _aiError.value = saveUserResult.exceptionOrNull()?.localizedMessage
                    ?: "Impossible d'enregistrer le message."
                return@launch
            }

            try {
                var finalReply = ""
                aiService.streamReply(
                    speakerMode = detectedSpeaker,
                    memory = promptMemory,
                    topic = topic,
                    history = history,
                    userPrompt = text,
                    preferFastGroq = _preferFastGroq.value && detectedSpeaker != SpeakerMode.WYATT
                ).collect { partial ->
                    finalReply = partial
                    _streamingText.value = partial
                }

                val requiresConfirm = finalReply.contains("[ACTION_IMPORTANTE]")
                val cleanReply = finalReply.replace("[ACTION_IMPORTANTE]", "").trim()

                repository.addMessage(
                    userId = currentUserId,
                    role = "hazard",
                    speakerMode = detectedSpeaker,
                    topic = topic,
                    text = cleanReply,
                    requiresConfirmation = requiresConfirm
                )

                // Automatic durable memory extraction from user's own words only (never Hazard's suppositions)
                if (text.length >= 12) {
                    val userOnlyMessage = ChatMessage(
                        userId = currentUserId,
                        role = "user",
                        speakerMode = detectedSpeaker.wireValue,
                        topic = topic.wireValue,
                        text = text
                    )
                    launch {
                        autoRetainDurableMemory(
                            speakerMode = detectedSpeaker,
                            currentMain = currentMemory,
                            currentWyatt = promptMemory,
                            recentMessages = history + userOnlyMessage
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini request timed out", e)
                _aiError.value = "Hazard a mis plus de 60 secondes à répondre. Réessaie."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed", e)
                _aiError.value = e.localizedMessage ?: "Erreur lors de la réponse de Hazard."
            } finally {
                _isGenerating.value = false
                _streamingText.value = null
            }
        }
    }

    private suspend fun autoRetainDurableMemory(
        speakerMode: SpeakerMode,
        currentMain: MemoryProfile,
        currentWyatt: MemoryProfile,
        recentMessages: List<ChatMessage>
    ) {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())
        try {
            if (speakerMode == SpeakerMode.WYATT) {
                val patch = aiService.extractWyattMinimalMemory(currentWyatt, recentMessages)
                if (!patch.hasAnyChange()) return
                val mergedLikes = MemoryPrivacyGuard.mergeLines(currentWyatt.wyattLikes, patch.wyattLikes, 1000, wyattMinimal = true)
                val mergedLearn = MemoryPrivacyGuard.mergeLines(currentWyatt.wyattWantsToLearn, patch.wyattWantsToLearn, 1000, wyattMinimal = true)
                val mergedMem = MemoryPrivacyGuard.mergeLines(currentWyatt.wyattMemories, patch.wyattMemories, 2500, wyattMinimal = true)
                val changed = mergedLikes != currentWyatt.wyattLikes ||
                    mergedLearn != currentWyatt.wyattWantsToLearn ||
                    mergedMem != currentWyatt.wyattMemories
                if (changed) {
                    repository.saveWyattMemory(
                        userId = currentUserId,
                        likes = mergedLikes,
                        learned = mergedLearn,
                        memories = mergedMem,
                        dateLabel = today
                    )
                    if (!lockedWyattMode) {
                        val syncedMain = currentMain.copy(
                            wyattLikes = mergedLikes,
                            wyattWantsToLearn = mergedLearn,
                            wyattMemories = mergedMem
                        )
                        repository.saveMemoryProfile(currentUserId, syncedMain, today)
                    }
                }
            } else {
                val patch = aiService.extractMemoryUpdates(currentMain, recentMessages)
                if (!patch.hasAnyChange()) return
                val merged = repository.applyPatch(currentMain, patch)
                if (merged != currentMain) {
                    val res = repository.saveMemoryProfile(currentUserId, merged, today)
                    if (merged.wyattLikes != currentMain.wyattLikes ||
                        merged.wyattWantsToLearn != currentMain.wyattWantsToLearn ||
                        merged.wyattMemories != currentMain.wyattMemories
                    ) {
                        repository.saveWyattMemory(
                            userId = currentUserId,
                            likes = merged.wyattLikes,
                            learned = merged.wyattWantsToLearn,
                            memories = merged.wyattMemories,
                            dateLabel = today
                        )
                    }
                    if (res.isSuccess) {
                        _statusBanner.value = "Mémoire mise à jour automatiquement."
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Auto memory retention skipped: ${e.javaClass.simpleName}")
        }
    }

    fun requestMemoryInterview() {
        val memory = (memoryState.value as? UiState.Success)?.data
            ?: MemoryProfile(userId = currentUserId)
        val missing = memory.missingFieldLabels()
        val prompt = if (missing.isEmpty()) {
            "Ma Fiche Mémoire est déjà bien remplie ! Pose-moi 2 questions malignes pour l'approfondir encore (projets, musique ou trading)."
        } else {
            "Interviewe-moi pour compléter ma Fiche Mémoire. Il manque encore : ${missing.joinToString(", ")}. Pose-moi 2 ou 3 questions courtes et fun sur un seul thème pour commencer."
        }
        launchPromptInChat(prompt, MissionTopic.GENERAL)
    }

    fun extractMemoryFromConversation() {
        val currentMemory = (memoryState.value as? UiState.Success)?.data ?: return
        val history = (messagesState.value as? UiState.Success)?.data.orEmpty()
        if (history.isEmpty()) {
            _statusBanner.value = "Discute un peu avec Hazard avant de mettre à jour la fiche."
            return
        }

        viewModelScope.launch {
            _isExtractingMemory.value = true
            _aiError.value = null
            try {
                val patch = aiService.extractMemoryUpdates(currentMemory, history)
                if (patch.hasAnyChange()) {
                    _pendingMemoryPatch.value = patch
                } else {
                    _statusBanner.value = "Aucune nouvelle information explicite détectée dans cet échange."
                }
            } catch (e: TimeoutCancellationException) {
                _aiError.value = "Extraction de la fiche expirée (timeout)."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Memory extraction failed", e)
                _aiError.value = e.localizedMessage ?: "Impossible d'analyser la conversation."
            } finally {
                _isExtractingMemory.value = false
            }
        }
    }

    fun confirmPendingMemoryPatch() {
        val patch = _pendingMemoryPatch.value ?: return
        val current = (memoryState.value as? UiState.Success)?.data ?: return
        val merged = repository.applyPatch(current, patch)
        _pendingMemoryPatch.value = null
        saveMemory(merged)
    }

    fun dismissPendingMemoryPatch() {
        _pendingMemoryPatch.value = null
    }

    fun saveMemory(updated: MemoryProfile) {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())
        viewModelScope.launch {
            val result = repository.saveMemoryProfile(currentUserId, updated, today)
            repository.saveWyattMemory(
                userId = currentUserId,
                likes = updated.wyattLikes,
                learned = updated.wyattWantsToLearn,
                memories = updated.wyattMemories,
                dateLabel = today
            )
            _statusBanner.value = if (result.isSuccess) {
                "Mémoire sauvegardée dans Firebase."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Erreur lors de la sauvegarde."
            }
        }
    }

    fun clearAllMemory() {
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())
        viewModelScope.launch {
            val result = repository.clearAllMemory(currentUserId, today)
            _statusBanner.value = if (result.isSuccess) {
                "Toute la mémoire de Hazard et Wyatt a été effacée."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Impossible d'effacer la mémoire."
            }
        }
    }

    fun saveCreation(
        category: CreationCategory,
        title: String,
        content: String,
        tags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            val result = repository.addCreation(
                userId = currentUserId,
                category = category,
                title = title,
                content = content,
                tags = tags
            )
            _statusBanner.value = if (result.isSuccess) {
                "Enregistré dans l'Atelier (${category.label})."
            } else {
                result.exceptionOrNull()?.localizedMessage ?: "Impossible d'enregistrer."
            }
        }
    }

    fun toggleCreationPin(item: CreationItem) {
        viewModelScope.launch {
            repository.toggleCreationPinned(currentUserId, item.id, item.pinned)
        }
    }

    fun deleteCreation(item: CreationItem) {
        viewModelScope.launch {
            val result = repository.deleteCreation(currentUserId, item.id)
            if (result.isFailure) {
                _statusBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Suppression impossible."
            }
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            val result = repository.clearConversation(currentUserId)
            if (result.isSuccess) {
                _speakerMode.value = SpeakerMode.UNKNOWN
                _statusBanner.value = "Nouvelle session démarrée."
            } else {
                _statusBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Impossible d'effacer la session."
            }
        }
    }

    private companion object {
        const val TAG = "HazardVM"
    }
}
