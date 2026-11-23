package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.example.model.CreationItem
import com.example.model.MemoryProfile
import com.example.model.UiPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

data class NetworkStatusInfo(
    val isConnected: Boolean,
    val connectionType: String, // "Wi-Fi / Starlink", "4G / 5G", "Hors-ligne"
    val isCellularRestricted: Boolean,
    val mobileDataUsedMb: Double,
    val mobileDataLimitMb: Double = 50.0,
    val pendingActionsCount: Int = 0,
    val lastSyncLabel: String = "Jamais"
)

data class BackupLocalFile(
    val file: File,
    val name: String,
    val dateLabel: String,
    val sizeKb: Long
)

object BackupAndNetworkManager {
    private const val TAG = "BackupAndNetworkManager"
    private const val DAILY_MOBILE_LIMIT_MB = 50.0

    private val _networkStatus = MutableStateFlow(
        NetworkStatusInfo(
            isConnected = true,
            connectionType = "Wi-Fi / Starlink",
            isCellularRestricted = false,
            mobileDataUsedMb = 0.0,
            mobileDataLimitMb = DAILY_MOBILE_LIMIT_MB,
            pendingActionsCount = 0,
            lastSyncLabel = "À l'instant"
        )
    )
    val networkStatus: StateFlow<NetworkStatusInfo> = _networkStatus.asStateFlow()

    private val pendingOfflineQueue = mutableListOf<String>()

    fun initNetworkMonitor(context: Context) {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
                    val type = if (isCellular) "4G / 5G" else "Wi-Fi / Starlink"
                    val current = _networkStatus.value
                    val restricted = isCellular && current.mobileDataUsedMb >= current.mobileDataLimitMb
                    _networkStatus.value = current.copy(
                        isConnected = true,
                        connectionType = type,
                        isCellularRestricted = restricted,
                        lastSyncLabel = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(Date())
                    )
                }

                override fun onLost(network: Network) {
                    _networkStatus.value = _networkStatus.value.copy(
                        isConnected = false,
                        connectionType = "Hors-ligne"
                    )
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "Network monitor init failed: ${e.message}")
        }
    }

    fun recordCellularUsage(bytes: Long) {
        val mb = bytes.toDouble() / (1024 * 1024)
        val current = _networkStatus.value
        val newTotal = current.mobileDataUsedMb + mb
        val restricted = current.connectionType == "4G / 5G" && newTotal >= current.mobileDataLimitMb
        _networkStatus.value = current.copy(
            mobileDataUsedMb = newTotal,
            isCellularRestricted = restricted
        )
    }

    fun queueOfflineAction(actionDesc: String) {
        pendingOfflineQueue.add(actionDesc)
        _networkStatus.value = _networkStatus.value.copy(pendingActionsCount = pendingOfflineQueue.size)
    }

    fun flushOfflineQueue(): List<String> {
        val list = pendingOfflineQueue.toList()
        pendingOfflineQueue.clear()
        _networkStatus.value = _networkStatus.value.copy(pendingActionsCount = 0)
        return list
    }

    // --- BACKUP & EXPORT / IMPORT ---

    fun createExportJsonBundle(
        memory: MemoryProfile?,
        wyattMemory: MemoryProfile?,
        creations: List<CreationItem>,
        uiPrefs: UiPreferences?,
        password: String? = null
    ): String {
        val root = JSONObject()
        root.put("app", "Hazard AI Assistant")
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.FRANCE).format(Date()))

        // Memory (Zero secrets or API keys)
        if (memory != null) {
            val memObj = JSONObject()
                .put("firstName", memory.firstName)
                .put("musicStyle", memory.musicStyle)
                .put("favoriteArtists", memory.favoriteArtists)
                .put("shortTermGoals", memory.shortTermGoals)
                .put("longTermGoals", memory.longTermGoals)
                .put("decisionsMade", memory.decisionsMade)
                .put("inProgress", memory.inProgress)
                .put("tradingLevel", memory.tradingLevel)
            root.put("memory", memObj)
        }

        // Wyatt Memory (Separated)
        if (wyattMemory != null) {
            val wyObj = JSONObject()
                .put("firstName", "Wyatt")
                .put("wyattLikes", wyattMemory.wyattLikes)
                .put("wyattWantsToLearn", wyattMemory.wyattWantsToLearn)
                .put("wyattMemories", wyattMemory.wyattMemories)
            root.put("wyattMemory", wyObj)
        }

        // Creations / Studio songs / Notes (Zero secrets)
        val creationsArr = JSONArray()
        creations.forEach { c ->
            val cObj = JSONObject()
                .put("category", c.category)
                .put("title", c.title)
                .put("content", c.content)
                .put("tags", JSONArray(c.tags))
                .put("pinned", c.pinned)
            creationsArr.put(cObj)
        }
        root.put("creations", creationsArr)

        // Settings / UI Prefs
        if (uiPrefs != null) {
            val prefsObj = JSONObject()
                .put("themeMode", uiPrefs.themeMode)
                .put("accentStyle", uiPrefs.accentStyle)
                .put("textScale", uiPrefs.textScale)
            root.put("uiPreferences", prefsObj)
        }

        val plainJson = root.toString(2)
        return if (!password.isNullOrBlank()) {
            encryptAes(plainJson, password)
        } else {
            plainJson
        }
    }

    fun createExportTextSummary(
        memory: MemoryProfile?,
        wyattMemory: MemoryProfile?,
        creations: List<CreationItem>
    ): String {
        return buildString {
            appendLine("=== SAUVEGARDE HAZARD ===")
            appendLine("Date : ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date())}")
            appendLine()
            if (memory != null) {
                appendLine("--- Fiche Mémoire Raphaël ---")
                appendLine(memory.formatAsCopyableSheet())
                appendLine()
            }
            if (wyattMemory != null) {
                appendLine("--- Fiche Mémoire Wyatt ---")
                appendLine(wyattMemory.formatMinimalWyattSheet())
                appendLine()
            }
            appendLine("--- Créations & Morceaux Studio (${creations.size}) ---")
            creations.forEach { c ->
                appendLine("[${c.category}] ${c.title}")
                appendLine(c.content.take(300))
                appendLine("---")
            }
        }
    }

    fun saveLocalBackupFile(context: Context, content: String, isEncrypted: Boolean): File {
        val dir = File(context.filesDir, "hazard_backups")
        if (!dir.exists()) dir.mkdirs()

        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.FRANCE).format(Date())
        val ext = if (isEncrypted) "enc.json" else "json"
        val file = File(dir, "backup_$stamp.$ext")
        file.writeText(content)

        // Enforce rotating max 5 copies
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()
        if (files.size > 5) {
            files.drop(5).forEach { it.delete() }
        }
        return file
    }

    fun getLatestLocalBackups(context: Context): List<BackupLocalFile> {
        val dir = File(context.filesDir, "hazard_backups")
        if (!dir.exists()) return emptyList()
        return dir.listFiles()?.sortedByDescending { it.lastModified() }?.map { f ->
            val dateLabel = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE).format(Date(f.lastModified()))
            BackupLocalFile(
                file = f,
                name = f.name,
                dateLabel = dateLabel,
                sizeKb = f.length() / 1024
            )
        }.orEmpty()
    }

    // --- AES ENCRYPTION / DECRYPTION ---

    private fun encryptAes(plainText: String, password: String): String {
        try {
            val salt = "HazardSecureSalt2026".toByteArray(Charsets.UTF_8)
            val keySpec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val tmp = factory.generateSecret(keySpec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            val base64 = Base64.encodeToString(combined, Base64.NO_WRAP)
            val wrapper = JSONObject()
                .put("encrypted", true)
                .put("payload", base64)
            return wrapper.toString()
        } catch (e: Exception) {
            Log.e(TAG, "AES encryption failed: ${e.message}")
            return plainText
        }
    }

    fun decryptAesIfEncrypted(rawContent: String, password: String?): String {
        try {
            val json = JSONObject(rawContent)
            if (!json.optBoolean("encrypted", false)) return rawContent
            if (password.isNullOrBlank()) {
                throw IllegalArgumentException("Ce fichier est chiffré. Entre ton mot de passe.")
            }
            val base64 = json.getString("payload")
            val combined = Base64.decode(base64, Base64.NO_WRAP)

            val salt = "HazardSecureSalt2026".toByteArray(Charsets.UTF_8)
            val keySpec = PBEKeySpec(password.toCharArray(), salt, 65536, 256)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val tmp = factory.generateSecret(keySpec)
            val secretKey = SecretKeySpec(tmp.encoded, "AES")

            val iv = ByteArray(16)
            System.arraycopy(combined, 0, iv, 0, 16)
            val encryptedBytes = ByteArray(combined.size - 16)
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(encryptedBytes)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "AES decryption failed: ${e.message}")
            throw IllegalArgumentException("Motif erroné ou fichier corrompu.")
        }
    }
}
