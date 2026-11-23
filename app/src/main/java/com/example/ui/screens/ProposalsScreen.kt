package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AppVersionSnapshot
import com.example.model.ImprovementProposal
import com.example.model.UiPreferences
import com.example.model.UiState
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProposalsScreen(
    currentWeekKey: String,
    proposalsState: UiState<List<ImprovementProposal>>,
    versionsState: UiState<List<AppVersionSnapshot>>,
    uiPrefs: UiPreferences,
    isScanning: Boolean,
    processingProposalId: String?,
    onRunWeeklyScan: () -> Unit,
    onApplyProposal: (ImprovementProposal) -> Unit,
    onRefuseProposal: (ImprovementProposal) -> Unit,
    onPostponeProposal: (ImprovementProposal) -> Unit,
    onStart24hTrial: (ImprovementProposal) -> Unit,
    onKeepTrial: () -> Unit,
    onCancelTrial: () -> Unit,
    onRollbackVersion: (AppVersionSnapshot) -> Unit,
    onBack: () -> Unit
) {
    var filterTab by remember { mutableIntStateOf(0) } // 0: À décider, 1: Historique, 2: Versions
    val proposals = (proposalsState as? UiState.Success)?.data.orEmpty()
    val versions = (versionsState as? UiState.Success)?.data.orEmpty()

    val usedThisWeek = proposals.count { it.weekKey == currentWeekKey }
    val pendingList = proposals.filter { it.status == "pending" || it.status == "postponed" }
    val historyList = proposals.filter { it.status in setOf("applied", "refused", "rolled_back") }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE) }

    val trialActive = uiPrefs.trialExpiresAtMs > System.currentTimeMillis() && uiPrefs.trialLabel.isNotBlank()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("proposals_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Propositions de Hazard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rien ne change sans ton « Appliquer » · Max 3/semaine",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (trialActive) {
            item {
                val hoursLeft = ((uiPrefs.trialExpiresAtMs - System.currentTimeMillis()) / (1000 * 60 * 60)).coerceAtLeast(1)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, contentDescription = "Essai 24 h", tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Essai visuel 24 h en cours (~${hoursLeft}h restantes)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = uiPrefs.trialLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onKeepTrial,
                                modifier = Modifier.weight(1f).testTag("keep_trial_button")
                            ) {
                                Text("Garder ce style")
                            }
                            OutlinedButton(
                                onClick = onCancelTrial,
                                modifier = Modifier.weight(1f).testTag("cancel_trial_button")
                            ) {
                                Text("Annuler")
                            }
                        }
                    }
                }
            }
        }

        // Weekly Scanner Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
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
                        Text(
                            text = "Amélioration Continue",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$usedThisWeek / 3 cette semaine",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = "Hazard inspecte les bugs, lenteurs, alertes sécurité, code à simplifier, ton usage réel des modules et les nouvelles API gratuites.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onRunWeeklyScan,
                        enabled = !isScanning && usedThisWeek < 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("run_weekly_scan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Analyse complète en cours...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (usedThisWeek >= 3) "Quota 3/3 atteint ($currentWeekKey)"
                                else "Lancer l'analyse (${3 - usedThisWeek} restante(s))"
                            )
                        }
                    }
                }
            }
        }

        // Sub-filter tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterTab == 0,
                    onClick = { filterTab = 0 },
                    label = { Text("À décider (${pendingList.size})") },
                    modifier = Modifier.testTag("proposals_tab_pending")
                )
                FilterChip(
                    selected = filterTab == 1,
                    onClick = { filterTab = 1 },
                    label = { Text("Historique (${historyList.size})") },
                    modifier = Modifier.testTag("proposals_tab_history")
                )
                FilterChip(
                    selected = filterTab == 2,
                    onClick = { filterTab = 2 },
                    label = { Text("Versions (${versions.size})") },
                    modifier = Modifier.testTag("proposals_tab_versions")
                )
            }
        }

        when (filterTab) {
            0 -> {
                if (proposalsState is UiState.Loading) {
                    item { CircularProgressIndicator() }
                } else if (pendingList.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Aucune proposition en attente. Lance une analyse hebdomadaire pour voir les idées de Hazard.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(pendingList, key = { it.id }) { proposal ->
                        ProposalCard(
                            proposal = proposal,
                            isProcessing = processingProposalId == proposal.id,
                            onApply = { onApplyProposal(proposal) },
                            onRefuse = { onRefuseProposal(proposal) },
                            onPostpone = { onPostponeProposal(proposal) },
                            onStart24hTrial = { onStart24hTrial(proposal) }
                        )
                    }
                }
            }
            1 -> {
                if (historyList.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Ton historique d'accords et de refus apparaîtra ici pour que Hazard apprenne tes préférences.",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(historyList, key = { it.id }) { proposal ->
                        ProposalCard(
                            proposal = proposal,
                            isProcessing = false,
                            onApply = { },
                            onRefuse = { },
                            onPostpone = { },
                            onStart24hTrial = { }
                        )
                    }
                }
            }
            2 -> {
                if (versions.isEmpty()) {
                    item {
                        Text(
                            text = "Aucune version sauvegardée.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(versions, key = { it.id }) { version ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(
                                1.dp,
                                if (version.current) ProfitEmerald else MaterialTheme.colorScheme.outlineVariant
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = version.versionTag,
                                            tint = if (version.current) ProfitEmerald else MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = version.versionTag,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (version.current) {
                                        Surface(
                                            color = ProfitEmerald.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Version active",
                                                color = ProfitEmerald,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Text(version.summary, style = MaterialTheme.typography.bodyMedium)
                                version.createdAt?.toDate()?.let { d ->
                                    Text(
                                        text = "Sauvegardée le ${dateFmt.format(d)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (!version.current) {
                                    OutlinedButton(
                                        onClick = { onRollbackVersion(version) },
                                        modifier = Modifier.testTag("rollback_${version.versionTag}")
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Revenir en arrière (1 clic)")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: ImprovementProposal,
    isProcessing: Boolean,
    onApply: () -> Unit,
    onRefuse: () -> Unit,
    onPostpone: () -> Unit,
    onStart24hTrial: () -> Unit
) {
    val categoryLabel = when (proposal.category) {
        "bug" -> "Bug"
        "perf" -> "Lenteur / Perf"
        "security" -> "Sécurité"
        "simplify" -> "Simplification"
        "feature" -> "Fonction / UI"
        "ecosystem" -> "Nouveautés & API"
        else -> proposal.category
    }
    val statusLabel = when (proposal.status) {
        "applied" -> "Appliquée"
        "refused" -> "Refusée"
        "postponed" -> "Plus tard"
        "rolled_back" -> "Annulée (Rollback)"
        else -> "En attente"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$categoryLabel · Priorité ${proposal.priority}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = when (proposal.status) {
                        "applied" -> ProfitEmerald
                        "refused", "rolled_back" -> RiskCoral
                        else -> MaterialTheme.colorScheme.secondary
                    }
                )
            }

            Text(
                text = proposal.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            ProposalDetailRow("Ce qu'il veut changer :", proposal.whatToChange)
            ProposalDetailRow("Pourquoi :", proposal.why)
            ProposalDetailRow("Impact :", proposal.impact)
            ProposalDetailRow("Risques :", proposal.risks)
            ProposalDetailRow("Comment annuler :", proposal.rollbackHowTo)

            if (proposal.beforePreview.isNotBlank() && proposal.afterPreview.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Aperçu Avant / Après",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("Avant : ${proposal.beforePreview}", style = MaterialTheme.typography.bodySmall)
                        Text("Après : ${proposal.afterPreview}", style = MaterialTheme.typography.bodySmall, color = ProfitEmerald)
                    }
                }
            }

            if (proposal.preparedPlan.isNotBlank()) {
                Surface(
                    color = ProfitEmerald.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Modification préparée :",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ProfitEmerald
                        )
                        Text(proposal.preparedPlan, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (proposal.status == "pending" || proposal.status == "postponed") {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApply,
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("apply_proposal_${proposal.id}")
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Appliquer")
                        }
                    }
                    OutlinedButton(
                        onClick = onPostpone,
                        enabled = !isProcessing,
                        modifier = Modifier.testTag("postpone_proposal_${proposal.id}")
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Plus tard")
                    }
                    OutlinedButton(
                        onClick = onRefuse,
                        enabled = !isProcessing,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCoral),
                        modifier = Modifier.testTag("refuse_proposal_${proposal.id}")
                    ) {
                        Text("Refuser")
                    }
                }

                if (proposal.supports24hTrial && proposal.trialAccentStyle.isNotBlank()) {
                    OutlinedButton(
                        onClick = onStart24hTrial,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trial_24h_${proposal.id}")
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tester cette apparence pendant 24 h")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProposalDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
