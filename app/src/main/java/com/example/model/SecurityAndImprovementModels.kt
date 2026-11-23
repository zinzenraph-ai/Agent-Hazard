package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class OwnerAccessPolicy(
    @DocumentId val id: String = "owner_policy",
    val ownerUid: String = "",
    val ownerEmail: String = "",
    val wyattEmail: String = "",
    val allowedEmails: List<String> = emptyList(),
    val lastLoginEmail: String = "",
    val lastLoginDevice: String = "",
    @ServerTimestamp val lastLoginAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SecurityEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val eventType: String = "info", // "login" | "anomaly" | "protection"
    val severity: String = "info", // "info" | "warning" | "critical"
    val title: String = "",
    val detail: String = "",
    val deviceLabel: String = "",
    val resolved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ImprovementProposal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = "security", // "bug" | "perf" | "security" | "simplify" | "feature" | "ecosystem"
    val priority: String = "haute", // "haute" | "moyenne"
    val whatToChange: String = "",
    val why: String = "",
    val impact: String = "",
    val risks: String = "",
    val rollbackHowTo: String = "",
    val preparedPlan: String = "",
    val weekKey: String = "",
    val status: String = "pending", // "pending" | "applied" | "refused" | "postponed" | "rolled_back"
    val appliedVersionId: String = "",
    val beforePreview: String = "",
    val afterPreview: String = "",
    val supports24hTrial: Boolean = false,
    val trialAccentStyle: String = "",
    val trialThemeMode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppVersionSnapshot(
    @DocumentId val id: String = "",
    val userId: String = "",
    val versionTag: String = "v1.0.0",
    val summary: String = "",
    val appliedProposalIds: List<String> = emptyList(),
    val activeNotes: String = "",
    val current: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UiPreferences(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val themeMode: String = "dark", // "dark" | "light"
    val accentStyle: String = "hazard", // "hazard" | "emerald" | "amber" | "violet" | "ocean"
    val textScale: Double = 1.0, // 0.9 | 1.0 | 1.15 | 1.3
    val moduleOrder: List<String> = listOf(
        "musique", "finance", "plans", "actualites", "wyatt", "securite", "propositions"
    ),
    val hiddenModules: List<String> = emptyList(),
    val enabledGadgets: List<String> = listOf("crypto", "news", "goal", "track"),
    val trialLabel: String = "",
    val trialExpiresAtMs: Long = 0L,
    val previousThemeSnapshot: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class ProtectionCheckItem(
    val id: String,
    val title: String,
    val description: String,
    val active: Boolean,
    val statusLabel: String
)

sealed interface AccountAccessState {
    data object Checking : AccountAccessState
    data class Authorized(
        val uid: String,
        val email: String,
        val isOwner: Boolean,
        val lockedWyattMode: Boolean
    ) : AccountAccessState
    data class Denied(val email: String, val reason: String) : AccountAccessState
}
