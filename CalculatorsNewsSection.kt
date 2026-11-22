package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.TradingAcademyData
import com.example.model.MissionTopic
import com.example.model.NewsArticle
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral

@Composable
fun CalculatorsNewsSection(
    newsArticles: List<NewsArticle>,
    newsProvider: String,
    newsSummary: String?,
    isLoadingNews: Boolean,
    onRefreshNews: () -> Unit,
    onAskHazard: (String, MissionTopic) -> Unit
) {
    var calcMode by remember { mutableIntStateOf(0) } // 0: Risk, 1: Compound, 2: PnL, 3: Fees
    var expandedConceptId by remember { mutableStateOf<String?>("risk_survival") }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 4 Calculators Card
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
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("4 Calculateurs Pro", style = MaterialTheme.typography.titleLarge)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Risque / Trade", "Intérêts Composés", "Gain / Perte", "Frais").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = calcMode == idx,
                            onClick = { calcMode = idx },
                            label = { Text(label) }
                        )
                    }
                }

                when (calcMode) {
                    0 -> RiskCalculatorBody(onAskHazard = onAskHazard)
                    1 -> CompoundCalculatorBody()
                    2 -> GainLossCalculatorBody()
                    else -> FeeCalculatorBody()
                }
            }
        }

        // Economic News + Hazard Simple French Summary
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Newspaper, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Actualités Éco & Résumé", style = MaterialTheme.typography.titleLarge)
                            if (newsProvider.isNotBlank()) {
                                Text("Source : $newsProvider", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    OutlinedButton(onClick = onRefreshNews, enabled = !isLoadingNews) {
                        if (isLoadingNews) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Charger & Résumer")
                    }
                }

                if (!newsSummary.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Résumé simple de Hazard", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Text(newsSummary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                newsArticles.take(5).forEach { art ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(art.title, style = MaterialTheme.typography.titleSmall)
                        Text("${art.source} · ${art.publishedAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        // Pro Academy
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(8.dp))
            Text("Académie Pro Trading & Crypto", style = MaterialTheme.typography.headlineSmall)
        }

        TradingAcademyData.modules.forEach { concept ->
            val expanded = expandedConceptId == concept.id
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedConceptId = if (expanded) null else concept.id }
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(concept.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }
                    Text(concept.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedVisibility(visible = expanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("Règles Pro :", style = MaterialTheme.typography.labelLarge, color = ProfitEmerald)
                            concept.proRules.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                            Text("Pièges :", style = MaterialTheme.typography.labelLarge, color = RiskCoral)
                            concept.commonTraps.forEach { Text("✕ $it", style = MaterialTheme.typography.bodyMedium) }
                            Button(
                                onClick = { onAskHazard(concept.hazardCoachPrompt, MissionTopic.TRADING) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Coacher ce module avec Hazard")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RiskCalculatorBody(onAskHazard: (String, MissionTopic) -> Unit) {
    var capital by remember { mutableStateOf("200") }
    var riskPct by remember { mutableStateOf("1.0") }
    var entry by remember { mutableStateOf("95000") }
    var stop by remember { mutableStateOf("93200") }
    var tp by remember { mutableStateOf("99500") }
    var leverage by remember { mutableIntStateOf(1) }

    val calc = remember(capital, riskPct, entry, stop, tp, leverage) {
        TradingAcademyData.calculatePosition(
            capitalEur = capital.replace(",", ".").toDoubleOrNull() ?: 0.0,
            riskPercent = riskPct.replace(",", ".").toDoubleOrNull() ?: 0.0,
            entryPrice = entry.replace(",", ".").toDoubleOrNull() ?: 0.0,
            stopLossPrice = stop.replace(",", ".").toDoubleOrNull() ?: 0.0,
            takeProfitPrice = tp.replace(",", ".").toDoubleOrNull() ?: 0.0,
            leverage = leverage
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = capital, onValueChange = { capital = it }, label = { Text("Capital €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = riskPct, onValueChange = { riskPct = it }, label = { Text("Risque %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = entry, onValueChange = { entry = it }, label = { Text("Entrée") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = stop, onValueChange = { stop = it }, label = { Text("Stop-Loss") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = tp, onValueChange = { tp = it }, label = { Text("Take-Profit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(1, 2, 3, 5, 10).forEach { lev ->
            FilterChip(selected = leverage == lev, onClick = { leverage = lev }, label = { Text("x$lev") })
        }
    }
    calc?.let {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Perte max : ${String.format("%.2f", it.riskAmountEur)} € | Gain cible : +${String.format("%.2f", it.potentialProfitEur)} € | R:R ${String.format("%.2f", it.rewardRiskRatio)}:1", fontWeight = FontWeight.Bold)
                Text("Taille : ${String.format("%.5f", it.positionUnits)} unités | Marge : ${String.format("%.2f", it.marginRequiredEur)} €")
                Text(it.riskVerdict, color = if (it.liquidationBeforeStop || it.rewardRiskRatio < 1.5) RiskCoral else ProfitEmerald)
            }
        }
        Button(
            onClick = {
                onAskHazard("Analyse ce setup : Capital $capital€, Risque $riskPct%, Entrée $entry, SL $stop, TP $tp, Levier x$leverage (${it.riskVerdict}).", MissionTopic.TRADING)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Demander l'avis de Hazard")
        }
    }
}

@Composable
private fun CompoundCalculatorBody() {
    var initStr by remember { mutableStateOf("100") }
    var monthlyStr by remember { mutableStateOf("25") }
    var rateStr by remember { mutableStateOf("8") }
    var yearsStr by remember { mutableStateOf("5") }
    val res = remember(initStr, monthlyStr, rateStr, yearsStr) {
        TradingAcademyData.calculateCompoundInterest(
            initialEur = initStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            monthlyContributionEur = monthlyStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            annualRatePct = rateStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            years = yearsStr.toIntOrNull() ?: 5
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = initStr, onValueChange = { initStr = it }, label = { Text("Départ €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = monthlyStr, onValueChange = { monthlyStr = it }, label = { Text("Par mois €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = rateStr, onValueChange = { rateStr = it }, label = { Text("Taux annuel %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = yearsStr, onValueChange = { yearsStr = it }, label = { Text("Années") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Capital final estimé : ${String.format("%.2f", res.finalCapitalEur)} €", fontWeight = FontWeight.Bold, color = ProfitEmerald)
            Text("Total versé : ${String.format("%.2f", res.totalInvestedEur)} € · Intérêts : +${String.format("%.2f", res.interestEarnedEur)} €")
        }
    }
}

@Composable
private fun GainLossCalculatorBody() {
    var buyStr by remember { mutableStateOf("100") }
    var sellStr by remember { mutableStateOf("112") }
    var invStr by remember { mutableStateOf("200") }
    var feeStr by remember { mutableStateOf("0.1") }
    val res = remember(buyStr, sellStr, invStr, feeStr) {
        TradingAcademyData.calculateGainLoss(
            buyPrice = buyStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            sellPrice = sellStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            investedEur = invStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            feePctPerSide = feeStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = buyStr, onValueChange = { buyStr = it }, label = { Text("Prix achat") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = sellStr, onValueChange = { sellStr = it }, label = { Text("Prix vente") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = invStr, onValueChange = { invStr = it }, label = { Text("Investi €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = feeStr, onValueChange = { feeStr = it }, label = { Text("Frais %/ordre") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
    }
    res?.let {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Net : ${String.format("%+.2f", it.netPnlEur)} € (${String.format("%+.2f", it.netPnlPct)}%)",
                    fontWeight = FontWeight.Bold,
                    color = if (it.netPnlEur >= 0) ProfitEmerald else RiskCoral
                )
                Text("Brut : ${String.format("%+.2f", it.grossPnlEur)} € · Frais : -${String.format("%.2f", it.totalFeesEur)} € · Break-even : ${String.format("%.2f", it.breakEvenPrice)}")
            }
        }
    }
}

@Composable
private fun FeeCalculatorBody() {
    var amtStr by remember { mutableStateOf("100") }
    var feeStr by remember { mutableStateOf("0.1") }
    var spreadStr by remember { mutableStateOf("0.2") }
    val res = remember(amtStr, feeStr, spreadStr) {
        TradingAcademyData.calculateFees(
            orderAmountEur = amtStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            feePctPerSide = feeStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
            spreadPct = spreadStr.replace(",", ".").toDoubleOrNull() ?: 0.0
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = amtStr, onValueChange = { amtStr = it }, label = { Text("Ordre €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = feeStr, onValueChange = { feeStr = it }, label = { Text("Frais %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
        OutlinedTextField(value = spreadStr, onValueChange = { spreadStr = it }, label = { Text("Spread %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
    }
    res?.let {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Coût aller-retour total : ${String.format("%.2f", it.totalFrictionEur)} €", fontWeight = FontWeight.Bold)
                Text("Hausse mini pour être à zéro : +${String.format("%.2f", it.requiredMovePctToBreakEven)}%")
            }
        }
    }
}
