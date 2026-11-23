package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.model.MemoryProfile
import com.example.model.UiState
import com.example.security.MemoryPrivacyGuard
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.RiskCoral
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MemoryBucket(val label: String, val wyattMinimal: Boolean = false) {
    FIRST_NAME("Prénom"),
    MOI_STYLE("Goûts & préférences de style"),
    MUSIC_STYLE("Style & thèmes musicaux"),
    MUSIC_ARTISTS("Artistes & références"),
    PROJECTS_IN_PROGRESS("Projets en cours (musique, appli, finance)"),
    PROJECTS_SHORT_GOALS("Objectifs court terme"),
    PROJECTS_LONG_GOALS("Objectifs long terme"),
    PROJECTS_DECISIONS("Décisions déjà prises"),
    FINANCE_NOTES("Trading, crypto & finance"),
    WYATT_LIKES("Ce que Wyatt aime", wyattMinimal = true),
    WYATT_LEARNED("Ce que Wyatt a appris / explore", wyattMinimal = true),
    WYATT_MEMORIES("Petits souvenirs rigolos", wyattMinimal = true)
}

private data class LineEditTarget(
    val bucket: MemoryBucket,
    val index: Int?, // null = adding new line
    val initialText: String
)

@Composable
fun MemoryScreen(
    memoryState: UiState<MemoryProfile>,
    wyattMemoryState: UiState<MemoryProfile>,
    isExtracting: Boolean,
    onSaveMemory: (MemoryProfile) -> Unit,
    onClearAllMemory: () -> Unit,
    onStartInterview: () -> Unit,
    onExtractFromChat: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    when (memoryState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = memoryState.message,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        is UiState.Success -> {
            val mainProfile = memoryState.data
            val wyattProfile = (wyattMemoryState as? UiState.Success)?.data
                ?: MemoryProfile(id = "wyatt", userId = mainProfile.userId, firstName = "Wyatt")

            // Combine Wyatt minimal memory (separate doc preferred, fallback to main fields)
            val effectiveWyattLikes = wyattProfile.wyattLikes.ifBlank { mainProfile.wyattLikes }
            val effectiveWyattLearned = wyattProfile.wyattWantsToLearn.ifBlank { mainProfile.wyattWantsToLearn }
            val effectiveWyattMemories = wyattProfile.wyattMemories.ifBlank { mainProfile.wyattMemories }

            val mergedForDisplay = remember(mainProfile, effectiveWyattLikes, effectiveWyattLearned, effectiveWyattMemories) {
                mainProfile.copy(
                    wyattLikes = effectiveWyattLikes,
                    wyattWantsToLearn = effectiveWyattLearned,
                    wyattMemories = effectiveWyattMemories
                )
            }

            var editTarget by remember { mutableStateOf<LineEditTarget?>(null) }
            var editInput by remember { mutableStateOf("") }
            var privacyWarning by remember { mutableStateOf<String?>(null) }
            var showClearAllConfirm by remember { mutableStateOf(false) }
            var exportedBanner by remember { mutableStateOf(false) }
            var showPreviewText by remember { mutableStateOf(false) }

            val todayLabel = remember {
                SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(Date())
            }
            val exportSheet = remember(mergedForDisplay, wyattProfile, todayLabel) {
                mergedForDisplay.formatGroupedMemoryExport(wyattProfile, todayLabel)
            }

            fun getBucketLines(bucket: MemoryBucket): List<String> {
                val allDecisions = MemoryPrivacyGuard.splitLines(mergedForDisplay.decisionsMade)
                return when (bucket) {
                    MemoryBucket.FIRST_NAME -> listOfNotNull(mergedForDisplay.firstName.trim().takeIf { it.isNotBlank() })
                    MemoryBucket.MOI_STYLE -> allDecisions.filter { it.startsWith("[Style] ") }
                        .map { it.removePrefix("[Style] ").trim() }
                    MemoryBucket.MUSIC_STYLE -> MemoryPrivacyGuard.splitLines(mergedForDisplay.musicStyle)
                    MemoryBucket.MUSIC_ARTISTS -> MemoryPrivacyGuard.splitLines(mergedForDisplay.favoriteArtists)
                    MemoryBucket.PROJECTS_IN_PROGRESS -> MemoryPrivacyGuard.splitLines(mergedForDisplay.inProgress)
                    MemoryBucket.PROJECTS_SHORT_GOALS -> MemoryPrivacyGuard.splitLines(mergedForDisplay.shortTermGoals)
                    MemoryBucket.PROJECTS_LONG_GOALS -> MemoryPrivacyGuard.splitLines(mergedForDisplay.longTermGoals)
                    MemoryBucket.PROJECTS_DECISIONS -> allDecisions
                        .filter { !it.startsWith("[Style] ") && !it.startsWith("[Finance] ") }
                    MemoryBucket.FINANCE_NOTES -> {
                        val primary = MemoryPrivacyGuard.splitLines(mergedForDisplay.tradingLevel)
                        val extra = allDecisions.filter { it.startsWith("[Finance] ") }
                            .map { it.removePrefix("[Finance] ").trim() }
                        primary + extra
                    }
                    MemoryBucket.WYATT_LIKES -> MemoryPrivacyGuard.splitLines(effectiveWyattLikes)
                    MemoryBucket.WYATT_LEARNED -> MemoryPrivacyGuard.splitLines(effectiveWyattLearned)
                    MemoryBucket.WYATT_MEMORIES -> MemoryPrivacyGuard.splitLines(effectiveWyattMemories)
                }
            }

            fun saveBucketLines(bucket: MemoryBucket, updatedLines: List<String>) {
                val clean = updatedLines.map { it.trim() }.filter { it.isNotBlank() }
                val currentDecisions = MemoryPrivacyGuard.splitLines(mergedForDisplay.decisionsMade)
                val styleLines = currentDecisions.filter { it.startsWith("[Style] ") }
                val financeDecLines = currentDecisions.filter { it.startsWith("[Finance] ") }
                val projectDecLines = currentDecisions.filter { !it.startsWith("[Style] ") && !it.startsWith("[Finance] ") }

                val updatedProfile = when (bucket) {
                    MemoryBucket.FIRST_NAME -> mergedForDisplay.copy(
                        firstName = clean.firstOrNull().orEmpty().take(80)
                    )
                    MemoryBucket.MOI_STYLE -> {
                        val rebuilt = clean.map { "[Style] $it" } + projectDecLines + financeDecLines
                        mergedForDisplay.copy(decisionsMade = rebuilt.joinToString("\n").take(2500))
                    }
                    MemoryBucket.MUSIC_STYLE -> mergedForDisplay.copy(
                        musicStyle = clean.joinToString("\n").take(500)
                    )
                    MemoryBucket.MUSIC_ARTISTS -> mergedForDisplay.copy(
                        favoriteArtists = clean.joinToString("\n").take(500)
                    )
                    MemoryBucket.PROJECTS_IN_PROGRESS -> mergedForDisplay.copy(
                        inProgress = clean.joinToString("\n").take(2500)
                    )
                    MemoryBucket.PROJECTS_SHORT_GOALS -> mergedForDisplay.copy(
                        shortTermGoals = clean.joinToString("\n").take(1000)
                    )
                    MemoryBucket.PROJECTS_LONG_GOALS -> mergedForDisplay.copy(
                        longTermGoals = clean.joinToString("\n").take(1000)
                    )
                    MemoryBucket.PROJECTS_DECISIONS -> {
                        val rebuilt = styleLines + clean + financeDecLines
                        mergedForDisplay.copy(decisionsMade = rebuilt.joinToString("\n").take(2500))
                    }
                    MemoryBucket.FINANCE_NOTES -> {
                        val tradingSb = StringBuilder()
                        val overflowFinance = mutableListOf<String>()
                        clean.forEach { line ->
                            val candidate = if (tradingSb.isEmpty()) line else "\n$line"
                            if (tradingSb.length + candidate.length <= 200) {
                                tradingSb.append(candidate)
                            } else {
                                overflowFinance.add("[Finance] $line")
                            }
                        }
                        val rebuiltDecisions = styleLines + projectDecLines + overflowFinance
                        mergedForDisplay.copy(
                            tradingLevel = tradingSb.toString(),
                            decisionsMade = rebuiltDecisions.joinToString("\n").take(2500)
                        )
                    }
                    MemoryBucket.WYATT_LIKES -> mergedForDisplay.copy(
                        wyattLikes = clean.joinToString("\n").take(1000)
                    )
                    MemoryBucket.WYATT_LEARNED -> mergedForDisplay.copy(
                        wyattWantsToLearn = clean.joinToString("\n").take(1000)
                    )
                    MemoryBucket.WYATT_MEMORIES -> mergedForDisplay.copy(
                        wyattMemories = clean.joinToString("\n").take(2500)
                    )
                }
                onSaveMemory(updatedProfile)
            }

            fun openEditor(bucket: MemoryBucket, index: Int?, initialText: String) {
                privacyWarning = null
                editInput = initialText
                editTarget = LineEditTarget(bucket = bucket, index = index, initialText = initialText)
            }

            fun deleteLine(bucket: MemoryBucket, index: Int) {
                val lines = getBucketLines(bucket).toMutableList()
                if (index in lines.indices) {
                    lines.removeAt(index)
                    saveBucketLines(bucket, lines)
                }
            }

            val missing = mergedForDisplay.missingFieldLabels()
            val totalTrackable = 9
            val progress = ((totalTrackable - missing.size).coerceAtLeast(0)) / totalTrackable.toFloat()

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Header card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Mémoire Intégrée",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hazard retient automatiquement tes goûts, projets, décisions, objectifs et Wyatt (jamais ses suppositions). " +
                                    "Aucun mot de passe, clé API, numéro de carte ni papier d'identité n'est jamais enregistré.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                            )
                            Text(
                                text = if (missing.isEmpty()) "Mémoire bien remplie ! Hazard l'utilise en silence dès chaque début de session."
                                else "${missing.size} point(s) encore libre(s) : ${missing.take(3).joinToString(", ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onStartInterview,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("interview_memory_button")
                                ) {
                                    Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Interview")
                                }
                                OutlinedButton(
                                    onClick = onExtractFromChat,
                                    enabled = !isExtracting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("auto_extract_memory_button")
                                ) {
                                    if (isExtracting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text("Analyser chat")
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        clipboard.setText(AnnotatedString(exportSheet))
                                        exportedBanner = true
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, "Mémoire de Hazard ($todayLabel)")
                                            putExtra(Intent.EXTRA_TEXT, exportSheet)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Exporter ma mémoire vers Notes"))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("export_memory_button")
                                ) {
                                    Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (exportedBanner) "Exporté !" else "Exporter ma mémoire")
                                }
                                OutlinedButton(
                                    onClick = { showClearAllConfirm = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCoral),
                                    border = BorderStroke(1.dp, RiskCoral),
                                    modifier = Modifier.testTag("clear_all_memory_button")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = "Tout effacer", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tout effacer")
                                }
                            }
                        }
                    }
                }

                // 1. MOI
                item {
                    MemoryThemeCard(
                        title = "1. Moi",
                        subtitle = "Identité & préférences de style",
                        icon = Icons.Default.Person
                    ) {
                        BucketSection(
                            bucket = MemoryBucket.FIRST_NAME,
                            lines = getBucketLines(MemoryBucket.FIRST_NAME),
                            singleItemOnly = true,
                            onAdd = { openEditor(MemoryBucket.FIRST_NAME, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.FIRST_NAME, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.FIRST_NAME, idx) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        BucketSection(
                            bucket = MemoryBucket.MOI_STYLE,
                            lines = getBucketLines(MemoryBucket.MOI_STYLE),
                            onAdd = { openEditor(MemoryBucket.MOI_STYLE, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.MOI_STYLE, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.MOI_STYLE, idx) }
                        )
                    }
                }

                // 2. MUSIQUE
                item {
                    MemoryThemeCard(
                        title = "2. Musique",
                        subtitle = "Paroles, Suno, styles & références",
                        icon = Icons.Default.MusicNote
                    ) {
                        BucketSection(
                            bucket = MemoryBucket.MUSIC_STYLE,
                            lines = getBucketLines(MemoryBucket.MUSIC_STYLE),
                            onAdd = { openEditor(MemoryBucket.MUSIC_STYLE, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.MUSIC_STYLE, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.MUSIC_STYLE, idx) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        BucketSection(
                            bucket = MemoryBucket.MUSIC_ARTISTS,
                            lines = getBucketLines(MemoryBucket.MUSIC_ARTISTS),
                            onAdd = { openEditor(MemoryBucket.MUSIC_ARTISTS, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.MUSIC_ARTISTS, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.MUSIC_ARTISTS, idx) }
                        )
                    }
                }

                // 3. PROJETS
                item {
                    MemoryThemeCard(
                        title = "3. Projets",
                        subtitle = "Musique, applis/SaaS, objectifs & décisions",
                        icon = Icons.Default.RocketLaunch
                    ) {
                        BucketSection(
                            bucket = MemoryBucket.PROJECTS_IN_PROGRESS,
                            lines = getBucketLines(MemoryBucket.PROJECTS_IN_PROGRESS),
                            onAdd = { openEditor(MemoryBucket.PROJECTS_IN_PROGRESS, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.PROJECTS_IN_PROGRESS, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.PROJECTS_IN_PROGRESS, idx) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        BucketSection(
                            bucket = MemoryBucket.PROJECTS_SHORT_GOALS,
                            lines = getBucketLines(MemoryBucket.PROJECTS_SHORT_GOALS),
                            onAdd = { openEditor(MemoryBucket.PROJECTS_SHORT_GOALS, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.PROJECTS_SHORT_GOALS, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.PROJECTS_SHORT_GOALS, idx) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        BucketSection(
                            bucket = MemoryBucket.PROJECTS_LONG_GOALS,
                            lines = getBucketLines(MemoryBucket.PROJECTS_LONG_GOALS),
                            onAdd = { openEditor(MemoryBucket.PROJECTS_LONG_GOALS, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.PROJECTS_LONG_GOALS, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.PROJECTS_LONG_GOALS, idx) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        BucketSection(
                            bucket = MemoryBucket.PROJECTS_DECISIONS,
                            lines = getBucketLines(MemoryBucket.PROJECTS_DECISIONS),
                            onAdd = { openEditor(MemoryBucket.PROJECTS_DECISIONS, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.PROJECTS_DECISIONS, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.PROJECTS_DECISIONS, idx) }
                        )
                    }
                }

                // 4. FINANCE
                item {
                    MemoryThemeCard(
                        title = "4. Finance",
                        subtitle = "Trading, crypto, règles de risque & budget",
                        icon = Icons.AutoMirrored.Filled.TrendingUp
                    ) {
                        BucketSection(
                            bucket = MemoryBucket.FINANCE_NOTES,
                            lines = getBucketLines(MemoryBucket.FINANCE_NOTES),
                            onAdd = { openEditor(MemoryBucket.FINANCE_NOTES, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.FINANCE_NOTES, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.FINANCE_NOTES, idx) }
                        )
                    }
                }

                // 5. WYATT (MÉMOIRE SÉPARÉE MINIMALE)
                item {
                    MemoryThemeCard(
                        title = "5. Wyatt (7 ans)",
                        subtitle = "Mémoire séparée minimale · Prénom : ${wyattProfile.firstName.ifBlank { "Wyatt" }} · Zéro info sensible",
                        icon = Icons.Default.ChildCare
                    ) {
                        BucketSection(
                            bucket = MemoryBucket.WYATT_LIKES,
                            lines = getBucketLines(MemoryBucket.WYATT_LIKES),
                            onAdd = { openEditor(MemoryBucket.WYATT_LIKES, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.WYATT_LIKES, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.WYATT_LIKES, idx) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        BucketSection(
                            bucket = MemoryBucket.WYATT_LEARNED,
                            lines = getBucketLines(MemoryBucket.WYATT_LEARNED),
                            onAdd = { openEditor(MemoryBucket.WYATT_LEARNED, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.WYATT_LEARNED, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.WYATT_LEARNED, idx) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        BucketSection(
                            bucket = MemoryBucket.WYATT_MEMORIES,
                            lines = getBucketLines(MemoryBucket.WYATT_MEMORIES),
                            onAdd = { openEditor(MemoryBucket.WYATT_MEMORIES, null, "") },
                            onEdit = { idx, value -> openEditor(MemoryBucket.WYATT_MEMORIES, idx, value) },
                            onDelete = { idx -> deleteLine(MemoryBucket.WYATT_MEMORIES, idx) }
                        )
                    }
                }

                // Collapsible plain-text preview
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aperçu export Notes",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(onClick = { showPreviewText = !showPreviewText }) {
                                    Text(if (showPreviewText) "Masquer" else "Afficher")
                                }
                            }
                            if (showPreviewText) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = exportSheet,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Line Add / Edit Dialog
            val activeEdit = editTarget
            if (activeEdit != null) {
                AlertDialog(
                    onDismissRequest = { editTarget = null },
                    title = {
                        Text(
                            if (activeEdit.index == null) "Ajouter · ${activeEdit.bucket.label}"
                            else "Modifier · ${activeEdit.bucket.label}"
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = editInput,
                                onValueChange = {
                                    editInput = it
                                    privacyWarning = null
                                },
                                label = { Text("Information durable") },
                                minLines = 1,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("memory_line_editor_input")
                            )
                            if (activeEdit.bucket.wyattMinimal) {
                                Text(
                                    text = "Wyatt : uniquement ce qu'il aime ou a appris (pas d'adresse, école ou photo).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            privacyWarning?.let { warn ->
                                Text(
                                    text = warn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RiskCoral
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val cleaned = editInput.trim()
                                if (cleaned.isBlank()) {
                                    editTarget = null
                                    return@TextButton
                                }
                                val blocked = if (activeEdit.bucket.wyattMinimal) {
                                    MemoryPrivacyGuard.containsWyattPersonalInfo(cleaned)
                                } else {
                                    MemoryPrivacyGuard.containsSensitiveData(cleaned)
                                }
                                if (blocked) {
                                    privacyWarning = "Bloqué par sécurité : aucun mot de passe, clé API, carte bancaire, papier d'identité ou info personnelle sensible."
                                    return@TextButton
                                }
                                val lines = getBucketLines(activeEdit.bucket).toMutableList()
                                if (activeEdit.index == null) {
                                    if (activeEdit.bucket == MemoryBucket.FIRST_NAME) {
                                        lines.clear()
                                    }
                                    lines.add(cleaned)
                                } else if (activeEdit.index in lines.indices) {
                                    lines[activeEdit.index] = cleaned
                                }
                                saveBucketLines(activeEdit.bucket, lines)
                                editTarget = null
                            }
                        ) {
                            Text("Enregistrer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editTarget = null }) {
                            Text("Annuler")
                        }
                    }
                )
            }

            // Clear All confirmation dialog
            if (showClearAllConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearAllConfirm = false },
                    title = { Text("Tout effacer ?") },
                    text = {
                        Text("Toutes les lignes mémorisées (Moi, Musique, Projets, Finance et Wyatt) seront définitivement effacées de Firebase.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showClearAllConfirm = false
                                onClearAllMemory()
                            }
                        ) {
                            Text("Oui, tout effacer", color = RiskCoral)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearAllConfirm = false }) {
                            Text("Annuler")
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun MemoryThemeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp).size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun BucketSection(
    bucket: MemoryBucket,
    lines: List<String>,
    singleItemOnly: Boolean = false,
    onAdd: () -> Unit,
    onEdit: (Int, String) -> Unit,
    onDelete: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bucket.label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            if (!singleItemOnly || lines.isEmpty()) {
                TextButton(
                    onClick = onAdd,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Ajouter")
                }
            }
        }
        if (lines.isEmpty()) {
            Text(
                text = "Rien d'enregistré ici pour le moment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            lines.forEachIndexed { index, line ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onEdit(index, line) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Modifier la ligne",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onDelete(index) }) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Supprimer la ligne",
                                modifier = Modifier.size(18.dp),
                                tint = RiskCoral
                            )
                        }
                    }
                }
            }
        }
    }
}
