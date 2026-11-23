package com.example.data

import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AccountAccessState
import com.example.model.AppVersionSnapshot
import com.example.model.ImprovementProposal
import com.example.model.OwnerAccessPolicy
import com.example.model.SecurityEvent
import com.example.model.UiPreferences
import com.example.security.InputSanitizer
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SecurityImprovementRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Connexion Google obligatoire requise.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun policyDoc() = db.collection("security_settings").document("owner_policy")

    private inline fun <reified T> observeUserCollection(
        userId: String,
        collectionName: String
    ): Flow<List<T>> = flow {
        val path = "users/$userId/$collectionName"
        emitAll(
            userDoc(userId).collection(collectionName)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    val ex = fe ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun verifyAndRecordSignIn(
        uid: String,
        rawEmail: String?,
        deviceLabel: String
    ): AccountAccessState {
        val normalizedEmail = rawEmail?.trim()?.lowercase().orEmpty()
        val safeDevice = InputSanitizer.sanitizeSingleLine(deviceLabel, 100).ifBlank { "Android" }
        if (normalizedEmail.isBlank()) {
            return AccountAccessState.Denied("", "Adresse email Google manquante ou non vérifiée.")
        }

        return try {
            val snap = policyDoc().get().await()
            if (!snap.exists()) {
                policyDoc().set(
                    mapOf(
                        "ownerUid" to uid,
                        "ownerEmail" to normalizedEmail,
                        "wyattEmail" to "",
                        "allowedEmails" to listOf(normalizedEmail),
                        "lastLoginEmail" to normalizedEmail,
                        "lastLoginDevice" to safeDevice,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                ensureBaselineVersionExists(uid)
                ensureUiPrefsExists(uid)
                logSecurityEvent(
                    userId = uid,
                    eventType = "login",
                    severity = "info",
                    title = "Compte propriétaire enregistré",
                    detail = "Liste blanche initialisée pour $normalizedEmail.",
                    deviceLabel = safeDevice
                )
                return AccountAccessState.Authorized(
                    uid = uid,
                    email = normalizedEmail,
                    isOwner = true,
                    lockedWyattMode = false
                )
            }

            val policy = snap.toObject(OwnerAccessPolicy::class.java) ?: OwnerAccessPolicy()
            val allowedNormalized = policy.allowedEmails.map { it.trim().lowercase() }
            val isOwner = (policy.ownerUid == uid) || (policy.ownerEmail.equals(normalizedEmail, true))
            val isAuthorized = isOwner || (normalizedEmail in allowedNormalized)

            if (!isAuthorized) {
                return AccountAccessState.Denied(
                    email = normalizedEmail,
                    reason = "Accès refusé : seuls le compte de Raphaël et le compte autorisé pour Wyatt peuvent entrer."
                )
            }

            val isWyattAccount = !isOwner && policy.wyattEmail.equals(normalizedEmail, ignoreCase = true)
            if (isOwner) {
                val deviceChanged = policy.lastLoginDevice.isNotBlank() &&
                    !policy.lastLoginDevice.equals(safeDevice, ignoreCase = true)
                policyDoc().update(
                    mapOf(
                        "lastLoginEmail" to normalizedEmail,
                        "lastLoginDevice" to safeDevice,
                        "lastLoginAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                if (deviceChanged) {
                    logSecurityEvent(
                        userId = uid,
                        eventType = "anomaly",
                        severity = "warning",
                        title = "Connexion depuis un nouvel appareil",
                        detail = "Connexion sur « $safeDevice » (précédent : ${policy.lastLoginDevice}).",
                        deviceLabel = safeDevice
                    )
                }
            }

            ensureBaselineVersionExists(uid)
            ensureUiPrefsExists(uid)
            logSecurityEvent(
                userId = uid,
                eventType = "login",
                severity = "info",
                title = if (isWyattAccount) "Connexion Mode Wyatt" else "Connexion Google vérifiée",
                detail = "Session ouverte pour $normalizedEmail.",
                deviceLabel = safeDevice
            )

            AccountAccessState.Authorized(
                uid = uid,
                email = normalizedEmail,
                isOwner = isOwner,
                lockedWyattMode = isWyattAccount
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "security_settings/owner_policy")
            AccountAccessState.Denied(
                email = normalizedEmail,
                reason = e.localizedMessage ?: "Vérification de sécurité impossible."
            )
        }
    }

    fun observeOwnerPolicy(): Flow<OwnerAccessPolicy?> = flow {
        emitAll(
            policyDoc().snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(OwnerAccessPolicy::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    emit(null)
                }
        )
    }

    suspend fun updateWyattAllowedEmail(rawWyattEmail: String): Result<Unit> = try {
        val uid = requireUserId()
        val snap = policyDoc().get().await()
        val policy = snap.toObject(OwnerAccessPolicy::class.java)
            ?: throw IllegalStateException("Politique propriétaire absente.")
        check(policy.ownerUid == uid) { "Seul Raphaël peut modifier les comptes autorisés." }

        val cleanedWyatt = if (rawWyattEmail.isBlank()) "" else {
            InputSanitizer.sanitizeEmail(rawWyattEmail).also {
                require(it.isNotBlank()) { "Adresse email Google invalide." }
            }
        }
        val allowed = buildList {
            add(policy.ownerEmail.lowercase())
            if (cleanedWyatt.isNotBlank() && cleanedWyatt != policy.ownerEmail.lowercase()) {
                add(cleanedWyatt)
            }
        }
        policyDoc().update(
            mapOf(
                "wyattEmail" to cleanedWyatt,
                "allowedEmails" to allowed,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        logSecurityEvent(
            userId = uid,
            eventType = "protection",
            severity = "info",
            title = if (cleanedWyatt.isBlank()) "Accès Wyatt révoqué" else "Compte Wyatt autorisé",
            detail = if (cleanedWyatt.isBlank()) "Seul ton compte principal est autorisé." else "Compte Wyatt ajouté à la liste blanche : $cleanedWyatt.",
            deviceLabel = "Paramètres Sécurité"
        )
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "security_settings/owner_policy")
        Result.failure(e)
    }

    fun observeSecurityEvents(userId: String): Flow<List<SecurityEvent>> =
        observeUserCollection(userId, "security_events")

    suspend fun logSecurityEvent(
        userId: String,
        eventType: String,
        severity: String,
        title: String,
        detail: String,
        deviceLabel: String = "Android"
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        userDoc(uid).collection("security_events").document().set(
            mapOf(
                "userId" to uid,
                "eventType" to eventType,
                "severity" to severity,
                "title" to InputSanitizer.sanitizeSingleLine(title, 150),
                "detail" to InputSanitizer.sanitizeText(detail, 1100),
                "deviceLabel" to InputSanitizer.sanitizeSingleLine(deviceLabel, 100),
                "resolved" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun markSecurityEventResolved(userId: String, eventId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        userDoc(uid).collection("security_events").document(eventId)
            .update(mapOf("resolved" to true, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    // Continuous Improvement Proposals & Version Snapshots
    fun observeImprovementProposals(userId: String): Flow<List<ImprovementProposal>> =
        observeUserCollection(userId, "improvement_proposals")

    fun observeAppVersions(userId: String): Flow<List<AppVersionSnapshot>> =
        observeUserCollection(userId, "app_versions")

    private suspend fun ensureBaselineVersionExists(userId: String) {
        val col = userDoc(userId).collection("app_versions")
        if (col.limit(1).get().await().isEmpty) {
            col.document("v1_0_0").set(
                mapOf(
                    "userId" to userId,
                    "versionTag" to "v1.0.0",
                    "summary" to "Style & configuration Hazard par défaut",
                    "appliedProposalIds" to emptyList<String>(),
                    "activeNotes" to "Identité Hazard par défaut, protections actives.",
                    "current" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun saveImprovementProposals(userId: String, proposals: List<ImprovementProposal>): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val batch = db.batch()
        proposals.forEach { p ->
            val ref = userDoc(uid).collection("improvement_proposals").document()
            batch.set(
                ref,
                mapOf(
                    "userId" to uid,
                    "title" to p.title.take(160),
                    "category" to p.category,
                    "priority" to p.priority,
                    "whatToChange" to p.whatToChange.take(2000),
                    "why" to p.why.take(2000),
                    "impact" to p.impact.take(1500),
                    "risks" to p.risks.take(1500),
                    "rollbackHowTo" to p.rollbackHowTo.take(1500),
                    "preparedPlan" to "",
                    "weekKey" to p.weekKey,
                    "status" to "pending",
                    "appliedVersionId" to "",
                    "beforePreview" to p.beforePreview.take(1000),
                    "afterPreview" to p.afterPreview.take(1000),
                    "supports24hTrial" to p.supports24hTrial,
                    "trialAccentStyle" to p.trialAccentStyle,
                    "trialThemeMode" to p.trialThemeMode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/improvement_proposals")
        Result.failure(e)
    }

    suspend fun applyProposalWithSnapshot(
        userId: String,
        proposal: ImprovementProposal,
        preparedPlan: String,
        existingVersionsCount: Int,
        currentAppliedIds: List<String>
    ): Result<String> = try {
        val uid = requireUserId()
        check(uid == userId)
        val nextVersionTag = "v1.0.${existingVersionsCount.coerceAtLeast(1)}"
        val versionsCol = userDoc(uid).collection("app_versions")
        val existing = versionsCol.get().await()
        val batch = db.batch()
        existing.documents.forEach { doc ->
            if (doc.getBoolean("current") == true) {
                batch.update(doc.reference, mapOf("current" to false, "updatedAt" to FieldValue.serverTimestamp()))
            }
        }
        val newVerRef = versionsCol.document()
        val updatedApplied = (currentAppliedIds + proposal.id).distinct().take(50)
        batch.set(
            newVerRef,
            mapOf(
                "userId" to uid,
                "versionTag" to nextVersionTag,
                "summary" to "Appliqué : ${proposal.title.take(120)}",
                "appliedProposalIds" to updatedApplied,
                "activeNotes" to preparedPlan.take(3500),
                "current" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val propRef = userDoc(uid).collection("improvement_proposals").document(proposal.id)
        batch.update(
            propRef,
            mapOf(
                "status" to "applied",
                "preparedPlan" to preparedPlan.take(5500),
                "appliedVersionId" to newVerRef.id,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(nextVersionTag)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/improvement_proposals/${proposal.id}")
        Result.failure(e)
    }

    suspend fun setProposalStatus(userId: String, proposalId: String, status: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        userDoc(uid).collection("improvement_proposals").document(proposalId)
            .update(mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun rollbackToVersion(userId: String, targetVersion: AppVersionSnapshot): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val versionsCol = userDoc(uid).collection("app_versions")
        val allVersions = versionsCol.get().await()
        val allProposals = userDoc(uid).collection("improvement_proposals").get().await()

        val batch = db.batch()
        allVersions.documents.forEach { doc ->
            batch.update(
                doc.reference,
                mapOf("current" to (doc.id == targetVersion.id), "updatedAt" to FieldValue.serverTimestamp())
            )
        }
        allProposals.documents.forEach { doc ->
            val st = doc.getString("status")
            if (st == "applied" && doc.id !in targetVersion.appliedProposalIds) {
                batch.update(
                    doc.reference,
                    mapOf("status" to "rolled_back", "updatedAt" to FieldValue.serverTimestamp())
                )
            }
        }
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/app_versions/${targetVersion.id}")
        Result.failure(e)
    }

    // UI Preferences & 24h Trial
    fun observeUiPreferences(userId: String): Flow<UiPreferences> = flow {
        val path = "users/$userId/ui_prefs/main"
        emitAll(
            userDoc(userId).collection("ui_prefs").document("main")
                .snapshots()
                .map { snap ->
                    snap.toObject(UiPreferences::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: UiPreferences(userId = userId)
                }
                .catch { error ->
                    val fe = generateSequence(error) { it.cause }.filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                    if (fe == null && error is CancellationException) throw error
                    val ex = fe ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) handleFirestoreError(ex, OperationType.GET, path)
                    throw error
                }
        )
    }

    private suspend fun ensureUiPrefsExists(userId: String) {
        val docRef = userDoc(userId).collection("ui_prefs").document("main")
        if (!docRef.get().await().exists()) {
            val defaults = UiPreferences(userId = userId)
            docRef.set(
                mapOf(
                    "userId" to userId,
                    "themeMode" to defaults.themeMode,
                    "accentStyle" to defaults.accentStyle,
                    "textScale" to defaults.textScale,
                    "moduleOrder" to defaults.moduleOrder,
                    "hiddenModules" to defaults.hiddenModules,
                    "enabledGadgets" to defaults.enabledGadgets,
                    "trialLabel" to "",
                    "trialExpiresAtMs" to 0L,
                    "previousThemeSnapshot" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

    suspend fun saveUiPreferences(userId: String, prefs: UiPreferences): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docRef = userDoc(uid).collection("ui_prefs").document("main")
        val data = mapOf(
            "themeMode" to prefs.themeMode,
            "accentStyle" to prefs.accentStyle,
            "textScale" to prefs.textScale.coerceIn(0.85, 1.45),
            "moduleOrder" to prefs.moduleOrder,
            "hiddenModules" to prefs.hiddenModules,
            "enabledGadgets" to prefs.enabledGadgets,
            "trialLabel" to prefs.trialLabel.take(120),
            "trialExpiresAtMs" to prefs.trialExpiresAtMs.coerceAtLeast(0L),
            "previousThemeSnapshot" to prefs.previousThemeSnapshot.take(160),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (docRef.get().await().exists()) {
            docRef.update(data).await()
        } else {
            val full = data.toMutableMap().apply {
                put("userId", uid)
                put("createdAt", FieldValue.serverTimestamp())
            }
            docRef.set(full).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/ui_prefs/main")
        Result.failure(e)
    }
}
