package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.model.RapTechniqueSheet
import java.text.Normalizer
import kotlin.math.max

data class LineRhymeInfo(
    val lineIndex: Int,
    val text: String,
    val syllables: Int,
    val rhymeKey: String,
    val schemeLetter: String,
    val familyColor: Color
)

data class LocalRepetitionClicheReport(
    val repeatedWords: List<Pair<String, Int>>,
    val detectedCliches: List<String>
)

object FrenchLyricAnalyzer {

    val rhymePalette = listOf(
        Color(0xFF38BDF8), // Sky blue
        Color(0xFFF59E0B), // Amber gold
        Color(0xFF10B981), // Emerald
        Color(0xFFF43F5E), // Coral rose
        Color(0xFFA855F7), // Violet
        Color(0xFFEC4899), // Pink
        Color(0xFF14B8A6), // Teal
        Color(0xFFFB923C)  // Orange
    )

    private val frenchVowels = setOf(
        'a', 'e', 'i', 'o', 'u', 'y',
        'à', 'â', 'ä', 'é', 'è', 'ê', 'ë',
        'î', 'ï', 'ô', 'ö', 'ù', 'û', 'ü', 'ÿ', 'œ', 'æ'
    )

    private val stopwords = setOf(
        "dans", "pour", "avec", "sans", "sous", "chez", "mais", "donc", "comme", "tout",
        "tous", "toute", "toutes", "plus", "moins", "rien", "faire", "fait", "suis", "sont",
        "etre", "être", "avoir", "elle", "nous", "vous", "leur", "leurs", "cette", "cet",
        "cela", "sais", "peux", "veux", "quand", "alors", "encore", "aussi", "meme", "même",
        "bien", "trop", "juste", "entre", "vers", "tres", "très"
    )

    private val commonRapCliches = listOf(
        "dans le game" to "Évoque plutôt ton terrain concret, une scène vécue ou une image physique.",
        "sur le bitume" to "Précise la matière, l'heure, la lumière ou un détail sensoriel du décor.",
        "coeur de pierre" to "Remplace par une métaphore organique ou un geste froid précis.",
        "cœur de pierre" to "Remplace par une métaphore organique ou un geste froid précis.",
        "pas le time" to "Remplace par la pression réelle du temps (montre, facture, horloge).",
        "au sommet" to "Décris la hauteur, le vertige ou ce qu'on voit depuis là-haut.",
        "le ciel pleure" to "Montre l'effet concret de la pluie ou du temps sur la rue ou les visages.",
        "mon vecu" to "Raconte un fait précis de ce vécu plutôt que de le nommer.",
        "mon vécu" to "Raconte un fait précis de ce vécu plutôt que de le nommer.",
        "des hauts et des bas" to "Illustre l'écart par un contraste net entre deux scènes.",
        "seul contre tous" to "Décris l'isolement réel dans une pièce ou face à un choix."
    )

    private val freestyleWordPool = listOf(
        "horloge", "bitume", "cendre", "mirage", "enclume", "horizon", "cicatrice", "néon",
        "boussole", "fissure", "tempête", "silence", "étincelle", "labyrinthe", "ancrage",
        "écho", "ardoise", "marée", "ferraille", "lucidité", "cadence", "brume", "verrou",
        "sommet", "phare", "gravier", "résonance", "pendule", "souterrain", "alliage",
        "trajectoire", "sursaut", "paradoxe", "météore", "balance", "ombre", "reflet", "étau"
    )

    fun randomFreestyleWords(count: Int = 5): List<String> =
        freestyleWordPool.shuffled().take(count.coerceIn(3, 8))

    fun countWordSyllables(rawWord: String): Int {
        val word = rawWord.lowercase()
            .replace("qu'", "")
            .replace("l'", "")
            .replace("d'", "")
            .replace("n'", "")
            .replace("j'", "")
            .replace("m'", "")
            .replace("t'", "")
            .replace("s'", "")
            .replace("c'", "")
            .filter { it.isLetter() }
        if (word.isBlank()) return 0

        var groups = 0
        var prevVowel = false
        var i = 0
        while (i < word.length) {
            val c = word[i]
            val isV = c in frenchVowels
            if (isV) {
                // Handle "qu" / "gu" before vowel where u is silent
                val silentUAfterQG = c == 'u' && i > 0 && (word[i - 1] == 'q' || word[i - 1] == 'g') &&
                    i + 1 < word.length && word[i + 1] in frenchVowels
                if (!silentUAfterQG && !prevVowel) {
                    groups++
                }
                prevVowel = !silentUAfterQG
            } else {
                prevVowel = false
            }
            i++
        }

        // Mute final e / es / ent in spoken French rap (unless short monosyllable like "le", "me", "te", "je")
        val endsWithMuteE = groups > 1 && (
            (word.endsWith("e") && !word.endsWith("é") && !word.endsWith("ee")) ||
                (word.endsWith("es") && !word.endsWith("és")) ||
                (word.endsWith("ent") && word.length > 4 && !word.endsWith("ment"))
            )
        if (endsWithMuteE) {
            groups = max(1, groups - 1)
        }
        return max(1, groups)
    }

    fun countFrenchSyllables(line: String): Int {
        val trimmed = line.trim()
        if (trimmed.isBlank() || trimmed.startsWith("[")) return 0
        val tokens = trimmed.split(Regex("""[\s,.;:!?()"«»\-]+""")).filter { it.isNotBlank() }
        return tokens.sumOf { countWordSyllables(it) }
    }

    fun extractPhoneticRhymeKey(line: String): String {
        val cleanLine = line.trim()
            .lowercase()
            .replace(Regex("""[^a-zàâäéèêëîïôöùûüÿœç'\s]"""), " ")
            .trim()
        if (cleanLine.isBlank() || cleanLine.startsWith("[")) return ""

        val lastWord = cleanLine.split(Regex("""\s+"""))
            .lastOrNull { it.isNotBlank() }
            ?.substringAfterLast('\'')
            ?: return ""
        if (lastWord.length < 2) return lastWord

        val normalized = normalizeFrenchPhonetics(lastWord)
        return normalized.takeLast(3).ifBlank { normalized }
    }

    private fun normalizeFrenchPhonetics(word: String): String {
        var w = word.lowercase()
        // Common French ending equivalences for rap rhyme grouping
        w = w.replace("eaux", "o")
            .replace("eau", "o")
            .replace("aux", "o")
            .replace("au", "o")
            .replace("ots", "o")
            .replace("ot", "o")
            .replace("os", "o")
            .replace("aient", "E")
            .replace("ais", "E")
            .replace("ait", "E")
            .replace("ai", "E")
            .replace("ées", "E")
            .replace("ée", "E")
            .replace("er", "E")
            .replace("ez", "E")
            .replace("et", "E")
            .replace("é", "E")
            .replace("è", "E")
            .replace("ê", "E")
            .replace("ain", "IN")
            .replace("ein", "IN")
            .replace("ien", "IEN")
            .replace("in", "IN")
            .replace("un", "IN")
            .replace("ant", "AN")
            .replace("ent", "AN")
            .replace("ans", "AN")
            .replace("an", "AN")
            .replace("en", "AN")
            .replace("ons", "ON")
            .replace("ont", "ON")
            .replace("on", "ON")
            .replace("om", "ON")
            .replace("ois", "WA")
            .replace("oit", "WA")
            .replace("oi", "WA")
            .replace("oux", "OU")
            .replace("out", "OU")
            .replace("ous", "OU")
            .replace("ou", "OU")
            .replace("eux", "EU")
            .replace("eu", "EU")
            .replace("œu", "EU")
            .replace("ie", "I")
            .replace("is", "I")
            .replace("it", "I")
            .replace("î", "I")
            .replace("ï", "I")
            .replace("us", "U")
            .replace("ut", "U")
            .replace("ue", "U")
            .replace("û", "U")
            .replace("as", "A")
            .replace("at", "A")
            .replace("à", "A")
            .replace("â", "A")

        return Normalizer.normalize(w, Normalizer.Form.NFD)
            .replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
    }

    fun analyzeLinesForDisplay(lyrics: String): List<LineRhymeInfo> {
        val rawLines = lyrics.lines()
        val keyToIndex = mutableMapOf<String, Int>()
        var nextIndex = 0

        return rawLines.mapIndexed { idx, line ->
            val trimmed = line.trim()
            if (trimmed.isBlank() || trimmed.startsWith("[")) {
                LineRhymeInfo(
                    lineIndex = idx,
                    text = line,
                    syllables = 0,
                    rhymeKey = "",
                    schemeLetter = "",
                    familyColor = Color.Transparent
                )
            } else {
                val syllables = countFrenchSyllables(trimmed)
                val key = extractPhoneticRhymeKey(trimmed)
                val familyIdx = if (key.isBlank()) -1 else keyToIndex.getOrPut(key) { nextIndex++ }
                val letter = if (familyIdx >= 0) ('A' + (familyIdx % 26)).toString() else ""
                val color = if (familyIdx >= 0) rhymePalette[familyIdx % rhymePalette.size] else Color.Transparent
                LineRhymeInfo(
                    lineIndex = idx,
                    text = line,
                    syllables = syllables,
                    rhymeKey = key,
                    schemeLetter = letter,
                    familyColor = color
                )
            }
        }
    }

    fun detectRepetitionsAndClichesLocally(lyrics: String): LocalRepetitionClicheReport {
        val lower = lyrics.lowercase()
        val words = lower.split(Regex("""[^a-zàâäéèêëîïôöùûüÿœç]+"""))
            .filter { it.length >= 4 && it !in stopwords }
        val counts = words.groupingBy { it }.eachCount()
            .filter { it.value >= 3 }
            .entries
            .sortedByDescending { it.value }
            .map { it.key to it.value }

        val cliches = commonRapCliches
            .filter { (phrase, _) -> lower.contains(phrase) }
            .map { (phrase, tip) -> "« $phrase » -> $tip" }

        return LocalRepetitionClicheReport(
            repeatedWords = counts,
            detectedCliches = cliches
        )
    }

    val rapTechniqueLibrary: List<RapTechniqueSheet> = listOf(
        RapTechniqueSheet(
            id = "furax_multi",
            title = "Furax Barbarossa · Densité & Multisyllabiques",
            tag = "FURAX / MULTI",
            explanation = "Son écriture empile 3 à 5 syllabes rimées sur la fin ET à l'intérieur des mesures, avec une syntaxe tendue sans mots de remplissage et des enjambements qui cassent la respiration attendue.",
            breakdownSteps = listOf(
                "1. Pose une image concrète lourde de sens en début de mesure.",
                "2. Construis un bloc phonétique de 3 voyelles identiques (ex: O - A - I).",
                "3. Découpe les mots pour créer une rime 'à cheval' sur deux mots.",
                "4. Enchaîne la phrase sur la mesure suivante (enjambement) pour garder la pression."
            ),
            exercisePrompt = "Prends 4 lignes de ton texte actuel et réécris les fins de mesures avec des rimes de 3 syllabes minimum sans ajouter un seul mot creux."
        ),
        RapTechniqueSheet(
            id = "rimes_internes",
            title = "Rimes internes & Rimes à cheval",
            tag = "ARCHITECTURE",
            explanation = "Placer des rebonds sonores aux temps 2 et 3 en plus du temps 4 donne au couplet un relief percussif immédiat, même à tempo lent.",
            breakdownSteps = listOf(
                "1. Repère le temps 2 (caisse claire) et place un accent qui rime avec le temps 4.",
                "2. Utilise la rime à cheval (2 mots courts qui sonnent comme 1 long mot).",
                "3. Alterne un couplet dense avec une chute courte pour créer du contraste."
            ),
            exercisePrompt = "Choisis une de tes phrases et ajoute une rime interne exacte juste avant la caisse claire."
        ),
        RapTechniqueSheet(
            id = "double_sens",
            title = "Double sens, Homonymes & Chutes",
            tag = "PUNCHLINES",
            explanation = "Une vraie punchline ne dit pas deux fois la même chose : elle prépare le sens A dans les 2 premières mesures et bascule sur le sens B au dernier mot.",
            breakdownSteps = listOf(
                "1. Trouve un mot pivot qui possède deux sens (ex: portée, balance, mesure, titre).",
                "2. Décris une scène du sens #1 sans révéler la chute.",
                "3. Frappe avec le mot pivot en fin de 2e ou 4e mesure."
            ),
            exercisePrompt = "Écris 2 mesures qui préparent un double sens autour d'un mot de ton univers (musique, temps, poids, ombre)."
        ),
        RapTechniqueSheet(
            id = "storytelling",
            title = "Narration & Cinéma en 16 mesures",
            tag = "STORYTELLING",
            explanation = "Montrer plutôt qu'expliquer : l'auditeur croit à ton histoire grâce aux détails minuscules (bruit, odeur, heure, geste) plus qu'aux grands adjectifs.",
            breakdownSteps = listOf(
                "1. Ouvre directement dans l'action (in medias res), sans introduction molle.",
                "2. Remplace 'j'étais triste' par un objet ou une scène visible.",
                "3. Garde une tension ou un retournement pour la 16e mesure."
            ),
            exercisePrompt = "Transforme une phrase trop générale de ton couplet en une scène filmée au téléphone en 10 secondes."
        ),
        RapTechniqueSheet(
            id = "flow_placement",
            title = "Placements décalés & Allitérations",
            tag = "FLOW",
            explanation = "Répéter une consonne dure (T, K, P, R) accélère la sensation de débit; démarrer une phrase une double-croche après le 1er temps crée le groove.",
            breakdownSteps = listOf(
                "1. Laisse un quart de temps vide après le kick du temps 1.",
                "2. Regroupe 3 ou 4 consonnes proches sur l'accélération.",
                "3. Marque une vraie respiration courte avant la rime finale."
            ),
            exercisePrompt = "Teste ton refrain sur la grille 16 cases en démarrant une case plus tard."
        )
    )
}
