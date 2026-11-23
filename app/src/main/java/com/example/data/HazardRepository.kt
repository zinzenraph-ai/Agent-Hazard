package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.ChatMessage
import com.example.model.CreationCategory
import com.example.model.CreationItem
import com.example.model.MemoryPatch
import com.example.model.MemoryProfile
import com.example.model.MissionTopic
import com.example.model.SpeakerMode
import com.example.security.MemoryPrivacyGuard
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

class HazardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Connexion Google requise avant d'accéder à Firestore.")
    }

    private fun usersDoc(uid: String) = db.collection("users").document(uid)

    fun observeMemory(userId: String): Flow<MemoryProfile> = flow {
        val path = "users/$userId/memories/main"
        emitAll(
            usersDoc(userId).collection("memories").document("main")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        MemoryProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: MemoryProfile(userId = userId)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    fun observeWyattMemory(userId: String): Flow<MemoryProfile> = flow {
        val path = "users/$userId/memories/wyatt"
        emitAll(
            usersDoc(userId).collection("memories").document("wyatt")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObject(
                        MemoryProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ) ?: MemoryProfile(id = "wyatt", userId = userId, firstName = "Wyatt")
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureInitialMemoryExists(userId: String): Result<Unit> {
        val docPath = "users/$userId/memories/main"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            val docRef = usersDoc(uid).collection("memories").document("main")
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val initial = MemoryProfile(userId = uid)
                val payload = mapOf(
                    "userId" to uid,
                    "rawText" to initial.formatAsCopyableSheet(),
                    "firstName" to initial.firstName,
                    "musicStyle" to initial.musicStyle,
                    "favoriteArtists" to initial.favoriteArtists,
                    "wyattLikes" to initial.wyattLikes,
                    "wyattWantsToLearn" to initial.wyattWantsToLearn,
                    "wyattMemories" to initial.wyattMemories,
                    "shortTermGoals" to initial.shortTermGoals,
                    "longTermGoals" to initial.longTermGoals,
                    "decisionsMade" to initial.decisionsMade,
                    "inProgress" to initial.inProgress,
                    "tradingLevel" to initial.tradingLevel,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensureInitialWyattMemoryExists(userId: String): Result<Unit> {
        val docPath = "users/$userId/memories/wyatt"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            val docRef = usersDoc(uid).collection("memories").document("wyatt")
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val initial = MemoryProfile(id = "wyatt", userId = uid, firstName = "Wyatt")
                val payload = mapOf(
                    "userId" to uid,
                    "rawText" to initial.formatMinimalWyattSheet(),
                    "firstName" to "Wyatt",
                    "musicStyle" to "",
                    "favoriteArtists" to "",
                    "wyattLikes" to "",
                    "wyattWantsToLearn" to "",
                    "wyattMemories" to "",
                    "shortTermGoals" to "",
                    "longTermGoals" to "",
                    "decisionsMade" to "",
                    "inProgress" to "",
                    "tradingLevel" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveWyattMemory(
        userId: String,
        likes: String,
        learned: String,
        memories: String,
        dateLabel: String
    ): Result<Unit> {
        val docPath = "users/$userId/memories/wyatt"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            val docRef = usersDoc(uid).collection("memories").document("wyatt")
            val snap = docRef.get().await()

            val cleanLikes = MemoryPrivacyGuard.scrubSensitiveLines(likes, wyattMinimal = true).take(1000)
            val cleanLearned = MemoryPrivacyGuard.scrubSensitiveLines(learned, wyattMinimal = true).take(1000)
            val cleanMemories = MemoryPrivacyGuard.scrubSensitiveLines(memories, wyattMinimal = true).take(2500)
            val minimalProfile = MemoryProfile(
                id = "wyatt",
                userId = uid,
                firstName = "Wyatt",
                wyattLikes = cleanLikes,
                wyattWantsToLearn = cleanLearned,
                wyattMemories = cleanMemories
            )
            val formattedRaw = minimalProfile.formatMinimalWyattSheet(dateLabel).take(15000)

            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "rawText" to formattedRaw,
                    "firstName" to "Wyatt",
                    "musicStyle" to "",
                    "favoriteArtists" to "",
                    "wyattLikes" to cleanLikes,
                    "wyattWantsToLearn" to cleanLearned,
                    "wyattMemories" to cleanMemories,
                    "shortTermGoals" to "",
                    "longTermGoals" to "",
                    "decisionsMade" to "",
                    "inProgress" to "",
                    "tradingLevel" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "rawText" to formattedRaw,
                        "firstName" to "Wyatt",
                        "musicStyle" to "",
                        "favoriteArtists" to "",
                        "wyattLikes" to cleanLikes,
                        "wyattWantsToLearn" to cleanLearned,
                        "wyattMemories" to cleanMemories,
                        "shortTermGoals" to "",
                        "longTermGoals" to "",
                        "decisionsMade" to "",
                        "inProgress" to "",
                        "tradingLevel" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearAllMemory(userId: String, dateLabel: String): Result<Unit> {
        val emptyMain = MemoryProfile(userId = userId, firstName = "")
        val mainRes = saveMemoryProfile(userId, emptyMain, dateLabel)
        val wyattRes = saveWyattMemory(userId, likes = "", learned = "", memories = "", dateLabel = dateLabel)
        return if (mainRes.isSuccess && wyattRes.isSuccess) {
            Result.success(Unit)
        } else {
            mainRes.exceptionOrNull()?.let { Result.failure(it) } ?: wyattRes
        }
    }

    suspend fun saveMemoryProfile(
        userId: String,
        updated: MemoryProfile,
        dateLabel: String
    ): Result<Unit> {
        val docPath = "users/$userId/memories/main"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            val docRef = usersDoc(uid).collection("memories").document("main")
            val snap = docRef.get().await()

            val cleanFirstName = MemoryPrivacyGuard.scrubSensitiveLines(updated.firstName).trim().take(80)
            val cleanMusicStyle = MemoryPrivacyGuard.scrubSensitiveLines(updated.musicStyle).take(500)
            val cleanArtists = MemoryPrivacyGuard.scrubSensitiveLines(updated.favoriteArtists).take(500)
            val cleanWyattLikes = MemoryPrivacyGuard.scrubSensitiveLines(updated.wyattLikes, wyattMinimal = true).take(1000)
            val cleanWyattLearn = MemoryPrivacyGuard.scrubSensitiveLines(updated.wyattWantsToLearn, wyattMinimal = true).take(1000)
            val cleanWyattMem = MemoryPrivacyGuard.scrubSensitiveLines(updated.wyattMemories, wyattMinimal = true).take(2500)
            val cleanShortGoals = MemoryPrivacyGuard.scrubSensitiveLines(updated.shortTermGoals).take(1000)
            val cleanLongGoals = MemoryPrivacyGuard.scrubSensitiveLines(updated.longTermGoals).take(1000)
            val cleanDecisions = MemoryPrivacyGuard.scrubSensitiveLines(updated.decisionsMade).take(2500)
            val cleanInProgress = MemoryPrivacyGuard.scrubSensitiveLines(updated.inProgress).take(2500)
            val cleanTrading = MemoryPrivacyGuard.scrubSensitiveLines(updated.tradingLevel).take(200)

            val formattedRaw = updated.copy(
                firstName = cleanFirstName,
                musicStyle = cleanMusicStyle,
                favoriteArtists = cleanArtists,
                wyattLikes = cleanWyattLikes,
                wyattWantsToLearn = cleanWyattLearn,
                wyattMemories = cleanWyattMem,
                shortTermGoals = cleanShortGoals,
                longTermGoals = cleanLongGoals,
                decisionsMade = cleanDecisions,
                inProgress = cleanInProgress,
                tradingLevel = cleanTrading
            ).formatAsCopyableSheet(dateLabel).take(15000)

            if (!snap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "rawText" to formattedRaw,
                    "firstName" to cleanFirstName,
                    "musicStyle" to cleanMusicStyle,
                    "favoriteArtists" to cleanArtists,
                    "wyattLikes" to cleanWyattLikes,
                    "wyattWantsToLearn" to cleanWyattLearn,
                    "wyattMemories" to cleanWyattMem,
                    "shortTermGoals" to cleanShortGoals,
                    "longTermGoals" to cleanLongGoals,
                    "decisionsMade" to cleanDecisions,
                    "inProgress" to cleanInProgress,
                    "tradingLevel" to cleanTrading,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                docRef.update(
                    mapOf(
                        "rawText" to formattedRaw,
                        "firstName" to cleanFirstName,
                        "musicStyle" to cleanMusicStyle,
                        "favoriteArtists" to cleanArtists,
                        "wyattLikes" to cleanWyattLikes,
                        "wyattWantsToLearn" to cleanWyattLearn,
                        "wyattMemories" to cleanWyattMem,
                        "shortTermGoals" to cleanShortGoals,
                        "longTermGoals" to cleanLongGoals,
                        "decisionsMade" to cleanDecisions,
                        "inProgress" to cleanInProgress,
                        "tradingLevel" to cleanTrading,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun applyPatch(current: MemoryProfile, patch: MemoryPatch): MemoryProfile {
        val cleanFirstName = patch.firstName
            ?.let { MemoryPrivacyGuard.scrubSensitiveLines(it).trim() }
            ?.takeIf { it.isNotBlank() && !it.contains("[...]") }
            ?.take(80)
            ?: current.firstName

        var mergedDecisions = MemoryPrivacyGuard.mergeLines(
            existing = current.decisionsMade,
            addition = patch.decisionsMade,
            maxLength = 2500
        )
        mergedDecisions = MemoryPrivacyGuard.mergeLines(
            existing = mergedDecisions,
            addition = patch.stylePreferences,
            maxLength = 2500,
            prefix = "[Style] "
        )
        mergedDecisions = MemoryPrivacyGuard.mergeLines(
            existing = mergedDecisions,
            addition = patch.financeNotes,
            maxLength = 2500,
            prefix = "[Finance] "
        )

        return current.copy(
            firstName = cleanFirstName,
            musicStyle = MemoryPrivacyGuard.mergeLines(current.musicStyle, patch.musicStyle, 500),
            favoriteArtists = MemoryPrivacyGuard.mergeLines(current.favoriteArtists, patch.favoriteArtists, 500),
            wyattLikes = MemoryPrivacyGuard.mergeLines(current.wyattLikes, patch.wyattLikes, 1000, wyattMinimal = true),
            wyattWantsToLearn = MemoryPrivacyGuard.mergeLines(current.wyattWantsToLearn, patch.wyattWantsToLearn, 1000, wyattMinimal = true),
            wyattMemories = MemoryPrivacyGuard.mergeLines(current.wyattMemories, patch.wyattMemories, 2500, wyattMinimal = true),
            shortTermGoals = MemoryPrivacyGuard.mergeLines(current.shortTermGoals, patch.shortTermGoals, 1000),
            longTermGoals = MemoryPrivacyGuard.mergeLines(current.longTermGoals, patch.longTermGoals, 1000),
            decisionsMade = mergedDecisions,
            inProgress = MemoryPrivacyGuard.mergeLines(current.inProgress, patch.inProgress, 2500),
            tradingLevel = MemoryPrivacyGuard.mergeLines(current.tradingLevel, patch.tradingLevel, 200)
        )
    }

    fun observeMessages(userId: String): Flow<List<ChatMessage>> = flow {
        val path = "users/$userId/messages"
        emitAll(
            usersDoc(userId).collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(80)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addMessage(
        userId: String,
        role: String,
        speakerMode: SpeakerMode,
        topic: MissionTopic,
        text: String,
        requiresConfirmation: Boolean = false
    ): Result<Unit> {
        var docPath = "users/$userId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            val cleanText = text.trim().take(15000)
            require(cleanText.isNotBlank()) { "Le message ne peut pas être vide." }
            val docRef = usersDoc(uid).collection("messages").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "role" to role,
                "speakerMode" to speakerMode.wireValue,
                "topic" to topic.wireValue.take(60),
                "text" to cleanText,
                "requiresConfirmation" to requiresConfirmation,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun clearConversation(userId: String): Result<Unit> {
        val path = "users/$userId/messages"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            val snap = usersDoc(uid).collection("messages").get().await()
            if (!snap.isEmpty) {
                val batch = db.batch()
                snap.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    fun observeCreations(userId: String): Flow<List<CreationItem>> = flow {
        val path = "users/$userId/creations"
        emitAll(
            usersDoc(userId).collection("creations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CreationItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addCreation(
        userId: String,
        category: CreationCategory,
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        pinned: Boolean = false
    ): Result<Unit> = addCreationReturningId(userId, category, title, content, tags, pinned).map { Unit }

    suspend fun addCreationReturningId(
        userId: String,
        category: CreationCategory,
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        pinned: Boolean = false
    ): Result<String> {
        var docPath = "users/$userId/creations"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            val cleanTitle = title.trim().take(140)
            val cleanContent = content.trim().take(15000)
            require(cleanTitle.isNotBlank() && cleanContent.isNotBlank()) {
                "Titre et contenu obligatoires."
            }
            val cleanTags = tags.map { it.trim().take(24) }.filter { it.isNotBlank() }.take(8)
            val docRef = usersDoc(uid).collection("creations").document()
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "category" to category.wireValue,
                "title" to cleanTitle,
                "content" to cleanContent,
                "tags" to cleanTags,
                "pinned" to pinned,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateCreation(
        userId: String,
        creationId: String,
        title: String,
        content: String,
        tags: List<String>
    ): Result<Unit> {
        val docPath = "users/$userId/creations/$creationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            require(creationId.isNotBlank()) { "ID de création invalide." }
            val cleanTitle = title.trim().take(140)
            val cleanContent = content.trim().take(15000)
            require(cleanTitle.isNotBlank() && cleanContent.isNotBlank()) {
                "Titre et contenu obligatoires."
            }
            val cleanTags = tags.map { it.trim().take(24) }.filter { it.isNotBlank() }.take(8)
            usersDoc(uid).collection("creations").document(creationId)
                .update(
                    mapOf(
                        "title" to cleanTitle,
                        "content" to cleanContent,
                        "tags" to cleanTags,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleCreationPinned(userId: String, creationId: String, currentPinned: Boolean): Result<Unit> {
        val docPath = "users/$userId/creations/$creationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            require(creationId.isNotBlank()) { "ID de création invalide." }
            usersDoc(uid).collection("creations").document(creationId)
                .update(
                    mapOf(
                        "pinned" to !currentPinned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCreation(userId: String, creationId: String): Result<Unit> {
        val docPath = "users/$userId/creations/$creationId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Utilisateur différent." }
            require(creationId.isNotBlank()) { "ID de création invalide." }
            usersDoc(uid).collection("creations").document(creationId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
