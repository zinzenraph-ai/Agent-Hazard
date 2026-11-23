package com.example.data

import android.util.Log
import android.util.Xml
import com.example.model.MarketQuote
import com.example.model.NewsArticle
import com.example.model.WatchlistAsset
import com.example.security.AppTelemetryMonitor
import com.example.security.RateLimiter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class MarketFeedResult(
    val quotes: List<MarketQuote>,
    val providerSummary: String,
    val warning: String? = null
)

data class NewsFeedResult(
    val articles: List<NewsArticle>,
    val provider: String,
    val warning: String? = null
)

class MarketDataService {

    // Clean OkHttp client with NO URL/header logging so API keys are never logged.
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val quoteCache = ConcurrentHashMap<String, MarketQuote>()
    private val providerCooldownUntilMs = ConcurrentHashMap<String, Long>()
    private var cachedNews: Pair<Long, NewsFeedResult>? = null

    private fun isProviderOnCooldown(provider: String): Boolean {
        val until = providerCooldownUntilMs[provider] ?: return false
        return System.currentTimeMillis() < until
    }

    private fun markRateLimited(provider: String, cooldownSeconds: Long = 120L) {
        providerCooldownUntilMs[provider] = System.currentTimeMillis() + cooldownSeconds * 1000L
    }

    suspend fun fetchLiveQuotes(
        assets: List<WatchlistAsset>,
        forceRefresh: Boolean = false
    ): MarketFeedResult = withContext(Dispatchers.IO) {
        if (!RateLimiter.tryAcquire("market_quotes")) {
            val cachedFallback = assets.mapNotNull { quoteCache[it.symbol] }
            return@withContext MarketFeedResult(
                quotes = cachedFallback,
                providerSummary = "Cache protecteur (limite req/min)",
                warning = "Limitation anti-abus active (${RateLimiter.bucketLimitsPerMinute["market_quotes"]}/min) : lecture du cache local."
            )
        }
        AppTelemetryMonitor.recordMarketCall()
        val now = System.currentTimeMillis()
        val resultMap = LinkedHashMap<String, MarketQuote>()
        val providersUsed = linkedSetOf<String>()
        val warnings = mutableListOf<String>()

        val cryptoAndGold = assets.filter { it.assetType == "crypto" || it.assetType == "gold" }
        val stocks = assets.filter { it.assetType == "stock" }

        // 1. Check cache for crypto & gold (60s TTL to protect free rate limits)
        val cryptoToFetch = if (forceRefresh) cryptoAndGold else cryptoAndGold.filter { asset ->
            val cached = quoteCache[asset.symbol]
            if (cached != null && (now - cached.updatedAtMs) < 60_000L) {
                resultMap[asset.symbol] = cached
                providersUsed += "${cached.sourceProvider} (cache)"
                false
            } else true
        }

        if (cryptoToFetch.isNotEmpty()) {
            var fetched = fetchCryptoFromCoinGecko(cryptoToFetch)
            if (fetched.isEmpty()) {
                fetched = fetchCryptoFromBinanceFallback(cryptoToFetch)
                if (fetched.isNotEmpty()) {
                    warnings += "CoinGecko indisponible ou limité : bascule automatique sur Binance."
                }
            }
            if (fetched.isEmpty()) {
                warnings += "Impossible de joindre CoinGecko/Binance pour la crypto."
            }
            fetched.forEach { q ->
                quoteCache[q.symbol] = q
                resultMap[q.symbol] = q
                providersUsed += q.sourceProvider
            }
        }

        // 2. Stocks & commodities (180s TTL to preserve AlphaVantage 25/day & TwelveData 8/min quotas)
        val stocksToFetch = if (forceRefresh) stocks else stocks.filter { asset ->
            val cached = quoteCache[asset.symbol]
            if (cached != null && (now - cached.updatedAtMs) < 180_000L) {
                resultMap[asset.symbol] = cached
                providersUsed += "${cached.sourceProvider} (cache)"
                false
            } else true
        }

        for (stock in stocksToFetch) {
            val quote = fetchStockWithFailover(stock)
            if (quote != null) {
                quoteCache[quote.symbol] = quote
                resultMap[quote.symbol] = quote
                providersUsed += quote.sourceProvider
            } else {
                quoteCache[stock.symbol]?.let { stale ->
                    resultMap[stock.symbol] = stale
                    providersUsed += "${stale.sourceProvider} (cache)"
                }
                warnings += "Quota/réseau limité pour ${stock.symbol}."
            }
        }

        // Fill any missing crypto from stale cache if network failed
        for (asset in assets) {
            if (!resultMap.containsKey(asset.symbol)) {
                quoteCache[asset.symbol]?.let { stale ->
                    resultMap[asset.symbol] = stale
                }
            }
        }

        MarketFeedResult(
            quotes = assets.mapNotNull { resultMap[it.symbol] },
            providerSummary = if (providersUsed.isEmpty()) "Aucune source joignable"
            else providersUsed.joinToString(" · "),
            warning = warnings.takeIf { it.isNotEmpty() }?.distinct()?.joinToString(" ")
        )
    }

    private fun fetchCryptoFromCoinGecko(assets: List<WatchlistAsset>): List<MarketQuote> {
        if (isProviderOnCooldown("CoinGecko")) return emptyList()
        val ids = assets.map { it.apiId }.distinct().joinToString(",")
        val url = "https://api.coingecko.com/api/v3/simple/price?ids=$ids&vs_currencies=eur&include_24hr_change=true"
        val reqBuilder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
        SecretsRegistry.coinGeckoKeyOrNull()?.let { key ->
            reqBuilder.header("x-cg-demo-api-key", key)
        }

        return try {
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.code == 429) {
                    markRateLimited("CoinGecko", 90L)
                    return emptyList()
                }
                if (!resp.isSuccessful) return emptyList()
                val body = resp.body?.string() ?: return emptyList()
                val json = JSONObject(body)
                val now = System.currentTimeMillis()
                assets.mapNotNull { asset ->
                    val obj = json.optJSONObject(asset.apiId) ?: return@mapNotNull null
                    val price = obj.optDouble("eur", Double.NaN)
                    if (price.isNaN()) return@mapNotNull null
                    val change = obj.optDouble("eur_24h_change", 0.0)
                    MarketQuote(
                        symbol = asset.symbol,
                        name = asset.name,
                        assetType = asset.assetType,
                        apiId = asset.apiId,
                        priceEur = price,
                        change24hPct = if (change.isNaN()) 0.0 else change,
                        sourceProvider = "CoinGecko",
                        updatedAtMs = now
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "CoinGecko request failed: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    private fun fetchCryptoFromBinanceFallback(assets: List<WatchlistAsset>): List<MarketQuote> {
        if (isProviderOnCooldown("Binance")) return emptyList()
        val now = System.currentTimeMillis()
        val quotes = mutableListOf<MarketQuote>()
        for (asset in assets) {
            val pair = when (asset.symbol.uppercase()) {
                "BTC" -> "BTCEUR"
                "ETH" -> "ETHEUR"
                "SOL" -> "SOLEUR"
                "XRP" -> "XRPEUR"
                "BNB" -> "BNBEUR"
                "PAXG" -> "PAXGUSDT"
                else -> "${asset.symbol.uppercase()}EUR"
            }
            val url = "https://api.binance.com/api/v3/ticker/24hr?symbol=$pair"
            val req = Request.Builder().url(url).build()
            try {
                client.newCall(req).execute().use { resp ->
                    if (resp.code == 429) {
                        markRateLimited("Binance", 90L)
                        return quotes
                    }
                    if (!resp.isSuccessful) return@use
                    val obj = JSONObject(resp.body?.string() ?: return@use)
                    val last = obj.optString("lastPrice").toDoubleOrNull() ?: return@use
                    val pct = obj.optString("priceChangePercent").toDoubleOrNull() ?: 0.0
                    val eurPrice = if (pair.endsWith("USDT")) last * 0.92 else last
                    quotes += MarketQuote(
                        symbol = asset.symbol,
                        name = asset.name,
                        assetType = asset.assetType,
                        apiId = asset.apiId,
                        priceEur = eurPrice,
                        change24hPct = pct,
                        sourceProvider = "Binance (secours)",
                        updatedAtMs = now
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Binance fallback failed for ${asset.symbol}: ${e.javaClass.simpleName}")
            }
        }
        return quotes
    }

    private fun fetchStockWithFailover(asset: WatchlistAsset): MarketQuote? {
        val tdKey = SecretsRegistry.twelveDataKeyOrNull()
        if (tdKey != null && !isProviderOnCooldown("TwelveData")) {
            fetchFromTwelveData(asset, tdKey)?.let { return it }
        }
        val avKey = SecretsRegistry.alphaVantageKeyOrNull()
        if (avKey != null && !isProviderOnCooldown("AlphaVantage")) {
            fetchFromAlphaVantage(asset, avKey)?.let { return it }
        }
        return fetchFromYahooPublic(asset)
    }

    private fun fetchFromTwelveData(asset: WatchlistAsset, key: String): MarketQuote? {
        val url = "https://api.twelvedata.com/quote?symbol=${asset.apiId}&apikey=$key"
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (resp.code == 429) {
                    markRateLimited("TwelveData", 90L)
                    return null
                }
                if (!resp.isSuccessful) return null
                val obj = JSONObject(resp.body?.string() ?: return null)
                if (obj.has("code") && obj.optInt("code") == 429) {
                    markRateLimited("TwelveData", 90L)
                    return null
                }
                val close = obj.optString("close").toDoubleOrNull() ?: return null
                val pct = obj.optString("percent_change").toDoubleOrNull() ?: 0.0
                MarketQuote(
                    symbol = asset.symbol,
                    name = asset.name,
                    assetType = asset.assetType,
                    apiId = asset.apiId,
                    priceEur = close * 0.92,
                    change24hPct = pct,
                    sourceProvider = "TwelveData",
                    updatedAtMs = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "TwelveData failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun fetchFromAlphaVantage(asset: WatchlistAsset, key: String): MarketQuote? {
        val url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=${asset.apiId}&apikey=$key"
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val obj = JSONObject(resp.body?.string() ?: return null)
                if (obj.has("Note") || obj.has("Information")) {
                    markRateLimited("AlphaVantage", 300L)
                    return null
                }
                val q = obj.optJSONObject("Global Quote") ?: return null
                val price = q.optString("05. price").toDoubleOrNull() ?: return null
                val pctRaw = q.optString("10. change percent").replace("%", "").toDoubleOrNull() ?: 0.0
                MarketQuote(
                    symbol = asset.symbol,
                    name = asset.name,
                    assetType = asset.assetType,
                    apiId = asset.apiId,
                    priceEur = price * 0.92,
                    change24hPct = pctRaw,
                    sourceProvider = "AlphaVantage",
                    updatedAtMs = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "AlphaVantage failed: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun fetchFromYahooPublic(asset: WatchlistAsset): MarketQuote? {
        if (isProviderOnCooldown("YahooFinance")) return null
        val url = "https://query1.finance.yahoo.com/v8/finance/chart/${asset.apiId}?interval=1d&range=2d"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (resp.code == 429) {
                    markRateLimited("YahooFinance", 120L)
                    return null
                }
                if (!resp.isSuccessful) return null
                val root = JSONObject(resp.body?.string() ?: return null)
                val meta = root.optJSONObject("chart")
                    ?.optJSONArray("result")
                    ?.optJSONObject(0)
                    ?.optJSONObject("meta") ?: return null
                val price = meta.optDouble("regularMarketPrice", Double.NaN)
                if (price.isNaN()) return null
                val prev = meta.optDouble("chartPreviousClose", price)
                val pct = if (prev > 0) ((price - prev) / prev) * 100.0 else 0.0
                val currency = meta.optString("currency", "USD")
                val eurPrice = if (currency.equals("EUR", ignoreCase = true)) price else price * 0.92
                MarketQuote(
                    symbol = asset.symbol,
                    name = asset.name,
                    assetType = asset.assetType,
                    apiId = asset.apiId,
                    priceEur = eurPrice,
                    change24hPct = pct,
                    sourceProvider = "Yahoo Public (secours)",
                    updatedAtMs = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Yahoo public chart failed: ${e.javaClass.simpleName}")
            null
        }
    }

    suspend fun fetchEconomicNews(forceRefresh: Boolean = false): NewsFeedResult =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val cached = cachedNews
            if (!forceRefresh && cached != null && (now - cached.first) < 300_000L) {
                return@withContext cached.second
            }
            if (!RateLimiter.tryAcquire("news_feed")) {
                return@withContext cached?.second ?: NewsFeedResult(
                    articles = emptyList(),
                    provider = "Limite/min",
                    warning = "Limite anti-abus actualités atteinte : attends quelques secondes."
                )
            }

            val gNewsKey = SecretsRegistry.gNewsKeyOrNull()
            if (gNewsKey != null && !isProviderOnCooldown("GNews")) {
                val gNews = fetchNewsFromGNews(gNewsKey)
                if (gNews.isNotEmpty()) {
                    val res = NewsFeedResult(gNews, "GNews")
                    cachedNews = now to res
                    return@withContext res
                }
            }

            val rssNews = fetchNewsFromRssFallback()
            val res = if (rssNews.isNotEmpty()) {
                NewsFeedResult(
                    articles = rssNews,
                    provider = "RSS Éco/Crypto (secours)",
                    warning = if (gNewsKey == null) "Clé GNews absente : flux RSS public utilisé automatiquement." else null
                )
            } else {
                NewsFeedResult(
                    articles = emptyList(),
                    provider = "Indisponible",
                    warning = "Impossible de récupérer les actualités pour le moment."
                )
            }
            if (rssNews.isNotEmpty()) cachedNews = now to res
            res
        }

    private fun fetchNewsFromGNews(key: String): List<NewsArticle> {
        val url = "https://gnews.io/api/v4/top-headlines?category=business&lang=fr&max=8&apikey=$key"
        return try {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                if (resp.code == 429 || resp.code == 403) {
                    markRateLimited("GNews", 300L)
                    return emptyList()
                }
                if (!resp.isSuccessful) return emptyList()
                val root = JSONObject(resp.body?.string() ?: return emptyList())
                val arr = root.optJSONArray("articles") ?: JSONArray()
                (0 until arr.length()).mapNotNull { i ->
                    val item = arr.optJSONObject(i) ?: return@mapNotNull null
                    val title = item.optString("title").trim()
                    if (title.isBlank()) return@mapNotNull null
                    NewsArticle(
                        title = title,
                        description = item.optString("description").trim(),
                        source = item.optJSONObject("source")?.optString("name").orEmpty().ifBlank { "GNews" },
                        publishedAt = item.optString("publishedAt").take(10),
                        sourceProvider = "GNews"
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GNews request failed: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    private fun fetchNewsFromRssFallback(): List<NewsArticle> {
        val req = Request.Builder()
            .url("https://fr.cointelegraph.com/rss")
            .header("User-Agent", "Mozilla/5.0")
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return emptyList()
                val xml = resp.body?.string() ?: return emptyList()
                parseRssXml(xml)
            }
        } catch (e: Exception) {
            Log.w(TAG, "RSS fallback failed: ${e.javaClass.simpleName}")
            emptyList()
        }
    }

    private fun parseRssXml(xml: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))
        var eventType = parser.eventType
        var insideItem = false
        var title = ""
        var desc = ""
        var pubDate = ""

        while (eventType != XmlPullParser.END_DOCUMENT && articles.size < 8) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (parser.name.lowercase()) {
                        "item" -> {
                            insideItem = true
                            title = ""
                            desc = ""
                            pubDate = ""
                        }
                        "title" -> if (insideItem) title = parser.nextText().trim()
                        "description" -> if (insideItem) {
                            desc = parser.nextText().replace(Regex("<[^>]*>"), " ").trim().take(240)
                        }
                        "pubdate" -> if (insideItem) pubDate = parser.nextText().trim().take(16)
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("item", ignoreCase = true) && insideItem) {
                        if (title.isNotBlank()) {
                            articles += NewsArticle(
                                title = title,
                                description = desc,
                                source = "CoinTelegraph FR",
                                publishedAt = pubDate,
                                sourceProvider = "RSS Public"
                            )
                        }
                        insideItem = false
                    }
                }
            }
            eventType = parser.next()
        }
        return articles
    }

    suspend fun executeRealSpotOrder(
        symbol: String,
        side: String,
        quoteAmountEur: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = SecretsRegistry.exchangeApiKeyOrNull()
        val apiSecret = SecretsRegistry.exchangeApiSecretOrNull()
        if (apiKey == null || apiSecret == null) {
            return@withContext Result.failure(
                IllegalStateException("Clés plateforme manquantes dans les Secrets AI Studio.")
            )
        }
        val pair = when (symbol.uppercase()) {
            "BTC" -> "BTCEUR"
            "ETH" -> "ETHEUR"
            "SOL" -> "SOLEUR"
            else -> "${symbol.uppercase()}EUR"
        }
        val sideUpper = if (side.equals("sell", ignoreCase = true)) "SELL" else "BUY"
        val timestamp = System.currentTimeMillis()
        val qtyStr = String.format(java.util.Locale.US, "%.2f", quoteAmountEur)
        val query = "symbol=$pair&side=$sideUpper&type=MARKET&quoteOrderQty=$qtyStr&timestamp=$timestamp"

        try {
            val mac = javax.crypto.Mac.getInstance("HmacSHA256")
            mac.init(javax.crypto.spec.SecretKeySpec(apiSecret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val signature = mac.doFinal(query.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }

            val url = "https://api.binance.com/api/v3/order?$query&signature=$signature"
            val req = Request.Builder()
                .url(url)
                .header("X-MBX-APIKEY", apiKey)
                .post("".toRequestBody(null))
                .build()

            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val msg = try {
                        JSONObject(body).optString("msg", "HTTP ${resp.code}")
                    } catch (_: Exception) {
                        "HTTP ${resp.code}"
                    }
                    return@withContext Result.failure(IllegalStateException("Plateforme: $msg"))
                }
                val json = JSONObject(body)
                val orderId = json.optLong("orderId", 0L)
                Result.success("Ordre réel #$orderId exécuté ($sideUpper $pair, $qtyStr €)")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Real exchange order failed: ${e.javaClass.simpleName}")
            Result.failure(IllegalStateException("Erreur réseau/plateforme lors de l'ordre réel."))
        }
    }

    private companion object {
        const val TAG = "MarketDataService"
    }
}
