package com.example.data

import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.BotStrategy
import com.example.model.BudgetEntry
import com.example.model.JournalEntry
import com.example.model.MusicRevenue
import com.example.model.PriceAlert
import com.example.model.SaasProject
import com.example.model.SavingsGoal
import com.example.model.SimPosition
import com.example.model.SimTrade
import com.example.model.SimWallet
import com.example.model.WatchlistAsset
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

class FinanceProjectsRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("Connexion Google requise avant d'accéder aux données.")

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private inline fun <reified T> observeCollection(
        userId: String,
        collectionName: String,
        orderField: String = "createdAt",
        descending: Boolean = true
    ): Flow<List<T>> = flow {
        val path = "users/$userId/$collectionName"
        val direction = if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
        emitAll(
            userDoc(userId).collection(collectionName)
                .orderBy(orderField, direction)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val ex = firestoreError ?: (error as? Exception)
                    if (ex != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(ex, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // Watchlist & Live Market Defaults
    fun observeWatchlist(userId: String): Flow<List<WatchlistAsset>> =
        observeCollection(userId, "watchlist", orderField = "createdAt", descending = false)

    suspend fun ensureDefaults(userId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val watchRef = userDoc(uid).collection("watchlist")
        if (watchRef.limit(1).get().await().isEmpty) {
            val defaults = listOf(
                Triple("BTC", "Bitcoin", "crypto" to "bitcoin"),
                Triple("ETH", "Ethereum", "crypto" to "ethereum"),
                Triple("SOL", "Solana", "crypto" to "solana"),
                Triple("PAXG", "Or (PAX Gold)", "gold" to "pax-gold"),
                Triple("AAPL", "Apple", "stock" to "AAPL"),
                Triple("SPY", "S&P 500 ETF", "stock" to "SPY")
            )
            val batch = db.batch()
            defaults.forEach { (sym, name, typeApi) ->
                val payload = mapOf(
                    "userId" to uid,
                    "symbol" to sym,
                    "name" to name,
                    "assetType" to typeApi.first,
                    "apiId" to typeApi.second,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(watchRef.document(sym), payload)
            }
            batch.commit().await()
        }

        val walletRef = userDoc(uid).collection("sim_wallet").document("main")
        if (!walletRef.get().await().exists()) {
            walletRef.set(
                mapOf(
                    "userId" to uid,
                    "cashEur" to 1000.0,
                    "initialEur" to 1000.0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/watchlist")
        Result.failure(e)
    }

    suspend fun toggleWatchlistAsset(
        userId: String,
        symbol: String,
        name: String,
        assetType: String,
        apiId: String,
        currentlySaved: Boolean
    ): Result<Unit> {
        val cleanId = symbol.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(24)
        val path = "users/$userId/watchlist/$cleanId"
        return try {
            val uid = requireUserId()
            check(uid == userId)
            val docRef = userDoc(uid).collection("watchlist").document(cleanId)
            if (currentlySaved) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "symbol" to symbol.take(24),
                        "name" to name.take(80),
                        "assetType" to assetType,
                        "apiId" to apiId.take(80),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // Price Alerts
    fun observeAlerts(userId: String): Flow<List<PriceAlert>> =
        observeCollection(userId, "price_alerts")

    suspend fun addPriceAlert(
        userId: String,
        symbol: String,
        assetName: String,
        apiId: String,
        assetType: String,
        targetPrice: Double,
        above: Boolean
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        require(targetPrice > 0.0)
        val docRef = userDoc(uid).collection("price_alerts").document()
        docRef.set(
            mapOf(
                "userId" to uid,
                "symbol" to symbol.take(24),
                "assetName" to assetName.take(80),
                "apiId" to apiId.take(80),
                "assetType" to assetType,
                "targetPrice" to targetPrice,
                "above" to above,
                "triggered" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/price_alerts")
        Result.failure(e)
    }

    suspend fun markAlertTriggered(userId: String, alertId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        userDoc(uid).collection("price_alerts").document(alertId)
            .update(mapOf("triggered" to true, "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/price_alerts/$alertId")
        Result.failure(e)
    }

    suspend fun deleteDoc(userId: String, collection: String, docId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        userDoc(uid).collection(collection).document(docId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/$userId/$collection/$docId")
        Result.failure(e)
    }

    // Simulated Paper Portfolio
    fun observeSimWallet(userId: String): Flow<SimWallet> = flow {
        val path = "users/$userId/sim_wallet/main"
        emitAll(
            userDoc(userId).collection("sim_wallet").document("main")
                .snapshots()
                .map { snap ->
                    snap.toObject(SimWallet::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: SimWallet(userId = userId)
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

    fun observeSimPositions(userId: String): Flow<List<SimPosition>> =
        observeCollection(userId, "sim_positions")

    fun observeSimTrades(userId: String): Flow<List<SimTrade>> =
        observeCollection(userId, "sim_trades")

    suspend fun executeSimulatedTrade(
        userId: String,
        symbol: String,
        assetName: String,
        assetType: String,
        side: String,
        amountEur: Double,
        unitPriceEur: Double
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        require(amountEur > 0.0 && unitPriceEur > 0.0) { "Montant et prix doivent être positifs." }

        val walletRef = userDoc(uid).collection("sim_wallet").document("main")
        val walletSnap = walletRef.get().await()
        val currentCash = (walletSnap.getDouble("cashEur")) ?: 1000.0
        val cleanId = symbol.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(24)
        val posRef = userDoc(uid).collection("sim_positions").document(cleanId)
        val posSnap = posRef.get().await()
        val existingQty = posSnap.getDouble("quantity") ?: 0.0
        val existingAvg = posSnap.getDouble("avgBuyPrice") ?: 0.0
        val qty = amountEur / unitPriceEur

        val batch = db.batch()
        if (side == "buy") {
            require(currentCash >= amountEur) { "Solde fictif insuffisant (${String.format("%.2f", currentCash)} € disponibles)." }
            batch.update(walletRef, mapOf("cashEur" to (currentCash - amountEur), "updatedAt" to FieldValue.serverTimestamp()))
            val newQty = existingQty + qty
            val newAvg = if (newQty > 0.0) ((existingQty * existingAvg) + (qty * unitPriceEur)) / newQty else unitPriceEur
            if (posSnap.exists()) {
                batch.update(posRef, mapOf("quantity" to newQty, "avgBuyPrice" to newAvg, "updatedAt" to FieldValue.serverTimestamp()))
            } else {
                batch.set(
                    posRef,
                    mapOf(
                        "userId" to uid,
                        "symbol" to symbol,
                        "assetName" to assetName,
                        "assetType" to assetType,
                        "quantity" to newQty,
                        "avgBuyPrice" to newAvg,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        } else {
            require(existingQty > 0.0) { "Aucune position fictive à vendre sur $symbol." }
            val qtyToSell = qty.coerceAtMost(existingQty)
            val actualEur = qtyToSell * unitPriceEur
            val remaining = (existingQty - qtyToSell).coerceAtLeast(0.0)
            batch.update(walletRef, mapOf("cashEur" to (currentCash + actualEur), "updatedAt" to FieldValue.serverTimestamp()))
            if (remaining <= 0.0000001) {
                batch.delete(posRef)
            } else {
                batch.update(posRef, mapOf("quantity" to remaining, "updatedAt" to FieldValue.serverTimestamp()))
            }
        }

        val tradeRef = userDoc(uid).collection("sim_trades").document()
        batch.set(
            tradeRef,
            mapOf(
                "userId" to uid,
                "symbol" to symbol,
                "assetName" to assetName,
                "side" to side,
                "quantity" to qty,
                "price" to unitPriceEur,
                "totalEur" to amountEur,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/sim_trades")
        Result.failure(e)
    }

    suspend fun resetSimulatedWallet(userId: String): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val batch = db.batch()
        val posSnap = userDoc(uid).collection("sim_positions").get().await()
        posSnap.documents.forEach { batch.delete(it.reference) }
        val walletRef = userDoc(uid).collection("sim_wallet").document("main")
        batch.set(
            walletRef,
            mapOf(
                "userId" to uid,
                "cashEur" to 1000.0,
                "initialEur" to 1000.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/$userId/sim_wallet/main")
        Result.failure(e)
    }

    // Trading Journal
    fun observeJournal(userId: String): Flow<List<JournalEntry>> =
        observeCollection(userId, "journal_entries")

    suspend fun addJournalEntry(
        userId: String,
        asset: String,
        direction: String,
        entryPrice: String,
        exitPrice: String,
        stopLoss: String,
        reason: String,
        emotion: String,
        outcome: String,
        aiFeedback: String = ""
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        require(asset.isNotBlank() && reason.isNotBlank())
        val docRef = userDoc(uid).collection("journal_entries").document()
        docRef.set(
            mapOf(
                "userId" to uid,
                "asset" to asset.trim().take(40),
                "direction" to direction,
                "entryPrice" to entryPrice.trim().take(40),
                "exitPrice" to exitPrice.trim().take(40),
                "stopLoss" to stopLoss.trim().take(40),
                "reason" to reason.trim().take(2000),
                "emotion" to emotion.trim().take(200),
                "outcome" to outcome.trim().take(500),
                "aiFeedback" to aiFeedback.trim().take(6000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/journal_entries")
        Result.failure(e)
    }

    // Auto Trading Bot Strategies
    fun observeBotStrategies(userId: String): Flow<List<BotStrategy>> =
        observeCollection(userId, "bot_strategies")

    suspend fun createBotStrategy(userId: String, strategy: BotStrategy): Result<String> = try {
        val uid = requireUserId()
        check(uid == userId)
        val docRef = userDoc(uid).collection("bot_strategies").document()
        docRef.set(
            mapOf(
                "userId" to uid,
                "name" to strategy.name.trim().take(100),
                "description" to strategy.description.trim().take(1000),
                "assetSymbol" to strategy.assetSymbol.trim().take(24),
                "apiId" to strategy.apiId.trim().take(80),
                "buyDropPct" to strategy.buyDropPct.coerceIn(0.1, 95.0),
                "takeProfitPct" to strategy.takeProfitPct.coerceIn(0.5, 1000.0),
                "stopLossPct" to strategy.stopLossPct.coerceIn(0.5, 95.0),
                "amountPerTradeEur" to strategy.amountPerTradeEur.coerceAtLeast(1.0),
                "maxTotalLossEur" to strategy.maxTotalLossEur.coerceAtLeast(1.0),
                "maxTradesCount" to strategy.maxTradesCount.coerceAtLeast(1L),
                "mode" to strategy.mode,
                "status" to strategy.status,
                "tradesExecuted" to strategy.tradesExecuted,
                "winsCount" to strategy.winsCount,
                "lossesCount" to strategy.lossesCount,
                "cumulativePnlEur" to strategy.cumulativePnlEur,
                "referencePrice" to strategy.referencePrice.coerceAtLeast(0.0),
                "lastActionNote" to strategy.lastActionNote.take(1000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(docRef.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/bot_strategies")
        Result.failure(e)
    }

    suspend fun updateBotStrategyState(
        userId: String,
        strategyId: String,
        updates: Map<String, Any>
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val safeUpdates = updates.toMutableMap()
        safeUpdates["updatedAt"] = FieldValue.serverTimestamp()
        userDoc(uid).collection("bot_strategies").document(strategyId).update(safeUpdates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/bot_strategies/$strategyId")
        Result.failure(e)
    }

    // Budget, Savings & Music Revenues
    fun observeBudget(userId: String): Flow<List<BudgetEntry>> =
        observeCollection(userId, "budget_entries")

    suspend fun addBudgetEntry(
        userId: String,
        kind: String,
        category: String,
        label: String,
        amountEur: Double,
        monthKey: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        require(amountEur > 0.0 && label.isNotBlank())
        userDoc(uid).collection("budget_entries").document().set(
            mapOf(
                "userId" to uid,
                "kind" to kind,
                "category" to category.trim().take(60),
                "label" to label.trim().take(120),
                "amountEur" to amountEur,
                "monthKey" to monthKey,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/budget_entries")
        Result.failure(e)
    }

    fun observeSavingsGoals(userId: String): Flow<List<SavingsGoal>> =
        observeCollection(userId, "savings_goals")

    suspend fun addSavingsGoal(
        userId: String,
        title: String,
        targetEur: Double,
        savedEur: Double
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        require(title.isNotBlank() && targetEur > 0.0)
        userDoc(uid).collection("savings_goals").document().set(
            mapOf(
                "userId" to uid,
                "title" to title.trim().take(120),
                "targetEur" to targetEur,
                "savedEur" to savedEur.coerceAtLeast(0.0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/savings_goals")
        Result.failure(e)
    }

    suspend fun updateSavingsProgress(userId: String, goalId: String, newSavedEur: Double): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        userDoc(uid).collection("savings_goals").document(goalId)
            .update(mapOf("savedEur" to newSavedEur.coerceAtLeast(0.0), "updatedAt" to FieldValue.serverTimestamp()))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/savings_goals/$goalId")
        Result.failure(e)
    }

    fun observeMusicRevenues(userId: String): Flow<List<MusicRevenue>> =
        observeCollection(userId, "music_revenues")

    suspend fun addMusicRevenue(
        userId: String,
        trackTitle: String,
        source: String,
        playsCount: Long,
        amountEur: Double,
        periodLabel: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        require(trackTitle.isNotBlank())
        userDoc(uid).collection("music_revenues").document().set(
            mapOf(
                "userId" to uid,
                "trackTitle" to trackTitle.trim().take(120),
                "source" to source,
                "playsCount" to playsCount.coerceAtLeast(0L),
                "amountEur" to amountEur.coerceAtLeast(0.0),
                "periodLabel" to periodLabel.trim().take(40),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/music_revenues")
        Result.failure(e)
    }

    // SaaS & App Projects
    fun observeSaasProjects(userId: String): Flow<List<SaasProject>> =
        observeCollection(userId, "saas_projects")

    suspend fun addSaasProject(userId: String, project: SaasProject): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        userDoc(uid).collection("saas_projects").document().set(
            mapOf(
                "userId" to uid,
                "title" to project.title.trim().take(140),
                "problem" to project.problem.trim().take(1500),
                "audience" to project.audience.trim().take(1000),
                "competition" to project.competition.trim().take(1500),
                "pricing" to project.pricing.trim().take(500),
                "businessPlan" to project.businessPlan.trim().take(8000),
                "roadmap" to project.roadmap.trim().take(8000),
                "nextAction" to project.nextAction.trim().take(500),
                "status" to project.status,
                "actionDone" to project.actionDone,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/$userId/saas_projects")
        Result.failure(e)
    }

    suspend fun updateSaasProject(
        userId: String,
        projectId: String,
        updates: Map<String, Any>
    ): Result<Unit> = try {
        val uid = requireUserId()
        check(uid == userId)
        val safe = updates.toMutableMap()
        safe["updatedAt"] = FieldValue.serverTimestamp()
        userDoc(uid).collection("saas_projects").document(projectId).update(safe).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$userId/saas_projects/$projectId")
        Result.failure(e)
    }
}
