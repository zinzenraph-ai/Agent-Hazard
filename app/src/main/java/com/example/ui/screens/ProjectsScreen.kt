package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.model.CreationItem
import com.example.model.MemoryGoalItem
import com.example.model.MemoryProfile
import com.example.model.MissionTopic
import com.example.model.SaasProject
import com.example.model.SpeakerMode
import com.example.model.UiState
import com.example.model.extractDashboardGoals
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral

@Composable
fun ProjectsScreen(
    speakerMode: SpeakerMode,
    memoryProfile: MemoryProfile,
    creationsState: UiState<List<CreationItem>>,
    saasProjectsState: UiState<List<SaasProject>>,
    isGeneratingSaas: Boolean,
    isGeneratingGoalActions: Boolean,
    onGenerateSaasIdea: (String) -> Unit,
    onGenerateActionsFromMemory: () -> Unit,
    onAddManualProjectAction: (String, String, String, String) -> Unit,
    onUpdateSaasNextAction: (SaasProject, String) -> Unit,
    onUpdateSaasStatus: (SaasProject, String) -> Unit,
    onToggleSaasActionDone: (SaasProject) -> Unit,
    onDeleteSaasProject: (String) -> Unit,
    onLaunchMission: (String, MissionTopic) -> Unit,
    onOpenMemory: () -> Unit,
    onTogglePin: (CreationItem) -> Unit,
    onDeleteCreation: (CreationItem) -> Unit,
    requestedSubTab: Int? = null,
    onClearRequestedSubTab: () -> Unit = {}
) {
    if (speakerMode == SpeakerMode.WYATT) {
        MissionsVaultScreen(
            speakerMode = speakerMode,
            creationsState = creationsState,
            onLaunchMission = onLaunchMission,
            onTogglePin = onTogglePin,
            onDeleteCreation = onDeleteCreation
        )
        return
    }

    // 0: Tableau de bord, 1: SaaS & Plans, 2: Missions & Coffre
    var subTab by remember { mutableIntStateOf(0) }
    LaunchedEffect(requestedSubTab) {
        if (requestedSubTab != null && requestedSubTab in 0..2) {
            subTab = requestedSubTab
            onClearRequestedSubTab()
        }
    }

    if (subTab == 2) {
        Column(modifier = Modifier.fillMaxSize()) {
            ProjectsSubTabBar(subTab = subTab, onSelect = { subTab = it })
            MissionsVaultScreen(
                speakerMode = speakerMode,
                creationsState = creationsState,
                onLaunchMission = onLaunchMission,
                onTogglePin = onTogglePin,
                onDeleteCreation = onDeleteCreation
            )
        }
        return
    }

    val projects = (saasProjectsState as? UiState.Success)?.data.orEmpty()
    val memoryGoals = remember(memoryProfile) { memoryProfile.extractDashboardGoals() }

    if (subTab == 0) {
        ProjectsDashboardTab(
            subTab = subTab,
            onSelectSubTab = { subTab = it },
            projects = projects,
            memoryGoals = memoryGoals,
            isGeneratingGoalActions = isGeneratingGoalActions,
            onGenerateActionsFromMemory = onGenerateActionsFromMemory,
            onAddManualProjectAction = onAddManualProjectAction,
            onUpdateSaasNextAction = onUpdateSaasNextAction,
            onUpdateSaasStatus = onUpdateSaasStatus,
            onToggleSaasActionDone = onToggleSaasActionDone,
            onDeleteSaasProject = onDeleteSaasProject,
            onLaunchMission = onLaunchMission,
            onOpenMemory = onOpenMemory
        )
        return
    }

    // Sub-tab 1: SaaS & Business Plans
    var themeHint by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ProjectsSubTabBar(
                subTab = subTab,
                onSelect = { subTab = it },
                horizontalPadding = 0.dp
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Générateur d'Idées Appli / SaaS", style = MaterialTheme.typography.titleLarge)
                    }
                    OutlinedTextField(
                        value = themeHint,
                        onValueChange = { themeHint = it },
                        label = { Text("Domaine (ex: musique, artisans, TikTok, freelance...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onGenerateSaasIdea(themeHint) },
                        enabled = !isGeneratingSaas,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isGeneratingSaas) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Générer Idée + Business Plan + Roadmap")
                    }
                }
            }
        }

        items(projects, key = { it.id }) { proj ->
            val expanded = expandedId == proj.id
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(proj.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onDeleteSaasProject(proj.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer projet", tint = RiskCoral, modifier = Modifier.size(20.dp))
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("idea" to "Idée", "building" to "En cours", "launched" to "Lancé").forEach { (k, lbl) ->
                            FilterChip(
                                selected = proj.status == k,
                                onClick = { onUpdateSaasStatus(proj, k) },
                                label = { Text(lbl) }
                            )
                        }
                    }

                    Text("Problème : ${proj.problem}", style = MaterialTheme.typography.bodyMedium)
                    Text("Public : ${proj.audience}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Concurrence : ${proj.competition}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Prix suggéré : ${proj.pricing}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { onToggleSaasActionDone(proj) }) {
                                Icon(
                                    imageVector = if (proj.actionDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = "Marquer action faite",
                                    tint = if (proj.actionDone) ProfitEmerald else MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Column {
                                Text("Prochaine action :", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(proj.nextAction, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    TextButton(
                        onClick = { expandedId = if (expanded) null else proj.id },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (expanded) "Masquer Business Plan & Roadmap" else "Voir Business Plan & Feuille de route")
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("Business Plan :", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(proj.businessPlan, style = MaterialTheme.typography.bodyMedium)
                            Text("Feuille de route :", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                            Text(proj.roadmap, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectsSubTabBar(
    subTab: Int,
    onSelect: (Int) -> Unit,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = subTab == 0,
            onClick = { onSelect(0) },
            label = { Text("Tableau de bord") },
            modifier = Modifier.testTag("projects_tab_dashboard")
        )
        FilterChip(
            selected = subTab == 1,
            onClick = { onSelect(1) },
            label = { Text("SaaS & Business Plans") },
            modifier = Modifier.testTag("projects_tab_saas")
        )
        FilterChip(
            selected = subTab == 2,
            onClick = { onSelect(2) },
            label = { Text("Missions & Coffre") },
            modifier = Modifier.testTag("projects_tab_vault")
        )
    }
}

@Composable
private fun ProjectsDashboardTab(
    subTab: Int,
    onSelectSubTab: (Int) -> Unit,
    projects: List<SaasProject>,
    memoryGoals: List<MemoryGoalItem>,
    isGeneratingGoalActions: Boolean,
    onGenerateActionsFromMemory: () -> Unit,
    onAddManualProjectAction: (String, String, String, String) -> Unit,
    onUpdateSaasNextAction: (SaasProject, String) -> Unit,
    onUpdateSaasStatus: (SaasProject, String) -> Unit,
    onToggleSaasActionDone: (SaasProject) -> Unit,
    onDeleteSaasProject: (String) -> Unit,
    onLaunchMission: (String, MissionTopic) -> Unit,
    onOpenMemory: () -> Unit
) {
    var actionFilter by remember { mutableStateOf("todo") } // "todo" | "all" | "done"
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<SaasProject?>(null) }
    var editingActionText by remember { mutableStateOf("") }
    var expandedRoadmapId by remember { mutableStateOf<String?>(null) }

    val doneCount = projects.count { it.actionDone }
    val todoCount = projects.count { !it.actionDone }
    val progress = if (projects.isEmpty()) 0f else doneCount.toFloat() / projects.size.toFloat()

    // Memory goals not yet tracked in existing projects list
    val plannedTexts = remember(projects) {
        projects.flatMap { listOf(it.title.lowercase(), it.problem.lowercase()) }
    }
    val unplannedMemoryGoals = remember(memoryGoals, plannedTexts) {
        memoryGoals.filter { goal ->
            val key = goal.text.lowercase()
            plannedTexts.none { it.contains(key) }
        }
    }

    val filteredProjects = remember(projects, actionFilter) {
        when (actionFilter) {
            "todo" -> projects.filter { !it.actionDone }
            "done" -> projects.filter { it.actionDone }
            else -> projects
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ProjectsSubTabBar(
                subTab = subTab,
                onSelect = onSelectSubTab,
                horizontalPadding = 0.dp
            )
        }

        // Hero Dashboard Overview Card
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RocketLaunch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(8.dp).size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Tableau de bord Projets",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${memoryGoals.size} objectif(s) détecté(s) dans ta Mémoire",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DashboardStatPill(
                                label = "Projets",
                                value = "${projects.size}",
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatPill(
                                label = "À faire",
                                value = "$todoCount",
                                accent = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardStatPill(
                                label = "Faites",
                                value = "$doneCount",
                                accent = ProfitEmerald,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (projects.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = "${(progress * 100).toInt()} % des prochaines actions cochées",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onGenerateActionsFromMemory,
                            enabled = !isGeneratingGoalActions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("projects_dashboard_generate_button")
                        ) {
                            if (isGeneratingGoalActions) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Générer actions depuis ma Mémoire")
                        }

                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("projects_dashboard_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ajouter un projet / action")
                        }
                    }
                }
            }
        }

        // Memory Goals Card (direct visibility + 1-tap convert to tracked action)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Objectifs de ta Mémoire", style = MaterialTheme.typography.titleMedium)
                        }
                        TextButton(onClick = onOpenMemory) {
                            Text("Ouvrir Mémoire")
                        }
                    }

                    if (memoryGoals.isEmpty()) {
                        Text(
                            text = "Aucun objectif n'est encore inscrit dans ta Mémoire (Court terme, Long terme, En cours, Musique ou Finance). Complète ta Mémoire ou appuie sur « Générer actions ».",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (unplannedMemoryGoals.isEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Tous les objectifs de ta Mémoire ont déjà une action planifiée ci-dessous.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ProfitEmerald
                            )
                        }
                    } else {
                        unplannedMemoryGoals.forEach { goal ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.sourceLabel.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = goal.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            onAddManualProjectAction(
                                                goal.text.take(60),
                                                goal.text,
                                                goal.sourceLabel,
                                                goal.suggestedNextAction
                                            )
                                        }
                                    ) {
                                        Text("+ Planifier")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Next Actions Filter Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Prochaines actions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = actionFilter == "todo",
                    onClick = { actionFilter = "todo" },
                    label = { Text("À faire ($todoCount)") }
                )
                FilterChip(
                    selected = actionFilter == "all",
                    onClick = { actionFilter = "all" },
                    label = { Text("Toutes") }
                )
                FilterChip(
                    selected = actionFilter == "done",
                    onClick = { actionFilter = "done" },
                    label = { Text("Faites") }
                )
            }
        }

        if (filteredProjects.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RocketLaunch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = if (projects.isEmpty()) {
                                "Aucune prochaine action planifiée pour l'instant."
                            } else {
                                "Aucune action dans ce filtre."
                            },
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "Appuie sur « Générer actions depuis ma Mémoire » pour transformer tes objectifs en étapes concrètes sur téléphone.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredProjects, key = { it.id }) { proj ->
                val expanded = expandedRoadmapId == proj.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (proj.actionDone) ProfitEmerald.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { onToggleSaasActionDone(proj) }) {
                                Icon(
                                    imageVector = if (proj.actionDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = if (proj.actionDone) "Action terminée" else "Marquer action terminée",
                                    tint = if (proj.actionDone) ProfitEmerald else MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                if (proj.audience.isNotBlank()) {
                                    Text(
                                        text = proj.audience.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = proj.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    textDecoration = if (proj.actionDone) TextDecoration.LineThrough else TextDecoration.None
                                )
                            }
                            IconButton(
                                onClick = {
                                    editingProject = proj
                                    editingActionText = proj.nextAction
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Modifier prochaine action",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = { onDeleteSaasProject(proj.id) }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Supprimer projet",
                                    tint = RiskCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (proj.problem.isNotBlank()) {
                            Text(
                                text = proj.problem,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "PROCHAINE ACTION :",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = proj.nextAction.ifBlank { "Définir la prochaine action" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("idea" to "Idée", "building" to "En cours", "launched" to "Lancé").forEach { (k, lbl) ->
                                FilterChip(
                                    selected = proj.status == k,
                                    onClick = { onUpdateSaasStatus(proj, k) },
                                    label = { Text(lbl) }
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(
                                onClick = {
                                    onLaunchMission(
                                        "Aide-moi à exécuter maintenant cette prochaine action de mon projet « ${proj.title} » : ${proj.nextAction}",
                                        MissionTopic.PROJECTS
                                    )
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Faire avec Hazard")
                            }
                        }

                        if (proj.roadmap.isNotBlank()) {
                            TextButton(
                                onClick = { expandedRoadmapId = if (expanded) null else proj.id },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                            ) {
                                Icon(
                                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (expanded) "Masquer les étapes" else "Voir la feuille de route")
                            }
                            AnimatedVisibility(visible = expanded) {
                                Text(
                                    text = proj.roadmap,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newGoal by remember { mutableStateOf("") }
        var newCategory by remember { mutableStateOf("Projet") }
        var newNextAction by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nouveau projet & action") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Nom du projet") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCategory,
                        onValueChange = { newCategory = it },
                        label = { Text("Thème (Musique, SaaS, Finance...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newGoal,
                        onValueChange = { newGoal = it },
                        label = { Text("Objectif lié (optionnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newNextAction,
                        onValueChange = { newNextAction = it },
                        label = { Text("Prochaine action concrète") },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newTitle.isNotBlank() && newNextAction.isNotBlank()) {
                            onAddManualProjectAction(newTitle, newGoal, newCategory, newNextAction)
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Annuler") }
            }
        )
    }

    val targetEdit = editingProject
    if (targetEdit != null) {
        AlertDialog(
            onDismissRequest = { editingProject = null },
            title = { Text("Modifier la prochaine action") },
            text = {
                OutlinedTextField(
                    value = editingActionText,
                    onValueChange = { editingActionText = it },
                    label = { Text(targetEdit.title) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editingActionText.isNotBlank()) {
                            onUpdateSaasNextAction(targetEdit, editingActionText)
                        }
                        editingProject = null
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProject = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun DashboardStatPill(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
