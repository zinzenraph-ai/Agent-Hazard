package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.CreationCategory
import com.example.model.CreationItem
import com.example.model.MissionTopic
import com.example.model.SpeakerMode
import com.example.model.UiState

private data class MissionTemplate(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val topic: MissionTopic,
    val prompt: String
)

@Composable
fun MissionsVaultScreen(
    speakerMode: SpeakerMode,
    creationsState: UiState<List<CreationItem>>,
    onLaunchMission: (String, MissionTopic) -> Unit,
    onTogglePin: (CreationItem) -> Unit,
    onDeleteCreation: (CreationItem) -> Unit
) {
    val isWyatt = speakerMode == SpeakerMode.WYATT
    val clipboard = LocalClipboardManager.current
    var selectedFilter by remember(isWyatt) {
        mutableStateOf<CreationCategory?>(if (isWyatt) CreationCategory.WYATT else null)
    }

    val missions = remember(isWyatt) {
        if (isWyatt) {
            listOf(
                MissionTemplate(
                    title = "Histoire Absurde",
                    subtitle = "Une aventure folle à choix multiples",
                    icon = Icons.Default.Explore,
                    topic = MissionTopic.WYATT_ADVENTURE,
                    prompt = "Invente une histoire absurde et rigolote dont je suis le héros, puis demande-moi ce que je choisis pour la suite !"
                ),
                MissionTemplate(
                    title = "Labo Science Rigolo",
                    subtitle = "Expérience sans danger + secret du monde",
                    icon = Icons.Default.Science,
                    topic = MissionTopic.WYATT_ADVENTURE,
                    prompt = "Explique-moi un secret fou sur les dinosaures, l'espace ou les volcans comme un grand frère, avec un mini défi !"
                ),
                MissionTemplate(
                    title = "Atelier Inventions",
                    subtitle = "Créer un objet débile mais génial",
                    icon = Icons.Default.RocketLaunch,
                    topic = MissionTopic.WYATT_ADVENTURE,
                    prompt = "Aidons-nous à inventer une machine complètement folle ! Pose-moi 2 questions pour dessiner son plan."
                )
            )
        } else {
            listOf(
                MissionTemplate(
                    title = "Musique & Prompt Suno",
                    subtitle = "2 versions de paroles + style/tempo/clip",
                    icon = Icons.Default.MusicNote,
                    topic = MissionTopic.MUSIC,
                    prompt = "On bosse sur un nouveau morceau. Pose-moi 2 questions rapides (thème et vibe) ou pars directement avec ce que tu sais de ma Fiche Mémoire pour écrire 2 versions de paroles + le prompt Suno complet + 1 idée de clip au téléphone."
                ),
                MissionTemplate(
                    title = "Promo TikTok Virale",
                    subtitle = "Hooks 3s, légendes, hashtags, planning",
                    icon = Icons.Default.Campaign,
                    topic = MissionTopic.PROMO,
                    prompt = "Construis-moi un plan TikTok réaliste tournable avec mon téléphone seul pour faire décoller mes sons : 3 hooks de 3 secondes, titres, descriptions, hashtags et rythme de publication."
                ),
                MissionTemplate(
                    title = "Administratif Prêt à Envoyer",
                    subtitle = "Mails, courriers, démarches étape par étape",
                    icon = Icons.Default.Description,
                    topic = MissionTopic.ADMIN,
                    prompt = "J'ai besoin d'un coup de main administratif (mail, courrier officiel ou démarche). Demande-moi le destinataire et l'objectif pour me sortir les étapes + un brouillon prêt à copier."
                ),
                MissionTemplate(
                    title = "Projets & Cash-Flow Mobile",
                    subtitle = "Pistes concrètes sans ordinateur, zéro arnaque",
                    icon = Icons.Default.WorkOutline,
                    topic = MissionTopic.PROJECTS,
                    prompt = "J'ai besoin de générer des revenus réalistes avec uniquement mon téléphone pour moi et Wyatt. Compare 3 vraies options concrètes (écriture, micro-services, aides, création), avertis des pièges et recommande-en une avec le plan d'action d'aujourd'hui."
                ),
                MissionTemplate(
                    title = "Coaching Trading & Crypto",
                    subtitle = "Analyse de marché, plan de risque, anti-FOMO",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    topic = MissionTopic.TRADING,
                    prompt = "Fais le point avec moi comme un mentor trading/crypto pro : quel exercice de lecture graphique ou de gestion du risque on fait aujourd'hui en Paper Trading ?"
                )
            )
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = if (isWyatt) "Missions d'Aventure de Wyatt" else "Missions de Hazard",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isWyatt) "Choisis une aventure et fonce !"
                else "Lance une mission directe avec le contexte de ta Fiche Mémoire.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(missions, key = { it.title }) { mission ->
            val idx = missions.indexOf(mission)
            val cardColor = if (isWyatt) {
                when (idx % 3) {
                    0 -> MaterialTheme.colorScheme.primaryContainer
                    1 -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
            } else {
                MaterialTheme.colorScheme.surface
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(if (isWyatt) 24.dp else 18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(if (isWyatt) 16.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isWyatt) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(if (isWyatt) 50.dp else 44.dp)
                    ) {
                        Icon(
                            imageVector = mission.icon,
                            contentDescription = mission.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mission.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = mission.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onLaunchMission(mission.prompt, mission.topic) },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (isWyatt) "Jouer !" else "Lancer")
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (isWyatt) "Trésors sauvegardés" else "Coffre des Créations & Brouillons",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (!isWyatt) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Tout") }
                    )
                    CreationCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedFilter == cat,
                            onClick = { selectedFilter = cat },
                            label = { Text(cat.label) }
                        )
                    }
                }
            }
        }

        when (creationsState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(
                        text = creationsState.message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                val filtered = creationsState.data
                    .filter { item ->
                        if (isWyatt) item.category == CreationCategory.WYATT.wireValue
                        else selectedFilter == null || item.category == selectedFilter?.wireValue
                    }
                    .sortedByDescending { it.pinned }

                if (filtered.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Rien d'enregistré ici pour l'instant. Utilise l'icône marque-page sur une réponse de Hazard ou le bouton Journal du Calculateur Trading.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { item ->
                        CreationCard(
                            item = item,
                            onCopy = { clipboard.setText(AnnotatedString(item.content)) },
                            onTogglePin = { onTogglePin(item) },
                            onDelete = { onDeleteCreation(item) },
                            onReopen = {
                                val topic = when (CreationCategory.fromWire(item.category)) {
                                    CreationCategory.MUSIC -> MissionTopic.MUSIC
                                    CreationCategory.PROMO -> MissionTopic.PROMO
                                    CreationCategory.ADMIN -> MissionTopic.ADMIN
                                    CreationCategory.PROJECTS -> MissionTopic.PROJECTS
                                    CreationCategory.TRADING -> MissionTopic.TRADING
                                    CreationCategory.WYATT -> MissionTopic.WYATT_ADVENTURE
                                }
                                onLaunchMission(
                                    "Reprenons et améliorons ce document sauvegardé (${item.title}) :\n\n${item.content}",
                                    topic
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreationCard(
    item: CreationItem,
    onCopy: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onReopen: () -> Unit
) {
    val category = CreationCategory.fromWire(item.category)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (item.pinned) 1.5.dp else 1.dp,
            color = if (item.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = category.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Row {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Épingler",
                            tint = if (item.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = item.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            OutlinedButton(
                onClick = onReopen,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Retravailler avec Hazard")
            }
        }
    }
}
