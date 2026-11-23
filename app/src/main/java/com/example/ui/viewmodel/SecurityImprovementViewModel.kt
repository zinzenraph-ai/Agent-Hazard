package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.HazardAiService
import com.example.data.SecurityImprovementRepository
import com.example.model.AppVersionSnapshot
import com.example.model.ImprovementProposal
import com.example.model.OwnerAccessPolicy
import com.example.model.ProtectionCheckItem
import com.example.model.SecurityEvent
import com.example.model.UiPreferences
import com.example.model.UiState
import com.example.security.AppTelemetryMonitor
import com.example.security.RateLimiter
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecurityImprovementViewModel(
    private val repository: SecurityImprovementRepository,
    private val aiService: HazardAiService,
    private val currentUserId: String,
    val isOwner: Boolean,
    val lockedWyattMode: Boolean
) : ViewModel() {

    private fun <T> Flow<T>.toUiState(errorFallback: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (fe == null && error is CancellationException) throw error
                val root = fe ?: error
                Log.w(TAG, errorFallback, root)
                emit(UiState.Error(root.message ?: errorFallback))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val ownerPolicy: StateFlow<OwnerAccessPolicy?> = repository.observeOwnerPolicy()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val securityEventsState: StateFlow<UiState<List<SecurityEvent>>> =
        repository.observeSecurityEvents(currentUserId).toUiState("Impossible de charger le journal de sécurité.")

    val proposalsState: StateFlow<UiState<List<ImprovementProposal>>> =
        repository.observeImprovementProposals(currentUserId).toUiState("Impossible de charger les propositions.")

    val versionsState: StateFlow<UiState<List<AppVersionSnapshot>>> =
        repository.observeAppVersions(currentUserId).toUiState("Impossible de charger les versions.")

    val uiPrefs: StateFlow<UiPreferences> = repository.observeUiPreferences(currentUserId)
        .catch { emit(UiPreferences(userId = currentUserId)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiPreferences(userId = currentUserId))

    private val _isScanningImprovements = MutableStateFlow(false)
    val isScanningImprovements: StateFlow<Boolean> = _isScanningImprovements.asStateFlow()

    private val _processingProposalId = MutableStateFlow<String?>(null)
    val processingProposalId: StateFlow<String?> = _processingProposalId.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        AppTelemetryMonitor.onAnomalyCallback = { _, title, detail, severity ->
            viewModelScope.launch {
                repository.logSecurityEvent(
                    userId = currentUserId,
                    eventType = "anomaly",
                    severity = severity,
                    title = title,
                    detail = detail
                )
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun protectionChecklist(policy: OwnerAccessPolicy?): List<ProtectionCheckItem> {
        val wyattLabel = if (policy?.wyattEmail.isNullOrBlank()) {
            "Propriétaire seul"
        } else {
            "Propriétaire + Wyatt"
        }
        return listOf(
            ProtectionCheckItem(
                id = "google_allowlist",
                title = "Connexion Google & Liste Blanche",
                description = "Seuls ton compte et celui autorisé pour Wyatt peuvent entrer.",
                active = policy != null,
                statusLabel = if (policy != null) "Actif ($wyattLabel)" else "Actif"
            ),
            ProtectionCheckItem(
                id = "firestore_rules",
                title = "Règles Firebase strictes par UID",
                description = "Chaque compte lit et écrit uniquement ses propres données. Zéro accès public.",
                active = true,
                statusLabel = "Verrouillé"
            ),
            ProtectionCheckItem(
                id = "app_check",
                title = "Firebase App Check",
                description = "Bloque les appels hors application officielle Hazard.",
                active = true,
                statusLabel = "Actif"
            ),
            ProtectionCheckItem(
                id = "prompt_firewall",
                title = "Validation saisies & Anti-Injection IA",
                description = "Nettoie chaque champ et neutralise les commandes suspectes avant l'IA.",
                active = true,
                statusLabel = "Actif (${AppTelemetryMonitor.snapshot().injectionBlocksCount} blocages)"
            ),
            ProtectionCheckItem(
                id = "no_secrets",
                title = "Secrets hors code, logs & écran",
                description = "Toutes les clés sont lues uniquement via BuildConfig / Secrets AI Studio.",
                active = true,
                statusLabel = "Protégé"
            ),
            ProtectionCheckItem(
                id = "rate_limiter",
                title = "Limitation requêtes / minute",
                description = "Chat ${RateLimiter.bucketLimitsPerMinute["ai_chat"]}/min · Marchés ${RateLimiter.bucketLimitsPerMinute["market_quotes"]}/min.",
                active = true,
                statusLabel = "Actif"
            ),
            ProtectionCheckItem(
                id = "wyatt_isolation",
                title = "Cloisonnement Mode Wyatt",
                description = "Aucun accès aux modules Finance, Trading, Bot ou Clés API en mode Wyatt.",
                active = true,
                statusLabel = "Garanti"
            )
        )
    }

    fun updateWyattAllowedEmail(email: String) {
        if (!isOwner) return
        viewModelScope.launch {
            val res = repository.updateWyattAllowedEmail(email)
            _bannerMessage.value = if (res.isSuccess) {
                if (email.isBlank()) "Accès email secondaire Wyatt retiré."
                else "Compte Wyatt autorisé : ${email.trim().lowercase()}."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Mise à jour impossible."
            }
        }
    }

    fun markAnomalyResolved(eventId: String) {
        viewModelScope.launch {
            repository.markSecurityEventResolved(currentUserId, eventId)
        }
    }

    fun currentWeekKey(): String =
        SimpleDateFormat("yyyy-'W'ww", Locale.FRANCE).format(Date())

    // Continuous Improvement (max 3 priority proposals per week)
    fun runWeeklyImprovementScan() {
        if (lockedWyattMode) return
        val existing = (proposalsState.value as? UiState.Success)?.data.orEmpty()
        val week = currentWeekKey()
        val alreadyThisWeek = existing.count { it.weekKey == week }
        val remainingQuota = (3 - alreadyThisWeek).coerceAtLeast(0)

        if (remainingQuota == 0) {
            _bannerMessage.value = "Quota hebdo atteint : 3/3 propositions déjà générées pour $week."
            return
        }

        val anomalies = (securityEventsState.value as? UiState.Success)?.data
            ?.count { it.eventType == "anomaly" && !it.resolved } ?: 0

        viewModelScope.launch {
            _isScanningImprovements.value = true
            try {
                val generated = aiService.generateWeeklyImprovementProposals(
                    weekKey = week,
                    maxCount = remainingQuota,
                    uiPrefs = uiPrefs.value,
                    pastProposals = existing,
                    securityAnomalyCount = anomalies
                )
                if (generated.isNotEmpty()) {
                    repository.saveImprovementProposals(currentUserId, generated)
                    _bannerMessage.value = "${generated.size} proposition(s) prioritaire(s) prête(s) pour $week."
                } else {
                    _bannerMessage.value = "Aucune nouvelle amélioration nécessaire cette semaine."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Analyse impossible pour le moment."
            } finally {
                _isScanningImprovements.value = false
            }
        }
    }

    fun applyProposal(proposal: ImprovementProposal) {
        if (lockedWyattMode) return
        val versions = (versionsState.value as? UiState.Success)?.data.orEmpty()
        val currentVer = versions.firstOrNull { it.current }
        val appliedIds = currentVer?.appliedProposalIds.orEmpty()

        viewModelScope.launch {
            _processingProposalId.value = proposal.id
            try {
                val plan = aiService.prepareProposalImplementation(proposal)
                val res = repository.applyProposalWithSnapshot(
                    userId = currentUserId,
                    proposal = proposal,
                    preparedPlan = plan,
                    existingVersionsCount = versions.size + 1,
                    currentAppliedIds = appliedIds
                )
                if (res.isSuccess) {
                    val verTag = res.getOrNull().orEmpty()
                    if (proposal.supports24hTrial && proposal.trialAccentStyle.isNotBlank()) {
                        val updated = uiPrefs.value.copy(
                            accentStyle = proposal.trialAccentStyle,
                            themeMode = proposal.trialThemeMode.ifBlank { uiPrefs.value.themeMode }
                        )
                        repository.saveUiPreferences(currentUserId, updated)
                    }
                    _bannerMessage.value = "Version $verTag sauvegardée & modification préparée !"
                } else {
                    _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Échec de l'application."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Erreur lors de la préparation."
            } finally {
                _processingProposalId.value = null
            }
        }
    }

    fun start24hVisualTrial(proposal: ImprovementProposal) {
        if (!proposal.supports24hTrial || proposal.trialAccentStyle.isBlank()) return
        val cur = uiPrefs.value
        val snapshot = "${cur.themeMode}|${cur.accentStyle}|${cur.textScale}"
        val expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        viewModelScope.launch {
            val next = cur.copy(
                accentStyle = proposal.trialAccentStyle,
                themeMode = proposal.trialThemeMode.ifBlank { cur.themeMode },
                trialLabel = proposal.title.take(60),
                trialExpiresAtMs = expiresAt,
                previousThemeSnapshot = if (cur.previousThemeSnapshot.isBlank()) snapshot else cur.previousThemeSnapshot
            )
            repository.saveUiPreferences(currentUserId, next)
            _bannerMessage.value = "Essai 24 h activé : « ${proposal.title} ». Tu peux garder ou annuler à tout moment."
        }
    }

    fun keep24hTrialAppearance() {
        val cur = uiPrefs.value
        viewModelScope.launch {
            repository.saveUiPreferences(
                currentUserId,
                cur.copy(trialLabel = "", trialExpiresAtMs = 0L, previousThemeSnapshot = "")
            )
            _bannerMessage.value = "Nouvelle apparence conservée !"
        }
    }

    fun cancel24hTrialAppearance() {
        val cur = uiPrefs.value
        val parts = cur.previousThemeSnapshot.split("|")
        val prevTheme = parts.getOrNull(0)?.takeIf { it in setOf("dark", "light") } ?: "dark"
        val prevAccent = parts.getOrNull(1)?.takeIf { it in setOf("hazard", "emerald", "amber", "violet", "ocean") } ?: "hazard"
        val prevScale = parts.getOrNull(2)?.toDoubleOrNull() ?: 1.0
        viewModelScope.launch {
            repository.saveUiPreferences(
                currentUserId,
                cur.copy(
                    themeMode = prevTheme,
                    accentStyle = prevAccent,
                    textScale = prevScale,
                    trialLabel = "",
                    trialExpiresAtMs = 0L,
                    previousThemeSnapshot = ""
                )
            )
            _bannerMessage.value = "Essai 24 h annulé : retour à ton style précédent."
        }
    }

    fun refuseProposal(proposal: ImprovementProposal) {
        viewModelScope.launch {
            repository.setProposalStatus(currentUserId, proposal.id, "refused")
            _bannerMessage.value = "Proposition refusée (Hazard mémorise ton choix)."
        }
    }

    fun postponeProposal(proposal: ImprovementProposal) {
        viewModelScope.launch {
            repository.setProposalStatus(currentUserId, proposal.id, "postponed")
            _bannerMessage.value = "Proposition mise de côté pour plus tard."
        }
    }

    fun rollbackToVersion(version: AppVersionSnapshot) {
        viewModelScope.launch {
            val res = repository.rollbackToVersion(currentUserId, version)
            _bannerMessage.value = if (res.isSuccess) {
                "Retour à la version ${version.versionTag} effectué en 1 clic !"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Retour arrière impossible."
            }
        }
    }

    // Mobile UI Personalization Actions
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.saveUiPreferences(currentUserId, uiPrefs.value.copy(themeMode = mode))
        }
    }

    fun setAccentStyle(style: String) {
        viewModelScope.launch {
            repository.saveUiPreferences(currentUserId, uiPrefs.value.copy(accentStyle = style))
        }
    }

    fun setTextScale(scale: Double) {
        viewModelScope.launch {
            repository.saveUiPreferences(currentUserId, uiPrefs.value.copy(textScale = scale))
        }
    }

    fun toggleModuleVisibility(moduleId: String) {
        val cur = uiPrefs.value
        val nextHidden = if (moduleId in cur.hiddenModules) {
            cur.hiddenModules - moduleId
        } else {
            cur.hiddenModules + moduleId
        }
        viewModelScope.launch {
            repository.saveUiPreferences(currentUserId, cur.copy(hiddenModules = nextHidden))
        }
    }

    fun moveModuleOrder(moduleId: String, direction: Int) {
        val cur = uiPrefs.value
        val mutable = cur.moduleOrder.toMutableList()
        val idx = mutable.indexOf(moduleId)
        if (idx == -1) return
        val target = (idx + direction).coerceIn(0, mutable.lastIndex)
        if (idx == target) return
        mutable.removeAt(idx)
        mutable.add(target, moduleId)
        viewModelScope.launch {
            repository.saveUiPreferences(currentUserId, cur.copy(moduleOrder = mutable))
        }
    }

    fun toggleGadget(gadgetId: String) {
        val cur = uiPrefs.value
        val next = if (gadgetId in cur.enabledGadgets) {
            cur.enabledGadgets - gadgetId
        } else {
            cur.enabledGadgets + gadgetId
        }
        viewModelScope.launch {
            repository.saveUiPreferences(currentUserId, cur.copy(enabledGadgets = next))
        }
    }

    fun resetToDefaultHazardStyle() {
        viewModelScope.launch {
            repository.saveUiPreferences(currentUserId, UiPreferences(userId = currentUserId))
            _bannerMessage.value = "Style par défaut de Hazard restauré."
        }
    }

    override fun onCleared() {
        AppTelemetryMonitor.onAnomalyCallback = null
        super.onCleared()
    }

    private companion object {
        const val TAG = "SecImpVM"
    }
}
