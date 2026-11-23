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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.model.OwnerAccessPolicy
import com.example.model.ProtectionCheckItem
import com.example.model.SecurityEvent
import com.example.model.UiState
import com.example.security.AppTelemetryMonitor
import com.example.security.RateLimiter
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SecurityScreen(
    isOwner: Boolean,
    policy: OwnerAccessPolicy?,
    protections: List<ProtectionCheckItem>,
    eventsState: UiState<List<SecurityEvent>>,
    onUpdateWyattEmail: (String) -> Unit,
    onMarkResolved: (String) -> Unit,
    onBack: () -> Unit
) {
    var wyattEmailInput by remember { mutableStateOf(policy?.wyattEmail.orEmpty()) }
    LaunchedEffect(policy?.wyattEmail) {
        if (policy != null && wyattEmailInput.isBlank() && policy.wyattEmail.isNotBlank()) {
            wyattEmailInput = policy.wyattEmail
        }
    }

    val telemetry = AppTelemetryMonitor.snapshot()
    val events = (eventsState as? UiState.Success)?.data.orEmpty()
    val unresolvedAnomalies = events.filter { it.eventType == "anomaly" && !it.resolved }
    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE) }

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
                    modifier = Modifier.testTag("security_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sécurité & Bouclier",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Zéro accès public · Secrets hors code · Anti-injection IA",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Bouclier actif",
                    tint = if (unresolvedAnomalies.isEmpty()) ProfitEmerald else RiskCoral,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (unresolvedAnomalies.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, RiskCoral)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WarningAmber, contentDescription = "Alerte sécurité", tint = RiskCoral)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${unresolvedAnomalies.size} alerte(s) à vérifier",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = unresolvedAnomalies.first().title + " — " + unresolvedAnomalies.first().detail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Last login & identity card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Dernière connexion",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Dernière connexion vérifiée",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Compte principal : ${policy?.ownerEmail?.ifBlank { "En cours..." } ?: "Chargement..."}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Dernière session : ${policy?.lastLoginEmail?.ifBlank { "-" } ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Appareil : ${policy?.lastLoginDevice?.ifBlank { "Android" } ?: "Android"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val dateStr = policy?.lastLoginAt?.toDate()?.let { dateFmt.format(it) } ?: "À l'instant"
                    Text(
                        text = "Date : $dateStr",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Authorized Wyatt email allowlist (owner only)
        if (isOwner) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "Compte Wyatt",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Compte Google autorisé pour Wyatt",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Seul ton compte Google et cet email secondaire peuvent ouvrir Hazard. Si Wyatt se connecte avec son email, l'appli reste verrouillée en Mode Wyatt (zéro finance/trading/clés).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = wyattEmailInput,
                            onValueChange = { wyattEmailInput = it },
                            label = { Text("Email Google de Wyatt (optionnel)") },
                            placeholder = { Text("ex: wyatt@gmail.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wyatt_email_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onUpdateWyattEmail(wyattEmailInput) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("save_wyatt_email_button")
                            ) {
                                Text("Autoriser cet email")
                            }
                            if (!policy?.wyattEmail.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        wyattEmailInput = ""
                                        onUpdateWyattEmail("")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskCoral),
                                    modifier = Modifier.testTag("revoke_wyatt_email_button")
                                ) {
                                    Text("Retirer")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Protection status checklist
        item {
            Text(
                text = "État des protections",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        items(protections, key = { it.id }) { check ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (check.active) Icons.Default.VerifiedUser else Icons.Default.Lock,
                        contentDescription = check.title,
                        tint = if (check.active) ProfitEmerald else RiskCoral,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(check.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(
                            text = check.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = if (check.active) ProfitEmerald.copy(alpha = 0.16f) else RiskCoral.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = check.statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (check.active) ProfitEmerald else RiskCoral,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Rate limiter & live telemetry card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = "Télémétrie", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Quotas anti-abus & Télémétrie live",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Appels IA session : ${telemetry.aiCallsCount} (moy. ${telemetry.avgAiLatencyMs} ms) · Marchés : ${telemetry.marketCallsCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Restant cette minute — Chat IA : ${RateLimiter.remainingThisMinute("ai_chat")}/${RateLimiter.bucketLimitsPerMinute["ai_chat"]} · Marchés : ${RateLimiter.remainingThisMinute("market_quotes")}/${RateLimiter.bucketLimitsPerMinute["market_quotes"]}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Injections bloquées : ${telemetry.injectionBlocksCount} · Limites atteintes : ${telemetry.rateLimitHits}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (telemetry.injectionBlocksCount > 0) RiskCoral else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Security events history
        item {
            Text(
                text = "Journal des événements & alertes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        if (eventsState is UiState.Loading) {
            item {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            }
        } else if (events.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Aucune anomalie détectée. Tout est verrouillé.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(events.take(25), key = { it.id }) { ev ->
                val isCritical = ev.severity == "critical" || (ev.eventType == "anomaly" && !ev.resolved)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (isCritical && !ev.resolved) RiskCoral else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (ev.eventType == "anomaly" && !ev.resolved) {
                                        Icons.Default.WarningAmber
                                    } else {
                                        Icons.Default.CheckCircle
                                    },
                                    contentDescription = ev.title,
                                    tint = when {
                                        ev.eventType == "anomaly" && !ev.resolved -> RiskCoral
                                        else -> ProfitEmerald
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(ev.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                            ev.createdAt?.toDate()?.let { d ->
                                Text(
                                    text = dateFmt.format(d),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (ev.detail.isNotBlank()) {
                            Text(ev.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (ev.deviceLabel.isNotBlank()) {
                            Text("Source : ${ev.deviceLabel}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (ev.eventType == "anomaly" && !ev.resolved) {
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { onMarkResolved(ev.id) },
                                modifier = Modifier.testTag("resolve_event_${ev.id}")
                            ) {
                                Text("Marquer comme vérifié")
                            }
                        }
                    }
                }
            }
        }
    }
}
