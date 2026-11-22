package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.BackupAndNetworkManager
import com.example.data.FinanceProjectsRepository
import com.example.data.HazardAiService
import com.example.data.HazardRepository
import com.example.data.MarketDataService
import com.example.data.MusicStudioAiService
import com.example.data.SecretsRegistry
import com.example.data.SecurityImprovementRepository
import com.example.firebase.signOut
import com.example.model.CreationCategory
import com.example.model.MemoryPatch
import com.example.model.MemoryProfile
import com.example.model.SpeakerMode
import com.example.model.UiState
import com.example.security.AppTelemetryMonitor
import com.example.ui.screens.BackupAndNetworkScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ConnectionsScreen
import com.example.ui.screens.MarketsFinanceScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.MoneyScreen
import com.example.ui.screens.MusicStudioScreen
import com.example.ui.screens.PersonalizeScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.ProposalsScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.theme.HazardTheme
import com.example.ui.theme.RiskCoral
import com.example.ui.viewmodel.FinanceProjectsViewModel
import com.example.ui.viewmodel.HazardTab
import com.example.ui.viewmodel.HazardViewModel
import com.example.ui.viewmodel.MusicStudioViewModel
import com.example.ui.viewmodel.SecurityImprovementViewModel
import com.google.firebase.firestore.FirebaseFirestore

private data class NavDestinationItem(
    val tab: HazardTab,
    val label: String,
    val icon: ImageVector
)

private enum class HazardOverlay {
    NONE,
    CONNECTIONS,
    SECURITY,
    PROPOSALS,
    PERSONALIZE,
    MUSIC_STUDIO,
    BACKUP_NETWORK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HazardApp(
    currentUserId: String,
    isOwner: Boolean = true,
    lockedWyattMode: Boolean = false,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: HazardViewModel = viewModel(
        key = "hazard_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                HazardViewModel(
                    repository = HazardRepository(db),
                    aiService = HazardAiService(),
                    currentUserId = currentUserId,
                    lockedWyattMode = lockedWyattMode
                )
            }
        }
    )

    val financeVm: FinanceProjectsViewModel = viewModel(
        key = "finance_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FinanceProjectsViewModel(
                    appContext = app.applicationContext,
                    repository = FinanceProjectsRepository(db),
                    marketService = MarketDataService(),
                    aiService = HazardAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val securityVm: SecurityImprovementViewModel = viewModel(
        key = "sec_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SecurityImprovementViewModel(
                    repository = SecurityImprovementRepository(db),
                    aiService = HazardAiService(),
                    currentUserId = currentUserId,
                    isOwner = isOwner,
                    lockedWyattMode = lockedWyattMode
                )
            }
        }
    )

    val studioVm: MusicStudioViewModel = viewModel(
        key = "studio_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                MusicStudioViewModel(
                    repository = HazardRepository(db),
                    aiService = MusicStudioAiService(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val speakerMode by viewModel.speakerMode.collectAsStateWithLifecycle()
    val activeTopic by viewModel.activeTopic.collectAsStateWithLifecycle()
    val memoryState by viewModel.memoryState.collectAsStateWithLifecycle()
    val wyattMemoryState by viewModel.wyattMemoryState.collectAsStateWithLifecycle()
    val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
    val creationsState by viewModel.creationsState.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val streamingText by viewModel.streamingText.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()
    val isExtractingMemory by viewModel.isExtractingMemory.collectAsStateWithLifecycle()
    val pendingMemoryPatch by viewModel.pendingMemoryPatch.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val preferFastGroq by viewModel.preferFastGroq.collectAsStateWithLifecycle()

    // Finance & Projects flows
    val watchlistState by financeVm.watchlistState.collectAsStateWithLifecycle()
    val alertsState by financeVm.alertsState.collectAsStateWithLifecycle()
    val quotes by financeVm.quotes.collectAsStateWithLifecycle()
    val quotesProvider by financeVm.quotesProvider.collectAsStateWithLifecycle()
    val quotesWarning by financeVm.quotesWarning.collectAsStateWithLifecycle()
    val isLoadingQuotes by financeVm.isLoadingQuotes.collectAsStateWithLifecycle()
    val simWalletState by financeVm.simWalletState.collectAsStateWithLifecycle()
    val simPositionsState by financeVm.simPositionsState.collectAsStateWithLifecycle()
    val simTradesState by financeVm.simTradesState.collectAsStateWithLifecycle()
    val botStrategiesState by financeVm.botStrategiesState.collectAsStateWithLifecycle()
    val botDraft by financeVm.botDraft.collectAsStateWithLifecycle()
    val isDraftingBot by financeVm.isDraftingBot.collectAsStateWithLifecycle()
    val journalState by financeVm.journalState.collectAsStateWithLifecycle()
    val journalAnalysis by financeVm.journalAnalysis.collectAsStateWithLifecycle()
    val isAnalyzingJournal by financeVm.isAnalyzingJournal.collectAsStateWithLifecycle()
    val scamVerdict by financeVm.scamVerdict.collectAsStateWithLifecycle()
    val isCheckingScam by financeVm.isCheckingScam.collectAsStateWithLifecycle()
    val newsArticles by financeVm.newsArticles.collectAsStateWithLifecycle()
    val newsProvider by financeVm.newsProvider.collectAsStateWithLifecycle()
    val newsSummary by financeVm.newsSummary.collectAsStateWithLifecycle()
    val isLoadingNews by financeVm.isLoadingNews.collectAsStateWithLifecycle()
    val budgetState by financeVm.budgetState.collectAsStateWithLifecycle()
    val savingsGoalsState by financeVm.savingsGoalsState.collectAsStateWithLifecycle()
    val musicRevenuesState by financeVm.musicRevenuesState.collectAsStateWithLifecycle()
    val financialPlan by financeVm.financialPlan.collectAsStateWithLifecycle()
    val isBuildingPlan by financeVm.isBuildingPlan.collectAsStateWithLifecycle()
    val saasProjectsState by financeVm.saasProjectsState.collectAsStateWithLifecycle()
    val isGeneratingSaas by financeVm.isGeneratingSaas.collectAsStateWithLifecycle()
    val isGeneratingGoalActions by financeVm.isGeneratingGoalActions.collectAsStateWithLifecycle()
    val financeBanner by financeVm.bannerMessage.collectAsStateWithLifecycle()

    // Security, Continuous Improvement & UI Preferences flows
    val ownerPolicy by securityVm.ownerPolicy.collectAsStateWithLifecycle()
    val securityEventsState by securityVm.securityEventsState.collectAsStateWithLifecycle()
    val proposalsState by securityVm.proposalsState.collectAsStateWithLifecycle()
    val versionsState by securityVm.versionsState.collectAsStateWithLifecycle()
    val uiPrefs by securityVm.uiPrefs.collectAsStateWithLifecycle()
    val isScanningImprovements by securityVm.isScanningImprovements.collectAsStateWithLifecycle()
    val processingProposalId by securityVm.processingProposalId.collectAsStateWithLifecycle()
    val secBanner by securityVm.bannerMessage.collectAsStateWithLifecycle()
    val studioBanner by studioVm.bannerMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        BackupAndNetworkManager.initNetworkMonitor(context)
    }
    val networkStatus by BackupAndNetworkManager.networkStatus.collectAsStateWithLifecycle()

    var activeOverlay by remember { mutableStateOf(HazardOverlay.NONE) }
    var showParentReturnDialog by remember { mutableStateOf(false) }
    var requestedMarketsSubTab by remember { mutableStateOf<Int?>(null) }
    var requestedProjectsSubTab by remember { mutableStateOf<Int?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner, financeBanner, secBanner, studioBanner) {
        val msg = statusBanner ?: financeBanner ?: secBanner ?: studioBanner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            if (statusBanner != null) viewModel.clearStatusBanner()
            if (financeBanner != null) financeVm.clearBanner()
            if (secBanner != null) securityVm.clearBanner()
            if (studioBanner != null) studioVm.clearBanner()
        }
    }

    LaunchedEffect(lockedWyattMode) {
        if (lockedWyattMode && speakerMode != SpeakerMode.WYATT) {
            viewModel.setSpeakerMode(SpeakerMode.WYATT)
        }
    }

    val isWyatt = lockedWyattMode || speakerMode == SpeakerMode.WYATT
    LaunchedEffect(isWyatt) {
        if (isWyatt && activeOverlay != HazardOverlay.NONE) {
            studioVm.metronome.stop()
            activeOverlay = HazardOverlay.NONE
        }
    }

    // Auto-expire 24h trial cleanly if its deadline has passed
    LaunchedEffect(uiPrefs.trialExpiresAtMs) {
        if (uiPrefs.trialExpiresAtMs > 0L && System.currentTimeMillis() > uiPrefs.trialExpiresAtMs) {
            securityVm.cancel24hTrialAppearance()
        }
    }

    if (activeOverlay != HazardOverlay.NONE) {
        BackHandler {
            studioVm.metronome.stop()
            activeOverlay = HazardOverlay.NONE
        }
    } else if (selectedTab != HazardTab.CHAT) {
        BackHandler { viewModel.selectTab(HazardTab.CHAT) }
    }

    val navItems = if (isWyatt) {
        listOf(
            NavDestinationItem(HazardTab.CHAT, stringResource(R.string.nav_chat), Icons.Default.Bolt),
            NavDestinationItem(HazardTab.PROJECTS, stringResource(R.string.nav_wyatt_adventures), Icons.Default.Explore)
        )
    } else {
        listOf(
            NavDestinationItem(HazardTab.CHAT, stringResource(R.string.nav_chat), Icons.Default.Bolt),
            NavDestinationItem(HazardTab.MARKETS, stringResource(R.string.nav_markets), Icons.AutoMirrored.Filled.TrendingUp),
            NavDestinationItem(HazardTab.MONEY, stringResource(R.string.nav_money), Icons.Default.AccountBalanceWallet),
            NavDestinationItem(HazardTab.PROJECTS, stringResource(R.string.nav_projects), Icons.Default.RocketLaunch),
            NavDestinationItem(HazardTab.MEMORY, stringResource(R.string.nav_memory), Icons.Default.Psychology)
        )
    }

    val currentMemory = (memoryState as? UiState.Success)?.data ?: MemoryProfile(userId = currentUserId)
    val unresolvedAlerts = (securityEventsState as? UiState.Success)?.data
        ?.count { it.eventType == "anomaly" && !it.resolved } ?: 0
    val pendingProposalsCount = (proposalsState as? UiState.Success)?.data
        ?.count { it.status == "pending" } ?: 0
    val primaryGoal = (savingsGoalsState as? UiState.Success)?.data?.firstOrNull()
    val nextMusicTrack = (creationsState as? UiState.Success)?.data
        ?.firstOrNull { it.category == CreationCategory.MUSIC.wireValue }

    fun navigateFromShortcut(shortcutId: String) {
        AppTelemetryMonitor.recordFeatureUse(shortcutId)
        if (isWyatt && shortcutId != "wyatt") return
        when (shortcutId) {
            "musique" -> {
                activeOverlay = HazardOverlay.MUSIC_STUDIO
            }
            "finance" -> {
                requestedMarketsSubTab = 0
                activeOverlay = HazardOverlay.NONE
                viewModel.selectTab(HazardTab.MARKETS)
            }
            "actualites" -> {
                requestedMarketsSubTab = 3
                activeOverlay = HazardOverlay.NONE
                viewModel.selectTab(HazardTab.MARKETS)
                financeVm.refreshEconomicNews()
            }
            "plans" -> {
                activeOverlay = HazardOverlay.NONE
                viewModel.selectTab(HazardTab.MONEY)
            }
            "wyatt" -> {
                if (!lockedWyattMode) {
                    activeOverlay = HazardOverlay.NONE
                    viewModel.setSpeakerMode(SpeakerMode.WYATT)
                }
            }
            "securite" -> activeOverlay = HazardOverlay.SECURITY
            "propositions" -> activeOverlay = HazardOverlay.PROPOSALS
            "connexions" -> activeOverlay = HazardOverlay.CONNECTIONS
        }
    }

    HazardTheme(
        isWyattMode = isWyatt,
        darkTheme = uiPrefs.themeMode == "dark",
        accentStyle = uiPrefs.accentStyle,
        textScale = uiPrefs.textScale.toFloat()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 680.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.navigationBars,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isWyatt) "Hazard · Mode Wyatt" else "Hazard",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Surface(
                                    color = if (isWyatt) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = if (isWyatt) SpeakerMode.WYATT.label else speakerMode.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isWyatt) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!isWyatt) {
                                IconButton(
                                    onClick = {
                                        AppTelemetryMonitor.recordFeatureUse("sauvegarde_reseau")
                                        activeOverlay = HazardOverlay.BACKUP_NETWORK
                                    },
                                    modifier = Modifier.testTag("open_backup_network_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = "Sauvegarde & Réseau",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        AppTelemetryMonitor.recordFeatureUse("musique")
                                        activeOverlay = HazardOverlay.MUSIC_STUDIO
                                    },
                                    modifier = Modifier.testTag("open_music_studio_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Studio Musique",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        AppTelemetryMonitor.recordFeatureUse("securite")
                                        activeOverlay = HazardOverlay.SECURITY
                                    },
                                    modifier = Modifier.testTag("open_security_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Sécurité",
                                        tint = if (unresolvedAlerts > 0) RiskCoral else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        AppTelemetryMonitor.recordFeatureUse("propositions")
                                        activeOverlay = HazardOverlay.PROPOSALS
                                    },
                                    modifier = Modifier.testTag("open_proposals_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Propositions IA",
                                        tint = if (pendingProposalsCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        AppTelemetryMonitor.recordFeatureUse("connexions")
                                        activeOverlay = HazardOverlay.CONNECTIONS
                                    },
                                    modifier = Modifier.testTag("open_connections_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsInputComponent,
                                        contentDescription = "Connexions API"
                                    )
                                }
                            }
                            if (isWyatt && !lockedWyattMode) {
                                IconButton(
                                    onClick = { showParentReturnDialog = true },
                                    modifier = Modifier.testTag("wyatt_exit_to_raph_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupervisorAccount,
                                        contentDescription = "Mode Parent (Raphaël)"
                                    )
                                }
                            }
                            if (!isWyatt || lockedWyattMode) {
                                IconButton(
                                    onClick = {
                                        financeVm.stopAllBotsImmediately()
                                        signOut(credentialManager, scope, onSignedOut)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = stringResource(R.string.sign_out)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    if (!useRail && activeOverlay == HazardOverlay.NONE) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            navItems.forEach { item ->
                                NavigationBarItem(
                                    selected = selectedTab == item.tab,
                                    onClick = {
                                        AppTelemetryMonitor.recordFeatureUse(item.label)
                                        viewModel.selectTab(item.tab)
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail && activeOverlay == HazardOverlay.NONE) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            navItems.forEach { item ->
                                NavigationRailItem(
                                    selected = selectedTab == item.tab,
                                    onClick = {
                                        AppTelemetryMonitor.recordFeatureUse(item.label)
                                        viewModel.selectTab(item.tab)
                                    },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when {
                            activeOverlay == HazardOverlay.CONNECTIONS && !isWyatt -> ConnectionsScreen(
                                statuses = financeVm.connectionStatuses(),
                                onBack = { activeOverlay = HazardOverlay.NONE }
                            )
                            activeOverlay == HazardOverlay.SECURITY && !isWyatt -> SecurityScreen(
                                isOwner = isOwner,
                                policy = ownerPolicy,
                                protections = securityVm.protectionChecklist(ownerPolicy),
                                eventsState = securityEventsState,
                                onUpdateWyattEmail = securityVm::updateWyattAllowedEmail,
                                onMarkResolved = securityVm::markAnomalyResolved,
                                onBack = { activeOverlay = HazardOverlay.NONE }
                            )
                            activeOverlay == HazardOverlay.PROPOSALS && !isWyatt -> ProposalsScreen(
                                currentWeekKey = securityVm.currentWeekKey(),
                                proposalsState = proposalsState,
                                versionsState = versionsState,
                                uiPrefs = uiPrefs,
                                isScanning = isScanningImprovements,
                                processingProposalId = processingProposalId,
                                onRunWeeklyScan = securityVm::runWeeklyImprovementScan,
                                onApplyProposal = securityVm::applyProposal,
                                onRefuseProposal = securityVm::refuseProposal,
                                onPostponeProposal = securityVm::postponeProposal,
                                onStart24hTrial = securityVm::start24hVisualTrial,
                                onKeepTrial = securityVm::keep24hTrialAppearance,
                                onCancelTrial = securityVm::cancel24hTrialAppearance,
                                onRollbackVersion = securityVm::rollbackToVersion,
                                onBack = { activeOverlay = HazardOverlay.NONE }
                            )
                            activeOverlay == HazardOverlay.PERSONALIZE && !isWyatt -> PersonalizeScreen(
                                uiPrefs = uiPrefs,
                                onSetThemeMode = securityVm::setThemeMode,
                                onSetAccentStyle = securityVm::setAccentStyle,
                                onSetTextScale = securityVm::setTextScale,
                                onMoveModule = securityVm::moveModuleOrder,
                                onToggleModuleVisibility = securityVm::toggleModuleVisibility,
                                onToggleGadget = securityVm::toggleGadget,
                                onResetToDefault = securityVm::resetToDefaultHazardStyle,
                                onBack = { activeOverlay = HazardOverlay.NONE }
                            )
                            activeOverlay == HazardOverlay.MUSIC_STUDIO && !isWyatt -> MusicStudioScreen(
                                studioVm = studioVm,
                                onBack = {
                                    studioVm.metronome.stop()
                                    activeOverlay = HazardOverlay.NONE
                                }
                            )
                            activeOverlay == HazardOverlay.BACKUP_NETWORK && !isWyatt -> BackupAndNetworkScreen(
                                networkStatus = networkStatus,
                                memory = currentMemory,
                                wyattMemory = (wyattMemoryState as? UiState.Success)?.data,
                                creations = (creationsState as? UiState.Success)?.data.orEmpty(),
                                uiPrefs = uiPrefs,
                                onBack = { activeOverlay = HazardOverlay.NONE },
                                onRestoredBackupJson = { _ ->
                                    viewModel.showStatusBanner("Sauvegarde importée avec succès.")
                                    activeOverlay = HazardOverlay.NONE
                                }
                            )
                            else -> when (selectedTab) {
                                HazardTab.CHAT -> ChatScreen(
                                    speakerMode = if (isWyatt) SpeakerMode.WYATT else speakerMode,
                                    lockedWyattMode = lockedWyattMode,
                                    activeTopic = activeTopic,
                                    messagesState = messagesState,
                                    isGenerating = isGenerating,
                                    streamingText = streamingText,
                                    aiError = aiError,
                                    hasGroqAvailable = SecretsRegistry.groqKeyOrNull() != null,
                                    preferFastGroq = preferFastGroq,
                                    uiPrefs = uiPrefs,
                                    topQuote = quotes.firstOrNull(),
                                    topNews = newsArticles.firstOrNull(),
                                    primaryGoal = primaryGoal,
                                    nextMusicTrack = nextMusicTrack,
                                    memoryProfile = currentMemory,
                                    unresolvedSecurityAlerts = unresolvedAlerts,
                                    pendingProposalsCount = pendingProposalsCount,
                                    onToggleFastGroq = viewModel::toggleFastGroq,
                                    onToggleThemeDarkLight = {
                                        val next = if (uiPrefs.themeMode == "dark") "light" else "dark"
                                        securityVm.setThemeMode(next)
                                    },
                                    onOpenPersonalize = { activeOverlay = HazardOverlay.PERSONALIZE },
                                    onKeepTrial = securityVm::keep24hTrialAppearance,
                                    onCancelTrial = securityVm::cancel24hTrialAppearance,
                                    onNavigateShortcut = ::navigateFromShortcut,
                                    onSelectSpeakerMode = { mode ->
                                        if (!lockedWyattMode) viewModel.setSpeakerMode(mode)
                                    },
                                    onSendMessage = viewModel::sendMessage,
                                    onSaveCreation = { cat, title, content ->
                                        viewModel.saveCreation(cat, title, content)
                                    },
                                    onDismissError = viewModel::clearAiError,
                                    onNewSession = viewModel::clearChatHistory,
                                    onRequestMemoryExtract = viewModel::extractMemoryFromConversation
                                )
                                HazardTab.MARKETS -> MarketsFinanceScreen(
                                    watchlistState = watchlistState,
                                    alertsState = alertsState,
                                    quotes = quotes,
                                    quotesProvider = quotesProvider,
                                    quotesWarning = quotesWarning,
                                    isLoadingQuotes = isLoadingQuotes,
                                    simWalletState = simWalletState,
                                    simPositionsState = simPositionsState,
                                    simTradesState = simTradesState,
                                    botStrategiesState = botStrategiesState,
                                    botDraft = botDraft,
                                    isDraftingBot = isDraftingBot,
                                    hasExchangeTradeKeys = financeVm.hasExchangeTradeKeys(),
                                    journalState = journalState,
                                    journalAnalysis = journalAnalysis,
                                    isAnalyzingJournal = isAnalyzingJournal,
                                    scamVerdict = scamVerdict,
                                    isCheckingScam = isCheckingScam,
                                    newsArticles = newsArticles,
                                    newsProvider = newsProvider,
                                    newsSummary = newsSummary,
                                    isLoadingNews = isLoadingNews,
                                    onRefreshQuotes = { financeVm.refreshQuotes(forceRefresh = true) },
                                    onAddAsset = financeVm::addCustomAsset,
                                    onRemoveAsset = financeVm::removeWatchlistAsset,
                                    onAddAlert = financeVm::addPriceAlert,
                                    onDeleteDoc = financeVm::deleteDoc,
                                    onExecuteSimOrder = financeVm::executeSimulatedOrder,
                                    onResetSimWallet = financeVm::resetSimulatedWallet,
                                    onDraftBotStrategy = financeVm::draftBotStrategyFromDescription,
                                    onSaveBotStrategy = financeVm::saveBotStrategy,
                                    onStartBot = financeVm::startBotStrategy,
                                    onTestBotCycle = financeVm::runOneSimulatedTestCycle,
                                    onStopAllBots = financeVm::stopAllBotsImmediately,
                                    onSetBotMode = financeVm::setBotMode,
                                    onAddJournalEntry = financeVm::addJournalEntry,
                                    onAnalyzeJournal = financeVm::analyzeJournal,
                                    onCheckScam = financeVm::checkScam,
                                    onRefreshNews = { financeVm.refreshEconomicNews(forceRefresh = true) },
                                    onAskHazard = viewModel::launchPromptInChat,
                                    onOpenConnections = { activeOverlay = HazardOverlay.CONNECTIONS },
                                    requestedSubTab = requestedMarketsSubTab,
                                    onClearRequestedSubTab = { requestedMarketsSubTab = null }
                                )
                                HazardTab.MONEY -> MoneyScreen(
                                    budgetState = budgetState,
                                    savingsGoalsState = savingsGoalsState,
                                    musicRevenuesState = musicRevenuesState,
                                    financialPlan = financialPlan,
                                    isBuildingPlan = isBuildingPlan,
                                    onAddBudgetEntry = financeVm::addBudgetEntry,
                                    onAddSavingsGoal = financeVm::addSavingsGoal,
                                    onAddSavingsProgress = financeVm::addSavingsAmount,
                                    onBuildFinancialPlan = { financeVm.buildFinancialPlan(currentMemory) },
                                    onAddMusicRevenue = financeVm::addMusicRevenue,
                                    onDeleteDoc = financeVm::deleteDoc
                                )
                                HazardTab.PROJECTS -> ProjectsScreen(
                                    speakerMode = if (isWyatt) SpeakerMode.WYATT else speakerMode,
                                    memoryProfile = currentMemory,
                                    creationsState = creationsState,
                                    saasProjectsState = saasProjectsState,
                                    isGeneratingSaas = isGeneratingSaas,
                                    isGeneratingGoalActions = isGeneratingGoalActions,
                                    onGenerateSaasIdea = { hint -> financeVm.generateSaasIdea(hint, currentMemory) },
                                    onGenerateActionsFromMemory = { financeVm.generateNextActionsFromMemory(currentMemory) },
                                    onAddManualProjectAction = financeVm::addManualProjectAction,
                                    onUpdateSaasNextAction = financeVm::updateSaasNextAction,
                                    onUpdateSaasStatus = financeVm::updateSaasStatus,
                                    onToggleSaasActionDone = financeVm::toggleSaasActionDone,
                                    onDeleteSaasProject = { id -> financeVm.deleteDoc("saas_projects", id) },
                                    onLaunchMission = viewModel::launchPromptInChat,
                                    onOpenMemory = { viewModel.selectTab(HazardTab.MEMORY) },
                                    onTogglePin = viewModel::toggleCreationPin,
                                    onDeleteCreation = viewModel::deleteCreation,
                                    requestedSubTab = requestedProjectsSubTab,
                                    onClearRequestedSubTab = { requestedProjectsSubTab = null }
                                )
                                HazardTab.MEMORY -> MemoryScreen(
                                    memoryState = memoryState,
                                    wyattMemoryState = wyattMemoryState,
                                    isExtracting = isExtractingMemory,
                                    onSaveMemory = viewModel::saveMemory,
                                    onClearAllMemory = viewModel::clearAllMemory,
                                    onStartInterview = viewModel::requestMemoryInterview,
                                    onExtractFromChat = viewModel::extractMemoryFromConversation
                                )
                            }
                        }
                    }
                }
            }
        }

        val patch = pendingMemoryPatch
        if (patch != null && !isWyatt) {
            MemoryPatchReviewDialog(
                patch = patch,
                onConfirm = viewModel::confirmPendingMemoryPatch,
                onDismiss = viewModel::dismissPendingMemoryPatch
            )
        }

        if (showParentReturnDialog && isWyatt && !lockedWyattMode) {
            AlertDialog(
                onDismissRequest = { showParentReturnDialog = false },
                title = { Text("Mode Parent · Raphaël") },
                text = { Text("Quitter le Mode Wyatt pour revenir au QG complet de Raphaël ?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showParentReturnDialog = false
                            viewModel.setSpeakerMode(SpeakerMode.RAPHAEL)
                        }
                    ) {
                        Text("Oui, revenir")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showParentReturnDialog = false }) {
                        Text("Rester avec Wyatt")
                    }
                }
            )
        }
    }
}

@Composable
private fun MemoryPatchReviewDialog(
    patch: MemoryPatch,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mettre à jour la Fiche Mémoire ?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Voici uniquement les faits que tu viens de donner (aucune supposition) :",
                    style = MaterialTheme.typography.bodyMedium
                )
                patch.firstName?.let { Text("• Prénom : $it") }
                patch.stylePreferences?.let { Text("• Style & goûts : $it") }
                patch.musicStyle?.let { Text("• Style musical : $it") }
                patch.favoriteArtists?.let { Text("• Artistes préférés : $it") }
                patch.tradingLevel?.let { Text("• Trading & Crypto : $it") }
                patch.financeNotes?.let { Text("• Finance : $it") }
                patch.wyattLikes?.let { Text("• Wyatt aime : $it") }
                patch.wyattWantsToLearn?.let { Text("• Wyatt a appris : $it") }
                patch.wyattMemories?.let { Text("• Souvenirs Wyatt : $it") }
                patch.shortTermGoals?.let { Text("• Objectifs court terme : $it") }
                patch.longTermGoals?.let { Text("• Objectifs long terme : $it") }
                patch.decisionsMade?.let { Text("• Décisions : $it") }
                patch.inProgress?.let { Text("• En cours : $it") }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Valider la mise à jour")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
