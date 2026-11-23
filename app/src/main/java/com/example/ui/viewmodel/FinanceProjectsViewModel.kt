package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FinanceProjectsRepository
import com.example.data.HazardAiService
import com.example.data.MarketDataService
import com.example.data.SecretsRegistry
import com.example.model.BotStrategy
import com.example.model.BotStrategyDraft
import com.example.model.BudgetEntry
import com.example.model.FINANCIAL_DISCLAIMER
import com.example.model.JournalEntry
import com.example.model.MarketQuote
import com.example.model.MemoryProfile
import com.example.model.MusicRevenue
import com.example.model.NewsArticle
import com.example.model.PriceAlert
import com.example.model.SaasProject
import com.example.model.SavingsGoal
import com.example.model.ServiceConnectionStatus
import com.example.model.SimPosition
import com.example.model.SimTrade
import com.example.model.SimWallet
import com.example.model.UiState
import com.example.model.WatchlistAsset
import com.example.utils.NotificationHelper
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class FinanceProjectsViewModel(
    private val appContext: Context,
    private val repository: FinanceProjectsRepository,
    private val marketService: MarketDataService,
    private val aiService: HazardAiService,
    private val currentUserId: String
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

    val watchlistState: StateFlow<UiState<List<WatchlistAsset>>> =
        repository.observeWatchlist(currentUserId).toUiState("Impossible de charger la watchlist.")

    val alertsState: StateFlow<UiState<List<PriceAlert>>> =
        repository.observeAlerts(currentUserId).toUiState("Impossible de charger les alertes.")

    val simWalletState: StateFlow<UiState<SimWallet>> =
        repository.observeSimWallet(currentUserId).toUiState("Impossible de charger le portefeuille fictif.")

    val simPositionsState: StateFlow<UiState<List<SimPosition>>> =
        repository.observeSimPositions(currentUserId).toUiState("Impossible de charger les positions fictives.")

    val simTradesState: StateFlow<UiState<List<SimTrade>>> =
        repository.observeSimTrades(currentUserId).toUiState("Impossible de charger l'historique simulé.")

    val journalState: StateFlow<UiState<List<JournalEntry>>> =
        repository.observeJournal(currentUserId).toUiState("Impossible de charger le journal de trading.")

    val botStrategiesState: StateFlow<UiState<List<BotStrategy>>> =
        repository.observeBotStrategies(currentUserId).toUiState("Impossible de charger les stratégies du bot.")

    val budgetState: StateFlow<UiState<List<BudgetEntry>>> =
        repository.observeBudget(currentUserId).toUiState("Impossible de charger le budget.")

    val savingsGoalsState: StateFlow<UiState<List<SavingsGoal>>> =
        repository.observeSavingsGoals(currentUserId).toUiState("Impossible de charger les objectifs d'épargne.")

    val musicRevenuesState: StateFlow<UiState<List<MusicRevenue>>> =
        repository.observeMusicRevenues(currentUserId).toUiState("Impossible de charger les revenus musique.")

    val saasProjectsState: StateFlow<UiState<List<SaasProject>>> =
        repository.observeSaasProjects(currentUserId).toUiState("Impossible de charger les projets SaaS.")

    private val _quotes = MutableStateFlow<List<MarketQuote>>(emptyList())
    val quotes: StateFlow<List<MarketQuote>> = _quotes.asStateFlow()

    private val _quotesProvider = MutableStateFlow("En attente")
    val quotesProvider: StateFlow<String> = _quotesProvider.asStateFlow()

    private val _quotesWarning = MutableStateFlow<String?>(null)
    val quotesWarning: StateFlow<String?> = _quotesWarning.asStateFlow()

    private val _isLoadingQuotes = MutableStateFlow(false)
    val isLoadingQuotes: StateFlow<Boolean> = _isLoadingQuotes.asStateFlow()

    private val _newsArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val newsArticles: StateFlow<List<NewsArticle>> = _newsArticles.asStateFlow()

    private val _newsProvider = MutableStateFlow("")
    val newsProvider: StateFlow<String> = _newsProvider.asStateFlow()

    private val _newsSummary = MutableStateFlow<String?>(null)
    val newsSummary: StateFlow<String?> = _newsSummary.asStateFlow()

    private val _isLoadingNews = MutableStateFlow(false)
    val isLoadingNews: StateFlow<Boolean> = _isLoadingNews.asStateFlow()

    private val _scamVerdict = MutableStateFlow<String?>(null)
    val scamVerdict: StateFlow<String?> = _scamVerdict.asStateFlow()

    private val _isCheckingScam = MutableStateFlow(false)
    val isCheckingScam: StateFlow<Boolean> = _isCheckingScam.asStateFlow()

    private val _journalAnalysis = MutableStateFlow<String?>(null)
    val journalAnalysis: StateFlow<String?> = _journalAnalysis.asStateFlow()

    private val _isAnalyzingJournal = MutableStateFlow(false)
    val isAnalyzingJournal: StateFlow<Boolean> = _isAnalyzingJournal.asStateFlow()

    private val _financialPlan = MutableStateFlow<String?>(null)
    val financialPlan: StateFlow<String?> = _financialPlan.asStateFlow()

    private val _isBuildingPlan = MutableStateFlow(false)
    val isBuildingPlan: StateFlow<Boolean> = _isBuildingPlan.asStateFlow()

    private val _botDraft = MutableStateFlow<BotStrategyDraft?>(null)
    val botDraft: StateFlow<BotStrategyDraft?> = _botDraft.asStateFlow()

    private val _isDraftingBot = MutableStateFlow(false)
    val isDraftingBot: StateFlow<Boolean> = _isDraftingBot.asStateFlow()

    private val _isGeneratingSaas = MutableStateFlow(false)
    val isGeneratingSaas: StateFlow<Boolean> = _isGeneratingSaas.asStateFlow()

    private val _isGeneratingGoalActions = MutableStateFlow(false)
    val isGeneratingGoalActions: StateFlow<Boolean> = _isGeneratingGoalActions.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val botJobs = ConcurrentHashMap<String, Job>()

    init {
        viewModelScope.launch {
            repository.ensureDefaults(currentUserId)
        }
        viewModelScope.launch {
            watchlistState.collect { state ->
                if (state is UiState.Success && state.data.isNotEmpty() && _quotes.value.isEmpty()) {
                    refreshQuotes(forceRefresh = false, assetsOverride = state.data)
                }
            }
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun connectionStatuses(): List<ServiceConnectionStatus> =
        SecretsRegistry.connectionStatuses()

    fun hasExchangeTradeKeys(): Boolean =
        SecretsRegistry.hasExchangeTradeKeys()

    fun refreshQuotes(
        forceRefresh: Boolean = false,
        assetsOverride: List<WatchlistAsset>? = null
    ) {
        val assets = assetsOverride
            ?: (watchlistState.value as? UiState.Success)?.data.orEmpty()
        if (assets.isEmpty()) return

        viewModelScope.launch {
            _isLoadingQuotes.value = true
            try {
                val res = marketService.fetchLiveQuotes(assets, forceRefresh = forceRefresh)
                _quotes.value = res.quotes
                _quotesProvider.value = res.providerSummary
                _quotesWarning.value = res.warning
                checkPriceAlerts(res.quotes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "refreshQuotes failed", e)
                _quotesWarning.value = "Erreur réseau lors de la mise à jour des cours."
            } finally {
                _isLoadingQuotes.value = false
            }
        }
    }

    private suspend fun checkPriceAlerts(quotes: List<MarketQuote>) {
        val alerts = (alertsState.value as? UiState.Success)?.data.orEmpty()
        if (alerts.isEmpty()) return
        val quoteBySymbol = quotes.associateBy { it.symbol.uppercase() }
        for (alert in alerts) {
            if (alert.triggered) continue
            val quote = quoteBySymbol[alert.symbol.uppercase()] ?: continue
            val hit = if (alert.above) {
                quote.priceEur >= alert.targetPrice
            } else {
                quote.priceEur <= alert.targetPrice
            }
            if (hit) {
                repository.markAlertTriggered(currentUserId, alert.id)
                val dir = if (alert.above) "au-dessus de" else "en dessous de"
                val msg = "${alert.symbol} à ${String.format("%.2f", quote.priceEur)} € ($dir ${String.format("%.2f", alert.targetPrice)} €)."
                _bannerMessage.value = "🔔 Alerte prix : $msg"
                NotificationHelper.sendNotification(
                    context = appContext,
                    title = "Alerte Prix Hazard · ${alert.symbol}",
                    message = msg,
                    notificationId = alert.id.hashCode()
                )
            }
        }
    }

    fun addCustomAsset(symbol: String, name: String, assetType: String, apiId: String) {
        val cleanSym = symbol.trim().uppercase()
        val cleanName = name.trim().ifBlank { cleanSym }
        val cleanApiId = apiId.trim().ifBlank { cleanSym.lowercase() }
        if (cleanSym.isBlank()) return
        viewModelScope.launch {
            val res = repository.toggleWatchlistAsset(
                userId = currentUserId,
                symbol = cleanSym,
                name = cleanName,
                assetType = assetType,
                apiId = cleanApiId,
                currentlySaved = false
            )
            if (res.isSuccess) {
                _bannerMessage.value = "$cleanSym ajouté aux favoris."
                refreshQuotes(forceRefresh = true)
            } else {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Ajout impossible."
            }
        }
    }

    fun removeWatchlistAsset(asset: WatchlistAsset) {
        viewModelScope.launch {
            repository.toggleWatchlistAsset(
                userId = currentUserId,
                symbol = asset.symbol,
                name = asset.name,
                assetType = asset.assetType,
                apiId = asset.apiId,
                currentlySaved = true
            )
        }
    }

    fun addPriceAlert(quote: MarketQuote, targetPrice: Double, above: Boolean) {
        viewModelScope.launch {
            val res = repository.addPriceAlert(
                userId = currentUserId,
                symbol = quote.symbol,
                assetName = quote.name,
                apiId = quote.apiId,
                assetType = quote.assetType,
                targetPrice = targetPrice,
                above = above
            )
            _bannerMessage.value = if (res.isSuccess) {
                "Alerte créée sur ${quote.symbol} (${String.format("%.2f", targetPrice)} €)."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Impossible de créer l'alerte."
            }
        }
    }

    fun deleteDoc(collection: String, docId: String) {
        viewModelScope.launch {
            repository.deleteDoc(currentUserId, collection, docId)
        }
    }

    // Simulated Portfolio Actions
    fun executeSimulatedOrder(quote: MarketQuote, side: String, amountEur: Double) {
        viewModelScope.launch {
            val res = repository.executeSimulatedTrade(
                userId = currentUserId,
                symbol = quote.symbol,
                assetName = quote.name,
                assetType = quote.assetType,
                side = side,
                amountEur = amountEur,
                unitPriceEur = quote.priceEur
            )
            _bannerMessage.value = if (res.isSuccess) {
                val verb = if (side == "buy") "Achat simulé" else "Vente simulée"
                "$verb de ${String.format("%.2f", amountEur)} € sur ${quote.symbol} enregistré."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Ordre simulé refusé."
            }
        }
    }

    fun resetSimulatedWallet() {
        viewModelScope.launch {
            val res = repository.resetSimulatedWallet(currentUserId)
            _bannerMessage.value = if (res.isSuccess) {
                "Portefeuille fictif réinitialisé à 1 000 €."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Erreur réinitialisation."
            }
        }
    }

    // Journal + AI Analysis
    fun addJournalEntry(
        asset: String,
        direction: String,
        entryPrice: String,
        exitPrice: String,
        stopLoss: String,
        reason: String,
        emotion: String,
        outcome: String
    ) {
        viewModelScope.launch {
            val res = repository.addJournalEntry(
                userId = currentUserId,
                asset = asset,
                direction = direction,
                entryPrice = entryPrice,
                exitPrice = exitPrice,
                stopLoss = stopLoss,
                reason = reason,
                emotion = emotion,
                outcome = outcome
            )
            _bannerMessage.value = if (res.isSuccess) {
                "Entrée ajoutée au journal de trading."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Erreur journal."
            }
        }
    }

    fun analyzeJournal() {
        val entries = (journalState.value as? UiState.Success)?.data.orEmpty()
        if (entries.isEmpty()) {
            _bannerMessage.value = "Ajoute d'abord au moins 1 trade dans le journal."
            return
        }
        viewModelScope.launch {
            _isAnalyzingJournal.value = true
            try {
                _journalAnalysis.value = aiService.analyzeTradingJournal(entries)
            } catch (e: TimeoutCancellationException) {
                _bannerMessage.value = "Analyse du journal expirée."
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Erreur d'analyse du journal."
            } finally {
                _isAnalyzingJournal.value = false
            }
        }
    }

    // Economic News & Scam Detector
    fun refreshEconomicNews(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingNews.value = true
            try {
                val res = marketService.fetchEconomicNews(forceRefresh = forceRefresh)
                _newsArticles.value = res.articles
                _newsProvider.value = res.provider
                res.warning?.let { _bannerMessage.value = it }
                if (res.articles.isNotEmpty()) {
                    _newsSummary.value = aiService.summarizeEconomicNews(res.articles)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Erreur lors du résumé des actualités."
            } finally {
                _isLoadingNews.value = false
            }
        }
    }

    fun checkScam(suspiciousText: String) {
        val clean = suspiciousText.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _isCheckingScam.value = true
            try {
                _scamVerdict.value = aiService.detectScam(clean)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Erreur d'analyse anti-arnaque."
            } finally {
                _isCheckingScam.value = false
            }
        }
    }

    // Automated Trading Bot
    fun draftBotStrategyFromDescription(description: String) {
        val clean = description.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            _isDraftingBot.value = true
            try {
                _botDraft.value = aiService.parseBotStrategy(clean)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Impossible d'interpréter la stratégie."
            } finally {
                _isDraftingBot.value = false
            }
        }
    }

    fun clearBotDraft() {
        _botDraft.value = null
    }

    fun saveBotStrategy(
        name: String,
        description: String,
        assetSymbol: String,
        apiId: String,
        buyDropPct: Double,
        takeProfitPct: Double,
        stopLossPct: Double,
        amountPerTradeEur: Double,
        maxTotalLossEur: Double,
        maxTradesCount: Long
    ) {
        viewModelScope.launch {
            val strategy = BotStrategy(
                userId = currentUserId,
                name = name.ifBlank { "Bot $assetSymbol" },
                description = description,
                assetSymbol = assetSymbol.uppercase(),
                apiId = apiId.lowercase(),
                buyDropPct = buyDropPct,
                takeProfitPct = takeProfitPct,
                stopLossPct = stopLossPct,
                amountPerTradeEur = amountPerTradeEur,
                maxTotalLossEur = maxTotalLossEur,
                maxTradesCount = maxTradesCount,
                mode = "simulation",
                status = "idle"
            )
            val res = repository.createBotStrategy(currentUserId, strategy)
            if (res.isSuccess) {
                _botDraft.value = null
                _bannerMessage.value = "Stratégie enregistrée en Simulation."
            } else {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Erreur sauvegarde stratégie."
            }
        }
    }

    fun setBotMode(strategy: BotStrategy, targetMode: String) {
        if (targetMode == "real" && !SecretsRegistry.hasExchangeTradeKeys()) {
            _bannerMessage.value =
                "Mode Réel bloqué : ajoute EXCHANGE_API_KEY et EXCHANGE_API_SECRET (droit trading sans retrait) dans les Secrets AI Studio."
            return
        }
        viewModelScope.launch {
            repository.updateBotStrategyState(
                currentUserId,
                strategy.id,
                mapOf(
                    "mode" to targetMode,
                    "status" to "stopped",
                    "lastActionNote" to if (targetMode == "real") {
                        "Mode RÉEL armé (prêt au démarrage manuel)."
                    } else {
                        "Basculé en SIMULATION (argent fictif)."
                    }
                )
            )
            botJobs.remove(strategy.id)?.cancel()
            _bannerMessage.value = if (targetMode == "real") {
                "⚠️ Mode RÉEL activé sur ${strategy.name}. Utilise STOP à tout moment."
            } else {
                "Mode SIMULATION activé sur ${strategy.name}."
            }
        }
    }

    fun startBotStrategy(strategy: BotStrategy) {
        if (strategy.mode == "real" && !SecretsRegistry.hasExchangeTradeKeys()) {
            _bannerMessage.value = "Impossible de lancer en Réel : clés échange absentes."
            return
        }
        botJobs.remove(strategy.id)?.cancel()
        val job = viewModelScope.launch {
            repository.updateBotStrategyState(
                currentUserId,
                strategy.id,
                mapOf(
                    "status" to "running",
                    "lastActionNote" to "Bot démarré (${strategy.mode.uppercase()}) : surveillance de ${strategy.assetSymbol}..."
                )
            )
            NotificationHelper.sendNotification(
                context = appContext,
                title = "Bot Hazard démarré · ${strategy.name}",
                message = "Surveillance ${strategy.assetSymbol} en mode ${strategy.mode.uppercase()}.",
                notificationId = strategy.id.hashCode()
            )

            var currentState = strategy.copy(status = "running")
            while (isActive) {
                if (currentState.tradesExecuted >= currentState.maxTradesCount ||
                    currentState.cumulativePnlEur <= -currentState.maxTotalLossEur
                ) {
                    val stopReason = "Limite atteinte (trades=${currentState.tradesExecuted}/${currentState.maxTradesCount}, PnL=${String.format("%.2f", currentState.cumulativePnlEur)} €)."
                    repository.updateBotStrategyState(
                        currentUserId,
                        strategy.id,
                        mapOf("status" to "stopped", "lastActionNote" to stopReason)
                    )
                    NotificationHelper.sendNotification(
                        context = appContext,
                        title = "Bot Hazard arrêté · ${strategy.name}",
                        message = stopReason,
                        notificationId = strategy.id.hashCode() + 1
                    )
                    break
                }

                currentState = evaluateBotTick(currentState, forceSimulatedTest = false)
                delay(25_000L)
                // Re-sync status from latest Firestore stream in case user hit STOP
                val latest = (botStrategiesState.value as? UiState.Success)?.data
                    ?.firstOrNull { it.id == strategy.id }
                if (latest == null || latest.status != "running") break
                currentState = latest
            }
        }
        botJobs[strategy.id] = job
    }

    fun runOneSimulatedTestCycle(strategy: BotStrategy) {
        viewModelScope.launch {
            evaluateBotTick(strategy, forceSimulatedTest = true)
        }
    }

    private suspend fun evaluateBotTick(
        strategy: BotStrategy,
        forceSimulatedTest: Boolean
    ): BotStrategy {
        val targetAsset = WatchlistAsset(
            symbol = strategy.assetSymbol,
            name = strategy.assetSymbol,
            assetType = if (strategy.assetSymbol.equals("PAXG", true)) "gold" else "crypto",
            apiId = strategy.apiId
        )
        val feed = marketService.fetchLiveQuotes(listOf(targetAsset), forceRefresh = false)
        val liveQuote = feed.quotes.firstOrNull()
        val livePrice = liveQuote?.priceEur ?: return strategy

        val ref = if (strategy.referencePrice <= 0.0) livePrice else strategy.referencePrice
        val positions = (simPositionsState.value as? UiState.Success)?.data.orEmpty()
        val openPos = positions.firstOrNull { it.symbol.equals(strategy.assetSymbol, ignoreCase = true) }

        var nextTrades = strategy.tradesExecuted
        var nextWins = strategy.winsCount
        var nextLosses = strategy.lossesCount
        var nextPnl = strategy.cumulativePnlEur
        var nextRef = ref.coerceAtLeast(livePrice)
        var actionNote = "Prix ${strategy.assetSymbol} = ${String.format("%.2f", livePrice)} € (Réf ${String.format("%.2f", nextRef)} €). En attente de -${strategy.buyDropPct}%."

        if (openPos == null) {
            val dropPct = ((ref - livePrice) / ref) * 100.0
            if (dropPct >= strategy.buyDropPct || forceSimulatedTest) {
                val ok = if (strategy.mode == "real" && !forceSimulatedTest) {
                    val realRes = marketService.executeRealSpotOrder(strategy.assetSymbol, "buy", strategy.amountPerTradeEur)
                    if (realRes.isFailure) {
                        val err = realRes.exceptionOrNull()?.message ?: "Erreur ordre réel"
                        repository.updateBotStrategyState(
                            currentUserId, strategy.id,
                            mapOf("status" to "stopped", "lastActionNote" to "Arrêt auto : $err")
                        )
                        NotificationHelper.sendNotification(appContext, "Bot Réel stoppé", err, strategy.id.hashCode() + 2)
                        return strategy.copy(status = "stopped", lastActionNote = err)
                    }
                    true
                } else {
                    repository.executeSimulatedTrade(
                        userId = currentUserId,
                        symbol = strategy.assetSymbol,
                        assetName = strategy.name,
                        assetType = targetAsset.assetType,
                        side = "buy",
                        amountEur = strategy.amountPerTradeEur,
                        unitPriceEur = livePrice
                    ).isSuccess
                }

                if (ok) {
                    nextTrades += 1
                    nextRef = livePrice
                    val reason = if (forceSimulatedTest) {
                        "Cycle test simulé : achat déclenché à ${String.format("%.2f", livePrice)} €."
                    } else {
                        "Repli de ${String.format("%.2f", dropPct)}% >= ${strategy.buyDropPct}% détecté : achat automatique de ${String.format("%.2f", strategy.amountPerTradeEur)} €."
                    }
                    actionNote = "ACHAT exécuté (${strategy.assetSymbol} @ ${String.format("%.2f", livePrice)} €)."
                    logBotAction(strategy, "buy", livePrice, reason)
                }
            }
        } else {
            val changeSinceBuyPct = ((livePrice - openPos.avgBuyPrice) / openPos.avgBuyPrice) * 100.0
            val shouldSellTp = changeSinceBuyPct >= strategy.takeProfitPct
            val shouldSellSl = changeSinceBuyPct <= -strategy.stopLossPct
            if (shouldSellTp || shouldSellSl || forceSimulatedTest) {
                val positionEur = openPos.quantity * livePrice
                val realizedPnl = (livePrice - openPos.avgBuyPrice) * openPos.quantity
                val ok = if (strategy.mode == "real" && !forceSimulatedTest) {
                    val realRes = marketService.executeRealSpotOrder(strategy.assetSymbol, "sell", positionEur)
                    if (realRes.isFailure) {
                        val err = realRes.exceptionOrNull()?.message ?: "Erreur vente réelle"
                        repository.updateBotStrategyState(
                            currentUserId, strategy.id,
                            mapOf("status" to "stopped", "lastActionNote" to "Arrêt auto : $err")
                        )
                        return strategy.copy(status = "stopped", lastActionNote = err)
                    }
                    true
                } else {
                    repository.executeSimulatedTrade(
                        userId = currentUserId,
                        symbol = strategy.assetSymbol,
                        assetName = strategy.name,
                        assetType = targetAsset.assetType,
                        side = "sell",
                        amountEur = positionEur,
                        unitPriceEur = livePrice
                    ).isSuccess
                }
                if (ok) {
                    nextTrades += 1
                    nextPnl += realizedPnl
                    if (realizedPnl >= 0.0) nextWins += 1 else nextLosses += 1
                    nextRef = livePrice
                    val tag = if (shouldSellTp) "Take-Profit" else if (shouldSellSl) "Stop-Loss" else "Clôture test"
                    val reason = "$tag sur ${strategy.assetSymbol} : variation ${String.format("%.2f", changeSinceBuyPct)}% -> PnL ${String.format("%.2f", realizedPnl)} €."
                    actionNote = "VENTE ($tag) · PnL ${String.format("%.2f", realizedPnl)} €."
                    logBotAction(strategy, "sell", livePrice, reason)
                }
            } else {
                actionNote = "Position ouverte sur ${strategy.assetSymbol} (${String.format("%+.2f", changeSinceBuyPct)}%). TP +${strategy.takeProfitPct}% / SL -${strategy.stopLossPct}%."
            }
        }

        repository.updateBotStrategyState(
            currentUserId,
            strategy.id,
            mapOf(
                "tradesExecuted" to nextTrades,
                "winsCount" to nextWins,
                "lossesCount" to nextLosses,
                "cumulativePnlEur" to nextPnl,
                "referencePrice" to nextRef,
                "lastActionNote" to actionNote
            )
        )
        return strategy.copy(
            tradesExecuted = nextTrades,
            winsCount = nextWins,
            lossesCount = nextLosses,
            cumulativePnlEur = nextPnl,
            referencePrice = nextRef,
            lastActionNote = actionNote
        )
    }

    private suspend fun logBotAction(
        strategy: BotStrategy,
        side: String,
        priceEur: Double,
        explanation: String
    ) {
        val sideFr = if (side == "buy") "ACHAT BOT (${strategy.mode.uppercase()})" else "VENTE BOT (${strategy.mode.uppercase()})"
        repository.addJournalEntry(
            userId = currentUserId,
            asset = strategy.assetSymbol,
            direction = sideFr,
            entryPrice = String.format("%.2f €", priceEur),
            exitPrice = if (side == "sell") String.format("%.2f €", priceEur) else "",
            stopLoss = "-${strategy.stopLossPct}%",
            reason = "${strategy.name} : $explanation",
            emotion = "Automatique (sans émotion)",
            outcome = "Montant ${String.format("%.2f", strategy.amountPerTradeEur)} €",
            aiFeedback = "$explanation\n$FINANCIAL_DISCLAIMER"
        )
        _bannerMessage.value = "🤖 $sideFr · ${strategy.assetSymbol}"
        NotificationHelper.sendNotification(
            context = appContext,
            title = "Bot Hazard · $sideFr",
            message = explanation,
            notificationId = abs((strategy.id + System.currentTimeMillis()).hashCode())
        )
    }

    fun stopAllBotsImmediately() {
        val strategies = (botStrategiesState.value as? UiState.Success)?.data.orEmpty()
        botJobs.values.forEach { it.cancel() }
        botJobs.clear()
        viewModelScope.launch {
            strategies.filter { it.status == "running" }.forEach { strat ->
                repository.updateBotStrategyState(
                    currentUserId,
                    strat.id,
                    mapOf(
                        "status" to "stopped",
                        "lastActionNote" to "Arrêt d'urgence STOP déclenché par l'utilisateur."
                    )
                )
            }
            _bannerMessage.value = "🛑 STOP IMMÉDIAT : Tous les bots arrêtés."
            NotificationHelper.sendNotification(
                context = appContext,
                title = "Hazard Bot · STOP IMMÉDIAT",
                message = "Tous les robots de trading ont été arrêtés immédiatement.",
                notificationId = 9001
            )
        }
    }

    // Budget, Savings, Financial Plan, Music Revenues
    fun addBudgetEntry(kind: String, category: String, label: String, amountEur: Double, monthKey: String) {
        viewModelScope.launch {
            val res = repository.addBudgetEntry(currentUserId, kind, category, label, amountEur, monthKey)
            if (res.isFailure) {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Erreur budget."
            }
        }
    }

    fun addSavingsGoal(title: String, targetEur: Double, savedEur: Double) {
        viewModelScope.launch {
            val res = repository.addSavingsGoal(currentUserId, title, targetEur, savedEur)
            if (res.isFailure) {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Erreur objectif."
            }
        }
    }

    fun addSavingsAmount(goal: SavingsGoal, deltaEur: Double) {
        viewModelScope.launch {
            repository.updateSavingsProgress(currentUserId, goal.id, (goal.savedEur + deltaEur).coerceAtLeast(0.0))
        }
    }

    fun buildFinancialPlan(memory: MemoryProfile) {
        val budget = (budgetState.value as? UiState.Success)?.data.orEmpty()
        val goals = (savingsGoalsState.value as? UiState.Success)?.data.orEmpty()
        val income = budget.filter { it.kind == "income" }.sumOf { it.amountEur }
        val expense = budget.filter { it.kind == "expense" }.sumOf { it.amountEur }
        val goalsSummary = if (goals.isEmpty()) "Aucun objectif chiffré encore saisi" else {
            goals.joinToString("; ") { "${it.title}: ${String.format("%.0f", it.savedEur)}/${String.format("%.0f", it.targetEur)} €" }
        }
        viewModelScope.launch {
            _isBuildingPlan.value = true
            try {
                _financialPlan.value = aiService.generateFinancialPlan(income, expense, goalsSummary, memory)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Impossible de générer le plan financier."
            } finally {
                _isBuildingPlan.value = false
            }
        }
    }

    fun addMusicRevenue(trackTitle: String, source: String, playsCount: Long, amountEur: Double, periodLabel: String) {
        viewModelScope.launch {
            val res = repository.addMusicRevenue(currentUserId, trackTitle, source, playsCount, amountEur, periodLabel)
            if (res.isSuccess) {
                _bannerMessage.value = "Revenu musique enregistré."
            } else {
                _bannerMessage.value = res.exceptionOrNull()?.localizedMessage ?: "Erreur revenus musique."
            }
        }
    }

    // SaaS & App Projects
    fun generateSaasIdea(themeHint: String, memory: MemoryProfile) {
        viewModelScope.launch {
            _isGeneratingSaas.value = true
            try {
                val project = aiService.generateSaasProject(themeHint, memory)
                val res = repository.addSaasProject(currentUserId, project)
                _bannerMessage.value = if (res.isSuccess) {
                    "Idée SaaS + Business Plan + Feuille de route enregistrés !"
                } else {
                    res.exceptionOrNull()?.localizedMessage ?: "Erreur sauvegarde projet."
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Impossible de générer l'idée SaaS."
            } finally {
                _isGeneratingSaas.value = false
            }
        }
    }

    fun updateSaasStatus(project: SaasProject, newStatus: String) {
        viewModelScope.launch {
            repository.updateSaasProject(currentUserId, project.id, mapOf("status" to newStatus))
        }
    }

    fun toggleSaasActionDone(project: SaasProject) {
        viewModelScope.launch {
            repository.updateSaasProject(currentUserId, project.id, mapOf("actionDone" to !project.actionDone))
        }
    }

    fun updateSaasNextAction(project: SaasProject, newNextAction: String) {
        val clean = newNextAction.trim().take(500)
        if (clean.isBlank()) return
        viewModelScope.launch {
            val res = repository.updateSaasProject(
                currentUserId,
                project.id,
                mapOf("nextAction" to clean, "actionDone" to false)
            )
            _bannerMessage.value = if (res.isSuccess) {
                "Prochaine action mise à jour."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Erreur mise à jour."
            }
        }
    }

    fun addManualProjectAction(
        title: String,
        goalSource: String,
        category: String,
        nextAction: String
    ) {
        val cleanTitle = title.trim().take(140)
        val cleanAction = nextAction.trim().take(500)
        if (cleanTitle.isBlank() || cleanAction.isBlank()) return
        val cleanGoal = goalSource.trim().ifBlank { "Objectif personnel" }.take(400)
        val cleanCategory = category.trim().ifBlank { "Projet" }.take(80)
        viewModelScope.launch {
            val item = SaasProject(
                title = cleanTitle,
                problem = "Objectif lié : $cleanGoal",
                audience = cleanCategory,
                competition = "Défini par Raphaël",
                pricing = "Prioritaire",
                businessPlan = "Projet suivi depuis le tableau de bord Hazard.",
                roadmap = "1. Exécuter l'action immédiate\n2. Valider le résultat avec Hazard",
                nextAction = cleanAction,
                status = "building",
                actionDone = false
            )
            val res = repository.addSaasProject(currentUserId, item)
            _bannerMessage.value = if (res.isSuccess) {
                "Action ajoutée au tableau de bord."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Erreur lors de l'ajout."
            }
        }
    }

    fun generateNextActionsFromMemory(memory: MemoryProfile) {
        val existing = (saasProjectsState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _isGeneratingGoalActions.value = true
            try {
                val generated = aiService.generateNextActionsFromMemory(memory, existing)
                if (generated.isEmpty()) {
                    _bannerMessage.value = "Toutes les pistes prioritaires de ta Mémoire sont déjà dans ton tableau de bord."
                } else {
                    var addedCount = 0
                    generated.forEach { proj ->
                        if (repository.addSaasProject(currentUserId, proj).isSuccess) {
                            addedCount++
                        }
                    }
                    _bannerMessage.value = "$addedCount prochaine(s) action(s) générée(s) depuis ta Mémoire !"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _bannerMessage.value = e.localizedMessage ?: "Impossible d'analyser les objectifs de la Mémoire."
            } finally {
                _isGeneratingGoalActions.value = false
            }
        }
    }

    override fun onCleared() {
        botJobs.values.forEach { it.cancel() }
        botJobs.clear()
        super.onCleared()
    }

    private companion object {
        const val TAG = "FinanceProjectsVM"
    }
}
