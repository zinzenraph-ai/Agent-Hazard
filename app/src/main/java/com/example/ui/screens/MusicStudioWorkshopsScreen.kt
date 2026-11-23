package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FrenchLyricAnalyzer
import com.example.model.PersonalUniverse
import com.example.model.StudioSong
import com.example.model.SunoExportBundle

@Composable
fun PersonalUniverseWorkspace(
    universe: PersonalUniverse,
    isAnalyzing: Boolean,
    onAnalyzeFromSongs: () -> Unit,
    onSaveUniverse: (PersonalUniverse) -> Unit
) {
    var themes by remember(universe) { mutableStateOf(universe.themes) }
    var images by remember(universe) { mutableStateOf(universe.images) }
    var favoriteWords by remember(universe) { mutableStateOf(universe.favoriteWords) }
    var emotions by remember(universe) { mutableStateOf(universe.emotions) }
    var references by remember(universe) { mutableStateOf(universe.references) }
    var styleSignature by remember(universe) { mutableStateOf(universe.styleSignature) }

    LaunchedEffect(universe) {
        themes = universe.themes
        images = universe.images
        favoriteWords = universe.favoriteWords
        emotions = universe.emotions
        references = universe.references
        styleSignature = universe.styleSignature
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Mon Univers Artistique",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Hazard nourrit cette fiche à partir de tes textes pour t'aider à affirmer TA propre plume sans copier personne.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAnalyzeFromSongs,
                    enabled = !isAnalyzing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("studio_analyze_universe_button")
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyse de ta plume…")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyser mes textes (IA)")
                    }
                }
            }
        }

        OutlinedTextField(
            value = themes,
            onValueChange = { themes = it },
            label = { Text("Mes thèmes récurrents") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = images,
            onValueChange = { images = it },
            label = { Text("Mes images & métaphores fortes") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = favoriteWords,
            onValueChange = { favoriteWords = it },
            label = { Text("Mes mots favoris & tournures") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = emotions,
            onValueChange = { emotions = it },
            label = { Text("Mes émotions dominantes") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = references,
            onValueChange = { references = it },
            label = { Text("Mes références personnelles") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = styleSignature,
            onValueChange = { styleSignature = it },
            label = { Text("Signature de style & axes à creuser") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                onSaveUniverse(
                    PersonalUniverse(
                        themes = themes,
                        images = images,
                        favoriteWords = favoriteWords,
                        emotions = emotions,
                        references = references,
                        styleSignature = styleSignature
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("studio_save_universe_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Enregistrer Mon Univers")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormationAndWorkshopsWorkspace(
    activeSong: StudioSong,
    isMetronomePlaying: Boolean,
    rapCoachAnswer: String?,
    isCoachLoading: Boolean,
    dailyChallengeText: String?,
    dailyChallengeFeedback: String?,
    isDailyChallengeLoading: Boolean,
    freestyleWords: List<String>,
    freestyleFeedback: String?,
    isFreestyleEvaluating: Boolean,
    trackReviewResult: String?,
    isReviewingTrack: Boolean,
    selectedAudioLabel: String?,
    onToggleMetronome: () -> Unit,
    onAskCoach: (String, Boolean) -> Unit,
    onGenerateDailyChallenge: () -> Unit,
    onEvaluateDailyChallenge: (String) -> Unit,
    onRollFreestyleWords: () -> Unit,
    onEvaluateFreestyle: (String) -> Unit,
    onAttachAudioUri: (android.net.Uri?) -> Unit,
    onClearAudio: () -> Unit,
    onReviewTrack: (String) -> Unit
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        0 to "Furax & Rap FR",
        1 to "Fiches Techniques",
        2 to "Défi & Freestyle",
        3 to "Revue de Morceau"
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subTabs.forEach { (idx, label) ->
                FilterChip(
                    selected = selectedSubTab == idx,
                    onClick = { selectedSubTab = idx },
                    label = { Text(label, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.height(44.dp)
                )
            }
        }

        when (selectedSubTab) {
            0 -> {
                var coachQuery by remember {
                    mutableStateOf("Analyse la technique d'écriture de Furax Barbarossa (multisyllabiques, jeux de sons, enchaînements) et propose un exercice sur mon texte.")
                }
                var useWebSearch by remember { mutableStateOf(true) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Formation Rap Français & Furax Barbarossa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "Analyse des mécaniques, écoles et interviews (sans jamais recopier de paroles protégées : exercices uniquement sur tes propres textes).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Recherche Web à jour", style = MaterialTheme.typography.bodyMedium)
                            }
                            Switch(checked = useWebSearch, onCheckedChange = { useWebSearch = it })
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "Méthode Furax Barbarossa" to "Décortique étape par étape la densité, les rimes multisyllabiques et les enchaînements de Furax Barbarossa avec un exercice sur mon texte.",
                                "Écoles & Histoire Rap FR" to "Explique les grandes écoles d'écriture du rap français (techniciens, narrateurs, punchliners) et comment forger ma propre voie.",
                                "Allitérations & Enjambements" to "Explique comment utiliser enjambements et allitérations pour tendre un couplet, avec exercice sur mes lignes."
                            ).forEach { (chipLabel, q) ->
                                AssistChip(
                                    onClick = {
                                        coachQuery = q
                                        onAskCoach(q, useWebSearch)
                                    },
                                    label = { Text(chipLabel) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = coachQuery,
                            onValueChange = { coachQuery = it },
                            label = { Text("Question / Technique à étudier") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { onAskCoach(coachQuery, useWebSearch) },
                            enabled = !isCoachLoading && coachQuery.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("studio_coach_ask_button")
                        ) {
                            if (isCoachLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Recherche & analyse…")
                            } else {
                                Text("Lancer la masterclass")
                            }
                        }
                    }
                }

                if (!rapCoachAnswer.isNullOrBlank()) {
                    StudioResultCard(title = "Analyse & Exercice", content = rapCoachAnswer)
                }
            }

            1 -> {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FrenchLyricAnalyzer.rapTechniqueLibrary.forEach { sheet ->
                        var expanded by remember { mutableStateOf(sheet.id == "furax_multi") }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                sheet.tag,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(sheet.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(onClick = { expanded = !expanded }) {
                                        Text(if (expanded) "Fermer" else "Lire")
                                    }
                                }
                                Text(sheet.explanation, style = MaterialTheme.typography.bodySmall)
                                if (expanded) {
                                    sheet.breakdownSteps.forEach { step ->
                                        Text(
                                            step,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Exercice : ${sheet.exercisePrompt}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            selectedSubTab = 0
                                            onAskCoach("Applique la fiche « ${sheet.title} » directement à mon morceau actuel avec un exercice guidé.", false)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Pratiquer sur mon texte")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                var challengeAttempt by remember { mutableStateOf("") }
                var freestyleAttempt by remember { mutableStateOf("") }

                // Défi du jour
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Défi du jour", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onGenerateDailyChallenge,
                            enabled = !isDailyChallengeLoading,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("studio_daily_challenge_btn")
                        ) {
                            if (isDailyChallengeLoading && dailyChallengeText == null) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(if (dailyChallengeText == null) "Tirer le défi du jour" else "Nouveau défi")
                            }
                        }
                        if (!dailyChallengeText.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(dailyChallengeText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                            }
                            OutlinedTextField(
                                value = challengeAttempt,
                                onValueChange = { challengeAttempt = it },
                                label = { Text("Écris ton essai ici (8 mesures)") },
                                minLines = 4,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { onEvaluateDailyChallenge(challengeAttempt) },
                                enabled = !isDailyChallengeLoading && challengeAttempt.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Retour honnête de Hazard")
                            }
                        }
                        if (!dailyChallengeFeedback.isNullOrBlank()) {
                            StudioResultCard(title = "Correction Défi", content = dailyChallengeFeedback)
                        }
                    }
                }

                // Mode Freestyle
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text("Mode Freestyle · ${activeSong.bpm} BPM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            freestyleWords.forEach { w ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        w.uppercase(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onRollFreestyleWords,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Default.Casino, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("5 nouveaux mots")
                            }
                            Button(
                                onClick = onToggleMetronome,
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(if (isMetronomePlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (isMetronomePlaying) "Stop Clic" else "Clic ${activeSong.bpm}")
                            }
                        }
                        OutlinedTextField(
                            value = freestyleAttempt,
                            onValueChange = { freestyleAttempt = it },
                            label = { Text("Note ou pose ton freestyle ici") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { onEvaluateFreestyle(freestyleAttempt) },
                            enabled = !isFreestyleEvaluating && freestyleAttempt.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            if (isFreestyleEvaluating) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Analyser le freestyle")
                            }
                        }
                        if (!freestyleFeedback.isNullOrBlank()) {
                            StudioResultCard(title = "Retour Freestyle", content = freestyleFeedback)
                        }
                    }
                }
            }

            3 -> {
                val context = LocalContext.current
                var extraLyricsToReview by remember { mutableStateOf("") }
                val audioPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    onAttachAudioUri(uri)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Revue de Morceau (DA honnête)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "Envoie un audio de maquette OU utilise les paroles du morceau actif. Retour cash : Points forts, Points à travailler, 3 priorités.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = { audioPicker.launch("audio/*") },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("studio_pick_audio_button")
                            ) {
                                Icon(Icons.Default.AudioFile, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(selectedAudioLabel ?: "Joindre un audio")
                            }
                            if (selectedAudioLabel != null) {
                                IconButton(onClick = onClearAudio) {
                                    Icon(Icons.Default.Close, contentDescription = "Retirer audio")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = extraLyricsToReview,
                            onValueChange = { extraLyricsToReview = it },
                            label = { Text("Paroles spécifiques (laisse vide pour revoir « ${activeSong.title} »)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = { onReviewTrack(extraLyricsToReview) },
                            enabled = !isReviewingTrack,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("studio_review_track_button")
                        ) {
                            if (isReviewingTrack) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Écoute & audit en cours…")
                            } else {
                                Icon(Icons.Default.GraphicEq, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Lancer la revue complète")
                            }
                        }
                    }
                }

                if (!trackReviewResult.isNullOrBlank()) {
                    StudioResultCard(title = "Bilan Directeur Artistique", content = trackReviewResult)
                }
            }
        }
    }
}

@Composable
fun SunoAndReleaseWorkspace(
    activeSong: StudioSong,
    bundle: SunoExportBundle?,
    isPreparing: Boolean,
    onPrepareSuno: () -> Unit,
    onSaveToVault: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Préparer pour Suno & Sortie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Morceau : ${activeSong.title} · ${activeSong.bpm} BPM · ${activeSong.instruStyle}",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = onPrepareSuno,
                    enabled = !isPreparing,
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("studio_prepare_suno_button")
                ) {
                    if (isPreparing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Génération du pack Suno & TikTok…")
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Préparer pour Suno")
                    }
                }
            }
        }

        if (bundle == null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Aperçu direct balisé (prêt à copier)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { clipboard.setText(AnnotatedString(activeSong.fullLyrics())) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copier")
                        }
                    }
                    Text(
                        activeSong.fullLyrics().ifBlank { "Écris d'abord tes sections dans l'onglet Écrire." },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            CopyableStudioBlock(
                title = "1. Prompt de style Suno",
                text = bundle.stylePrompt,
                onCopy = { clipboard.setText(AnnotatedString(bundle.stylePrompt)) }
            )
            CopyableStudioBlock(
                title = "2. Paroles balisées pour Suno",
                text = bundle.taggedLyrics,
                onCopy = { clipboard.setText(AnnotatedString(bundle.taggedLyrics)) }
            )
            CopyableStudioBlock(
                title = "3. Titre, Description & Hashtags TikTok",
                text = "Titre : ${bundle.tikTokTitle}\n\n${bundle.tikTokDescription}\n\n${bundle.hashtags}",
                onCopy = {
                    clipboard.setText(
                        AnnotatedString("${bundle.tikTokTitle}\n\n${bundle.tikTokDescription}\n\n${bundle.hashtags}")
                    )
                }
            )
            StudioResultCard(title = "4. Calendrier de publication", content = bundle.releaseSchedule)
            StudioResultCard(title = "5. Idées de clips & vidéos courtes (0 €)", content = bundle.clipIdeas)

            Button(
                onClick = onSaveToVault,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("studio_save_suno_vault")
            ) {
                Icon(Icons.Default.BookmarkAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Sauvegarder le pack dans mon Coffre")
            }
        }
    }
}

@Composable
fun StudioResultCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CopyableStudioBlock(
    title: String,
    text: String,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onCopy) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copier")
                }
            }
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
