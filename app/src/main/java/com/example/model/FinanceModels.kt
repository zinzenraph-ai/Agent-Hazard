package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val FINANCIAL_DISCLAIMER =
    "⚠️ Ceci n'est pas un conseil financier garanti : tu peux perdre tout ou partie de ton capital."

@Keep
@IgnoreExtraProperties
data class WatchlistAsset(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val name: String = "",
    val assetType: String = "crypto", // "crypto" | "gold" | "stock"
    val apiId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PriceAlert(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val assetName: String = "",
    val apiId: String = "",
    val assetType: String = "crypto",
    val targetPrice: Double = 0.0,
    val above: Boolean = true,
    val triggered: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SimWallet(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val cashEur: Double = 1000.0,
    val initialEur: Double = 1000.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SimPosition(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val assetName: String = "",
    val assetType: String = "crypto",
    val quantity: Double = 0.0,
    val avgBuyPrice: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SimTrade(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "",
    val assetName: String = "",
    val side: String = "buy", // "buy" | "sell"
    val quantity: Double = 0.0,
    val price: Double = 0.0,
    val totalEur: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class JournalEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val asset: String = "",
    val direction: String = "spot", // "long" | "short" | "spot"
    val entryPrice: String = "",
    val exitPrice: String = "",
    val stopLoss: String = "",
    val reason: String = "",
    val emotion: String = "",
    val outcome: String = "",
    val aiFeedback: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BudgetEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val kind: String = "expense", // "income" | "expense"
    val category: String = "",
    val label: String = "",
    val amountEur: Double = 0.0,
    val monthKey: String = "2026-11",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavingsGoal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val targetEur: Double = 100.0,
    val savedEur: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MusicRevenue(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackTitle: String = "",
    val source: String = "streams", // "streams" | "tiktok" | "sales"
    val playsCount: Long = 0L,
    val amountEur: Double = 0.0,
    val periodLabel: String = "Nov 2026",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SaasProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val problem: String = "",
    val audience: String = "",
    val competition: String = "",
    val pricing: String = "",
    val businessPlan: String = "",
    val roadmap: String = "",
    val nextAction: String = "",
    val status: String = "idea", // "idea" | "building" | "launched"
    val actionDone: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class BotStrategy(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val assetSymbol: String = "BTC",
    val apiId: String = "bitcoin",
    val buyDropPct: Double = 5.0,
    val takeProfitPct: Double = 8.0,
    val stopLossPct: Double = 4.0,
    val amountPerTradeEur: Double = 50.0,
    val maxTotalLossEur: Double = 150.0,
    val maxTradesCount: Long = 10L,
    val mode: String = "simulation", // "simulation" | "real"
    val status: String = "idle", // "idle" | "running" | "stopped"
    val tradesExecuted: Long = 0L,
    val winsCount: Long = 0L,
    val lossesCount: Long = 0L,
    val cumulativePnlEur: Double = 0.0,
    val referencePrice: Double = 0.0,
    val lastActionNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MarketQuote(
    val symbol: String,
    val name: String,
    val assetType: String,
    val apiId: String,
    val priceEur: Double,
    val change24hPct: Double,
    val sourceProvider: String,
    val updatedAtMs: Long
)

data class NewsArticle(
    val title: String,
    val description: String,
    val source: String,
    val publishedAt: String,
    val sourceProvider: String
)

data class ServiceConnectionStatus(
    val serviceName: String,
    val envKeyName: String,
    val purpose: String,
    val connected: Boolean,
    val statusText: String
)

data class BotStrategyDraft(
    val name: String,
    val assetSymbol: String,
    val apiId: String,
    val buyDropPct: Double,
    val takeProfitPct: Double,
    val stopLossPct: Double,
    val explanation: String
)
