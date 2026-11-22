package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SpeakerMode(val wireValue: String, val label: String) {
    UNKNOWN("unknown", "À identifier"),
    RAPHAEL("raphael", "Raphaël"),
    WYATT("wyatt", "Wyatt (7 ans)");

    companion object {
        fun fromWire(value: String?): SpeakerMode =
            entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class MissionTopic(val wireValue: String, val label: String) {
    GENERAL("general", "Général"),
    MUSIC("music", "Musique & Suno"),
    PROMO("promo", "Promo TikTok"),
    ADMIN("admin", "Administratif"),
    PROJECTS("projects", "Projets & Revenus"),
    TRADING("trading", "Trading & Crypto"),
    WYATT_ADVENTURE("wyatt_adventure", "Aventure Wyatt");

    companion object {
        fun fromWire(value: String?): MissionTopic =
            entries.firstOrNull { it.wireValue == value } ?: GENERAL
    }
}

enum class CreationCategory(val wireValue: String, val label: String) {
    MUSIC("music", "Musique"),
    PROMO("promo", "Promo"),
    ADMIN("admin", "Admin"),
    PROJECTS("projects", "Projets"),
    TRADING("trading", "Trading"),
    WYATT("wyatt", "Wyatt");

    companion object {
        fun fromWire(value: String?): CreationCategory =
            entries.firstOrNull { it.wireValue == value } ?: MUSIC
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class MemoryProfile(
    @DocumentId val id: String = "main",
    val userId: String = "",
    val rawText: String = DEFAULT_MEMORY_TEXT,
    val firstName: String = "Raphaël",
    val musicStyle: String = "",
    val favoriteArtists: String = "",
    val wyattLikes: String = "",
    val wyattWantsToLearn: String = "",
    val wyattMemories: String = "",
    val shortTermGoals: String = "",
    val longTermGoals: String = "",
    val decisionsMade: String = "",
    val inProgress: String = "",
    val tradingLevel: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun missingFieldLabels(): List<String> {
        val missing = mutableListOf<String>()
        if (firstName.isBlank()) missing += "Prénom"
        if (musicStyle.isBlank()) missing += "Mon style / mes thèmes musicaux"
        if (favoriteArtists.isBlank()) missing += "Mes artistes préférés"
        if (wyattLikes.isBlank()) missing += "Ce que Wyatt aime"
        if (wyattWantsToLearn.isBlank()) missing += "Ce que Wyatt veut apprendre"
        if (wyattMemories.isBlank()) missing += "Souvenirs de Wyatt avec Hazard"
        if (shortTermGoals.isBlank()) missing += "Objectifs court terme"
        if (longTermGoals.isBlank()) missing += "Objectifs long terme"
        if (tradingLevel.isBlank()) missing += "Niveau & focus Trading / Crypto"
        return missing
    }

    fun formatAsCopyableSheet(dateLabel: String = "22/11/2026"): String {
        fun valueOrPlaceholder(value: String): String =
            value.trim().ifBlank { "[...]" }

        return buildString {
            appendLine("=== FICHE MÉMOIRE DE HAZARD ===")
            appendLine("(Mise à jour : $dateLabel)")
            appendLine()
            appendLine("## MOI")
            appendLine("- Prénom : ${valueOrPlaceholder(firstName)}")
            appendLine("- Francophone, je n'ai qu'un téléphone (pas d'ordinateur)")
            appendLine("- J'aime écrire, et je pense bien écrire")
            appendLine("- Ouvert à créer une appli ou un SaaS un jour")
            appendLine()
            appendLine("## MA MUSIQUE")
            appendLine("- J'écris mes textes de chansons")
            appendLine("- Je crée mes sons avec Suno, j'en suis content")
            appendLine("- Compte TikTok où je publie mes sons (pour le kiff pour l'instant, pas sérieux)")
            appendLine("- Mon style / mes thèmes : ${valueOrPlaceholder(musicStyle)}")
            appendLine("- Mes artistes préférés : ${valueOrPlaceholder(favoriteArtists)}")
            appendLine("- Je veux : apprendre à faire de la musique, promouvoir mes sons, faire des clips, gérer mes réseaux")
            appendLine()
            appendLine("## TRADING & CRYPTO")
            appendLine("- Niveau / focus actuel : ${valueOrPlaceholder(tradingLevel)}")
            appendLine()
            appendLine("## WYATT (mon fils, 7 ans)")
            appendLine("- Ce qu'il aime : ${valueOrPlaceholder(wyattLikes)}")
            appendLine("- Ce qu'il veut apprendre : ${valueOrPlaceholder(wyattWantsToLearn)}")
            appendLine("- Souvenirs de nos moments avec Hazard : ${valueOrPlaceholder(wyattMemories)}")
            appendLine()
            appendLine("## MES OBJECTIFS")
            appendLine("- Court terme : ${valueOrPlaceholder(shortTermGoals)}")
            appendLine("- Long terme : ${valueOrPlaceholder(longTermGoals)}")
            appendLine()
            appendLine("## MES RÈGLES POUR HAZARD")
            appendLine("- Il me dit la vérité, même quand ça pique")
            appendLine("- Il propose, je décide pour les actions importantes")
            appendLine("- Il a le droit de se tromper, on apprend ensemble")
            appendLine()
            appendLine("## DÉCISIONS DÉJÀ PRISES")
            appendLine("- ${decisionsMade.trim().ifBlank { "[rien pour l'instant]" }}")
            appendLine()
            appendLine("## EN COURS")
            append("- ${inProgress.trim().ifBlank { "[rien pour l'instant]" }}")
        }
    }

    fun formatMinimalWyattSheet(dateLabel: String = "22/11/2026"): String {
        fun cleanBlock(value: String): String =
            value.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(" · ")
                .ifBlank { "[...]" }

        return buildString {
            appendLine("=== MÉMOIRE WYATT (MINIMALE) ===")
            appendLine("(Mise à jour : $dateLabel)")
            appendLine("- Prénom : ${firstName.trim().ifBlank { "Wyatt" }}")
            appendLine("- Ce qu'il aime : ${cleanBlock(wyattLikes)}")
            appendLine("- Ce qu'il a appris / explore : ${cleanBlock(wyattWantsToLearn)}")
            append("- Petits souvenirs : ${cleanBlock(wyattMemories)}")
        }
    }

    fun formatGroupedMemoryExport(
        wyattProfile: MemoryProfile,
        dateLabel: String = "22/11/2026"
    ): String {
        val styleLines = decisionsMade.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("[Style] ") }
            .map { it.removePrefix("[Style] ").trim() }
            .toList()
        val financeExtraLines = decisionsMade.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("[Finance] ") }
            .map { it.removePrefix("[Finance] ").trim() }
            .toList()
        val projectDecisions = decisionsMade.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("[Style] ") && !it.startsWith("[Finance] ") }
            .toList()

        fun appendLines(sb: StringBuilder, label: String, raw: String) {
            val items = raw.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.toList()
            if (items.isEmpty()) {
                sb.appendLine("- $label : [...]")
            } else if (items.size == 1) {
                sb.appendLine("- $label : ${items.first()}")
            } else {
                sb.appendLine("- $label :")
                items.forEach { sb.appendLine("  • $it") }
            }
        }

        return buildString {
            appendLine("=== MÉMOIRE DE HAZARD ===")
            appendLine("Export du $dateLabel")
            appendLine()
            appendLine("## 1. MOI")
            appendLine("- Prénom : ${firstName.trim().ifBlank { "[...]" }}")
            if (styleLines.isEmpty()) {
                appendLine("- Goûts & préférences de style : [...]")
            } else {
                appendLine("- Goûts & préférences de style :")
                styleLines.forEach { appendLine("  • $it") }
            }
            appendLine()
            appendLine("## 2. MUSIQUE")
            appendLines(this, "Style & thèmes", musicStyle)
            appendLines(this, "Artistes & références", favoriteArtists)
            appendLine()
            appendLine("## 3. PROJETS")
            appendLines(this, "Projets en cours", inProgress)
            appendLines(this, "Objectifs court terme", shortTermGoals)
            appendLines(this, "Objectifs long terme", longTermGoals)
            if (projectDecisions.isEmpty()) {
                appendLine("- Décisions : [...]")
            } else {
                appendLine("- Décisions :")
                projectDecisions.forEach { appendLine("  • $it") }
            }
            appendLine()
            appendLine("## 4. FINANCE")
            appendLines(this, "Trading & Crypto", tradingLevel)
            financeExtraLines.forEach { appendLine("  • $it") }
            appendLine()
            appendLine("## 5. WYATT (Mémoire séparée minimale)")
            appendLine("- Prénom : ${wyattProfile.firstName.trim().ifBlank { "Wyatt" }}")
            appendLines(this, "Ce qu'il aime", wyattProfile.wyattLikes.ifBlank { wyattLikes })
            appendLines(this, "Ce qu'il a appris / explore", wyattProfile.wyattWantsToLearn.ifBlank { wyattWantsToLearn })
            appendLines(this, "Souvenirs rigolos", wyattProfile.wyattMemories.ifBlank { wyattMemories })
        }.trimEnd()
    }

    companion object {
        const val DEFAULT_MEMORY_TEXT = """=== FICHE MÉMOIRE DE HAZARD ===
(Mise à jour : 22/11/2026)

## MOI
- Prénom : Raphaël
- Francophone, je n'ai qu'un téléphone (pas d'ordinateur)
- J'aime écrire, et je pense bien écrire
- Ouvert à créer une appli ou un SaaS un jour

## MA MUSIQUE
- J'écris mes textes de chansons
- Je crée mes sons avec Suno, j'en suis content
- Compte TikTok où je publie mes sons (pour le kiff pour l'instant, pas sérieux)
- Mon style / mes thèmes : [...]
- Mes artistes préférés : [...]
- Je veux : apprendre à faire de la musique, promouvoir mes sons, faire des clips, gérer mes réseaux

## TRADING & CRYPTO
- Niveau / focus actuel : [...]

## WYATT (mon fils, 7 ans)
- Ce qu'il aime : [...]
- Ce qu'il veut apprendre : [...]
- Souvenirs de nos moments avec Hazard : [...]

## MES OBJECTIFS
- Court terme : [...]
- Long terme : [...]

## MES RÈGLES POUR HAZARD
- Il me dit la vérité, même quand ça pique
- Il propose, je décide pour les actions importantes
- Il a le droit de se tromper, on apprend ensemble

## DÉCISIONS DÉJÀ PRISES
- [rien pour l'instant]

## EN COURS
- [rien pour l'instant]"""
    }
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "hazard",
    val speakerMode: String = SpeakerMode.UNKNOWN.wireValue,
    val topic: String = MissionTopic.GENERAL.wireValue,
    val text: String = "",
    val requiresConfirmation: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val category: String = CreationCategory.MUSIC.wireValue,
    val title: String = "",
    val content: String = "",
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MemoryGoalItem(
    val sourceLabel: String,
    val text: String,
    val topic: MissionTopic,
    val suggestedNextAction: String
)

fun MemoryProfile.extractDashboardGoals(): List<MemoryGoalItem> {
    fun cleanLines(raw: String): List<String> = raw.lineSequence()
        .map { it.trim().removePrefix("- ").removePrefix("• ").removePrefix("* ").trim() }
        .filter {
            it.isNotBlank() &&
                it != "[...]" &&
                it != "[rien pour l'instant]"
        }
        .toList()

    val result = mutableListOf<MemoryGoalItem>()
    val seen = mutableSetOf<String>()

    fun addItems(raw: String, sourceLabel: String, topic: MissionTopic, actionPrefix: String) {
        cleanLines(raw).forEach { line ->
            val cleanText = line.removePrefix("[Finance] ").removePrefix("[Style] ").trim()
            if (cleanText.isNotBlank() && seen.add(cleanText.lowercase())) {
                result.add(
                    MemoryGoalItem(
                        sourceLabel = sourceLabel,
                        text = cleanText,
                        topic = topic,
                        suggestedNextAction = "$actionPrefix : $cleanText"
                    )
                )
            }
        }
    }

    addItems(inProgress, "En cours", MissionTopic.PROJECTS, "Définir et lancer la prochaine étape sur téléphone pour")
    addItems(shortTermGoals, "Court terme", MissionTopic.PROJECTS, "Découper en 1ère action concrète aujourd'hui")
    addItems(longTermGoals, "Long terme", MissionTopic.PROJECTS, "Valider le premier jalon de cette semaine pour")
    addItems(musicStyle, "Musique", MissionTopic.MUSIC, "Écrire un couplet / prompt Suno aligné avec")
    addItems(tradingLevel, "Finance", MissionTopic.TRADING, "Appliquer la checklist risque & simulation pour")
    val financeDecisions = cleanLines(decisionsMade)
        .filter { it.startsWith("[Finance] ") }
        .joinToString("\n")
    addItems(financeDecisions, "Finance", MissionTopic.TRADING, "Mettre en place l'étape financière")

    return result
}

data class MemoryPatch(
    val firstName: String? = null,
    val stylePreferences: String? = null,
    val musicStyle: String? = null,
    val favoriteArtists: String? = null,
    val wyattLikes: String? = null,
    val wyattWantsToLearn: String? = null,
    val wyattMemories: String? = null,
    val shortTermGoals: String? = null,
    val longTermGoals: String? = null,
    val decisionsMade: String? = null,
    val inProgress: String? = null,
    val tradingLevel: String? = null,
    val financeNotes: String? = null
) {
    fun hasAnyChange(): Boolean = listOf(
        firstName, stylePreferences, musicStyle, favoriteArtists, wyattLikes, wyattWantsToLearn,
        wyattMemories, shortTermGoals, longTermGoals, decisionsMade, inProgress, tradingLevel, financeNotes
    ).any { !it.isNullOrBlank() }
}

data class TradingConcept(
    val id: String,
    val title: String,
    val tag: String,
    val summary: String,
    val proRules: List<String>,
    val commonTraps: List<String>,
    val hazardCoachPrompt: String
)
