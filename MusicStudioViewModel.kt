package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.StudioMetronomePlayer
import com.example.data.FrenchLyricAnalyzer
import com.example.data.HazardRepository
import com.example.data.MusicStudioAiService
import com.example.model.CreationCategory
import com.example.model.PersonalUniverse
import com.example.model.RhythmBeatPlan
import com.example.model.SmartCorrectionResult
import com.example.model.SongSectionContent
import com.example.model.SongSectionType
import com.example.model.SongVersionSnapshot
import com.example.model.StudioSong
import com.example.model.SunoExportBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class MusicStudioViewModel(
    private val repository: HazardRepository,
    private val aiService: MusicStudioAiService,
    private val currentUserId: String
) : ViewModel() {

    companion object {
        const val TAG_STUDIO_SONG = "studio_song"
        const val TAG_STUDIO_UNIVERSE = "studio_universe"
        private const val MAX_AUDIO_BYTES = 18 * 1024 * 1024
    }

    val metronome = StudioMetronomePlayer()

    private val creationsFlow = repository.observeCreations(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val songs: StateFlow<List<StudioSong>> = creationsFlow
        .map { list ->
            list.filter { it.category == CreationCategory.MUSIC.wireValue && TAG_STUDIO_UNIVERSE !in it.tags }
                .map { StudioSong.fromJsonOrPlain(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val universeCreationPair: StateFlow<Pair<String, PersonalUniverse>> = creationsFlow
        .map { list ->
            val doc = list.firstOrNull {
                it.category == CreationCategory.MUSIC.wireValue && TAG_STUDIO_UNIVERSE in it.tags
            }
            if (doc == null) "" to PersonalUniverse()
            else doc.id to PersonalUniverse.fromJson(doc.content)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "" to PersonalUniverse())

    val universe: StateFlow<PersonalUniverse> = universeCreationPair
        .map { it.second }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PersonalUniverse())

    private val _activeSong = MutableStateFlow(StudioSong())
    val activeSong: StateFlow<StudioSong> = _activeSong.asStateFlow()

    private val _autoSaveStatus = MutableStateFlow("Prêt · Privé")
    val autoSaveStatus: StateFlow<String> = _autoSaveStatus.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    // Section rewrite & smart correction
    private val _passageRewriteResult = MutableStateFlow<String?>(null)
    val passageRewriteResult: StateFlow<String?> = _passageRewriteResult.asStateFlow()

    private val _isRewritingPassage = MutableStateFlow(false)
    val isRewritingPassage: StateFlow<Boolean> = _isRewritingPassage.asStateFlow()

    private val _smartCorrectionResult = MutableStateFlow<SmartCorrectionResult?>(null)
    val smartCorrectionResult: StateFlow<SmartCorrectionResult?> = _smartCorrectionResult.asStateFlow()

    private val _isCorrecting = MutableStateFlow(false)
    val isCorrecting: StateFlow<Boolean> = _isCorrecting.asStateFlow()

    // Rhymes & Words
    private val _rhymeResult = MutableStateFlow<String?>(null)
    val rhymeResult: StateFlow<String?> = _rhymeResult.asStateFlow()

    private val _isSearchingRhymes = MutableStateFlow(false)
    val isSearchingRhymes: StateFlow<Boolean> = _isSearchingRhymes.asStateFlow()

    private val _lexicalResult = MutableStateFlow<String?>(null)
    val lexicalResult: StateFlow<String?> = _lexicalResult.asStateFlow()

    private val _isGeneratingLexical = MutableStateFlow(false)
    val isGeneratingLexical: StateFlow<Boolean> = _isGeneratingLexical.asStateFlow()

    private val _clicheReport = MutableStateFlow<String?>(null)
    val clicheReport: StateFlow<String?> = _clicheReport.asStateFlow()

    private val _isAuditingCliches = MutableStateFlow(false)
    val isAuditingCliches: StateFlow<Boolean> = _isAuditingCliches.asStateFlow()

    // Rhythm & Flows
    private val _rhythmPlan = MutableStateFlow(
        RhythmBeatPlan(
            bpm = 92,
            style = "Boom bap sombre",
            activeSteps = setOf(0, 2, 4, 6, 8, 10, 12, 14),
            accentSteps = setOf(0, 4, 8, 12),
            breathSteps = setOf(15),
            stepSyllableHints = listOf("Syl", "·", "Syl", "·", "ACC", "·", "Syl", "·", "ACC", "·", "Syl", "·", "ACC", "·", "Fin", "RESP"),
            offbeatAdvice = "Lance le métronome puis clique 'Générer flow IA' pour adapter les 16 cases à ton couplet.",
            flows = emptyList()
        )
    )
    val rhythmPlan: StateFlow<RhythmBeatPlan> = _rhythmPlan.asStateFlow()

    private val _isGeneratingRhythm = MutableStateFlow(false)
    val isGeneratingRhythm: StateFlow<Boolean> = _isGeneratingRhythm.asStateFlow()

    // Personal Universe
    private val _isAnalyzingUniverse = MutableStateFlow(false)
    val isAnalyzingUniverse: StateFlow<Boolean> = _isAnalyzingUniverse.asStateFlow()

    // Formation & Ateliers
    private val _rapCoachAnswer = MutableStateFlow<String?>(null)
    val rapCoachAnswer: StateFlow<String?> = _rapCoachAnswer.asStateFlow()

    private val _isCoachLoading = MutableStateFlow(false)
    val isCoachLoading: StateFlow<Boolean> = _isCoachLoading.asStateFlow()

    private val _dailyChallengeText = MutableStateFlow<String?>(null)
    val dailyChallengeText: StateFlow<String?> = _dailyChallengeText.asStateFlow()

    private val _dailyChallengeFeedback = MutableStateFlow<String?>(null)
    val dailyChallengeFeedback: StateFlow<String?> = _dailyChallengeFeedback.asStateFlow()

    private val _isDailyChallengeLoading = MutableStateFlow(false)
    val isDailyChallengeLoading: StateFlow<Boolean> = _isDailyChallengeLoading.asStateFlow()

    private val _freestyleWords = MutableStateFlow(FrenchLyricAnalyzer.randomFreestyleWords(5))
    val freestyleWords: StateFlow<List<String>> = _freestyleWords.asStateFlow()

    private val _freestyleFeedback = MutableStateFlow<String?>(null)
    val freestyleFeedback: StateFlow<String?> = _freestyleFeedback.asStateFlow()

    private val _isFreestyleEvaluating = MutableStateFlow(false)
    val isFreestyleEvaluating: StateFlow<Boolean> = _isFreestyleEvaluating.asStateFlow()

    private val _trackReviewResult = MutableStateFlow<String?>(null)
    val trackReviewResult: StateFlow<String?> = _trackReviewResult.asStateFlow()

    private val _isReviewingTrack = MutableStateFlow(false)
    val isReviewingTrack: StateFlow<Boolean> = _isReviewingTrack.asStateFlow()

    private val _selectedAudioLabel = MutableStateFlow<String?>(null)
    val selectedAudioLabel: StateFlow<String?> = _selectedAudioLabel.asStateFlow()

    private var selectedAudioUri: Uri? = null

    // Suno & Release
    private val _sunoBundle = MutableStateFlow<SunoExportBundle?>(null)
    val sunoBundle: StateFlow<SunoExportBundle?> = _sunoBundle.asStateFlow()

    private val _isPreparingSuno = MutableStateFlow(false)
    val isPreparingSuno: StateFlow<Boolean> = _isPreparingSuno.asStateFlow()

    private var autoSaveJob: Job? = null
    private var hasInitializedFromDb = false

    init {
        viewModelScope.launch {
            songs.collect { list ->
                if (!hasInitializedFromDb && list.isNotEmpty() && _activeSong.value.id.isBlank() && _activeSong.value.fullLyrics().isBlank()) {
                    _activeSong.value = list.first()
                    _rhythmPlan.value = _rhythmPlan.value.copy(bpm = list.first().bpm, style = list.first().instruStyle)
                    hasInitializedFromDb = true
                }
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun selectSong(song: StudioSong) {
        autoSaveJob?.cancel()
        _activeSong.value = song
        _rhythmPlan.value = _rhythmPlan.value.copy(bpm = song.bpm, style = song.instruStyle)
        _smartCorrectionResult.value = null
        _passageRewriteResult.value = null
        _sunoBundle.value = null
        _autoSaveStatus.value = "Chargé · Privé"
    }

    fun createNewSong() {
        autoSaveJob?.cancel()
        _activeSong.value = StudioSong(title = "Nouveau morceau")
        _smartCorrectionResult.value = null
        _passageRewriteResult.value = null
        _sunoBundle.value = null
        _autoSaveStatus.value = "Brouillon prêt"
    }

    fun updateTitle(newTitle: String) {
        _activeSong.value = _activeSong.value.copy(title = newTitle.take(140))
        scheduleAutoSave()
    }

    fun updateBpmAndStyle(bpm: Int, instruStyle: String) {
        val safeBpm = bpm.coerceIn(50, 200)
        _activeSong.value = _activeSong.value.copy(bpm = safeBpm, instruStyle = instruStyle.take(250))
        _rhythmPlan.value = _rhythmPlan.value.copy(bpm = safeBpm, style = instruStyle)
        if (metronome.isPlaying.value) {
            metronome.start(viewModelScope, safeBpm, _rhythmPlan.value.activeSteps)
        }
        scheduleAutoSave()
    }

    fun updateSectionLyrics(sectionType: SongSectionType, newLyrics: String) {
        val current = _activeSong.value
        val updatedSections = current.sections.map {
            if (it.type == sectionType) it.copy(lyrics = newLyrics) else it
        }
        _activeSong.value = current.copy(sections = updatedSections)
        scheduleAutoSave()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        _autoSaveStatus.value = "Modification en cours…"
        autoSaveJob = viewModelScope.launch {
            delay(1_200L)
            persistSongInternal(isSnapshot = false)
        }
    }

    fun saveNow() {
        autoSaveJob?.cancel()
        viewModelScope.launch {
            persistSongInternal(isSnapshot = false)
        }
    }

    fun saveNamedVersionSnapshot(label: String) {
        autoSaveJob?.cancel()
        val current = _activeSong.value
        val stamp = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date())
        val snap = SongVersionSnapshot(
            id = UUID.randomUUID().toString().take(8),
            label = label.trim().ifBlank { "Version $stamp" }.take(50),
            savedAtLabel = stamp,
            sections = current.sections.map { SongSectionContent(it.type, it.lyrics) }
        )
        _activeSong.value = current.copy(
            versions = (listOf(snap) + current.versions).take(10),
            updatedAtLabel = stamp
        )
        viewModelScope.launch {
            persistSongInternal(isSnapshot = true)
            _bannerMessage.value = "Version « ${snap.label} » sauvegardée."
        }
    }

    fun rollbackToVersion(snapshot: SongVersionSnapshot) {
        val current = _activeSong.value
        val stamp = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date())
        val backupBeforeRollback = SongVersionSnapshot(
            id = UUID.randomUUID().toString().take(8),
            label = "Avant retour ($stamp)",
            savedAtLabel = stamp,
            sections = current.sections.map { SongSectionContent(it.type, it.lyrics) }
        )
        _activeSong.value = current.copy(
            sections = snapshot.sections.map { SongSectionContent(it.type, it.lyrics) },
            versions = (listOf(backupBeforeRollback) + current.versions).take(10),
            updatedAtLabel = stamp
        )
        viewModelScope.launch {
            persistSongInternal(isSnapshot = true)
            _bannerMessage.value = "Retour sur « ${snapshot.label} » effectué."
        }
    }

    fun deleteSong(songId: String) {
        if (songId.isBlank()) return
        viewModelScope.launch {
            repository.deleteCreation(currentUserId, songId)
                .onSuccess {
                    if (_activeSong.value.id == songId) {
                        _activeSong.value = StudioSong()
                    }
                    _bannerMessage.value = "Morceau supprimé."
                }
                .onFailure { _bannerMessage.value = it.message ?: "Suppression impossible." }
        }
    }

    private suspend fun persistSongInternal(isSnapshot: Boolean) {
        val song = _activeSong.value
        if (song.fullLyrics().isBlank() && song.title.isBlank()) return
        val stamp = SimpleDateFormat("HH:mm:ss", Locale.FRANCE).format(Date())
        val updatedSong = song.copy(
            title = song.title.trim().ifBlank { "Sans titre" },
            updatedAtLabel = stamp
        )
        val payload = updatedSong.toJsonString()
        if (updatedSong.id.isBlank()) {
            repository.addCreationReturningId(
                userId = currentUserId,
                category = CreationCategory.MUSIC,
                title = updatedSong.title,
                content = payload,
                tags = listOf(TAG_STUDIO_SONG),
                pinned = false
            ).onSuccess { newId ->
                _activeSong.value = _activeSong.value.copy(id = newId, updatedAtLabel = stamp)
                _autoSaveStatus.value = if (isSnapshot) "Version sauvée à $stamp" else "Enregistré auto · $stamp"
            }.onFailure {
                _autoSaveStatus.value = "Erreur sauvegarde"
                _bannerMessage.value = it.message
            }
        } else {
            repository.updateCreation(
                userId = currentUserId,
                creationId = updatedSong.id,
                title = updatedSong.title,
                content = payload,
                tags = listOf(TAG_STUDIO_SONG)
            ).onSuccess {
                _activeSong.value = _activeSong.value.copy(updatedAtLabel = stamp)
                _autoSaveStatus.value = if (isSnapshot) "Version sauvée à $stamp" else "Enregistré auto · $stamp"
            }.onFailure {
                _autoSaveStatus.value = "Erreur sauvegarde"
                _bannerMessage.value = it.message
            }
        }
    }

    fun rewritePassage(passage: String, action: String) {
        if (passage.isBlank()) {
            _bannerMessage.value = "Surligne ou sélectionne un passage d'abord."
            return
        }
        viewModelScope.launch {
            _isRewritingPassage.value = true
            try {
                _passageRewriteResult.value = aiService.rewritePassage(
                    passage = passage,
                    fullLyricsContext = _activeSong.value.fullLyrics(),
                    action = action,
                    universe = universe.value
                )
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Impossible de réécrire le passage."
            } finally {
                _isRewritingPassage.value = false
            }
        }
    }

    fun dismissPassageRewrite() {
        _passageRewriteResult.value = null
    }

    fun runSmartCorrection(sectionLyrics: String) {
        if (sectionLyrics.isBlank()) {
            _bannerMessage.value = "Écris d'abord quelques lignes dans cette section."
            return
        }
        viewModelScope.launch {
            _isCorrecting.value = true
            try {
                _smartCorrectionResult.value = aiService.smartCorrectLyrics(sectionLyrics)
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Correction intelligente impossible."
            } finally {
                _isCorrecting.value = false
            }
        }
    }

    fun applySmartCorrection(sectionType: SongSectionType) {
        val result = _smartCorrectionResult.value ?: return
        updateSectionLyrics(sectionType, result.correctedText)
        _smartCorrectionResult.value = null
        _bannerMessage.value = "Correction appliquée (ton style et tes élisions sont conservés)."
    }

    fun dismissSmartCorrection() {
        _smartCorrectionResult.value = null
    }

    fun searchRhymes(query: String, rhymeType: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearchingRhymes.value = true
            try {
                _rhymeResult.value = aiService.searchRhymes(query, rhymeType)
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Recherche de rimes impossible."
            } finally {
                _isSearchingRhymes.value = false
            }
        }
    }

    fun generateLexicalField(themeOrEmotion: String) {
        if (themeOrEmotion.isBlank()) return
        viewModelScope.launch {
            _isGeneratingLexical.value = true
            try {
                _lexicalResult.value = aiService.generateLexicalField(themeOrEmotion)
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Génération du champ lexical impossible."
            } finally {
                _isGeneratingLexical.value = false
            }
        }
    }

    fun auditClichesAndRepetitions(lyrics: String) {
        val target = lyrics.ifBlank { _activeSong.value.fullLyrics() }
        if (target.isBlank()) {
            _bannerMessage.value = "Aucun texte à analyser."
            return
        }
        viewModelScope.launch {
            _isAuditingCliches.value = true
            try {
                _clicheReport.value = aiService.auditClichesAndRepetitions(target)
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Audit impossible."
            } finally {
                _isAuditingCliches.value = false
            }
        }
    }

    fun generateRhythmAndFlows(sampleLyrics: String) {
        val song = _activeSong.value
        val sample = sampleLyrics.ifBlank { song.fullLyrics() }
        viewModelScope.launch {
            _isGeneratingRhythm.value = true
            try {
                val plan = aiService.generateRhythmAndFlows(
                    bpm = song.bpm,
                    instruStyle = song.instruStyle,
                    sampleLyrics = sample
                )
                _rhythmPlan.value = plan
                if (metronome.isPlaying.value) {
                    metronome.start(viewModelScope, plan.bpm, plan.activeSteps)
                }
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Génération rythmique impossible."
            } finally {
                _isGeneratingRhythm.value = false
            }
        }
    }

    fun toggleStepInGrid(stepIndex: Int) {
        if (stepIndex !in 0..15) return
        val current = _rhythmPlan.value
        val nextActive = if (stepIndex in current.activeSteps) {
            current.activeSteps - stepIndex
        } else {
            current.activeSteps + stepIndex
        }
        _rhythmPlan.value = current.copy(activeSteps = nextActive)
        if (metronome.isPlaying.value) {
            metronome.start(viewModelScope, _activeSong.value.bpm, nextActive)
        }
    }

    fun toggleMetronome() {
        if (metronome.isPlaying.value) {
            metronome.stop()
        } else {
            metronome.start(viewModelScope, _activeSong.value.bpm, _rhythmPlan.value.activeSteps)
        }
    }

    fun saveUniverse(updated: PersonalUniverse) {
        viewModelScope.launch {
            val docId = universeCreationPair.value.first
            val json = updated.toJsonString()
            if (docId.isBlank()) {
                repository.addCreation(
                    userId = currentUserId,
                    category = CreationCategory.MUSIC,
                    title = "Mon Univers Artistique",
                    content = json,
                    tags = listOf(TAG_STUDIO_UNIVERSE),
                    pinned = true
                ).onSuccess {
                    _bannerMessage.value = "Univers personnel enregistré."
                }.onFailure {
                    _bannerMessage.value = it.message
                }
            } else {
                repository.updateCreation(
                    userId = currentUserId,
                    creationId = docId,
                    title = "Mon Univers Artistique",
                    content = json,
                    tags = listOf(TAG_STUDIO_UNIVERSE)
                ).onSuccess {
                    _bannerMessage.value = "Univers personnel mis à jour."
                }.onFailure {
                    _bannerMessage.value = it.message
                }
            }
        }
    }

    fun analyzeUniverseFromSongs() {
        val allSongs = songs.value
        if (allSongs.isEmpty() && _activeSong.value.fullLyrics().isBlank()) {
            _bannerMessage.value = "Écris d'abord un premier texte pour que Hazard en extraye ton univers."
            return
        }
        val combined = (listOf(_activeSong.value) + allSongs).distinctBy { it.title + it.id }
        viewModelScope.launch {
            _isAnalyzingUniverse.value = true
            try {
                val enriched = aiService.enrichPersonalUniverse(universe.value, combined)
                saveUniverse(enriched)
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Analyse d'univers impossible."
            } finally {
                _isAnalyzingUniverse.value = false
            }
        }
    }

    fun askFrenchRapCoach(question: String, useWebSearch: Boolean = true) {
        if (question.isBlank()) return
        viewModelScope.launch {
            _isCoachLoading.value = true
            try {
                _rapCoachAnswer.value = aiService.coachFrenchRapAndFurax(
                    question = question,
                    userLyrics = _activeSong.value.fullLyrics(),
                    useWebSearch = useWebSearch
                )
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Formation indisponible."
            } finally {
                _isCoachLoading.value = false
            }
        }
    }

    fun generateDailyChallenge() {
        viewModelScope.launch {
            _isDailyChallengeLoading.value = true
            _dailyChallengeFeedback.value = null
            try {
                _dailyChallengeText.value = aiService.generateDailyChallenge(universe.value)
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Impossible de créer le défi."
            } finally {
                _isDailyChallengeLoading.value = false
            }
        }
    }

    fun evaluateDailyChallenge(attempt: String) {
        if (attempt.isBlank()) return
        viewModelScope.launch {
            _isDailyChallengeLoading.value = true
            try {
                _dailyChallengeFeedback.value = aiService.evaluateWorkshopAttempt(
                    modeLabel = "Défi du jour",
                    challengePrompt = _dailyChallengeText.value ?: "Exercice libre rap français",
                    userAttempt = attempt
                )
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Évaluation impossible."
            } finally {
                _isDailyChallengeLoading.value = false
            }
        }
    }

    fun rollFreestyleWords() {
        _freestyleWords.value = FrenchLyricAnalyzer.randomFreestyleWords(5)
        _freestyleFeedback.value = null
    }

    fun evaluateFreestyle(attempt: String) {
        if (attempt.isBlank()) return
        viewModelScope.launch {
            _isFreestyleEvaluating.value = true
            try {
                val promptDesc = "Mode Freestyle à ${_activeSong.value.bpm} BPM avec mots imposés : ${_freestyleWords.value.joinToString(", ")}"
                _freestyleFeedback.value = aiService.evaluateWorkshopAttempt(
                    modeLabel = "Mode Freestyle",
                    challengePrompt = promptDesc,
                    userAttempt = attempt
                )
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Évaluation freestyle impossible."
            } finally {
                _isFreestyleEvaluating.value = false
            }
        }
    }

    fun attachAudioFile(context: Context, uri: Uri?) {
        if (uri == null) return
        selectedAudioUri = uri
        var displayName = "Audio sélectionné"
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            displayName = cursor.getString(idx) ?: displayName
                        }
                    }
                }
        } catch (_: Exception) {
        }
        _selectedAudioLabel.value = displayName
    }

    fun clearSelectedAudio() {
        selectedAudioUri = null
        _selectedAudioLabel.value = null
    }

    fun reviewCurrentTrack(context: Context, extraText: String) {
        val lyrics = extraText.ifBlank { _activeSong.value.fullLyrics() }
        val uri = selectedAudioUri
        if (lyrics.isBlank() && uri == null) {
            _bannerMessage.value = "Envoie un texte ou sélectionne un fichier audio à analyser."
            return
        }
        viewModelScope.launch {
            _isReviewingTrack.value = true
            try {
                var audioBytes: ByteArray? = null
                var mimeType: String? = null
                if (uri != null) {
                    val loaded = withContext(Dispatchers.IO) {
                        val mime = context.contentResolver.getType(uri) ?: "audio/mpeg"
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        mime to bytes
                    }
                    val bytes = loaded.second
                    if (bytes == null || bytes.isEmpty()) {
                        throw IllegalStateException("Impossible de lire ce fichier audio.")
                    }
                    if (bytes.size > MAX_AUDIO_BYTES) {
                        throw IllegalStateException("Audio trop lourd (>18 Mo). Envoie un extrait ou compresse en MP3/M4A.")
                    }
                    mimeType = loaded.first
                    audioBytes = bytes
                }
                _trackReviewResult.value = aiService.reviewTrackTextOrAudio(
                    lyricsText = lyrics,
                    audioBytes = audioBytes,
                    audioMimeType = mimeType,
                    universe = universe.value
                )
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Revue de morceau impossible."
            } finally {
                _isReviewingTrack.value = false
            }
        }
    }

    fun prepareSunoAndRelease() {
        val song = _activeSong.value
        if (song.fullLyrics().isBlank()) {
            _bannerMessage.value = "Écris d'abord des paroles dans au moins une section."
            return
        }
        viewModelScope.launch {
            _isPreparingSuno.value = true
            try {
                _sunoBundle.value = aiService.prepareSunoAndRelease(song, universe.value)
            } catch (e: Exception) {
                _bannerMessage.value = e.message ?: "Préparation Suno impossible."
            } finally {
                _isPreparingSuno.value = false
            }
        }
    }

    fun saveSunoPromoPlanToVault() {
        val bundle = _sunoBundle.value ?: return
        val song = _activeSong.value
        val fullDoc = buildString {
            appendLine("=== PROMPT SUNO ===")
            appendLine(bundle.stylePrompt)
            appendLine()
            appendLine("=== PAROLES BALISÉES ===")
            appendLine(bundle.taggedLyrics)
            appendLine()
            appendLine("=== TIKTOK & SORTIE ===")
            appendLine("Titre : ${bundle.tikTokTitle}")
            appendLine("Description : ${bundle.tikTokDescription}")
            appendLine("Hashtags : ${bundle.hashtags}")
            appendLine("Calendrier :\n${bundle.releaseSchedule}")
            appendLine("Idées de clips :\n${bundle.clipIdeas}")
        }
        viewModelScope.launch {
            repository.addCreation(
                userId = currentUserId,
                category = CreationCategory.PROMO,
                title = "Sortie · ${song.title}",
                content = fullDoc,
                tags = listOf("suno", "tiktok")
            ).onSuccess {
                _bannerMessage.value = "Pack Suno & Promo enregistré dans ton Coffre."
            }.onFailure {
                _bannerMessage.value = it.message
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        metronome.stop()
    }
}
