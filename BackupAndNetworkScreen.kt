package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.data.BackupAndNetworkManager
import com.example.data.BackupLocalFile
import com.example.data.NetworkStatusInfo
import com.example.model.CreationItem
import com.example.model.MemoryProfile
import com.example.model.UiPreferences
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndNetworkScreen(
    networkStatus: NetworkStatusInfo,
    memory: MemoryProfile?,
    wyattMemory: MemoryProfile?,
    creations: List<CreationItem>,
    uiPrefs: UiPreferences?,
    onBack: () -> Unit,
    onRestoredBackupJson: (String) -> Unit
) {
    val context = LocalContext.current
    var exportPassword by remember { mutableStateOf("") }
    var useEncryption by remember { mutableStateOf(false) }
    var importPassword by remember { mutableStateOf("") }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportFileUri by remember { mutableStateOf<Uri?>(null) }
    var noticeMessage by remember { mutableStateOf<String?>(null) }

    var localBackups by remember { mutableStateOf(BackupAndNetworkManager.getLatestLocalBackups(context)) }

    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            pendingImportFileUri = uri
            showImportPasswordDialog = true
        }
    }

    fun performExport(isWyattOnly: Boolean = false) {
        val targetMem = if (isWyattOnly) null else memory
        val targetWyatt = if (isWyattOnly) wyattMemory else null
        val targetCreations = if (isWyattOnly) emptyList() else creations

        val json = BackupAndNetworkManager.createExportJsonBundle(
            targetMem, targetWyatt, targetCreations, uiPrefs,
            password = if (useEncryption) exportPassword.trim().takeIf { it.isNotBlank() } else null
        )
        val file = BackupAndNetworkManager.saveLocalBackupFile(context, json, useEncryption)
        localBackups = BackupAndNetworkManager.getLatestLocalBackups(context)
        noticeMessage = "Sauvegarde exportée : ${file.name}"

        // Share intent
        try {
            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Envoyer la sauvegarde Hazard"))
        } catch (_: Exception) {
        }
    }

    fun performImport(uri: Uri, password: String?) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use {
                it.reader(Charsets.UTF_8).readText()
            } ?: throw IllegalStateException("Impossible de lire le fichier.")
            val decrypted = BackupAndNetworkManager.decryptAesIfEncrypted(content, password)
            onRestoredBackupJson(decrypted)
            noticeMessage = "Restauration réussie !"
        } catch (e: Exception) {
            noticeMessage = e.message ?: "Échec de la restauration (mot de passe incorrect ?)."
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            .testTag("backup_network_screen")
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("backup_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("Sauvegarde & Réseau", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Données privées, persistance et bascule Starlink / Mobile", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (noticeMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(noticeMessage.orEmpty(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        TextButton(onClick = { noticeMessage = null }) { Text("OK") }
                    }
                }
            }
        }

        // 1. NETWORK STATUS CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (networkStatus.isConnected) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (networkStatus.isConnected) ProfitEmerald else RiskCoral
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Réseau & Connexion", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = if (networkStatus.isConnected) ProfitEmerald.copy(alpha = 0.15f) else RiskCoral.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                networkStatus.connectionType,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (networkStatus.isConnected) ProfitEmerald else RiskCoral,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text("Dernière synchronisation : ${networkStatus.lastSyncLabel}", style = MaterialTheme.typography.bodySmall)
                    if (networkStatus.pendingActionsCount > 0) {
                        Text("Actions en attente de synchro : ${networkStatus.pendingActionsCount}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }

                    // Mobile data saver bar (50 Mo limit)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Data mobile utilisée aujourd'hui", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${String.format(java.util.Locale.FRANCE, "%.1f", networkStatus.mobileDataUsedMb)} / ${networkStatus.mobileDataLimitMb} Mo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (networkStatus.mobileDataUsedMb / networkStatus.mobileDataLimitMb).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 2. EXPORT / IMPORT BACKUP CARD
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Sauvegarde & Restauration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Export complet (JSON & Texte) de la mémoire, des morceaux du studio et des réglages (sans aucun secret ni clé API).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Chiffrer avec mot de passe", style = MaterialTheme.typography.bodyMedium)
                        }
                        Switch(checked = useEncryption, onCheckedChange = { useEncryption = it })
                    }

                    if (useEncryption) {
                        OutlinedTextField(
                            value = exportPassword,
                            onValueChange = { exportPassword = it },
                            label = { Text("Mot de passe secret d'export") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { performExport(isWyattOnly = false) },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("export_backup_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Exporter Tout")
                        }
                        OutlinedButton(
                            onClick = { importPickerLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f).height(48.dp).testTag("import_backup_btn")
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Importer")
                        }
                    }

                    OutlinedButton(
                        onClick = { performExport(isWyattOnly = true) },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Exporter uniquement le Mode Wyatt")
                    }
                }
            }
        }

        // 3. LOCAL ROTATING BACKUPS (5 copies)
        if (localBackups.isNotEmpty()) {
            item {
                Text("Sauvegardes locales automatiques (5 dernières)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            items(localBackups, key = { it.file.absolutePath }) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${item.dateLabel} • ${item.sizeKb} Ko", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = {
                            pendingImportFileUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", item.file)
                            showImportPasswordDialog = true
                        }) {
                            Icon(Icons.Default.Restore, contentDescription = "Restaurer")
                        }
                        IconButton(onClick = {
                            item.file.delete()
                            localBackups = BackupAndNetworkManager.getLatestLocalBackups(context)
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer")
                        }
                    }
                }
            }
        }
    }

    if (showImportPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showImportPasswordDialog = false },
            title = { Text("Restaurer la sauvegarde") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Si ce fichier a été chiffré, entre le mot de passe secret. Sinon, laisse vide.")
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Mot de passe (optionnel)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val uri = pendingImportFileUri
                    if (uri != null) {
                        performImport(uri, importPassword.trim().takeIf { it.isNotBlank() })
                    }
                    showImportPasswordDialog = false
                    importPassword = ""
                }) {
                    Text("Restaurer")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImportPasswordDialog = false
                    importPassword = ""
                }) {
                    Text("Annuler")
                }
            }
        )
    }
}
