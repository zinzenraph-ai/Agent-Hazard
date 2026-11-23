package com.example.data

import android.util.Log
import com.example.model.FlowOption
import com.example.model.PersonalUniverse
import com.example.model.RhythmBeatPlan
import com.example.model.SmartCorrectionResult
import com.example.model.StudioSong
import com.example.model.SunoExportBundle
import com.example.security.AppTelemetryMonitor
import com.example.security.InputSanitizer
import com.example.security.RateLimiter
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MusicStudioAiService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val daPersona = """
        Tu es Hazard, directeur artistique rap français & partenaire d'écriture de Raphaël.
        RÈGLES ABSOLUES :
        1. Tu ne dis JAMAIS "c'est super" ou "c'est génial" par défaut. Tu es honnête, franc, précis et constructif.
        2. Si une rime est faible, un cliché traîne ou un placement boite, tu le dis cash et tu montres comment l'élever.
        3. Tu aides Raphaël à développer SON propre style et son univers (jamais de copier-coller d'un autre rappeur).
        4. Droit d'auteur strict : ne recopie JAMAIS de paroles protégées complètes ou de longs couplets d'artistes existants. Tu analyses les mécaniques techniques uniquement.
    """.trimIndent()

    private fun acquireRateLimitOrThrow() {
        if (!RateLimiter.tryAcquire("ai_analysis")) {
            throw IllegalStateException("Limite de requêtes atteinte : attends quelques secondes.")
        }
    }

    private fun sanitize(input: String, maxLen: Int = 5000): String {
        val inspected = InputSanitizer.inspectAndSanitizePrompt(input, maxLen)
        if (inspected.injectionDetected) {
            AppTelemetryMonitor.recordPromptInjectionBlocked("Injection détectée dans Studio Musique")
        }
        return inspected.sanitizedText
    }

    private suspend fun generateTextWithFailover(prompt: String, useWebSearch: Boolean = false): String {
        val startMs = System.currentTimeMillis()
        try {
            val model = if (useWebSearch) {
                Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    tools = listOf(Tool.googleSearch()),
                    systemInstruction = content { text(daPersona) }
                )
            } else {
                Firebase.ai.generativeModel(
                    modelName = "gemini-3.8-flash",
                    systemInstruction = content { text(daPersona) }
                )
            }
            val resp = withTimeout(45_000L) { model.generateContent(prompt) }
            val text = resp.text?.trim()
            if (!text.isNullOrBlank()) {
                AppTelemetryMonitor.recordAiCall(System.currentTimeMillis() - startMs)
                return text
            }
        } catch (e: CancellationException) {
            if (e !is TimeoutCancellationException) throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini studio text failed (${e.javaClass.simpleName}), trying failover")
        }

        // Failover to Groq / OpenRouter if available
        SecretsRegistry.groqKeyOrNull()?.let { key ->
            callOpenAiCompatible(
                endpoint = "https://api.groq.com/openai/v1/chat/completions",
                apiKey = key,
                model = "llama-3.3-70b-versatile",
                prompt = prompt
            )?.let {
                AppTelemetryMonitor.recordAiCall(System.currentTimeMillis() - startMs)
                return "[Bascule auto · Groq]\n$it"
            }
        }
        SecretsRegistry.openRouterKeyOrNull()?.let { key ->
            callOpenAiCompatible(
                endpoint = "https://openrouter.ai/api/v1/chat/completions",
                apiKey = key,
                model = "google/gemini-2.0-flash-001",
                prompt = prompt
            )?.let {
                AppTelemetryMonitor.recordAiCall(System.currentTimeMillis() - startMs)
                return "[Bascule auto · OpenRouter]\n$it"
            }
        }

        throw IllegalStateException("Service IA indisponible pour le Studio Musique.")
    }

    private suspend fun callOpenAiCompatible(
        endpoint: String,
        apiKey: String,
        model: String,
        prompt: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val msgs = JSONArray()
                .put(JSONObject().put("role", "system").put("content", daPersona))
                .put(JSONObject().put("role", "user").put("content", prompt))
            val bodyJson = JSONObject()
                .put("model", model)
                .put("messages", msgs)
                .put("temperature", 0.7)
                .toString()
            val req = Request.Builder()
                .url(endpoint)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val raw = resp.body?.string() ?: return@withContext null
                JSONObject(raw).optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun rewritePassage(
        passage: String,
        fullLyricsContext: String,
        action: String,
        universe: PersonalUniverse
    ): String {
        acquireRateLimitOrThrow()
        val cleanPassage = sanitize(passage, 1500)
        val cleanCtx = sanitize(fullLyricsContext, 3500)
        return generateTextWithFailover(
            """
            Action demandée sur le passage surligné : "$action"
            (Exemples : reformule, plus percutant, plus sombre, plus imagé, trouve-moi 5 variantes).

            Passage exact sélectionné par Raphaël :
            "$cleanPassage"

            Contexte du morceau :
            $cleanCtx

            Univers de Raphaël :
            ${universe.asSummaryPrompt()}

            Consigne :
            - Respecte sa voix, son rythme et ses rimes.
            - Si "trouve-moi 5 variantes" : numérote 1 à 5, de la plus fidèle à la plus audacieuse.
            - Sinon donne 2 propositions nettes + 1 phrase d'explication sur ce qui change en percussion/image.
            """.trimIndent()
        )
    }

    suspend fun searchRhymes(query: String, rhymeType: String): String {
        acquireRateLimitOrThrow()
        val cleanQuery = sanitize(query, 200)
        return generateTextWithFailover(
            """
            Dictionnaire de rimes rap français avancé.
            Mot ou groupe de mots cible : "$cleanQuery"
            Catégorie demandée : "$rhymeType"
            (Rimes riches / Assonances / Multisyllabiques 2, 3, 4+ syllabes / Rimes internes / Rimes à cheval sur plusieurs mots).

            Donne une liste organisée et directement utilisable en écriture :
            - Pour les multisyllabiques, découpe les sons voyelles (ex: [É - A - ON]).
            - Pour les rimes à cheval, montre des combinaisons de 2 ou 3 mots qui collent phonétiquement.
            - Ajoute 3 exemples d'amorces courtes (1 mesure max) pour montrer le placement, sans clichés.
            """.trimIndent()
        )
    }

    suspend fun generateLexicalField(themeOrEmotion: String): String {
        acquireRateLimitOrThrow()
        val cleanTheme = sanitize(themeOrEmotion, 300)
        return generateTextWithFailover(
            """
            Génère un réseau d'images et de mots rares/évocateurs autour de : "$cleanTheme"
            Structure en 4 blocs courts pour un auteur de rap :
            1. Champ lexical sensoriel (bruits, matières, lumières, gestes — zéro mot banal).
            2. Réseau d'images & métaphores originales (5 images visuelles fortes).
            3. Mots à double sens & homonymes exploitables pour punchlines (avec explication du basculement).
            4. 4 jeux de sons / allitérations prêtes à tisser.
            """.trimIndent()
        )
    }

    suspend fun auditClichesAndRepetitions(lyrics: String): String {
        acquireRateLimitOrThrow()
        val cleanLyrics = sanitize(lyrics, 4500)
        val local = FrenchLyricAnalyzer.detectRepetitionsAndClichesLocally(cleanLyrics)
        val localNotes = buildString {
            if (local.repeatedWords.isNotEmpty()) {
                appendLine("Répétitions détectées localement : " + local.repeatedWords.joinToString { "${it.first} (${it.second}x)" })
            }
            if (local.detectedCliches.isNotEmpty()) {
                appendLine("Clichés repérés :\n" + local.detectedCliches.joinToString("\n"))
            }
        }
        return generateTextWithFailover(
            """
            Passe ce texte au détecteur anti-clichés et anti-répétitions (sans complaisance) :
            $cleanLyrics

            Pré-analyse locale :
            $localNotes

            Donne :
            1) Les mots béquilles / répétitions qui affaiblissent le texte.
            2) Les formules vues et revues (clichés) et pourquoi elles manquent de relief.
            3) Pour chaque faiblesse, 2 alternatives concrètes plus originales et imagées.
            """.trimIndent()
        )
    }

    suspend fun smartCorrectLyrics(lyrics: String): SmartCorrectionResult {
        acquireRateLimitOrThrow()
        val cleanLyrics = sanitize(lyrics, 4500)
        val schema = Schema.obj(
            properties = mapOf(
                "correctedText" to Schema.string(),
                "changesSummary" to Schema.array(Schema.string()),
                "styleDoubtsToConfirm" to Schema.array(Schema.string())
            )
        )
        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(daPersona) },
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                temperature = 0.15f
            }
        )
        val prompt = """
            Correction intelligente d'un texte de rap/chanson en français.
            Texte original :
            $cleanLyrics

            RÈGLES STRICTES :
            - Corrige les vraies fautes d'orthographe, d'accords évidents, de grammaire involontaire et de ponctuation utile.
            - CONSERVE INTACTS l'argot, le verlan, les élisions orales ("j'sais", "p'tit", "t'as", "m'en", "qu'un"), les anglicismes voulus et les orthographes volontairement phonétiques servant la rime ou le débit.
            - EN CAS DE DOUTE sur un mot (ex: jeu de mots, néologisme, licence poétique ou faute réelle), NE LE MODIFIE PAS dans correctedText : pose la question dans styleDoubtsToConfirm !
            - Liste les corrections appliquées dans changesSummary.
        """.trimIndent()

        val raw = withTimeout(45_000L) { model.generateContent(prompt).text }
            ?: throw IllegalStateException("Correction indisponible.")
        val json = JSONObject(raw)
        val changes = json.optJSONArray("changesSummary")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        }.orEmpty()
        val doubts = json.optJSONArray("styleDoubtsToConfirm")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
        }.orEmpty()

        return SmartCorrectionResult(
            correctedText = json.optString("correctedText", cleanLyrics),
            changesSummary = changes,
            styleDoubtsToConfirm = doubts
        )
    }

    suspend fun generateRhythmAndFlows(
        bpm: Int,
        instruStyle: String,
        sampleLyrics: String
    ): RhythmBeatPlan {
        acquireRateLimitOrThrow()
        val cleanStyle = sanitize(instruStyle, 250)
        val cleanSample = sanitize(sampleLyrics, 1500)

        val flowSchema = Schema.obj(
            properties = mapOf(
                "name" to Schema.string(),
                "description" to Schema.string(),
                "placementTip" to Schema.string()
            )
        )
        val schema = Schema.obj(
            properties = mapOf(
                "activeSteps" to Schema.array(Schema.integer()),
                "accentSteps" to Schema.array(Schema.integer()),
                "breathSteps" to Schema.array(Schema.integer()),
                "stepSyllableHints" to Schema.array(Schema.string()),
                "offbeatAdvice" to Schema.string(),
                "flows" to Schema.array(flowSchema)
            )
        )
        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(daPersona) },
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                temperature = 0.5f
            }
        )
        val prompt = """
            Conçois un plan rythmique de 4 temps divisé en 16 cases (indices 0 à 15 : Temps 1 = 0..3, Temps 2 = 4..7, Temps 3 = 8..11, Temps 4 = 12..15).
            Tempo : $bpm BPM | Style d'instru : $cleanStyle
            Extrait du texte de Raphaël :
            ${cleanSample.ifBlank { "(Pas encore de texte : propose un schéma type sur 16 cases)" }}

            Remplis :
            - activeSteps : indices (0..15) où placer les syllabes.
            - accentSteps : indices (0..15) des accents toniques / kicks de voix.
            - breathSteps : indices (0..15) pour respirer ou laisser un silence percutant.
            - stepSyllableHints : tableau de EXACTEMENT 16 chaînes courtes (syllabe ou "·" pour vide ou "RESP" pour respiration) adaptées à son texte.
            - offbeatAdvice : explication précise des placements décalés et appuis.
            - flows : EXACTEMENT 4 flows différents avec name = "Posé", "Rapide", "Syncopé", "En rafale", avec description et placementTip adaptés au texte réel.
        """.trimIndent()

        val raw = withTimeout(45_000L) { model.generateContent(prompt).text }
            ?: throw IllegalStateException("Impossible de générer le schéma rythmique.")
        val json = JSONObject(raw)

        fun intSet(key: String, defaultSet: Set<Int>): Set<Int> {
            val arr = json.optJSONArray(key) ?: return defaultSet
            return (0 until arr.length()).map { arr.optInt(it, -1) }.filter { it in 0..15 }.toSet()
                .ifEmpty { defaultSet }
        }

        val hintsArr = json.optJSONArray("stepSyllableHints")
        val hints = (0 until 16).map { idx ->
            hintsArr?.optString(idx, "·")?.take(10)?.ifBlank { "·" } ?: "·"
        }

        val flowsArr = json.optJSONArray("flows")
        val flows = if (flowsArr != null) {
            (0 until flowsArr.length()).mapNotNull { i ->
                val f = flowsArr.optJSONObject(i) ?: return@mapNotNull null
                FlowOption(
                    name = f.optString("name", "Flow"),
                    description = f.optString("description", ""),
                    placementTip = f.optString("placementTip", "")
                )
            }
        } else emptyList()

        return RhythmBeatPlan(
            bpm = bpm,
            style = cleanStyle,
            activeSteps = intSet("activeSteps", setOf(0, 2, 4, 6, 8, 9, 12, 14)),
            accentSteps = intSet("accentSteps", setOf(0, 4, 8, 12)),
            breathSteps = intSet("breathSteps", setOf(15)),
            stepSyllableHints = hints,
            offbeatAdvice = json.optString("offbeatAdvice", ""),
            flows = flows
        )
    }

    suspend fun enrichPersonalUniverse(
        current: PersonalUniverse,
        songs: List<StudioSong>
    ): PersonalUniverse {
        acquireRateLimitOrThrow()
        val corpus = songs.take(8).joinToString("\n---\n") { song ->
            "Titre: ${song.title}\n${song.fullLyrics().take(1200)}"
        }.take(6000)

        val schema = Schema.obj(
            properties = mapOf(
                "themes" to Schema.string(),
                "images" to Schema.string(),
                "favoriteWords" to Schema.string(),
                "emotions" to Schema.string(),
                "references" to Schema.string(),
                "styleSignature" to Schema.string()
            )
        )
        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(daPersona) },
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                temperature = 0.3f
            }
        )
        val prompt = """
            Analyse les textes réels de Raphaël pour nourrir sa page "Mon Univers" et l'aider à affirmer SA propre identité artistique (jamais pour imiter un autre artiste).

            Univers déjà saisi :
            ${current.asSummaryPrompt()}

            Ses textes :
            $corpus

            Synthétise fidèlement (en fusionnant avec l'existant sans perdre ses notes) :
            - themes : thèmes récurrents
            - images : métaphores, décors, objets qui reviennent
            - favoriteWords : mots ou tournures qui lui sont propres
            - emotions : palette émotionnelle dominante
            - references : références culturelles/personnelles repérées
            - styleSignature : analyse honnête de ses forces, tics d'écriture à surveiller et pistes pour creuser son style unique.
        """.trimIndent()

        val raw = withTimeout(45_000L) { model.generateContent(prompt).text }
            ?: throw IllegalStateException("Analyse d'univers impossible.")
        val json = JSONObject(raw)
        return PersonalUniverse(
            themes = json.optString("themes", current.themes).take(1200),
            images = json.optString("images", current.images).take(1200),
            favoriteWords = json.optString("favoriteWords", current.favoriteWords).take(1000),
            emotions = json.optString("emotions", current.emotions).take(800),
            references = json.optString("references", current.references).take(1000),
            styleSignature = json.optString("styleSignature", current.styleSignature).take(1800)
        )
    }

    suspend fun coachFrenchRapAndFurax(
        question: String,
        userLyrics: String,
        useWebSearch: Boolean = true
    ): String {
        acquireRateLimitOrThrow()
        val cleanQ = sanitize(question, 1000)
        val cleanUserLyrics = sanitize(userLyrics, 2500)
        return generateTextWithFailover(
            prompt = """
                Formation technique au rap français (histoire, écoles, mécaniques d'écriture, Furax Barbarossa, multisyllabiques, allitérations, assonances, enjambements, narration).
                Question / Sujet demandé : "$cleanQ"

                Texte personnel actuel de Raphaël (à utiliser pour l'exercice pratique) :
                ${cleanUserLyrics.ifBlank { "(Pas de texte sélectionné : propose un exercice sur 4 mesures avec un thème de son univers)" }}

                RÈGLES STRICTES :
                - Utilise la recherche web si utile pour vérifier les infos, interviews ou analyses récentes.
                - Décris et analyse les TECHNIQUES étape par étape, mais NE RECOPIE JAMAIS de paroles protégées (au maximum de minuscules fragments de 2-4 mots pour illustrer un son, jamais de couplets).
                - Propose ensuite un exercice concret à faire directement sur les propres mots de Raphaël.
            """.trimIndent(),
            useWebSearch = useWebSearch
        )
    }

    suspend fun generateDailyChallenge(universe: PersonalUniverse): String {
        acquireRateLimitOrThrow()
        return generateTextWithFailover(
            """
            Crée un Défi du jour d'écriture rap français court, exigeant et stimulant pour Raphaël.
            Son univers : ${universe.asSummaryPrompt()}
            Format exact :
            - Thème imposé (ancré dans le réel)
            - Contrainte technique précise (ex : 8 mesures, rimes multisyllabiques 3 syllabes ou rimes internes au temps 2, sans aucun mot cliché)
            - Conseil rapide de méthode (3 lignes max) pour démarrer en 2 minutes.
            """.trimIndent()
        )
    }

    suspend fun evaluateWorkshopAttempt(
        modeLabel: String,
        challengePrompt: String,
        userAttempt: String
    ): String {
        acquireRateLimitOrThrow()
        val cleanAttempt = sanitize(userAttempt, 3500)
        val cleanPrompt = sanitize(challengePrompt, 1200)
        return generateTextWithFailover(
            """
            Évaluation honnête ($modeLabel) :
            Consigne / Contexte : $cleanPrompt

            Essai de Raphaël :
            $cleanAttempt

            Donne un retour précis et exigeant (ne dis jamais "c'est super" par défaut) :
            1. Ce qui claque vraiment (images, rythme, sonorités précises).
            2. Ce qui bloque ou reste trop attendu (mots faibles, schéma de rime cassé, syllabes en trop).
            3. 2 pistes concrètes de réécriture immédiate sur ses propres lignes.
            """.trimIndent()
        )
    }

    suspend fun reviewTrackTextOrAudio(
        lyricsText: String,
        audioBytes: ByteArray?,
        audioMimeType: String?,
        universe: PersonalUniverse
    ): String {
        acquireRateLimitOrThrow()
        val cleanLyrics = sanitize(lyricsText, 4500)
        val startMs = System.currentTimeMillis()
        val promptText = """
            Revue complète et honnête de morceau (Directeur Artistique).
            Univers de Raphaël : ${universe.asSummaryPrompt()}
            ${if (cleanLyrics.isNotBlank()) "Paroles fournies :\n$cleanLyrics" else "Aucune parole écrite jointe : analyse surtout l'audio (voix, placement, émotion, articulation, rythme)."}

            RÈGLE : Pas de complaisance. Ne dis jamais "c'est super" par défaut.
            Structure OBLIGATOIRE :
            ### 1. Points forts précis
            ### 2. Points à travailler (écriture / flow / placement / mix-voix)
            ### 3. 3 améliorations prioritaires concrètes (classées 1, 2, 3, actionnables tout de suite)
        """.trimIndent()

        if (audioBytes != null && audioBytes.isNotEmpty() && !audioMimeType.isNullOrBlank()) {
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash",
                systemInstruction = content { text(daPersona) }
            )
            val multimodal = content {
                inlineData(audioBytes, audioMimeType)
                text(promptText)
            }
            val resp = withTimeout(60_000L) { model.generateContent(multimodal) }
            val result = resp.text?.trim()
            if (!result.isNullOrBlank()) {
                AppTelemetryMonitor.recordAiCall(System.currentTimeMillis() - startMs)
                return result
            }
            throw IllegalStateException("L'analyse audio n'a renvoyé aucune réponse.")
        }

        return generateTextWithFailover(promptText)
    }

    suspend fun prepareSunoAndRelease(
        song: StudioSong,
        universe: PersonalUniverse
    ): SunoExportBundle {
        acquireRateLimitOrThrow()
        val cleanTitle = sanitize(song.title, 120)
        val cleanInstru = sanitize(song.instruStyle, 250)
        val cleanLyrics = sanitize(song.fullLyrics(), 6000)

        val schema = Schema.obj(
            properties = mapOf(
                "stylePrompt" to Schema.string(),
                "taggedLyrics" to Schema.string(),
                "tikTokTitle" to Schema.string(),
                "tikTokDescription" to Schema.string(),
                "hashtags" to Schema.string(),
                "releaseSchedule" to Schema.string(),
                "clipIdeas" to Schema.string()
            )
        )
        val model = Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(daPersona) },
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
                temperature = 0.55f
            }
        )
        val prompt = """
            Prépare ce morceau pour Suno AI ET pour sa sortie TikTok / réseaux (téléphone uniquement).
            Titre : $cleanTitle | BPM : ${song.bpm} | Style visé : $cleanInstru
            Univers : ${universe.asSummaryPrompt()}
            Paroles par sections :
            $cleanLyrics

            Remplis :
            - stylePrompt : prompt Suno complet en anglais concis (genre, mood, instruments, ${song.bpm} BPM, texture de voix masculine grave/sincère ou adaptée, production).
            - taggedLyrics : paroles complètes prêtes à coller dans Suno avec balises [Intro], [Verse 1], [Chorus], etc. + indications d'ambiance entre parenthèses si utile, SANS altérer ses mots.
            - tikTokTitle : hook titre court pour TikTok/Reels.
            - tikTokDescription : description engageante prête à copier.
            - hashtags : 8 hashtags pertinents.
            - releaseSchedule : calendrier réaliste sur 7 jours (Teaser -> Extrait refrain -> Coulisses écriture -> Sortie).
            - clipIdeas : 3 idées de clips ou formats courts tournables seul avec un smartphone à 0 €.
        """.trimIndent()

        val raw = withTimeout(45_000L) { model.generateContent(prompt).text }
            ?: throw IllegalStateException("Impossible de préparer le pack Suno.")
        val json = JSONObject(raw)
        return SunoExportBundle(
            stylePrompt = json.optString("stylePrompt", ""),
            taggedLyrics = json.optString("taggedLyrics", song.fullLyrics()),
            tikTokTitle = json.optString("tikTokTitle", song.title),
            tikTokDescription = json.optString("tikTokDescription", ""),
            hashtags = json.optString("hashtags", ""),
            releaseSchedule = json.optString("releaseSchedule", ""),
            clipIdeas = json.optString("clipIdeas", "")
        )
    }

    private companion object {
        const val TAG = "MusicStudioAiService"
    }
}
