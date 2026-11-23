package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.BudgetEntry
import com.example.model.FINANCIAL_DISCLAIMER
import com.example.model.MusicRevenue
import com.example.model.SavingsGoal
import com.example.model.UiState
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral
import kotlin.math.abs
import kotlin.math.max

@Composable
fun MoneyScreen(
    budgetState: UiState<List<BudgetEntry>>,
    savingsGoalsState: UiState<List<SavingsGoal>>,
    musicRevenuesState: UiState<List<MusicRevenue>>,
    financialPlan: String?,
    isBuildingPlan: Boolean,
    onAddBudgetEntry: (String, String, String, Double, String) -> Unit,
    onAddSavingsGoal: (String, Double, Double) -> Unit,
    onAddSavingsProgress: (SavingsGoal, Double) -> Unit,
    onBuildFinancialPlan: () -> Unit,
    onAddMusicRevenue: (String, String, Long, Double, String) -> Unit,
    onDeleteDoc: (String, String) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Budget & Plan, 1: Revenus Musique

    val entries = (budgetState as? UiState.Success)?.data.orEmpty()
    val goals = (savingsGoalsState as? UiState.Success)?.data.orEmpty()
    val music = (musicRevenuesState as? UiState.Success)?.data.orEmpty()

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
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(FINANCIAL_DISCLAIMER, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = subTab == 0, onClick = { subTab = 0 }, label = { Text("Budget, Épargne & Plan") })
                FilterChip(selected = subTab == 1, onClick = { subTab = 1 }, label = { Text("Revenus Musique") })
            }
        }

        item {
            if (subTab == 0) {
                BudgetAndPlanBody(
                    entries = entries,
                    goals = goals,
                    financialPlan = financialPlan,
                    isBuildingPlan = isBuildingPlan,
                    onAddBudgetEntry = onAddBudgetEntry,
                    onAddSavingsGoal = onAddSavingsGoal,
                    onAddSavingsProgress = onAddSavingsProgress,
                    onBuildFinancialPlan = onBuildFinancialPlan,
                    onDeleteDoc = onDeleteDoc
                )
            } else {
                MusicRevenueBody(
                    revenues = music,
                    onAddMusicRevenue = onAddMusicRevenue,
                    onDeleteMusic = { id -> onDeleteDoc("music_revenues", id) }
                )
            }
        }
    }
}

@Composable
private fun BudgetAndPlanBody(
    entries: List<BudgetEntry>,
    goals: List<SavingsGoal>,
    financialPlan: String?,
    isBuildingPlan: Boolean,
    onAddBudgetEntry: (String, String, String, Double, String) -> Unit,
    onAddSavingsGoal: (String, Double, Double) -> Unit,
    onAddSavingsProgress: (SavingsGoal, Double) -> Unit,
    onBuildFinancialPlan: () -> Unit,
    onDeleteDoc: (String, String) -> Unit
) {
    val income = entries.filter { it.kind == "income" }.sumOf { it.amountEur }
    val expense = entries.filter { it.kind == "expense" }.sumOf { it.amountEur }
    val net = income - expense

    var kind by remember { mutableStateOf("expense") }
    var category by remember { mutableStateOf("Essentiel") }
    var label by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }

    var goalTitle by remember { mutableStateOf("") }
    var goalTargetStr by remember { mutableStateOf("") }

    val primaryColor = MaterialTheme.colorScheme.primary
    val netColor = if (net >= 0) ProfitEmerald else RiskCoral

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Monthly Overview + Compose Canvas Chart
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Budget & Graphique du Mois", style = MaterialTheme.typography.titleLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Revenus", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("+${String.format("%.2f", income)} €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ProfitEmerald)
                    }
                    Column {
                        Text("Dépenses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("-${String.format("%.2f", expense)} €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RiskCoral)
                    }
                    Column {
                        Text("Solde net", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format("%+.2f", net)} €", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = netColor)
                    }
                }

                // Custom Canvas Bar Chart
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val maxVal = max(100.0, max(income, max(expense, abs(net)))).toFloat()
                    val barW = size.width / 5f
                    val maxH = size.height - 16f

                    val incH = ((income.toFloat() / maxVal) * maxH).coerceAtLeast(8f)
                    val expH = ((expense.toFloat() / maxVal) * maxH).coerceAtLeast(8f)
                    val netH = ((abs(net).toFloat() / maxVal) * maxH).coerceAtLeast(8f)

                    drawRoundRect(
                        color = ProfitEmerald,
                        topLeft = Offset(barW * 0.5f, size.height - incH),
                        size = Size(barW, incH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = RiskCoral,
                        topLeft = Offset(barW * 2.0f, size.height - expH),
                        size = Size(barW, expH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(barW * 3.5f, size.height - netH),
                        size = Size(barW, netH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text("Revenus", style = MaterialTheme.typography.labelSmall, color = ProfitEmerald)
                    Text("Dépenses", style = MaterialTheme.typography.labelSmall, color = RiskCoral)
                    Text("Solde Net", style = MaterialTheme.typography.labelSmall, color = primaryColor)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "income", onClick = { kind = "income" }, label = { Text("+ Revenu") })
                    FilterChip(selected = kind == "expense", onClick = { kind = "expense" }, label = { Text("- Dépense") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Libellé (ex: Courses, Mission)") }, singleLine = true, modifier = Modifier.weight(1.4f))
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("€") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.7f))
                }
                Button(
                    onClick = {
                        val amt = amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (label.isNotBlank() && amt > 0.0) {
                            onAddBudgetEntry(kind, category, label, amt, "2026-11")
                            label = ""; amountStr = ""
                        }
                    },
                    enabled = label.isNotBlank() && (amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ajouter au budget") }
            }
        }

        // Savings Goals
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(8.dp))
                    Text("Objectifs d'Épargne", style = MaterialTheme.typography.titleLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = goalTitle, onValueChange = { goalTitle = it }, label = { Text("Objectif (ex: Matelas sécurité)") }, singleLine = true, modifier = Modifier.weight(1.4f))
                    OutlinedTextField(value = goalTargetStr, onValueChange = { goalTargetStr = it }, label = { Text("Cible €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(0.7f))
                }
                Button(
                    onClick = {
                        val target = goalTargetStr.replace(",", ".").toDoubleOrNull() ?: 0.0
                        if (goalTitle.isNotBlank() && target > 0.0) {
                            onAddSavingsGoal(goalTitle, target, 0.0)
                            goalTitle = ""; goalTargetStr = ""
                        }
                    },
                    enabled = goalTitle.isNotBlank() && (goalTargetStr.replace(",", ".").toDoubleOrNull() ?: 0.0) > 0.0,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Créer objectif") }

                goals.forEach { goal ->
                    val progress = (goal.savedEur / goal.targetEur.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${goal.title} · ${String.format("%.0f", goal.savedEur)} / ${String.format("%.0f", goal.targetEur)} €", style = MaterialTheme.typography.titleSmall)
                            IconButton(onClick = { onDeleteDoc("savings_goals", goal.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = RiskCoral, modifier = Modifier.size(16.dp))
                            }
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onAddSavingsProgress(goal, 10.0) }) { Text("+10 €") }
                            OutlinedButton(onClick = { onAddSavingsProgress(goal, 50.0) }) { Text("+50 €") }
                        }
                    }
                }
            }
        }

        // Hazard Step-by-Step Financial Plan
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Plan Financier par Étapes (Hazard)", style = MaterialTheme.typography.titleLarge)
                Button(
                    onClick = onBuildFinancialPlan,
                    enabled = !isBuildingPlan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isBuildingPlan) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Construire mon Plan Financier")
                }
                if (!financialPlan.isNullOrBlank()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Text(financialPlan, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        entries.take(15).forEach { e ->
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(e.label, style = MaterialTheme.typography.titleSmall)
                        Text(if (e.kind == "income") "Revenu" else "Dépense", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "${if (e.kind == "income") "+" else "-"}${String.format("%.2f", e.amountEur)} €",
                        fontWeight = FontWeight.Bold,
                        color = if (e.kind == "income") ProfitEmerald else RiskCoral
                    )
                    IconButton(onClick = { onDeleteDoc("budget_entries", e.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun MusicRevenueBody(
    revenues: List<MusicRevenue>,
    onAddMusicRevenue: (String, String, Long, Double, String) -> Unit,
    onDeleteMusic: (String) -> Unit
) {
    val totalEur = revenues.sumOf { it.amountEur }
    val totalPlays = revenues.sumOf { it.playsCount }

    var trackTitle by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("streams") }
    var playsStr by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var periodLabel by remember { mutableStateOf("Nov 2026") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Suivi Revenus Musique", style = MaterialTheme.typography.titleLarge)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total gagné : ${String.format("%.2f", totalEur)} €", fontWeight = FontWeight.Bold, color = ProfitEmerald)
                    Text("Écoutes / Vues : $totalPlays", style = MaterialTheme.typography.labelLarge)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("streams" to "Streams", "tiktok" to "TikTok", "sales" to "Ventes").forEach { (k, lbl) ->
                        FilterChip(selected = source == k, onClick = { source = k }, label = { Text(lbl) })
                    }
                }
                OutlinedTextField(value = trackTitle, onValueChange = { trackTitle = it }, label = { Text("Titre du morceau") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = playsStr, onValueChange = { playsStr = it }, label = { Text("Streams/Vues") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = amountStr, onValueChange = { amountStr = it }, label = { Text("Revenu €") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = periodLabel, onValueChange = { periodLabel = it }, label = { Text("Mois") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Button(
                    onClick = {
                        onAddMusicRevenue(
                            trackTitle,
                            source,
                            playsStr.toLongOrNull() ?: 0L,
                            amountStr.replace(",", ".").toDoubleOrNull() ?: 0.0,
                            periodLabel
                        )
                        trackTitle = ""; playsStr = ""; amountStr = ""
                    },
                    enabled = trackTitle.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Ajouter ce revenu musique") }
            }
        }

        revenues.forEach { r ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.trackTitle, style = MaterialTheme.typography.titleMedium)
                        Text("${r.source.uppercase()} · ${r.playsCount} vues/écoutes · ${r.periodLabel}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("+${String.format("%.2f", r.amountEur)} €", fontWeight = FontWeight.Bold, color = ProfitEmerald)
                    IconButton(onClick = { onDeleteMusic(r.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = RiskCoral)
                    }
                }
            }
        }
    }
}
