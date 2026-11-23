package com.example.data

import com.example.BuildConfig
import com.example.model.ServiceConnectionStatus

object SecretsRegistry {

    private fun isRealKey(value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val trimmed = value.trim()
        return !trimmed.startsWith("MY_") && !trimmed.contains("DEFAULT_VALUE")
    }

    internal fun groqKeyOrNull(): String? =
        BuildConfig.GROQ_API_KEY.takeIf { isRealKey(it) }

    internal fun openRouterKeyOrNull(): String? =
        BuildConfig.OPENROUTER_API_KEY.takeIf { isRealKey(it) }

    internal fun coinGeckoKeyOrNull(): String? =
        BuildConfig.COINGECKO_API_KEY.takeIf { isRealKey(it) }

    internal fun alphaVantageKeyOrNull(): String? =
        BuildConfig.ALPHAVANTAGE_API_KEY.takeIf { isRealKey(it) }

    internal fun twelveDataKeyOrNull(): String? =
        BuildConfig.TWELVEDATA_API_KEY.takeIf { isRealKey(it) }

    internal fun gNewsKeyOrNull(): String? =
        BuildConfig.GNEWS_API_KEY.takeIf { isRealKey(it) }

    internal fun hasExchangeTradeKeys(): Boolean =
        isRealKey(BuildConfig.EXCHANGE_API_KEY) && isRealKey(BuildConfig.EXCHANGE_API_SECRET)

    internal fun exchangeApiKeyOrNull(): String? =
        BuildConfig.EXCHANGE_API_KEY.takeIf { isRealKey(it) }

    internal fun exchangeApiSecretOrNull(): String? =
        BuildConfig.EXCHANGE_API_SECRET.takeIf { isRealKey(it) }

    fun connectionStatuses(): List<ServiceConnectionStatus> = listOf(
        ServiceConnectionStatus(
            serviceName = "Gemini (Cerveau de Hazard)",
            envKeyName = "Firebase AI Logic",
            purpose = "IA principale, streaming, JSON structuré (aucune clé API requise)",
            connected = true,
            statusText = "connecté"
        ),
        ServiceConnectionStatus(
            serviceName = "Groq",
            envKeyName = "GROQ_API_KEY",
            purpose = "Réponses ultra-rapides & bascule automatique",
            connected = groqKeyOrNull() != null,
            statusText = if (groqKeyOrNull() != null) "connecté" else "clé manquante"
        ),
        ServiceConnectionStatus(
            serviceName = "OpenRouter",
            envKeyName = "OPENROUTER_API_KEY",
            purpose = "Modèles supplémentaires & secours IA",
            connected = openRouterKeyOrNull() != null,
            statusText = if (openRouterKeyOrNull() != null) "connecté" else "clé manquante"
        ),
        ServiceConnectionStatus(
            serviceName = "CoinGecko",
            envKeyName = "COINGECKO_API_KEY",
            purpose = "Cours crypto & or en direct (mode public actif en secours)",
            connected = coinGeckoKeyOrNull() != null,
            statusText = if (coinGeckoKeyOrNull() != null) "connecté" else "clé manquante"
        ),
        ServiceConnectionStatus(
            serviceName = "Alpha Vantage",
            envKeyName = "ALPHAVANTAGE_API_KEY",
            purpose = "Actions & forex",
            connected = alphaVantageKeyOrNull() != null,
            statusText = if (alphaVantageKeyOrNull() != null) "connecté" else "clé manquante"
        ),
        ServiceConnectionStatus(
            serviceName = "Twelve Data",
            envKeyName = "TWELVEDATA_API_KEY",
            purpose = "Marchés actions & matières premières",
            connected = twelveDataKeyOrNull() != null,
            statusText = if (twelveDataKeyOrNull() != null) "connecté" else "clé manquante"
        ),
        ServiceConnectionStatus(
            serviceName = "GNews",
            envKeyName = "GNEWS_API_KEY",
            purpose = "Actualités économiques (flux RSS public en secours)",
            connected = gNewsKeyOrNull() != null,
            statusText = if (gNewsKeyOrNull() != null) "connecté" else "clé manquante"
        ),
        ServiceConnectionStatus(
            serviceName = "Plateforme Trading (Mode Réel)",
            envKeyName = "EXCHANGE_API_KEY / SECRET",
            purpose = "Droit de trading uniquement, sans retrait (simulation active par défaut)",
            connected = hasExchangeTradeKeys(),
            statusText = if (hasExchangeTradeKeys()) "connecté" else "clé manquante"
        )
    )
}
