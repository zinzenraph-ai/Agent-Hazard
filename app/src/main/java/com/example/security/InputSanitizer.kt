package com.example.security

data class SanitizedPromptResult(
    val sanitizedText: String,
    val injectionDetected: Boolean,
    val matchedSignal: String? = null
)

object InputSanitizer {

    private val zeroWidthRegex = Regex("[\\u200B-\\u200D\\uFEFF\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]")
    private val multiSpaceRegex = Regex("[ \\t]{2,}")
    private val symbolRegex = Regex("^[A-Z0-9._\\-]{1,20}$")

    private val promptInjectionPatterns: List<Pair<String, Regex>> = listOf(
        "Tentative d'override système" to Regex("(?i)(ignore|oublie|forget|disregard|bypass)\\s+(all\\s+|toutes\\s+les\\s+|les\\s+)?(previous\\s+|précédentes\\s+)?(instructions?|règles|rules|consignes)"),
        "Demande d'extraction du prompt système" to Regex("(?i)(reveal|show|affiche|donne|print|dump)\\s+(your\\s+|ton\\s+|le\\s+)?(system\\s+prompt|instructions?\\s+système|buildconfig|api[_\\s]?key|secret)"),
        "Balise de délimitation système injectée" to Regex("(?i)(<\\|im_start\\|>|<\\|system\\|>|</?system>|\\[SYSTEM\\]|###\\s*SYSTEM)"),
        "Tentative d'exécution de commande shell" to Regex("(?i)(;\\s*rm\\s+-rf|&&\\s*curl\\s+|\\$\\(.*\\)|`.*`|\\bsudo\\s+|\\bchmod\\s+777)")
    )

    fun sanitizeText(raw: String, maxLength: Int = 4000): String {
        val stripped = raw
            .replace(zeroWidthRegex, "")
            .replace("\r\n", "\n")
            .trim()
        val collapsed = multiSpaceRegex.replace(stripped, " ")
        return collapsed.take(maxLength)
    }

    fun sanitizeSingleLine(raw: String, maxLength: Int = 120): String =
        sanitizeText(raw, maxLength).replace("\n", " ").trim()

    fun sanitizeAssetSymbol(raw: String): String {
        val cleaned = raw.uppercase().trim().replace(Regex("[^A-Z0-9._\\-]"), "").take(20)
        return if (symbolRegex.matches(cleaned)) cleaned else ""
    }

    fun sanitizePositiveDouble(raw: String, min: Double = 0.000001, max: Double = 10_000_000.0): Double? {
        val parsed = raw.trim().replace(",", ".").toDoubleOrNull() ?: return null
        if (parsed.isNaN() || parsed.isInfinite()) return null
        if (parsed < min || parsed > max) return null
        return parsed
    }

    fun sanitizeEmail(raw: String): String {
        val cleaned = sanitizeSingleLine(raw, 160).lowercase()
        val emailRegex = Regex("^[a-z0-9._%+\\-]+@[a-z0-9.\\-]+\\.[a-z]{2,}$")
        return if (emailRegex.matches(cleaned)) cleaned else ""
    }

    fun inspectAndSanitizePrompt(rawPrompt: String, maxLength: Int = 4000): SanitizedPromptResult {
        val cleaned = sanitizeText(rawPrompt, maxLength)
        for ((label, regex) in promptInjectionPatterns) {
            if (regex.containsMatchIn(cleaned)) {
                val neutralized = cleaned
                    .replace("<", "‹")
                    .replace(">", "›")
                return SanitizedPromptResult(
                    sanitizedText = neutralized,
                    injectionDetected = true,
                    matchedSignal = label
                )
            }
        }
        return SanitizedPromptResult(
            sanitizedText = cleaned,
            injectionDetected = false,
            matchedSignal = null
        )
    }

    fun wrapUntrustedExternalData(rawData: String, maxLength: Int = 3500): String {
        val cleaned = sanitizeText(rawData, maxLength)
            .replace("<", "‹")
            .replace(">", "›")
        return "<DONNEES_EXTERIEURES_NON_FIABLES>\n$cleaned\n</DONNEES_EXTERIEURES_NON_FIABLES>"
    }
}
