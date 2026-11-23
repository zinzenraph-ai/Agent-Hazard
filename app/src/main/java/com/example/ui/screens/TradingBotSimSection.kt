package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BotStrategy
import com.example.model.BotStrategyDraft
import com.example.model.MarketQuote
import com.example.model.SimPosition
import com.example.model.SimTrade
import com.example.model.SimWallet
import com.example.model.UiState
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral

@Composable
fun TradingBotSimSection(
    quotes: List<MarketQuote>,
    simWalletState: UiState<SimWallet>,
    simPositionsState: UiState<List<SimPosition>>,
    simTradesState: UiState<List<SimTrade>>,
    botStrategiesState: UiState<List<BotStrategy>>,
    botDraft: BotStrategyDraft?,
    isDraftingBot: Boolean,
    hasExchangeTradeKeys: Boolean,
    onExecuteSimOrder: (MarketQuote, String, Double) -> Unit,
    onResetSimWallet: () -> Unit,
    onDraftBotStrategy: (String) -> Unit,
    onSaveBotStrategy: (
        name: String,
        description: String,
        assetSymbol: String,
        apiId: String,
        buyDropPct: Double,
        takeProfitPct: Double,
        stopLossPct: Double,
        amountPerTradeEur: Double,
        maxTotalLossEur: Double,
        maxTradesCount: Long
    ) -> Unit,
    onStartBot: (BotStrategy) -> Unit,
    onTestBotCycle: (BotStrategy) -> Unit,
    onStopAllBots: () -> Unit,
    onSetBotMode: (BotStrategy, String) -> Unit,
    onDeleteBot: (String) -> Unit,
    onEnsureNotificationPermission: () -> Unit
) {
    val wallet = (simWalletState as? UiState.Success)?.data ?: SimWallet()
    val positions = (simPositionsState as? UiState.Success)?.data.orEmpty()
    val trades = (simTradesState as? UiState.Success)?.data.orEmpty()
    val strategies = (botStrategiesState as? UiState.Success)?.data.orEmpty()

    var selectedSymbol by remember(quotes) { mutableStateOf(quotes.firstOrNull()?.symbol ?: "BTC") }
    val selectedQuote = quotes.firstOrNull { it.symbol == selectedSymbol } ?: quotes.firstOrNull()
    var simAmountStr by remember { mutableStateOf("100") }

    // Bot builder states
    var strategyPrompt by remember { mutableStateOf("Achète BTC si le prix baisse de 5 %, revends à +8 % avec stop à -4 %") }
    var botName by remember { mutableStateOf("Rebond BTC -5% / +8%") }
    var botAsset by remember { mutableStateOf("BTC") }
    var botApiId by remember { mutableStateOf("bitcoin") }
    var buyDropStr by remember { mutableStateOf("5.0") }
    var takeProfitStr by remember { mutableStateOf("8.0") }
    var stopLossStr by remember { mutableStateOf("4.0") }
    var amountPerTradeStr by remember { mutableStateOf("50") }
    var maxLossStr by remember { mutableStateOf("200") }
    var maxTradesStr by remember { mutableStateOf("10") }

    var pendingRealModeStrategy by remember { mutableStateOf<BotStrategy?>(null) }

    LaunchedEffect(botDraft) {
        botDraft?.let { d ->
            botName = d.name
            botAsset = d.assetSymbol
            botApiId = d.apiId
            buyDropStr = d.buyDropPct.toString()
            takeProfitStr = d.takeProfitPct.toString()
            stopLossStr = d.stopLossPct.toString()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Emergency STOP bar
        val anyRunning = strategies.any { it.status == "running" }
        Button(
            onClick = onStopAllBots,
            colors = ButtonDefaults.buttonColors(
                containerColor = RiskCoral,
                contentColor = MaterialTheme.colorScheme.onError
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("emergency_stop_bot_button")
        ) {
            Icon(Icons.Default.Stop, contentDescription = "STOP immédiat")
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (anyRunning) "STOP IMMÉDIAT (BOT ACTIF)" else "STOP IMMÉDIAT · COUT D'ARRÊT BOT",
                fontWeight = FontWeight.Bold
            )
        }

        // Paper Portfolio Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "SIMULATION · ARGENT FICTIF",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    TextButton(onClick = onResetSimWallet) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Réinit. 1 000 €")
                    }
                }

                val holdingsValue = positions.sumOf { pos ->
                    val live = quotes.firstOrNull { it.symbol.equals(pos.symbol, true) }?.priceEur ?: pos.avgBuyPrice
                    pos.quantity * live
                }
                val totalValue = wallet.cashEur + holdingsValue
                val totalPnl = totalValue - wallet.initialEur

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Cash fictif", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.2f", wallet.cashEur)} €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Valeur totale", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%.2f", totalValue)} €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Gain / Perte", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${String.format("%+.2f", totalPnl)} €",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (totalPnl >= 0) ProfitEmerald else RiskCoral
                        )
                    }
                }

                if (quotes.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quotes.forEach { q ->
                            FilterChip(
                                selected = q.symbol == selectedQuote?.symbol,
                                onClick = { selectedSymbol = q.symbol },
                                label = { Text("${q.symbol} ${String.format("%.2f", q.priceEur)}€") }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = simAmountStr,
                            onValueChange = { simAmountStr = it },
                            label = { Text("Montant fictif (€)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val amt = simAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (selectedQuote != null && amt > 0) {
                                    onExecuteSimOrder(selectedQuote, "buy", amt)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitEmerald)
                        ) {
                            Text("Acheter")
                        }
                        OutlinedButton(
                            onClick = {
                                val amt = simAmountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                                if (selectedQuote != null && amt > 0) {
                                    onExecuteSimOrder(selectedQuote, "sell", amt)
                                }
                            }
                        ) {
                            Text("Vendre")
                        }
                    }
                }

                if (positions.isNotEmpty()) {
                    Text("Positions fictives :", style = MaterialTheme.typography.labelLarge)
                    positions.forEach { pos ->
                        val live = quotes.firstOrNull { it.symbol.equals(pos.symbol, true) }?.priceEur ?: pos.avgBuyPrice
                        val pnl = (live - pos.avgBuyPrice) * pos.quantity
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${pos.symbol} · ${String.format("%.5f", pos.quantity)}", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${String.format("%.2f", pos.quantity * live)} € (${String.format("%+.2f", pnl)} €)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (pnl >= 0) ProfitEmerald else RiskCoral
                            )
                        }
                    }
                }

                if (trades.isNotEmpty()) {
                    Text(
                        "Dernier ordre : ${trades.first().side.uppercase()} ${trades.first().symbol} (${String.format("%.2f", trades.first().totalEur)} €)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Automated Trading Bot Creator
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SmartToy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Créateur de Stratégie Bot Auto", style = MaterialTheme.typography.titleLarge)
                }

                OutlinedTextField(
                    value = strategyPrompt,
                    onValueChange = { strategyPrompt = it },
                    label = { Text("Décris ta règle en français") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onDraftBotStrategy(strategyPrompt) },
                    enabled = !isDraftingBot && strategyPrompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isDraftingBot) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Transformer en règles avec Hazard")
                }

                botDraft?.let { draft ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = draft.explanation,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = botName,
                        onValueChange = { botName = it },
                        label = { Text("Nom bot") },
                        singleLine = true,
                        modifier = Modifier.weight(1.4f)
                    )
                    OutlinedTextField(
                        value = botAsset,
                        onValueChange = { botAsset = it.uppercase() },
                        label = { Text("Symbole") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = buyDropStr,
                        onValueChange = { buyDropStr = it },
                        label = { Text("Achat -%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = takeProfitStr,
                        onValueChange = { takeProfitStr = it },
                        label = { Text("Vente +%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = stopLossStr,
                        onValueChange = { stopLossStr = it },
                        label = { Text("Stop -%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountPerTradeStr,
                        onValueChange = { amountPerTradeStr = it },
                        label = { Text("Montant (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxLossStr,
                        onValueChange = { maxLossStr = it },
                        label = { Text("Perte max (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxTradesStr,
                        onValueChange = { maxTradesStr = it },
                        label = { Text("Nb trades max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        onEnsureNotificationPermission()
                        onSaveBotStrategy(
                            botName,
                            strategyPrompt,
                            botAsset,
                            botApiId,
                            buyDropStr.replace(",", ".").toDoubleOrNull() ?: 5.0,
                            takeProfitStr.replace(",", ".").toDoubleOrNull() ?: 8.0,
                            stopLossStr.replace(",", ".").toDoubleOrNull() ?: 4.0,
                            amountPerTradeStr.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(1.0) ?: 50.0,
                            maxLossStr.replace(",", ".").toDoubleOrNull()?.coerceAtLeast(1.0) ?: 200.0,
                            maxTradesStr.toLongOrNull()?.coerceAtLeast(1L) ?: 10L
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sauvegarder la stratégie (Simulation d'abord)")
                }
            }
        }

        // Active Strategies List
        strategies.forEach { strat ->
            val winRate = if (strat.tradesExecuted > 0) {
                (strat.winsCount.toDouble() / strat.tradesExecuted.toDouble()) * 100.0
            } else 0.0
            val isReal = strat.mode == "real"
            val isRunning = strat.status == "running"

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isRunning) 1.5.dp else 1.dp,
                    color = if (isRunning) ProfitEmerald else MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strat.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${strat.assetSymbol} · Achat -${strat.buyDropPct}% · TP +${strat.takeProfitPct}% · SL -${strat.stopLossPct}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = if (isReal) RiskCoral.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isReal) "RÉEL" else "SIMU",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isReal) RiskCoral else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Trades: ${strat.tradesExecuted}/${strat.maxTradesCount}", style = MaterialTheme.typography.labelMedium)
                        Text("G:${strat.winsCount} P:${strat.lossesCount} (${String.format("%.0f", winRate)}%)", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "PnL: ${String.format("%+.2f", strat.cumulativePnlEur)} €",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (strat.cumulativePnlEur >= 0) ProfitEmerald else RiskCoral
                        )
                    }

                    if (strat.lastActionNote.isNotBlank()) {
                        Text(
                            text = strat.lastActionNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                onEnsureNotificationPermission()
                                onStartBot(strat)
                            },
                            enabled = !isRunning,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isRunning) "Actif..." else "Démarrer")
                        }
                        OutlinedButton(
                            onClick = {
                                onEnsureNotificationPermission()
                                onTestBotCycle(strat)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test 1 cycle")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isReal,
                            onClick = {
                                if (isReal) {
                                    onSetBotMode(strat, "simulation")
                                } else {
                                    pendingRealModeStrategy = strat
                                }
                            },
                            label = {
                                Text(
                                    if (isReal) "Passer en Simulation"
                                    else if (hasExchangeTradeKeys) "Activer Mode Réel"
                                    else "Mode Réel (clés manquantes)"
                                )
                            }
                        )
                        TextButton(onClick = { onDeleteBot(strat.id) }) {
                            Text("Supprimer", color = RiskCoral)
                        }
                    }
                }
            }
        }
    }

    val confirmTarget = pendingRealModeStrategy
    if (confirmTarget != null) {
        AlertDialog(
            onDismissRequest = { pendingRealModeStrategy = null },
            icon = { Icon(Icons.Default.WarningAmber, contentDescription = null, tint = RiskCoral) },
            title = { Text("Activer le Trading RÉEL ?") },
            text = {
                Text(
                    "En Mode Réel, le robot envoie de vrais ordres sur ton compte via les clés des Secrets AI Studio (droit de trading uniquement, sans retrait). Tu peux perdre de l'argent réel rapidement. Veux-tu vraiment armer le Mode Réel pour « ${confirmTarget.name} » ?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetBotMode(confirmTarget, "real")
                        pendingRealModeStrategy = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskCoral)
                ) {
                    Text("Oui, j'assume le risque")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRealModeStrategy = null }) {
                    Text("Rester en Simulation")
                }
            }
        )
    }
}
