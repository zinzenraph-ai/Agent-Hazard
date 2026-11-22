package com.example.model

import org.json.JSONArray
import org.json.JSONObject

enum class SongSectionType(val label: String, val sunoTag: String) {
    INTRO("Intro", "[Intro]"),
    COUPLET_1("Couplet 1", "[Verse 1]"),
    REFRAIN("Refrain", "[Chorus]"),
    COUPLET_2("Couplet 2", "[Verse 2]"),
    PONT("Pont", "[Bridge]"),
    OUTRO("Outro", "[Outro]");

    companion object {
        fun fromLabel(value: String): SongSectionType =
            entries.firstOrNull { it.label == value || it.name == value } ?: COUPLET_1
    }
}

data class SongSectionContent(
    val type: SongSectionType,
    val lyrics: String = ""
)

data class SongVersionSnapshot(
    val id: String,
    val label: String,
    val savedAtLabel: String,
    val sections: List<SongSectionContent>
)

data class StudioSong(
    val id: String = "",
    val title: String = "Nouveau morceau",
    val bpm: Int = 92,
    val instruStyle: String = "Boom bap sombre / piano mélancolique",
    val sections: List<SongSectionContent> = defaultSections(),
    val versions: List<SongVersionSnapshot> = emptyList(),
    val updatedAtLabel: String = ""
) {
    fun fullLyrics(): String = sections
        .filter { it.lyrics.isNotBlank() }
        .joinToString("\n\n") { "${it.type.sunoTag}\n${it.lyrics.trim()}" }

    fun toJsonString(): String {
        var keptVersions = versions.take(10)
        while (true) {
            val secArr = JSONArray().apply {
                sections.forEach { s ->
                    put(JSONObject().put("type", s.type.name).put("lyrics", s.lyrics))
                }
            }
            val verArr = JSONArray().apply {
                keptVersions.forEach { v ->
                    val vs = JSONArray().apply {
                        v.sections.forEach { s ->
                            put(JSONObject().put("type", s.type.name).put("lyrics", s.lyrics))
                        }
                    }
                    put(
                        JSONObject()
                            .put("id", v.id)
                            .put("label", v.label)
                            .put("savedAtLabel", v.savedAtLabel)
                            .put("sections", vs)
                    )
                }
            }
            val candidate = JSONObject()
                .put("title", title.take(140))
                .put("bpm", bpm.coerceIn(50, 200))
                .put("instruStyle", instruStyle.take(250))
                .put("updatedAtLabel", updatedAtLabel)
                .put("sections", secArr)
                .put("versions", verArr)
                .toString()
            if (candidate.length <= 14_500 || keptVersions.isEmpty()) {
                return candidate.take(14_900)
            }
            keptVersions = keptVersions.dropLast(1)
        }
    }

    companion object {
        fun defaultSections(): List<SongSectionContent> =
            SongSectionType.entries.map { SongSectionContent(it, "") }

        fun fromJsonOrPlain(creation: CreationItem): StudioSong {
            return try {
                val obj = JSONObject(creation.content)
                val secArr = obj.optJSONArray("sections") ?: JSONArray()
                val parsedSections = SongSectionType.entries.map { type ->
                    var text = ""
                    for (i in 0 until secArr.length()) {
                        val sObj = secArr.optJSONObject(i) ?: continue
                        if (sObj.optString("type") == type.name) {
                            text = sObj.optString("lyrics", "")
                            break
                        }
                    }
                    SongSectionContent(type, text)
                }
                val verArr = obj.optJSONArray("versions") ?: JSONArray()
                val parsedVersions = (0 until verArr.length()).mapNotNull { i ->
                    val vObj = verArr.optJSONObject(i) ?: return@mapNotNull null
                    val vSecs = vObj.optJSONArray("sections") ?: JSONArray()
                    val vList = SongSectionType.entries.map { type ->
                        var text = ""
                        for (j in 0 until vSecs.length()) {
                            val sObj = vSecs.optJSONObject(j) ?: continue
                            if (sObj.optString("type") == type.name) {
                                text = sObj.optString("lyrics", "")
                                break
                            }
                        }
                        SongSectionContent(type, text)
                    }
                    SongVersionSnapshot(
                        id = vObj.optString("id", "v_$i"),
                        label = vObj.optString("label", "Version ${i + 1}"),
                        savedAtLabel = vObj.optString("savedAtLabel", ""),
                        sections = vList
                    )
                }
                StudioSong(
                    id = creation.id,
                    title = obj.optString("title", creation.title).ifBlank { creation.title },
                    bpm = obj.optInt("bpm", 92).coerceIn(50, 200),
                    instruStyle = obj.optString("instruStyle", "Boom bap sombre / piano mélancolique"),
                    sections = parsedSections,
                    versions = parsedVersions,
                    updatedAtLabel = obj.optString("updatedAtLabel", "")
                )
            } catch (_: Exception) {
                StudioSong(
                    id = creation.id,
                    title = creation.title.ifBlank { "Texte importé" },
                    sections = SongSectionType.entries.map { type ->
                        if (type == SongSectionType.COUPLET_1) SongSectionContent(type, creation.content)
                        else SongSectionContent(type, "")
                    }
                )
            }
        }
    }
}

data class PersonalUniverse(
    val themes: String = "",
    val images: String = "",
    val favoriteWords: String = "",
    val emotions: String = "",
    val references: String = "",
    val styleSignature: String = ""
) {
    fun toJsonString(): String = JSONObject()
        .put("themes", themes)
        .put("images", images)
        .put("favoriteWords", favoriteWords)
        .put("emotions", emotions)
        .put("references", references)
        .put("styleSignature", styleSignature)
        .toString()

    fun asSummaryPrompt(): String = buildString {
        if (themes.isNotBlank()) appendLine("- Thèmes récurrents : $themes")
        if (images.isNotBlank()) appendLine("- Images fortes : $images")
        if (favoriteWords.isNotBlank()) appendLine("- Mots / tics favoris : $favoriteWords")
        if (emotions.isNotBlank()) appendLine("- Émotions dominantes : $emotions")
        if (references.isNotBlank()) appendLine("- Références : $references")
        if (styleSignature.isNotBlank()) appendLine("- Signature de style repérée : $styleSignature")
    }.ifBlank { "Univers en cours de construction à partir de tes premiers textes." }

    companion object {
        fun fromJson(raw: String): PersonalUniverse {
            return try {
                val obj = JSONObject(raw)
                PersonalUniverse(
                    themes = obj.optString("themes", ""),
                    images = obj.optString("images", ""),
                    favoriteWords = obj.optString("favoriteWords", ""),
                    emotions = obj.optString("emotions", ""),
                    references = obj.optString("references", ""),
                    styleSignature = obj.optString("styleSignature", "")
                )
            } catch (_: Exception) {
                PersonalUniverse()
            }
        }
    }
}

data class SmartCorrectionResult(
    val correctedText: String,
    val changesSummary: List<String>,
    val styleDoubtsToConfirm: List<String>
)

data class FlowOption(
    val name: String,
    val description: String,
    val placementTip: String
)

data class RhythmBeatPlan(
    val bpm: Int,
    val style: String,
    val activeSteps: Set<Int>,      // 0..15 in 4/4 16th grid
    val accentSteps: Set<Int>,      // strong syllables
    val breathSteps: Set<Int>,      // pauses / breaths
    val stepSyllableHints: List<String>, // 16 labels
    val offbeatAdvice: String,
    val flows: List<FlowOption>
)

data class RapTechniqueSheet(
    val id: String,
    val title: String,
    val tag: String,
    val explanation: String,
    val breakdownSteps: List<String>,
    val exercisePrompt: String
)

data class SunoExportBundle(
    val stylePrompt: String,
    val taggedLyrics: String,
    val tikTokTitle: String,
    val tikTokDescription: String,
    val hashtags: String,
    val releaseSchedule: String,
    val clipIdeas: String
)
