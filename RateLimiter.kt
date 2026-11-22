package com.example.security

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

data class TelemetrySnapshot(
    val aiCallsCount: Int,
    val marketCallsCount: Int,
    val avgAiLatencyMs: Long,
    val errorCount: Int,
    val rateLimitHits: Int,
    val injectionBlocksCount: Int,
    val topUsedFeatures: String
)

object RateLimiter {
    private val windows = ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>>()

    // Per-minute safety ceilings to prevent abuse and API quota/billing spikes
    val bucketLimitsPerMinute: Map<String, Int> = mapOf(
        "ai_chat" to 12,
        "ai_analysis" to 8,
        "market_quotes" to 20,
        "news_feed" to 6,
        "improvement_scan" to 3
    )

    @Synchronized
    fun tryAcquire(bucket: String): Boolean {
        val limit = bucketLimitsPerMinute[bucket] ?: 15
        val now = System.currentTimeMillis()
        val cutoff = now - 60_000L
        val deque = windows.getOrPut(bucket) { ConcurrentLinkedDeque() }
        while (true) {
            val head = deque.peekFirst() ?: break
            if (head < cutoff) deque.pollFirst() else break
        }
        if (deque.size >= limit) {
            AppTelemetryMonitor.recordRateLimitHit(bucket)
            return false
        }
        deque.addLast(now)
        return true
    }

    fun remainingThisMinute(bucket: String): Int {
        val limit = bucketLimitsPerMinute[bucket] ?: 15
        val cutoff = System.currentTimeMillis() - 60_000L
        val deque = windows[bucket] ?: return limit
        val active = deque.count { it >= cutoff }
        return (limit - active).coerceAtLeast(0)
    }
}

object AppTelemetryMonitor {
    private val aiCalls = AtomicInteger(0)
    private val marketCalls = AtomicInteger(0)
    private val totalAiLatencyMs = AtomicLong(0L)
    private val errors = AtomicInteger(0)
    private val rateLimitHits = AtomicInteger(0)
    private val injectionBlocks = AtomicInteger(0)
    private val featureCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val recentErrors = ConcurrentLinkedDeque<String>()

    var onAnomalyCallback: ((code: String, title: String, detail: String, severity: String) -> Unit)? = null

    fun recordFeatureUse(featureName: String) {
        featureCounts.getOrPut(featureName) { AtomicInteger(0) }.incrementAndGet()
    }

    fun recordAiCall(latencyMs: Long) {
        aiCalls.incrementAndGet()
        totalAiLatencyMs.addAndGet(latencyMs.coerceAtLeast(0L))
    }

    fun recordMarketCall() {
        marketCalls.incrementAndGet()
    }

    fun recordError(source: String, message: String) {
        errors.incrementAndGet()
        val safeMsg = InputSanitizer.sanitizeSingleLine("$source: $message", 160)
        recentErrors.addFirst(safeMsg)
        while (recentErrors.size > 10) recentErrors.pollLast()
    }

    fun recordRateLimitHit(bucket: String) {
        rateLimitHits.incrementAndGet()
        onAnomalyCallback?.invoke(
            "rate_limit",
            "Limitation de requêtes déclenchée ($bucket)",
            "Trop d'appels en moins d'1 minute : blocage protecteur activé pour éviter les abus et dépassements de quota.",
            "warning"
        )
    }

    fun recordPromptInjectionBlocked(signal: String) {
        injectionBlocks.incrementAndGet()
        onAnomalyCallback?.invoke(
            "prompt_injection",
            "Tentative d'injection IA bloquée",
            "Motif suspect neutralisé avant envoi au modèle : $signal.",
            "critical"
        )
    }

    fun recentErrorsList(): List<String> = recentErrors.toList()

    fun snapshot(): TelemetrySnapshot {
        val calls = aiCalls.get()
        val avgMs = if (calls > 0) totalAiLatencyMs.get() / calls else 0L
        val topFeatures = featureCounts.entries
            .sortedByDescending { it.value.get() }
            .take(5)
            .joinToString(", ") { "${it.key} (${it.value.get()})" }
            .ifBlank { "Chat Hazard, Marchés" }
        return TelemetrySnapshot(
            aiCallsCount = calls,
            marketCallsCount = marketCalls.get(),
            avgAiLatencyMs = avgMs,
            errorCount = errors.get(),
            rateLimitHits = rateLimitHits.get(),
            injectionBlocksCount = injectionBlocks.get(),
            topUsedFeatures = topFeatures
        )
    }
}
