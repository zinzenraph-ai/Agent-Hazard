const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const RAPH_UID = "raphael_uid_001";
const OTHER_UID = "other_uid_002";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated access denied across all Hazard collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const cols = [
    "memories", "messages", "creations", "watchlist", "price_alerts",
    "sim_wallet", "sim_positions", "sim_trades", "journal_entries",
    "budget_entries", "savings_goals", "music_revenues", "saas_projects",
    "bot_strategies", "security_events", "improvement_proposals", "app_versions"
  ];
  for (const col of cols) {
    await assertFails(unauthDb.collection("users").doc(RAPH_UID).collection(col).get());
  }
  await assertFails(unauthDb.collection("security_settings").doc("owner_policy").get());
});

test("Watchlist, Price Alerts, and Simulated Portfolio enforce owner isolation & bounds", async () => {
  const raphDb = testEnv.authenticatedContext(RAPH_UID).firestore();
  const otherDb = testEnv.authenticatedContext(OTHER_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("watchlist").doc("BTC").set({
      userId: RAPH_UID,
      symbol: "BTC",
      name: "Bitcoin",
      assetType: "crypto",
      apiId: "bitcoin",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("price_alerts").doc("al_1").set({
      userId: RAPH_UID,
      symbol: "BTC",
      assetName: "Bitcoin",
      apiId: "bitcoin",
      assetType: "crypto",
      targetPrice: 90000,
      above: true,
      triggered: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("price_alerts").doc("al_1").update({
      triggered: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("sim_wallet").doc("main").set({
      userId: RAPH_UID,
      cashEur: 800,
      initialEur: 1000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("sim_positions").doc("BTC").set({
      userId: RAPH_UID,
      symbol: "BTC",
      assetName: "Bitcoin",
      assetType: "crypto",
      quantity: 0.002,
      avgBuyPrice: 90000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("sim_trades").doc("tr_1").set({
      userId: RAPH_UID,
      symbol: "BTC",
      assetName: "Bitcoin",
      side: "buy",
      quantity: 0.002,
      price: 90000,
      totalEur: 200,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Cross-user rejected
  await assertFails(otherDb.collection("users").doc(RAPH_UID).collection("sim_wallet").doc("main").get());
  await assertFails(otherDb.collection("users").doc(RAPH_UID).collection("price_alerts").get());
});

test("Journal, Budget, Savings, Music Revenues, and SaaS Projects validate payloads", async () => {
  const raphDb = testEnv.authenticatedContext(RAPH_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("journal_entries").doc("j_1").set({
      userId: RAPH_UID,
      asset: "BTC/EUR",
      direction: "spot",
      entryPrice: "85000",
      exitPrice: "89000",
      stopLoss: "83000",
      reason: "Sweep du bas de range + divergence RSI",
      emotion: "Calme",
      outcome: "+2R",
      aiFeedback: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("budget_entries").doc("b_1").set({
      userId: RAPH_UID,
      kind: "expense",
      category: "Courses",
      label: "Courses semaine Wyatt",
      amountEur: 65.5,
      monthKey: "2026-11",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Zero or negative budget amount fails
  await assertFails(
    raphDb.collection("users").doc(RAPH_UID).collection("budget_entries").doc("b_bad").set({
      userId: RAPH_UID,
      kind: "expense",
      category: "Test",
      label: "Bad",
      amountEur: -10,
      monthKey: "2026-11",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("savings_goals").doc("sg_1").set({
      userId: RAPH_UID,
      title: "Matelas sécurité famille",
      targetEur: 1500,
      savedEur: 120,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("music_revenues").doc("mr_1").set({
      userId: RAPH_UID,
      trackTitle: "Nuit d'Or",
      source: "streams",
      playsCount: 1240,
      amountEur: 4.15,
      periodLabel: "Nov 2026",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("saas_projects").doc("sp_1").set({
      userId: RAPH_UID,
      title: "SunoPrompt Studio",
      problem: "Les créateurs mobiles galèrent à structurer leurs prompts Suno.",
      audience: "Artistes indépendants sur téléphone",
      competition: "Notes brutes, générateurs génériques",
      pricing: "Freemium + 4,99 €/mois",
      businessPlan: "MVP mobile -> TikTok démo -> micro-abonnement",
      roadmap: "1. Prototype\n2. 10 testeurs",
      nextAction: "Publier 1 vidéo TikTok démo",
      status: "idea",
      actionDone: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("saas_projects").doc("sp_1").update({
      actionDone: true,
      status: "building",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("bot_strategies").doc("bot_1").set({
      userId: RAPH_UID,
      name: "Dip BTC -5% / +8%",
      description: "Achète si BTC baisse de 5%, revend à +8%",
      assetSymbol: "BTC",
      apiId: "bitcoin",
      buyDropPct: 5,
      takeProfitPct: 8,
      stopLossPct: 4,
      amountPerTradeEur: 100,
      maxTotalLossEur: 200,
      maxTradesCount: 10,
      mode: "simulation",
      status: "idle",
      tradesExecuted: 0,
      winsCount: 0,
      lossesCount: 0,
      cumulativePnlEur: 0,
      referencePrice: 88000,
      lastActionNote: "Prêt en simulation",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("bot_strategies").doc("bot_1").update({
      status: "stopped",
      lastActionNote: "Arrêt immédiat",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Allowlist owner_policy blocks unauthorized Google accounts & Continuous Improvement snapshots validate", async () => {
  const ts = new Date(Date.now() - 2000);
  const raphDb = testEnv.authenticatedContext(RAPH_UID, { email: "raph@example.com" }).firestore();
  const wyattDb = testEnv.authenticatedContext("wyatt_uid_007", { email: "wyatt@example.com" }).firestore();
  const intruderDb = testEnv.authenticatedContext("intruder_999", { email: "stranger@example.com" }).firestore();

  // Owner claims allowlist
  await assertSucceeds(
    raphDb.collection("security_settings").doc("owner_policy").set({
      ownerUid: RAPH_UID,
      ownerEmail: "raph@example.com",
      wyattEmail: "wyatt@example.com",
      allowedEmails: ["raph@example.com", "wyatt@example.com"],
      lastLoginEmail: "raph@example.com",
      lastLoginDevice: "Pixel",
      lastLoginAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Stranger is blocked even from writing to their OWN user collection once owner_policy is claimed!
  await assertFails(
    intruderDb.collection("users").doc("intruder_999").collection("security_events").doc("ev_bad").set({
      userId: "intruder_999",
      eventType: "login",
      severity: "info",
      title: "Intruder",
      detail: "Should fail",
      deviceLabel: "Unknown",
      resolved: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Stranger cannot overwrite owner_policy
  await assertFails(
    intruderDb.collection("security_settings").doc("owner_policy").update({
      wyattEmail: "stranger@example.com",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Authorized Wyatt email can write to Wyatt's own security_events (and cannot read Raph's)
  await assertSucceeds(
    wyattDb.collection("users").doc("wyatt_uid_007").collection("security_events").doc("ev_w1").set({
      userId: "wyatt_uid_007",
      eventType: "login",
      severity: "info",
      title: "Connexion Wyatt",
      detail: "Mode Wyatt verrouillé",
      deviceLabel: "Tablet",
      resolved: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(wyattDb.collection("users").doc(RAPH_UID).collection("security_events").get());

  // Owner continuous improvement proposal & rollback version snapshot
  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("improvement_proposals").doc("prop_1").set({
      userId: RAPH_UID,
      title: "Réduire TTL cache actions",
      category: "perf",
      priority: "haute",
      whatToChange: "Passer cache TwelveData à 240s",
      why: "Éviter dépassement quota gratuit",
      impact: "-25% requêtes API",
      risks: "Cours actions rafraîchi toutes les 4 min",
      rollbackHowTo: "Revenir à v1.0.0 en 1 clic",
      preparedPlan: "",
      weekKey: "2026-W47",
      status: "pending",
      appliedVersionId: "",
      beforePreview: "Accueil standard 4 gadgets",
      afterPreview: "Raccourci Finance en tête",
      supports24hTrial: true,
      trialAccentStyle: "emerald",
      trialThemeMode: "dark",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("ui_prefs").doc("main").set({
      userId: RAPH_UID,
      themeMode: "dark",
      accentStyle: "hazard",
      textScale: 1.0,
      moduleOrder: ["musique", "finance", "plans"],
      hiddenModules: [],
      enabledGadgets: ["crypto", "news"],
      trialLabel: "",
      trialExpiresAtMs: 0,
      previousThemeSnapshot: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    raphDb.collection("users").doc(RAPH_UID).collection("app_versions").doc("ver_1").set({
      userId: RAPH_UID,
      versionTag: "v1.0.1",
      summary: "Cache actions 240s appliqué",
      appliedProposalIds: ["prop_1"],
      activeNotes: "Cache optimisé",
      current: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
