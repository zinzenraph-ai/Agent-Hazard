package com.example.security

import java.util.regex.Pattern

object MemoryPrivacyGuard {

    private fun unicodeRegex(pattern: String): Regex =
        Pattern.compile(pattern, Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CHARACTER_CLASS).toRegex()

    private val sensitivePatterns = listOf(
        unicodeRegex("""\b(mot\s*de\s*passe|mdp|password|passphrase|code\s*pin|pin\s*[:=])\b"""),
        unicodeRegex("""\b(cl[eé]\s*api|api[_\s-]*key|secret[_\s-]*key|private\s*key|seed\s*phrase|phrase\s*secr[eè]te|mnemonic|token\s*[:=])\b"""),
        Regex("""AIza[0-9A-Za-z\-_]{20,}"""),
        Regex("""\b(sk-[A-Za-z0-9\-_]{16,}|gsk_[A-Za-z0-9\-_]{16,}|ghp_[A-Za-z0-9]{20,}|xox[baprs]-[A-Za-z0-9\-]{10,})"""),
        unicodeRegex("""\bBearer\s+[A-Za-z0-9\-._~+/]+=*"""),
        // Credit / debit card sequences (13 to 19 digits separated by spaces/dashes)
        Regex("""(?<!\d)(?:\d[ \-]?){13,19}(?!\d)"""),
        unicodeRegex("""\b(cvv|cvc|cryptogramme|num[eé]ro\s*de\s*carte|carte\s*bancaire|cb\s*[:=])\b"""),
        // IBAN / RIB
        Regex("""(?i)\b[A-Z]{2}\d{2}[A-Z0-9]{11,30}\b"""),
        unicodeRegex("""\b(iban|bic|rib)\s*[:=\-]"""),
        // Identity papers
        unicodeRegex("""\b(carte\s*d['’]?identit[eé]|num[eé]ro\s*cni|\bcni\b|passeport|titre\s*de\s*s[eé]jour|num[eé]ro\s*de\s*s[eé]cu|s[eé]curit[eé]\s*sociale|\bssn\b|permis\s*de\s*conduire)\b""")
    )

    private val wyattPersonalInfoPatterns = listOf(
        unicodeRegex("""\b(adresse|j['’]?habite\s+[àa]|rue\s+|avenue\s+|boulevard\s+|code\s*postal|\b[0-9]{5}\b)\b"""),
        unicodeRegex("""\b([eé]cole|mon\s+[eé]cole|ma[iî]tresse|instit|classe\s*de)\b"""),
        unicodeRegex("""\b(0[67](?:[\s.\-]?\d{2}){4}|t[eé]l[eé]phone)\b"""),
        Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")
    )

    fun containsSensitiveData(text: String): Boolean {
        if (text.isBlank()) return false
        return sensitivePatterns.any { it.containsMatchIn(text) }
    }

    fun containsWyattPersonalInfo(text: String): Boolean {
        if (containsSensitiveData(text)) return true
        return wyattPersonalInfoPatterns.any { it.containsMatchIn(text) }
    }

    fun scrubSensitiveLines(raw: String, wyattMinimal: Boolean = false): String {
        if (raw.isBlank()) return ""
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { line ->
                if (wyattMinimal) containsWyattPersonalInfo(line) else containsSensitiveData(line)
            }
            .joinToString("\n")
    }

    fun normalizeBulletLine(line: String): String {
        return line.trim()
            .removePrefix("- ")
            .removePrefix("• ")
            .removePrefix("* ")
            .trim()
    }

    fun splitLines(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .map { normalizeBulletLine(it) }
            .filter { it.isNotBlank() && it != "[...]" && it != "[rien pour l'instant]" }
            .toList()
    }

    fun mergeLines(
        existing: String,
        addition: String?,
        maxLength: Int,
        prefix: String = "",
        wyattMinimal: Boolean = false
    ): String {
        val currentLines = splitLines(scrubSensitiveLines(existing, wyattMinimal)).toMutableList()
        if (!addition.isNullOrBlank()) {
            val cleanedAdd = scrubSensitiveLines(addition, wyattMinimal)
            val existingKeys = currentLines.map { line ->
                line.removePrefix("[Style] ").removePrefix("[Finance] ").trim().lowercase()
            }.toMutableSet()

            splitLines(cleanedAdd).forEach { rawLine ->
                val cleanBody = rawLine.removePrefix("[Style] ").removePrefix("[Finance] ").trim()
                if (cleanBody.isNotBlank() && existingKeys.add(cleanBody.lowercase())) {
                    val formatted = if (prefix.isNotBlank() && !rawLine.startsWith(prefix)) {
                        "$prefix$cleanBody"
                    } else {
                        rawLine
                    }
                    currentLines.add(formatted)
                }
            }
        }

        val joined = StringBuilder()
        for (line in currentLines) {
            val next = if (joined.isEmpty()) line else "\n$line"
            if (joined.length + next.length <= maxLength) {
                joined.append(next)
            } else {
                break
            }
        }
        return joined.toString()
    }
}
