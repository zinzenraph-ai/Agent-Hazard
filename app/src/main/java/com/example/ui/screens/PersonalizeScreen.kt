package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.UiPreferences

private data class AccentOption(val id: String, val label: String, val color: Color)

private val accentOptions = listOf(
    AccentOption("hazard", "Hazard Ambre", Color(0xFFFFB300)),
    AccentOption("emerald", "Émeraude", Color(0xFF22C55E)),
    AccentOption("amber", "Or Solaire", Color(0xFFF59E0B)),
    AccentOption("violet", "Studio Violet", Color(0xFFA78BFA)),
    AccentOption("ocean", "Océan Cyan", Color(0xFF38BDF8))
)

private val gadgetOptions = listOf(
    "crypto" to "Cours crypto en direct",
    "news" to "Actualité économique du jour",
    "goal" to "Objectif d'épargne du mois",
    "track" to "Prochain morceau / son à finir"
)

fun moduleDisplayName(moduleId: String): String = when (moduleId) {
    "musique" -> "Musique & Suno"
    "finance" -> "Finance & Trading"
    "plans" -> "Budget & Plans"
    "actualites" -> "Actualités Éco"
    "wyatt" -> "Mode Wyatt"
    "securite" -> "Sécurité"
    "propositions" -> "Propositions IA"
    else -> moduleId
}

@Composable
fun PersonalizeScreen(
    uiPrefs: UiPreferences,
    onSetThemeMode: (String) -> Unit,
    onSetAccentStyle: (String) -> Unit,
    onSetTextScale: (Double) -> Unit,
    onMoveModule: (String, Int) -> Unit,
    onToggleModuleVisibility: (String) -> Unit,
    onToggleGadget: (String) -> Unit,
    onResetToDefault: () -> Unit,
    onBack: () -> Unit
) {
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
                    modifier = Modifier.testTag("personalize_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
                Spacer(Modifier.width(4.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Personnaliser l'accueil",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Thème, taille du texte, ordre des modules et gadgets",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Reset to Hazard Default Style
        item {
            OutlinedButton(
                onClick = onResetToDefault,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("reset_hazard_style_button")
            ) {
                Icon(Icons.Default.RestartAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Revenir au style Hazard par défaut")
            }
        }

        // Theme Mode (Sombre / Clair) & Colors
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = "Thème et couleurs", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Thème & Couleurs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onSetThemeMode("dark") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiPrefs.themeMode == "dark") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (uiPrefs.themeMode == "dark") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f).testTag("theme_dark_button")
                        ) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mode Sombre")
                        }
                        Button(
                            onClick = { onSetThemeMode("light") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiPrefs.themeMode == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (uiPrefs.themeMode == "light") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f).testTag("theme_light_button")
                        ) {
                            Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mode Clair")
                        }
                    }

                    Text("Couleur d'accent", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        accentOptions.forEach { option ->
                            val selected = uiPrefs.accentStyle == option.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onSetAccentStyle(option.id) }
                                    .padding(4.dp)
                                    .testTag("accent_${option.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(option.color)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = option.label.split(" ").first(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Text size scale
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
                        Icon(Icons.Default.FormatSize, contentDescription = "Taille du texte", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Taille du texte sur téléphone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0.9 to "90%", 1.0 to "100%", 1.15 to "115%", 1.3 to "130%").forEach { (scale, label) ->
                            FilterChip(
                                selected = kotlin.math.abs(uiPrefs.textScale - scale) < 0.04,
                                onClick = { onSetTextScale(scale) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f).testTag("text_scale_$label")
                            )
                        }
                    }
                }
            }
        }

        // Home Gadgets
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
                        Icon(Icons.Default.Widgets, contentDescription = "Gadgets d'accueil", tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Gadgets sur l'écran d'accueil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    gadgetOptions.forEach { (id, label) ->
                        val enabled = id in uiPrefs.enabledGadgets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Switch(
                                checked = enabled,
                                onCheckedChange = { onToggleGadget(id) },
                                modifier = Modifier.testTag("gadget_switch_$id")
                            )
                        }
                    }
                }
            }
        }

        // Module ordering & show/hide
        item {
            Text(
                text = "Ordre & affichage des raccourcis",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        itemsIndexed(uiPrefs.moduleOrder, key = { _, id -> id }) { index, moduleId ->
            val visible = moduleId !in uiPrefs.hiddenModules
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = moduleDisplayName(moduleId),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onMoveModule(moduleId, -1) },
                        enabled = index > 0
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Monter ${moduleDisplayName(moduleId)}")
                    }
                    IconButton(
                        onClick = { onMoveModule(moduleId, 1) },
                        enabled = index < uiPrefs.moduleOrder.lastIndex
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre ${moduleDisplayName(moduleId)}")
                    }
                    Switch(
                        checked = visible,
                        onCheckedChange = { onToggleModuleVisibility(moduleId) },
                        modifier = Modifier.testTag("module_visible_$moduleId")
                    )
                }
            }
        }
    }
}
