package com.example.data

import com.example.model.TradingConcept
import kotlin.math.abs

data class PositionCalculation(
    val riskAmountEur: Double,
    val stopDistancePct: Double,
    val positionUnits: Double,
    val notionalValueEur: Double,
    val marginRequiredEur: Double,
    val rewardRiskRatio: Double,
    val potentialProfitEur: Double,
    val estimatedLiquidationPrice: Double?,
    val liquidationBeforeStop: Boolean,
    val riskVerdict: String
)

object TradingAcademyData {

    fun calculatePosition(
        capitalEur: Double,
        riskPercent: Double,
        entryPrice: Double,
        stopLossPrice: Double,
        takeProfitPrice: Double,
        leverage: Int
    ): PositionCalculation? {
        if (capitalEur <= 0.0 || riskPercent <= 0.0 || entryPrice <= 0.0 || stopLossPrice <= 0.0) {
            return null
        }
        val stopDiff = abs(entryPrice - stopLossPrice)
        if (stopDiff <= 0.0) return null

        val isLong = entryPrice > stopLossPrice
        val stopDistancePct = (stopDiff / entryPrice) * 100.0
        val riskAmountEur = capitalEur * (riskPercent / 100.0)
        val positionUnits = riskAmountEur / stopDiff
        val notionalValueEur = positionUnits * entryPrice
        val safeLeverage = leverage.coerceAtLeast(1)
        val marginRequiredEur = notionalValueEur / safeLeverage

        val profitDiff = if (isLong) {
            (takeProfitPrice - entryPrice).coerceAtLeast(0.0)
        } else {
            (entryPrice - takeProfitPrice).coerceAtLeast(0.0)
        }
        val rewardRiskRatio = if (stopDiff > 0.0) profitDiff / stopDiff else 0.0
        val potentialProfitEur = riskAmountEur * rewardRiskRatio

        val liqPrice = if (safeLeverage > 1) {
            val liqMove = 0.90 / safeLeverage.toDouble()
            if (isLong) entryPrice * (1.0 - liqMove) else entryPrice * (1.0 + liqMove)
        } else {
            null
        }

        val liquidationBeforeStop = if (liqPrice != null) {
            if (isLong) liqPrice >= stopLossPrice else liqPrice <= stopLossPrice
        } else {
            false
        }

        val verdict = when {
            liquidationBeforeStop ->
                "DANGER CRITIQUE : Levier x$safeLeverage trop élevé — tu es liquidé AVANT ton Stop-Loss."
            riskPercent > 2.0 ->
                "RISQUE EXCESSIF (${String.format("%.1f", riskPercent)}%) : Un pro reste entre 0,5% et 1,5% max pour survivre aux séries perdantes."
            rewardRiskRatio < 1.5 ->
                "R:R FAIBLE (${String.format("%.2f", rewardRiskRatio)}R) : Cherche minimum 2.0R pour être rentable même avec 40% de réussite."
            marginRequiredEur > capitalEur ->
                "MARGE INSUFFISANTE : Ce trade demande plus de capital que ton compte total. Réduis le risque ou élargis l'horizon."
            else ->
                "SETUP PROPRE : Risque maîtrisé (${String.format("%.2f", riskAmountEur)} €) avec R:R de ${String.format("%.2f", rewardRiskRatio)}:1."
        }

        return PositionCalculation(
            riskAmountEur = riskAmountEur,
            stopDistancePct = stopDistancePct,
            positionUnits = positionUnits,
            notionalValueEur = notionalValueEur,
            marginRequiredEur = marginRequiredEur,
            rewardRiskRatio = rewardRiskRatio,
            potentialProfitEur = potentialProfitEur,
            estimatedLiquidationPrice = liqPrice,
            liquidationBeforeStop = liquidationBeforeStop,
            riskVerdict = verdict
        )
    }

    data class CompoundCalcResult(
        val totalInvestedEur: Double,
        val finalCapitalEur: Double,
        val interestEarnedEur: Double
    )

    fun calculateCompoundInterest(
        initialEur: Double,
        monthlyContributionEur: Double,
        annualRatePct: Double,
        years: Int
    ): CompoundCalcResult {
        val safeYears = years.coerceIn(1, 50)
        val monthlyRate = (annualRatePct / 100.0) / 12.0
        var balance = initialEur.coerceAtLeast(0.0)
        var invested = initialEur.coerceAtLeast(0.0)
        val months = safeYears * 12
        repeat(months) {
            balance = balance * (1.0 + monthlyRate) + monthlyContributionEur.coerceAtLeast(0.0)
            invested += monthlyContributionEur.coerceAtLeast(0.0)
        }
        return CompoundCalcResult(
            totalInvestedEur = invested,
            finalCapitalEur = balance,
            interestEarnedEur = balance - invested
        )
    }

    data class PnLCalcResult(
        val grossPnlEur: Double,
        val totalFeesEur: Double,
        val netPnlEur: Double,
        val netPnlPct: Double,
        val breakEvenPrice: Double
    )

    fun calculateGainLoss(
        buyPrice: Double,
        sellPrice: Double,
        investedEur: Double,
        feePctPerSide: Double
    ): PnLCalcResult? {
        if (buyPrice <= 0.0 || sellPrice <= 0.0 || investedEur <= 0.0) return null
        val units = investedEur / buyPrice
        val grossExit = units * sellPrice
        val entryFee = investedEur * (feePctPerSide.coerceAtLeast(0.0) / 100.0)
        val exitFee = grossExit * (feePctPerSide.coerceAtLeast(0.0) / 100.0)
        val totalFees = entryFee + exitFee
        val grossPnl = grossExit - investedEur
        val netPnl = grossPnl - totalFees
        val netPct = (netPnl / investedEur) * 100.0
        val feeRate = feePctPerSide.coerceAtLeast(0.0) / 100.0
        val breakEven = buyPrice * ((1.0 + feeRate) / (1.0 - feeRate).coerceAtLeast(0.01))
        return PnLCalcResult(
            grossPnlEur = grossPnl,
            totalFeesEur = totalFees,
            netPnlEur = netPnl,
            netPnlPct = netPct,
            breakEvenPrice = breakEven
        )
    }

    data class FeeCalcResult(
        val roundTripFeesEur: Double,
        val spreadCostEur: Double,
        val totalFrictionEur: Double,
        val requiredMovePctToBreakEven: Double
    )

    fun calculateFees(
        orderAmountEur: Double,
        feePctPerSide: Double,
        spreadPct: Double
    ): FeeCalcResult? {
        if (orderAmountEur <= 0.0) return null
        val roundTrip = orderAmountEur * (feePctPerSide.coerceAtLeast(0.0) / 100.0) * 2.0
        val spreadCost = orderAmountEur * (spreadPct.coerceAtLeast(0.0) / 100.0)
        val total = roundTrip + spreadCost
        return FeeCalcResult(
            roundTripFeesEur = roundTrip,
            spreadCostEur = spreadCost,
            totalFrictionEur = total,
            requiredMovePctToBreakEven = (total / orderAmountEur) * 100.0
        )
    }

    val modules: List<TradingConcept> = listOf(
        TradingConcept(
            id = "risk_survival",
            title = "1. Survie & Money Management (Règle #1)",
            tag = "FONDAMENTAL",
            summary = "Un pro ne prédit pas l'avenir : il gère l'asymétrie entre ses pertes et ses gains. Si tu perds 50% de ton capital, il faut +100% juste pour revenir à zéro.",
            proRules = listOf(
                "Ne jamais trader l'argent du loyer, de la nourriture ou des factures. Zéro exception.",
                "Risque fixe de 0,5% à 1% du capital trading par trade : 10 pertes d'affilée = -10% seulement.",
                "Espérance mathématique : E = (Winrate × Gain moyen) - (Lossrate × Perte moyenne). Avec R:R 1:3, 35% de réussite suffit pour gagner.",
                "Commence obligatoirement en Paper Trading (compte démo / journal papier) pendant 30 à 50 trades avant 1€ réel."
            ),
            commonTraps = listOf(
                "Doubler la mise après une perte (Martingale) = ruine mathématique assurée.",
                "Déplacer son Stop-Loss plus loin quand le prix s'approche : tu transformes une petite perte maîtrisée en catastrophe.",
                "Calculer son gain potentiel en € avant de savoir combien on accepte de perdre."
            ),
            hazardCoachPrompt = "Fais-moi un cours pratique et direct sur le Money Management et l'espérance mathématique avec un petit capital, puis teste-moi avec 2 cas concrets."
        ),
        TradingConcept(
            id = "market_structure",
            title = "2. Structure de Marché & Price Action",
            tag = "TECHNIQUE",
            summary = "Lire le graphique nu avant tout indicateur : savoir qui contrôle le marché (acheteurs ou vendeurs) et où se trouvent les niveaux d'invalidation.",
            proRules = listOf(
                "Tendance haussière = sommets et creux de plus en plus hauts (HH / HL). Baissière = LH / LL.",
                "BOS (Break of Structure) confirme la continuation ; CHoCH (Change of Character) signale un premier retournement de structure.",
                "Analyse Multi-Timeframes (Top-Down) : Biais sur Daily/4H, zone d'intérêt sur 1H, déclencheur précis sur 15m.",
                "Range : acheter le bas (support) / vendre le haut (résistance) — jamais au milieu du range."
            ),
            commonTraps = listOf(
                "Trader en 1 minute ou 5 minutes contre la tendance Daily/4H (se faire écraser par le flux majeur).",
                "Confondre une simple mèche de liquidité (Sweep) avec une vraie cassure validée en clôture de bougie."
            ),
            hazardCoachPrompt = "Explique-moi comment lire la structure de marché (HH/HL, BOS, CHoCH) en Top-Down (Daily -> 1H -> 15m) sur Bitcoin avec un exemple pas à pas."
        ),
        TradingConcept(
            id = "liquidity_orderflow",
            title = "3. Liquidité, Smart Money & Order Flow",
            tag = "AVANCÉ",
            summary = "Les gros capitaux ont besoin de contrepartie pour entrer. Ils chassent les zones évidentes où les particuliers placent leurs Stop-Loss.",
            proRules = listOf(
                "Repère les Equal Highs / Equal Lows (doubles sommets/creux) : ce sont des aimants à liquidité (Stop Hunts).",
                "FVG (Fair Value Gap) : déséquilibre rapide à 3 bougies où le prix revient souvent combler avant de repartir.",
                "Volume Profile : le POC (Point of Control) montre le prix où le plus de volume a été échangé (zone d'équilibre).",
                "En crypto perpétuel : surveille le Funding Rate et l'Open Interest pour repérer les Long/Short Squeezes imminents."
            ),
            commonTraps = listOf(
                "Entrer avant que la prise de liquidité (Sweep) et le rejet soient confirmés.",
                "Croire qu'un Order Block fonctionne 100% du temps sans contexte macro ni tendance."
            ),
            hazardCoachPrompt = "Apprends-moi à repérer un Sweep de liquidité + FVG + Funding Rate en crypto pour éviter de me faire piéger comme la masse."
        ),
        TradingConcept(
            id = "indicators_edge",
            title = "4. Indicateurs Pro (Utiles vs Bruit)",
            tag = "OUTILS",
            summary = "Les indicateurs ne prédisent rien : ils traduisent le prix et le volume. Moins tu en as, plus ta lecture est nette.",
            proRules = listOf(
                "EMA 20 / 50 / 200 : filtre de tendance dynamique et zones de pullback en tendance forte.",
                "VWAP (Volume Weighted Average Price) : boussole institutionnelle intraday. Cherche les longs au-dessus, shorts en dessous.",
                "RSI en divergence : prix fait un plus bas, RSI fait un plus haut = essoufflement des vendeurs.",
                "ATR (Average True Range) : mesure la volatilité réelle pour placer ton Stop-Loss à minimum 1.5× ATR hors du bruit."
            ),
            commonTraps = listOf(
                "Vendre bêtement parce que le RSI dépasse 70 en plein bull run (le RSI peut rester suracheté des semaines).",
                "Empiler 8 indicateurs contradictoires qui paralysent la décision."
            ),
            hazardCoachPrompt = "Montre-moi comment combiner uniquement Structure + VWAP + Divergence RSI + ATR pour construire une stratégie claire et simple."
        ),
        TradingConcept(
            id = "crypto_tokenomics",
            title = "5. Spécificités Crypto, Cycles & Tokenomics",
            tag = "CRYPTO",
            summary = "En crypto, la technologie ne fait pas tout : les flux de liquidité macro, les unlocks de tokens et la rotation du Bitcoin dictent la loi.",
            proRules = listOf(
                "Cycle de rotation : Liquidité Macro -> Bitcoin (BTC) -> BTC Dominance baisse -> Ethereum & Large Caps -> Mid/Small Caps.",
                "Toujours comparer Market Cap vs FDV (Fully Diluted Valuation) : un token avec 10% en circulation et 90% bloqués va subir une énorme pression vendeuse aux unlocks.",
                "Vérifier le calendrier des Vesting / Cliffs avant d'acheter un altcoin.",
                "Sur DEX : surveiller la profondeur de liquidité (Liquidity Pool), le slippage et les frais de gas."
            ),
            commonTraps = listOf(
                "Acheter un 'projet révolutionnaire' qui a un FDV à 5 milliards mais seulement 5% de tokens en circulation.",
                "Rester all-in en altcoins quand la BTC Dominance casse vers le haut."
            ),
            hazardCoachPrompt = "Décortique-moi la Tokenomics (Market Cap vs FDV, unlocks, inflation) et comment lire la BTC Dominance pour savoir quand être en BTC, en altcoins ou en stablecoins."
        ),
        TradingConcept(
            id = "scam_shield_psy",
            title = "6. Bouclier Anti-Arnaques & Psychologie",
            tag = "PROTECTION",
            summary = "90% des pertes des débutants viennent des arnaques, du levier abusif et des émotions (FOMO, Revenge Trading), pas du manque d'indicateurs.",
            proRules = listOf(
                "Règle absolue : quiconque promet des gains garantis, un robot miracle ou des signaux VIP payants vend de l'illusion.",
                "Sur DEX/Memecoins : vérifier si le contrat est renoncé, si la liquidité est lockée/burn, et tester s'il n'est pas un Honeypot (impossible de vendre).",
                "Zéro levier > x3 pour un débutant (idéalement 1x Spot uniquement au début).",
                "Si tu as peur ou envie de te refaire après une perte (Revenge Trading) : ferme l'écran immédiatement."
            ),
            commonTraps = listOf(
                "Cliquer sur un lien d'airdrop inconnu et signer une transaction qui vide le wallet.",
                "Prendre du levier x20/x50 avec un petit compte 'pour aller plus vite' : c'est le chemin le plus court vers zéro."
            ),
            hazardCoachPrompt = "Donne-moi la checklist complète du pro pour auditer un token/projet crypto avant d'y toucher et les règles psychologiques anti-FOMO."
        )
    )
}
