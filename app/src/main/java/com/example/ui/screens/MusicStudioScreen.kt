package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FrenchLyricAnalyzer
import com.example.model.RhythmBeatPlan
import com.example.model.SmartCorrectionResult
import com.example.model.SongSectionType
import com.example.model.SongVersionSnapshot
import com.example.model.StudioSong
import com.example.ui.viewmodel.MusicStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicStudioScreen(
    studioVm: MusicStudioViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val songs by studioVm.songs.collectAsStateWithLifecycle()
    val activeSong by studioVm.activeSong.collectAsStateWithLifecycle()
    val universe by studioVm.universe.collectAsStateWithLifecycle()
    val autoSaveStatus by studioVm.autoSaveStatus.collectAsStateWithLifecycle()

    val passageRewriteResult by studioVm.passageRewriteResult.collectAsStateWithLifecycle()
    val isRewritingPassage by studioVm.isRewritingPassage.collectAsStateWithLifecycle()
    val smartCorrectionResult by studioVm.smartCorrectionResult.collectAsStateWithLifecycle()
    val isCorrecting by studioVm.isCorrecting.collectAsStateWithLifecycle()

    val rhymeResult by studioVm.rhymeResult.collectAsStateWithLifecycle()
    val isSearchingRhymes by studioVm.isSearchingRhymes.collectAsStateWithLifecycle()
    val lexicalResult by studioVm.lexicalResult.collectAsStateWithLifecycle()
    val isGeneratingLexical by studioVm.isGeneratingLexical.collectAsStateWithLifecycle()
    val clicheReport by studioVm.clicheReport.collectAsStateWithLifecycle()
    val isAuditingCliches by studioVm.isAuditingCliches.collectAsStateWithLifecycle()

    val rhythmPlan by studioVm.rhythmPlan.collectAsStateWithLifecycle()
    val isGeneratingRhythm by studioVm.isGeneratingRhythm.collectAsStateWithLifecycle()
    val isMetronomePlaying by studioVm.metronome.isPlaying.collectAsStateWithLifecycle()
    val currentBeatStep by studioVm.metronome.currentStep.collectAsStateWithLifecycle()

    val isAnalyzingUniverse by studioVm.isAnalyzingUniverse.collectAsStateWithLifecycle()
    val rapCoachAnswer by studioVm.rapCoachAnswer.collectAsStateWithLifecycle()
    val isCoachLoading by studioVm.isCoachLoading.collectAsStateWithLifecycle()
    val dailyChallengeText by studioVm.dailyChallengeText.collectAsStateWithLifecycle()
    val dailyChallengeFeedback by studioVm.dailyChallengeFeedback.collectAsStateWithLifecycle()
    val isDailyChallengeLoading by studioVm.isDailyChallengeLoading.collectAsStateWithLifecycle()
    val freestyleWords by studioVm.freestyleWords.collectAsStateWithLifecycle()
    val freestyleFeedback by studioVm.freestyleFeedback.collectAsStateWithLifecycle()
    val isFreestyleEvaluating by studioVm.isFreestyleEvaluating.collectAsStateWithLifecycle()
    val trackReviewResult by studioVm.trackReviewResult.collectAsStateWithLifecycle()
    val isReviewingTrack by studioVm.isReviewingTrack.collectAsStateWithLifecycle()
    val selectedAudioLabel by studioVm.selectedAudioLabel.collectAsStateWithLifecycle()

    val sunoBundle by studioVm.sunoBundle.collectAsStateWithLifecycle()
    val isPreparingSuno by studioVm.isPreparingSuno.collectAsStateWithLifecycle()

    var selectedWorkspace by remember { mutableIntStateOf(0) }
    var selectedSection by remember { mutableStateOf(SongSectionType.COUPLET_1) }
    var showSongsDialog by remember { mutableStateOf(false) }
    var showVersionsDialog by remember { mutableStateOf(false) }
    var showSaveSnapshotDialog by remember { mutableStateOf(false) }

    val workspaces = listOf(
        "Écrire",
        "Rimes & Mots",
        "Rythme & Beat",
        "Mon Univers",
        "Formation & Ateliers",
        "Suno & Sortie"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("studio_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                title = {
                    Column {
                        Text(
                            activeSong.title.ifBlank { "Studio Musique" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            autoSaveStatus,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSongsDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_songs_dialog_button")
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Mes morceaux")
                    }
                    IconButton(
                        onClick = { showVersionsDialog = true },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("studio_versions_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "Versions et retour arrière")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                workspaces.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = selectedWorkspace == idx,
                        onClick = { selectedWorkspace = idx },
                        label = { Text(label, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.height(44.dp).testTag("studio_ws_$idx")
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = "workspace_body_$selectedWorkspace") {
                    when (selectedWorkspace) {
                        0 -> WritingWorkspace(
                            song = activeSong,
                            selectedSection = selectedSection,
                            passageRewriteResult = passageRewriteResult,
                            isRewritingPassage = isRewritingPassage,
                            smartCorrectionResult = smartCorrectionResult,
                            isCorrecting = isCorrecting,
                            onSelectSection = { selectedSection = it },
                            onTitleChange = studioVm::updateTitle,
                            onLyricsChange = { studioVm.updateSectionLyrics(selectedSection, it) },
                            onRewritePassage = studioVm::rewritePassage,
                            onDismissRewrite = studioVm::dismissPassageRewrite,
                            onRunSmartCorrection = {
                                val txt = activeSong.sections.firstOrNull { it.type == selectedSection }?.lyrics.orEmpty()
                                studioVm.runSmartCorrection(txt)
                            },
                            onApplyCorrection = { studioVm.applySmartCorrection(selectedSection) },
                            onDismissCorrection = studioVm::dismissSmartCorrection,
                            onOpenSnapshotSave = { showSaveSnapshotDialog = true },
                            onQuickPrepareSuno = {
                                selectedWorkspace = 5
                                studioVm.prepareSunoAndRelease()
                            }
                        )

                        1 -> RhymesAndWordsWorkspace(
                            activeLyrics = activeSong.sections.firstOrNull { it.type == selectedSection }?.lyrics
                                ?.ifBlank { activeSong.fullLyrics() }.orEmpty(),
                            rhymeResult = rhymeResult,
                            isSearchingRhymes = isSearchingRhymes,
                            lexicalResult = lexicalResult,
                            isGeneratingLexical = isGeneratingLexical,
                            clicheReport = clicheReport,
                            isAuditingCliches = isAuditingCliches,
                            onSearchRhymes = studioVm::searchRhymes,
                            onGenerateLexical = studioVm::generateLexicalField,
                            onAuditCliches = studioVm::auditClichesAndRepetitions
                        )

                        2 -> RhythmAndBeatWorkspace(
                            song = activeSong,
                            plan = rhythmPlan,
                            isGeneratingRhythm = isGeneratingRhythm,
                            isPlaying = isMetronomePlaying,
                            currentStep = currentBeatStep,
                            onUpdateBpmStyle = studioVm::updateBpmAndStyle,
                            onToggleMetronome = studioVm::toggleMetronome,
                            onToggleStep = studioVm::toggleStepInGrid,
                            onGeneratePlan = {
                                val sec = activeSong.sections.firstOrNull { it.type == selectedSection }?.lyrics.orEmpty()
                                studioVm.generateRhythmAndFlows(sec)
                            }
                        )

                        3 -> PersonalUniverseWorkspace(
                            universe = universe,
                            isAnalyzing = isAnalyzingUniverse,
                            onAnalyzeFromSongs = studioVm::analyzeUniverseFromSongs,
                            onSaveUniverse = studioVm::saveUniverse
                        )

                        4 -> FormationAndWorkshopsWorkspace(
                            activeSong = activeSong,
                            isMetronomePlaying = isMetronomePlaying,
                            rapCoachAnswer = rapCoachAnswer,
                            isCoachLoading = isCoachLoading,
                            dailyChallengeText = dailyChallengeText,
                            dailyChallengeFeedback = dailyChallengeFeedback,
                            isDailyChallengeLoading = isDailyChallengeLoading,
                            freestyleWords = freestyleWords,
                            freestyleFeedback = freestyleFeedback,
                            isFreestyleEvaluating = isFreestyleEvaluating,
                            trackReviewResult = trackReviewResult,
                            isReviewingTrack = isReviewingTrack,
                            selectedAudioLabel = selectedAudioLabel,
                            onToggleMetronome = studioVm::toggleMetronome,
                            onAskCoach = studioVm::askFrenchRapCoach,
                            onGenerateDailyChallenge = studioVm::generateDailyChallenge,
                            onEvaluateDailyChallenge = studioVm::evaluateDailyChallenge,
                            onRollFreestyleWords = studioVm::rollFreestyleWords,
                            onEvaluateFreestyle = studioVm::evaluateFreestyle,
                            onAttachAudioUri = { uri -> studioVm.attachAudioFile(context, uri) },
                            onClearAudio = studioVm::clearSelectedAudio,
                            onReviewTrack = { txt -> studioVm.reviewCurrentTrack(context, txt) }
                        )

                        5 -> SunoAndReleaseWorkspace(
                            activeSong = activeSong,
                            bundle = sunoBundle,
                            isPreparing = isPreparingSuno,
                            onPrepareSuno = studioVm::prepareSunoAndRelease,
                            onSaveToVault = studioVm::saveSunoPromoPlanToVault
                        )
                    }
                }
            }
        }
    }

    if (showSongsDialog) {
        SongsManagerDialog(
            songs = songs,
            activeSongId = activeSong.id,
            onNewSong = {
                studioVm.createNewSong()
                showSongsDialog = false
            },
            onSelectSong = {
                studioVm.selectSong(it)
                showSongsDialog = false
            },
            onDeleteSong = studioVm::deleteSong,
            onDismiss = { showSongsDialog = false }
        )
    }

    if (showVersionsDialog) {
        VersionsRollbackDialog(
            versions = activeSong.versions,
            onSaveNewSnapshot = {
                showVersionsDialog = false
                showSaveSnapshotDialog = true
            },
            onRollback = { snap ->
                studioVm.rollbackToVersion(snap)
                showVersionsDialog = false
            },
            onDismiss = { showVersionsDialog = false }
        )
    }

    if (showSaveSnapshotDialog) {
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveSnapshotDialog = false },
            title = { Text("Sauver une version") },
            text = {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Nom de la version (ex: Couplet 1 V2)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    studioVm.saveNamedVersionSnapshot(label)
                    showSaveSnapshotDialog = false
                }) {
                    Text("Sauvegarder")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveSnapshotDialog = false }) { Text("Annuler") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WritingWorkspace(
    song: StudioSong,
    selectedSection: SongSectionType,
    passageRewriteResult: String?,
    isRewritingPassage: Boolean,
    smartCorrectionResult: SmartCorrectionResult?,
    isCorrecting: Boolean,
    onSelectSection: (SongSectionType) -> Unit,
    onTitleChange: (String) -> Unit,
    onLyricsChange: (String) -> Unit,
    onRewritePassage: (String, String) -> Unit,
    onDismissRewrite: () -> Unit,
    onRunSmartCorrection: () -> Unit,
    onApplyCorrection: () -> Unit,
    onDismissCorrection: () -> Unit,
    onOpenSnapshotSave: () -> Unit,
    onQuickPrepareSuno: () -> Unit
) {
    val currentText = song.sections.firstOrNull { it.type == selectedSection }?.lyrics.orEmpty()
    var tfv by remember(song.id, selectedSection) {
        mutableStateOf(TextFieldValue(currentText, TextRange(currentText.length)))
    }
    if (tfv.text != currentText) {
        tfv = tfv.copy(
            text = currentText,
            selection = TextRange(
                tfv.selection.start.coerceIn(0, currentText.length),
                tfv.selection.end.coerceIn(0, currentText.length)
            )
        )
    }

    val selectedOrCurrentLine = remember(tfv) {
        val text = tfv.text
        if (text.isBlank()) ""
        else if (!tfv.selection.collapsed) {
            val start = minOf(tfv.selection.start, tfv.selection.end).coerceIn(0, text.length)
            val end = maxOf(tfv.selection.start, tfv.selection.end).coerceIn(0, text.length)
            text.substring(start, end).trim()
        } else {
            val cursor = tfv.selection.start.coerceIn(0, text.length)
            val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
            val lineEnd = text.indexOf('\n', cursor).let { if (it < 0) text.length else it }
            text.substring(lineStart, lineEnd).trim()
        }
    }

    val analyzedLines = remember(currentText) {
        FrenchLyricAnalyzer.analyzeLinesForDisplay(currentText)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = song.title,
            onValueChange = onTitleChange,
            label = { Text("Titre du morceau") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("studio_song_title_input")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SongSectionType.entries.forEach { sec ->
                val hasText = song.sections.firstOrNull { it.type == sec }?.lyrics?.isNotBlank() == true
                FilterChip(
                    selected = selectedSection == sec,
                    onClick = { onSelectSection(sec) },
                    label = {
                        Text(
                            if (hasText) "${sec.label} •" else sec.label,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    modifier = Modifier.height(44.dp).testTag("studio_section_${sec.name}")
                )
            }
        }

        OutlinedTextField(
            value = tfv,
            onValueChange = { next ->
                tfv = next
                if (next.text != currentText) {
                    onLyricsChange(next.text)
                }
            },
            label = { Text("${selectedSection.label} (${selectedSection.sunoTag})") },
            placeholder = { Text("Écris tes mesures ici… Surligne un passage pour le retravailler avec Hazard.") },
            minLines = 6,
            maxLines = 14,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().testTag("studio_lyrics_editor")
        )

        // Passage selection & 5 quick DA rewrite tools
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (selectedOrCurrentLine.isBlank()) "Surligne un passage (ou place le curseur sur une ligne) :"
                    else "Passage ciblé : « ${selectedOrCurrentLine.take(70)} »",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "reformule",
                        "plus percutant",
                        "plus sombre",
                        "plus imagé",
                        "trouve-moi 5 variantes"
                    ).forEach { action ->
                        AssistChip(
                            onClick = { onRewritePassage(selectedOrCurrentLine, action) },
                            enabled = !isRewritingPassage && selectedOrCurrentLine.isNotBlank(),
                            label = { Text(action) }
                        )
                    }
                }
                if (isRewritingPassage) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Hazard retravaille ce passage…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (!passageRewriteResult.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(passageRewriteResult, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onDismissRewrite, modifier = Modifier.align(Alignment.End)) {
                                Text("Fermer")
                            }
                        }
                    }
                }
            }
        }

        // Action row: Smart Correction + Save Snapshot + Suno
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRunSmartCorrection,
                enabled = !isCorrecting && currentText.isNotBlank(),
                modifier = Modifier.weight(1f).height(48.dp).testTag("studio_smart_correct_btn")
            ) {
                if (isCorrecting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Spellcheck, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Corriger style OK")
                }
            }
            OutlinedButton(
                onClick = onOpenSnapshotSave,
                modifier = Modifier.weight(1f).height(48.dp).testTag("studio_save_snapshot_btn")
            ) {
                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Sauver version")
            }
        }

        if (smartCorrectionResult != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Correction intelligente (argot, verlan & élisions préservés)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (smartCorrectionResult.changesSummary.isNotEmpty()) {
                        Text("Modifications proposées :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        smartCorrectionResult.changesSummary.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    } else {
                        Text("Rien à corriger d'évident, ton texte tient debout.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (smartCorrectionResult.styleDoubtsToConfirm.isNotEmpty()) {
                        Text("Questions avant de toucher (doute stylistique) :", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        smartCorrectionResult.styleDoubtsToConfirm.forEach { Text("? $it", style = MaterialTheme.typography.bodySmall) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onApplyCorrection, modifier = Modifier.weight(1f).height(48.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Appliquer")
                        }
                        OutlinedButton(onClick = onDismissCorrection, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("Garder tel quel")
                        }
                    }
                }
            }
        }

        // Syllable counter + color-coded rhyme visualizer
        if (analyzedLines.any { it.text.isNotBlank() }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Syllabes & Schéma de rimes (couleurs)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    analyzedLines.filter { it.text.isNotBlank() }.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    "${line.syllables} syl",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                            if (line.schemeLetter.isNotBlank()) {
                                Surface(
                                    color = line.familyColor.copy(alpha = 0.25f),
                                    border = BorderStroke(1.dp, line.familyColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        line.schemeLetter,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = line.familyColor,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Text(
                                text = line.text.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Button(
            onClick = onQuickPrepareSuno,
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("studio_quick_suno_btn")
        ) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Préparer pour Suno")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RhymesAndWordsWorkspace(
    activeLyrics: String,
    rhymeResult: String?,
    isSearchingRhymes: Boolean,
    lexicalResult: String?,
    isGeneratingLexical: Boolean,
    clicheReport: String?,
    isAuditingCliches: Boolean,
    onSearchRhymes: (String, String) -> Unit,
    onGenerateLexical: (String) -> Unit,
    onAuditCliches: (String) -> Unit
) {
    var rhymeWord by remember { mutableStateOf("") }
    var rhymeType by remember { mutableStateOf("Multisyllabiques 2-4+ syllabes") }
    var themeInput by remember { mutableStateOf("") }
    val localAudit = remember(activeLyrics) {
        FrenchLyricAnalyzer.detectRepetitionsAndClichesLocally(activeLyrics)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. French Rhyme Dictionary
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Dictionnaire de rimes français", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = rhymeWord,
                    onValueChange = { rhymeWord = it },
                    label = { Text("Mot ou fin de phrase (ex: horizon, à l'ancienne)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("studio_rhyme_input")
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Rimes riches",
                        "Assonances",
                        "Multisyllabiques 2-4+ syllabes",
                        "Rimes internes",
                        "Rimes à cheval sur plusieurs mots"
                    ).forEach { mode ->
                        FilterChip(
                            selected = rhymeType == mode,
                            onClick = { rhymeType = mode },
                            label = { Text(mode) }
                        )
                    }
                }
                Button(
                    onClick = { onSearchRhymes(rhymeWord, rhymeType) },
                    enabled = !isSearchingRhymes && rhymeWord.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("studio_rhyme_search_btn")
                ) {
                    if (isSearchingRhymes) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Trouver des rimes")
                    }
                }
                if (!rhymeResult.isNullOrBlank()) {
                    StudioResultCard(title = "Rimes ($rhymeType)", content = rhymeResult)
                }
            }
        }

        // 2. Lexical Fields, Double meanings, Homonyms
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Champs lexicaux, Images & Double sens", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = themeInput,
                    onValueChange = { themeInput = it },
                    label = { Text("Thème, émotion ou mot-clé (ex: nuit froide, revanche)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onGenerateLexical(themeInput) },
                    enabled = !isGeneratingLexical && themeInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isGeneratingLexical) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Générer réseau d'images")
                    }
                }
                if (!lexicalResult.isNullOrBlank()) {
                    StudioResultCard(title = "Réseau d'images & Double sens", content = lexicalResult)
                }
            }
        }

        // 3. Repetitions & Cliches Detector
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Détecteur de répétitions & clichés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                if (localAudit.repeatedWords.isEmpty() && localAudit.detectedCliches.isEmpty()) {
                    Text("Scan local instantané : aucune répétition lourde ni cliché connu sur la section active.", style = MaterialTheme.typography.bodySmall)
                } else {
                    if (localAudit.repeatedWords.isNotEmpty()) {
                        Text(
                            "Mots répétés : " + localAudit.repeatedWords.joinToString { "${it.first} (${it.second}x)" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    localAudit.detectedCliches.forEach {
                        Text("• $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Button(
                    onClick = { onAuditCliches(activeLyrics) },
                    enabled = !isAuditingCliches && activeLyrics.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (isAuditingCliches) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Audit complet & alternatives (IA)")
                    }
                }
                if (!clicheReport.isNullOrBlank()) {
                    StudioResultCard(title = "Audit anti-clichés", content = clicheReport)
                }
            }
        }
    }
}

@Composable
private fun RhythmAndBeatWorkspace(
    song: StudioSong,
    plan: RhythmBeatPlan,
    isGeneratingRhythm: Boolean,
    isPlaying: Boolean,
    currentStep: Int,
    onUpdateBpmStyle: (Int, String) -> Unit,
    onToggleMetronome: () -> Unit,
    onToggleStep: (Int) -> Unit,
    onGeneratePlan: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Tempo & Instru", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = { onUpdateBpmStyle(song.bpm - 5, song.instruStyle) }) { Text("-5") }
                    OutlinedButton(onClick = { onUpdateBpmStyle(song.bpm - 1, song.instruStyle) }) { Text("-1") }
                    Text(
                        "${song.bpm} BPM",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(onClick = { onUpdateBpmStyle(song.bpm + 1, song.instruStyle) }) { Text("+1") }
                    OutlinedButton(onClick = { onUpdateBpmStyle(song.bpm + 5, song.instruStyle) }) { Text("+5") }
                }
                OutlinedTextField(
                    value = song.instruStyle,
                    onValueChange = { onUpdateBpmStyle(song.bpm, it) },
                    label = { Text("Style d'instru (ex: Boom bap sombre, Drill, Piano)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onToggleMetronome,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("studio_metronome_toggle")
                    ) {
                        Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (isPlaying) "Stop Clic" else "Jouer Clic")
                    }
                    OutlinedButton(
                        onClick = onGeneratePlan,
                        enabled = !isGeneratingRhythm,
                        modifier = Modifier.weight(1f).height(52.dp).testTag("studio_generate_rhythm_btn")
                    ) {
                        if (isGeneratingRhythm) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Flow IA (16 cases)")
                        }
                    }
                }
            }
        }

        // 4 beats x 4 subdivisions = 16-box grid
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Grille 4 temps · 16 cases (touche une case pour cliquer)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                for (beat in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "T${beat + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        for (sub in 0 until 4) {
                            val idx = beat * 4 + sub
                            val isHit = currentStep == idx
                            val isActive = idx in plan.activeSteps
                            val isAccent = idx in plan.accentSteps
                            val isBreath = idx in plan.breathSteps
                            val hint = plan.stepSyllableHints.getOrElse(idx) { "·" }

                            val bg: Color = when {
                                isHit -> MaterialTheme.colorScheme.primary
                                isAccent -> MaterialTheme.colorScheme.primaryContainer
                                isBreath -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                                isActive -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            val fg: Color = when {
                                isHit -> MaterialTheme.colorScheme.onPrimary
                                isAccent -> MaterialTheme.colorScheme.onPrimaryContainer
                                isBreath -> MaterialTheme.colorScheme.onErrorContainer
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Surface(
                                color = bg,
                                shape = RoundedCornerShape(10.dp),
                                border = if (isAccent && !isHit) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleStep(idx) }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(2.dp)) {
                                    Text(
                                        text = if (isBreath) "RESP" else hint,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isAccent || isHit) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = fg,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                if (plan.offbeatAdvice.isNotBlank()) {
                    Text(plan.offbeatAdvice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (plan.flows.isNotEmpty()) {
            Text("4 Flows adaptés à ton texte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            plan.flows.forEach { flow ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(flow.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(flow.description, style = MaterialTheme.typography.bodySmall)
                        Text("Placement : ${flow.placementTip}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SongsManagerDialog(
    songs: List<StudioSong>,
    activeSongId: String,
    onNewSong: () -> Unit,
    onSelectSong: (StudioSong) -> Unit,
    onDeleteSong: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mes morceaux (privés)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onNewSong, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Nouveau morceau")
                }
                if (songs.isEmpty()) {
                    Text("Aucun morceau sauvegardé pour l'instant.", style = MaterialTheme.typography.bodySmall)
                } else {
                    songs.take(12).forEach { s ->
                        Surface(
                            color = if (s.id == activeSongId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectSong(s) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(s.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${s.bpm} BPM · ${s.versions.size} version(s)", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { onDeleteSong(s.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}

@Composable
private fun VersionsRollbackDialog(
    versions: List<SongVersionSnapshot>,
    onSaveNewSnapshot: () -> Unit,
    onRollback: (SongVersionSnapshot) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Versions sauvegardées") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSaveNewSnapshot, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Icon(Icons.Default.Bookmark, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Sauver un instantané maintenant")
                }
                if (versions.isEmpty()) {
                    Text("Aucune version figée encore. Enregistre une version avant de tester de grosses modifs !", style = MaterialTheme.typography.bodySmall)
                } else {
                    versions.forEach { snap ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(snap.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(snap.savedAtLabel, style = MaterialTheme.typography.labelSmall)
                                }
                                OutlinedButton(onClick = { onRollback(snap) }) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Revenir")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fermer") } }
    )
}
