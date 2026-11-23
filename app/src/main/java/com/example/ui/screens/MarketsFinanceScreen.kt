package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.BotStrategy
import com.example.model.BotStrategyDraft
import com.example.model.FINANCIAL_DISCLAIMER
import com.example.model.JournalEntry
import com.example.model.MarketQuote
import com.example.model.MissionTopic
import com.example.model.NewsArticle
import com.example.model.PriceAlert
import com.example.model.SimPosition
import com.example.model.SimTrade
import com.example.model.SimWallet
import com.example.model.UiState
import com.example.model.WatchlistAsset
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral

@Composable
fun MarketsFinanceScreen(
    watchlistState: UiState<List<WatchlistAsset>>,
    alertsState: UiState<List<PriceAlert>>,
    quotes: List<MarketQuote>,
    quotesProvider: String,
    quotesWarning: String?,
    isLoadingQuotes: Boolean,
    simWalletState: UiState<SimWallet>,
    simPositionsState: UiState<List<SimPosition>>,
    simTradesState: UiState<List<SimTrade>>,
    botStrategiesState: UiState<List<BotStrategy>>,
    botDraft: BotStrategyDraft?,
    isDraftingBot: Boolean,
    hasExchangeTradeKeys: Boolean,
    journalState: UiState<List<JournalEntry>>,
    journalAnalysis: String?,
    isAnalyzingJournal: Boolean,
    scamVerdict: String?,
    isCheckingScam: Boolean,
    newsArticles: List<NewsArticle>,
    newsProvider: String,
    newsSummary: String?,
    isLoadingNews: Boolean,
    onRefreshQuotes: () -> Unit,
    onAddAsset: (String, String, String, String) -> Unit,
    onRemoveAsset: (WatchlistAsset) -> Unit,
    onAddAlert: (MarketQuote, Double, Boolean) -> Unit,
    onDeleteDoc: (String, String) -> Unit,
    onExecuteSimOrder: (MarketQuote, String, Double) -> Unit,
    onResetSimWallet: () -> Unit,
    onDraftBotStrategy: (String) -> Unit,
    onSaveBotStrategy: (String, String, String, String, Double, Double, Double, Double, Double, Long) -> Unit,
    onStartBot: (BotStrategy) -> Unit,
    onTestBotCycle: (BotStrategy) -> Unit,
    onStopAllBots: () -> Unit,
    onSetBotMode: (BotStrategy, String) -> Unit,
    onAddJournalEntry: (String, String, String, String, String, String, String, String) -> Unit,
    onAnalyzeJournal: () -> Unit,
    onCheckScam: (String) -> Unit,
    onRefreshNews: () -> Unit,
    onAskHazard: (String, MissionTopic) -> Unit,
    onOpenConnections: () -> Unit,
    requestedSubTab: Int? = null,
    onClearRequestedSubTab: () -> Unit = {}
) {
    val context = LocalContext.current
    var subTab by remember { mutableIntStateOf(0) } // 0: Cours&Alertes, 1: Simu&Bot, 2: Journal&Arnaques, 3: Calculs&Actu

    androidx.compose.runtime.LaunchedEffect(requestedSubTab) {
        if (requestedSubTab != null && requestedSubTab in 0..3) {
            subTab = requestedSubTab
            onClearRequestedSubTab()
        }
    }

    val notificationPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = FINANCIAL_DISCLAIMER,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Cours & Alertes", "Simu & Bot", "Journal & Arnaques", "Calculs & Actu").forEachIndexed { idx, label ->
                    FilterChip(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("markets_subtab_$idx")
                    )
                }
            }
        }

        item {
            when (subTab) {
                0 -> LiveQuotesAndAlertsSection(
                    watchlistState = watchlistState,
                    alertsState = alertsState,
                    quotes = quotes,
                    quotesProvider = quotesProvider,
                    quotesWarning = quotesWarning,
                    isLoadingQuotes = isLoadingQuotes,
                    onRefreshQuotes = onRefreshQuotes,
                    onAddAsset = onAddAsset,
                    onRemoveAsset = onRemoveAsset,
                    onAddAlert = { quote, price, above ->
                        ensureNotificationPermission()
                        onAddAlert(quote, price, above)
                    },
                    onDeleteAlert = { id -> onDeleteDoc("price_alerts", id) },
                    onOpenConnections = onOpenConnections
                )
                1 -> TradingBotSimSection(
                    quotes = quotes,
                    simWalletState = simWalletState,
                    simPositionsState = simPositionsState,
                    simTradesState = simTradesState,
                    botStrategiesState = botStrategiesState,
                    botDraft = botDraft,
                    isDraftingBot = isDraftingBot,
                    hasExchangeTradeKeys = hasExchangeTradeKeys,
                    onExecuteSimOrder = onExecuteSimOrder,
                    onResetSimWallet = onResetSimWallet,
                    onDraftBotStrategy = onDraftBotStrategy,
                    onSaveBotStrategy = onSaveBotStrategy,
                    onStartBot = onStartBot,
                    onTestBotCycle = onTestBotCycle,
                    onStopAllBots = onStopAllBots,
                    onSetBotMode = onSetBotMode,
                    onDeleteBot = { id -> onDeleteDoc("bot_strategies", id) },
                    onEnsureNotificationPermission = ::ensureNotificationPermission
                )
                2 -> JournalAndScamSection(
                    journalState = journalState,
                    journalAnalysis = journalAnalysis,
                    isAnalyzingJournal = isAnalyzingJournal,
                    scamVerdict = scamVerdict,
                    isCheckingScam = isCheckingScam,
                    onAddJournalEntry = onAddJournalEntry,
                    onDeleteJournal = { id -> onDeleteDoc("journal_entries", id) },
                    onAnalyzeJournal = onAnalyzeJournal,
                    onCheckScam = onCheckScam
                )
                else -> CalculatorsNewsSection(
                    newsArticles = newsArticles,
                    newsProvider = newsProvider,
                    newsSummary = newsSummary,
                    isLoadingNews = isLoadingNews,
                    onRefreshNews = onRefreshNews,
                    onAskHazard = onAskHazard
                )
            }
        }
    }
}

@Composable
private fun LiveQuotesAndAlertsSection(
    watchlistState: UiState<List<WatchlistAsset>>,
    alertsState: UiState<List<PriceAlert>>,
    quotes: List<MarketQuote>,
    quotesProvider: String,
    quotesWarning: String?,
    isLoadingQuotes: Boolean,
    onRefreshQuotes: () -> Unit,
    onAddAsset: (String, String, String, String) -> Unit,
    onRemoveAsset: (WatchlistAsset) -> Unit,
    onAddAlert: (MarketQuote, Double, Boolean) -> Unit,
    onDeleteAlert: (String) -> Unit,
    onOpenConnections: () -> Unit
) {
    val watchlist = (watchlistState as? UiState.Success)?.data.orEmpty()
    val alerts = (alertsState as? UiState.Success)?.data.orEmpty()

    var showAddAsset by remember { mutableStateOf(false) }
    var newSymbol by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }
    var newApiId by remember { mutableStateOf("") }
    var newType by remember { mutableStateOf("crypto") }

    var alertTargetQuote by remember { mutableStateOf<MarketQuote?>(null) }
    var alertPriceStr by remember { mutableStateOf("") }
    var alertAbove by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Cours en Direct (Lecture seule)", style = MaterialTheme.typography.titleLarge)
                Text("Sources : $quotesProvider", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Row {
                IconButton(onClick = onOpenConnections) {
                    Icon(Icons.Default.SettingsInputComponent, contentDescription = "Connexions API")
                }
                IconButton(onClick = { showAddAsset = !showAddAsset }) {
                    Icon(Icons.Default.Add, contentDescription = "Ajouter un favori")
                }
                IconButton(onClick = onRefreshQuotes, enabled = !isLoadingQuotes) {
                    if (isLoadingQuotes) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, contentDescription = "Actualiser les cours")
                }
            }
        }

        if (!quotesWarning.isNullOrBlank()) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Text(quotesWarning, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
            }
        }

        if (showAddAsset) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ajouter un actif favori", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("crypto" to "Crypto", "gold" to "Or", "stock" to "Action").forEach { (k, lbl) ->
                            FilterChip(selected = newType == k, onClick = { newType = k }, label = { Text(lbl) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newSymbol, onValueChange = { newSymbol = it }, label = { Text("Symbole (ex: AVAX)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = newApiId, onValueChange = { newApiId = it }, label = { Text("ID API (ex: avalanche-2)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nom") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            onAddAsset(newSymbol, newName, newType, newApiId)
                            newSymbol = ""; newName = ""; newApiId = ""; showAddAsset = false
                        },
                        enabled = newSymbol.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Ajouter aux favoris") }
                }
            }
        }

        quotes.forEach { quote ->
            val savedAsset = watchlist.firstOrNull { it.symbol.equals(quote.symbol, true) }
            val changeColor = if (quote.change24hPct >= 0.0) ProfitEmerald else RiskCoral
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${quote.symbol} · ${quote.name}", style = MaterialTheme.typography.titleMedium)
                        Text("${quote.assetType.uppercase()} · ${quote.sourceProvider}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${String.format("%.2f", quote.priceEur)} €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${String.format("%+.2f", quote.change24hPct)} %", style = MaterialTheme.typography.labelMedium, color = changeColor)
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = {
                        alertTargetQuote = quote
                        alertPriceStr = String.format("%.2f", quote.priceEur)
                    }) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = "Créer alerte prix", tint = MaterialTheme.colorScheme.primary)
                    }
                    if (savedAsset != null) {
                        IconButton(onClick = { onRemoveAsset(savedAsset) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Retirer favori", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        if (alerts.isNotEmpty()) {
            Text("Mes Alertes Prix", style = MaterialTheme.typography.titleMedium)
            alerts.forEach { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${alert.symbol} ${if (alert.above) ">=" else "<="} ${String.format("%.2f", alert.targetPrice)} €",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                if (alert.triggered) "DÉCLENCHÉE" else "En surveillance",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (alert.triggered) ProfitEmerald else MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = { onDeleteAlert(alert.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer alerte", tint = RiskCoral)
                        }
                    }
                }
            }
        }
    }

    val targetQuote = alertTargetQuote
    if (targetQuote != null) {
        AlertDialog(
            onDismissRequest = { alertTargetQuote = null },
            title = { Text("Alerte Prix · ${targetQuote.symbol}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Cours actuel : ${String.format("%.2f", targetQuote.priceEur)} €")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = alertAbove, onClick = { alertAbove = true }, label = { Text("Au-dessus (>=") })
                        FilterChip(selected = !alertAbove, onClick = { alertAbove = false }, label = { Text("En dessous (<=") })
                    }
                    OutlinedTextField(
                        value = alertPriceStr,
                        onValueChange = { alertPriceStr = it },
                        label = { Text("Seuil (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val threshold = alertPriceStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (threshold > 0.0) {
                        onAddAlert(targetQuote, threshold, alertAbove)
                    }
                    alertTargetQuote = null
                }) { Text("Créer") }
            },
            dismissButton = {
                TextButton(onClick = { alertTargetQuote = null }) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun JournalAndScamSection(
    journalState: UiState<List<JournalEntry>>,
    journalAnalysis: String?,
    isAnalyzingJournal: Boolean,
    scamVerdict: String?,
    isCheckingScam: Boolean,
    onAddJournalEntry: (String, String, String, String, String, String, String, String) -> Unit,
    onDeleteJournal: (String) -> Unit,
    onAnalyzeJournal: () -> Unit,
    onCheckScam: (String) -> Unit
) {
    val entries = (journalState as? UiState.Success)?.data.orEmpty()

    var suspiciousText by remember { mutableStateOf("") }
    var asset by remember { mutableStateOf("BTC") }
    var direction by remember { mutableStateOf("spot") }
    var entryPrice by remember { mutableStateOf("") }
    var stopLoss by remember { mutableStateOf("") }
    var exitPrice by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var emotion by remember { mutableStateOf("Calme") }
    var outcome by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Scam Detector
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = RiskCoral)
                    Spacer(Modifier.width(8.dp))
                    Text("Détecteur d'Arnaques Hazard", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    value = suspiciousText,
                    onValueChange = { suspiciousText = it },
                    label = { Text("Colle une offre, un DM Telegram ou une promesse de gain...") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onCheckScam(suspiciousText) },
                    enabled = !isCheckingScam && suspiciousText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCoral),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingScam) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Vérifier si c'est une arnaque")
                }
                if (!scamVerdict.isNullOrBlank()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                        Text(scamVerdict, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        // Trading Journal Entry Form + AI Coach
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Journal de Trading", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = asset, onValueChange = { asset = it }, label = { Text("Actif") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = emotion, onValueChange = { emotion = it }, label = { Text("Émotion") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("spot" to "Spot", "long" to "Long", "short" to "Short").forEach { (k, lbl) ->
                        FilterChip(selected = direction == k, onClick = { direction = k }, label = { Text(lbl) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = entryPrice, onValueChange = { entryPrice = it }, label = { Text("Entrée") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = stopLoss, onValueChange = { stopLoss = it }, label = { Text("Stop-Loss") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = exitPrice, onValueChange = { exitPrice = it }, label = { Text("Sortie") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Pourquoi j'ai pris cette décision ?") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = outcome, onValueChange = { outcome = it }, label = { Text("Résultat (+15 € / -8 € / leçon)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            onAddJournalEntry(asset, direction, entryPrice, exitPrice, stopLoss, reason, emotion, outcome)
                            entryPrice = ""; stopLoss = ""; exitPrice = ""; reason = ""; outcome = ""
                        },
                        enabled = asset.isNotBlank() && reason.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Enregistrer") }
                    OutlinedButton(
                        onClick = onAnalyzeJournal,
                        enabled = !isAnalyzingJournal && entries.isNotEmpty(),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isAnalyzingJournal) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Analyser")
                    }
                }

                if (!journalAnalysis.isNullOrBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Text(journalAnalysis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        entries.forEach { entry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${entry.asset} · ${entry.direction}", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onDeleteJournal(entry.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = RiskCoral, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (entry.outcome.isNotBlank()) Text("Résultat : ${entry.outcome}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Raison : ${entry.reason}", style = MaterialTheme.typography.bodyMedium)
                    if (entry.aiFeedback.isNotBlank()) {
                        Text(entry.aiFeedback, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
