package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.model.ChatMessage
import com.example.model.CreationCategory
import com.example.model.CreationItem
import com.example.model.MarketQuote
import com.example.model.MemoryProfile
import com.example.model.MissionTopic
import com.example.model.NewsArticle
import com.example.model.SavingsGoal
import com.example.model.SpeakerMode
import com.example.model.UiPreferences
import com.example.model.UiState
import com.example.ui.theme.ProfitEmerald
import com.example.ui.theme.RiskCoral
import com.example.voice.VoiceDictationHelper
import java.util.Locale

private data class QuickPromptChip(
    val label: String,
    val prompt: String,
    val topic: MissionTopic
)

private data class HomeModuleShortcut(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun ChatScreen(
    speakerMode: SpeakerMode,
    lockedWyattMode: Boolean = false,
    activeTopic: MissionTopic,
    messagesState: UiState<List<ChatMessage>>,
    isGenerating: Boolean,
    streamingText: String?,
    aiError: String?,
    hasGroqAvailable: Boolean = false,
    preferFastGroq: Boolean = false,
    uiPrefs: UiPreferences = UiPreferences(),
    topQuote: MarketQuote? = null,
    topNews: NewsArticle? = null,
    primaryGoal: SavingsGoal? = null,
    nextMusicTrack: CreationItem? = null,
    memoryProfile: MemoryProfile? = null,
    unresolvedSecurityAlerts: Int = 0,
    pendingProposalsCount: Int = 0,
    onToggleFastGroq: () -> Unit = {},
    onToggleThemeDarkLight: () -> Unit = {},
    onOpenPersonalize: () -> Unit = {},
    onKeepTrial: () -> Unit = {},
    onCancelTrial: () -> Unit = {},
    onNavigateShortcut: (String) -> Unit = {},
    onSelectSpeakerMode: (SpeakerMode) -> Unit,
    onSendMessage: (String, MissionTopic?) -> Unit,
    onSaveCreation: (CreationCategory, String, String) -> Unit,
    onDismissError: () -> Unit,
    onNewSession: () -> Unit,
    onRequestMemoryExtract: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var partialVoiceText by remember { mutableStateOf("") }
    var voiceRmsLevel by remember { mutableFloatStateOf(0f) }
    var voiceNotice by remember { mutableStateOf<String?>(null) }

    val isWyatt = speakerMode == SpeakerMode.WYATT || lockedWyattMode

    // Text-to-Speech in French
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.FRANCE
                ttsReady = true
            }
        }
        instance
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    val speechFallbackLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        partialVoiceText = ""
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = VoiceDictationHelper.mergeDictatedText(inputText, spoken)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    fun startVoiceListening() {
        voiceNotice = null
        partialVoiceText = ""
        voiceRmsLevel = 0f

        val recognizer = speechRecognizer
        if (recognizer == null) {
            val fallbackIntent = VoiceDictationHelper.createFrenchSpeechIntent()
            if (fallbackIntent.resolveActivity(context.packageManager) != null) {
                isListening = true
                speechFallbackLauncher.launch(fallbackIntent)
            } else {
                voiceNotice = "Reconnaissance vocale indisponible sur cet appareil."
            }
            return
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {
                voiceRmsLevel = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                isListening = false
                voiceRmsLevel = 0f
            }
            override fun onError(error: Int) {
                isListening = false
                partialVoiceText = ""
                voiceRmsLevel = 0f
                if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                    recognizer.cancel()
                }
                voiceNotice = VoiceDictationHelper.mapSpeechErrorToFrench(error)
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                partialVoiceText = ""
                voiceRmsLevel = 0f
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    inputText = VoiceDictationHelper.mergeDictatedText(inputText, spoken)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    partialVoiceText = partial
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer.startListening(VoiceDictationHelper.createFrenchSpeechIntent())
    }

    val audioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceListening()
        } else {
            voiceNotice = "Permission micro refusée. Tu peux taper ton message."
        }
    }

    fun triggerVoiceOrPerm() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            voiceRmsLevel = 0f
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                startVoiceListening()
            } else {
                audioPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val rawMessages = (messagesState as? UiState.Success)?.data.orEmpty()
    val messages = remember(rawMessages, isWyatt) {
        if (isWyatt) rawMessages.filter { it.speakerMode == SpeakerMode.WYATT.wireValue }
        else rawMessages
    }
    var lastMessageCount by remember { mutableIntStateOf(-1) }
    LaunchedEffect(messages.size, streamingText) {
        if (lastMessageCount in 0 until messages.size || streamingText != null) {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
        lastMessageCount = messages.size
    }

    val quickChips = remember(isWyatt) {
        if (isWyatt) {
            listOf(
                QuickPromptChip("🚀 Aventure folle", "Invente-nous une aventure spatiale complètement folle dont je suis le héros !", MissionTopic.WYATT_ADVENTURE),
                QuickPromptChip("🔬 Expérience science", "Apprends-moi un secret de science incroyable avec un truc qu'on a à la maison !", MissionTopic.WYATT_ADVENTURE),
                QuickPromptChip("🦖 Défi rigolo", "Lance-moi un défi idiot mais trop marrant à faire maintenant !", MissionTopic.WYATT_ADVENTURE),
                QuickPromptChip("🎵 Chanson débile", "Écrivons une chanson super drôle sur un dinosaure qui fait du skate !", MissionTopic.WYATT_ADVENTURE)
            )
        } else {
            listOf(
                QuickPromptChip("🎵 Paroles (2 versions)", "Écris-moi un texte de chanson percutant en 2 versions différentes + le prompt Suno complet (style, instruments, BPM).", MissionTopic.MUSIC),
                QuickPromptChip("📈 Analyser un setup", "Aide-moi à analyser un setup trading/crypto comme un pro : structure, liquidité, R:R et risques.", MissionTopic.TRADING),
                QuickPromptChip("🛡️ Audit anti-arnaque", "Donne-moi ton filtre pro pour vérifier qu'un projet crypto ou une offre de gain n'est pas une arnaque.", MissionTopic.TRADING),
                QuickPromptChip("📱 Promo TikTok", "Fais-moi un plan TikTok concret avec 3 hooks de 3 secondes, description et hashtags pour pousser un son.", MissionTopic.PROMO),
                QuickPromptChip("💶 Revenus téléphone", "Propose-moi 3 pistes réalistes pour gagner de l'argent honnêtement avec uniquement mon téléphone, puis recommande la meilleure.", MissionTopic.PROJECTS),
                QuickPromptChip("✉️ Brouillon Admin", "J'ai un courrier ou mail administratif important à rédiger : demande-moi les détails pour sortir un brouillon prêt à copier.", MissionTopic.ADMIN)
            )
        }
    }

    val trialActive = !isWyatt && uiPrefs.trialExpiresAtMs > System.currentTimeMillis() && uiPrefs.trialLabel.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            item(key = "hero_speaker") {
                HazardHeroSpeakerCard(
                    speakerMode = if (isWyatt) SpeakerMode.WYATT else speakerMode,
                    lockedWyattMode = lockedWyattMode,
                    activeTopic = activeTopic,
                    hasGroqAvailable = hasGroqAvailable,
                    preferFastGroq = preferFastGroq,
                    themeMode = uiPrefs.themeMode,
                    onToggleFastGroq = onToggleFastGroq,
                    onToggleThemeDarkLight = onToggleThemeDarkLight,
                    onOpenPersonalize = onOpenPersonalize,
                    onSelectSpeakerMode = onSelectSpeakerMode,
                    onNewSession = onNewSession,
                    onRequestMemoryExtract = onRequestMemoryExtract
                )
            }

            if (trialActive) {
                item(key = "trial_banner") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Science, contentDescription = "Essai 24 h", tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Essai visuel 24 h : ${uiPrefs.trialLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onKeepTrial, modifier = Modifier.weight(1f)) {
                                    Text("Garder")
                                }
                                OutlinedButton(onClick = onCancelTrial, modifier = Modifier.weight(1f)) {
                                    Text("Annuler")
                                }
                            }
                        }
                    }
                }
            }

            // Mobile-first big input field directly on Home screen
            item(key = "home_big_talk_box") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (isWyatt) "Parle à ton grand frère Hazard !" else "Parler à Hazard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val displayedHomeText = if (isListening && partialVoiceText.isNotBlank()) {
                            VoiceDictationHelper.mergeDictatedText(inputText, partialVoiceText)
                        } else {
                            inputText
                        }
                        if (isListening) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Micro actif",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Écoute en cours… Parle maintenant à Hazard",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { voiceRmsLevel.coerceIn(0.12f, 1f) },
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = displayedHomeText,
                            onValueChange = {
                                if (!isListening) {
                                    inputText = it
                                }
                            },
                            placeholder = {
                                Text(
                                    if (isWyatt) "Pose ta question rigolote ou inventons un jeu..."
                                    else "Écris ou dicte ta demande (son Suno, trade, TikTok, budget, admin)..."
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = ::triggerVoiceOrPerm,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("home_field_mic_button")
                                ) {
                                    Icon(
                                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = if (isListening) "Arrêter la dictée vocale" else "Dicter dans le champ d'accueil",
                                        tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_talk_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = ::triggerVoiceOrPerm,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("home_voice_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isListening) "Arrêter la dictée" else "Dicter à voix haute",
                                    tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (isListening) "Stop" else "Dictée")
                            }
                            Button(
                                onClick = {
                                    val toSend = inputText.trim()
                                    if (toSend.isNotEmpty()) {
                                        inputText = ""
                                        onSendMessage(toSend, null)
                                    }
                                },
                                enabled = inputText.isNotBlank() && !isGenerating && !isListening,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("home_send_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (isWyatt) "C'est parti !" else "Envoyer à Hazard")
                            }
                        }
                    }
                }
            }

            // Thumb-friendly Home Module Shortcuts underneath
            item(key = "home_shortcuts_grid") {
                if (isWyatt) {
                    WyattOversizedShortcuts(onSendMessage = onSendMessage)
                } else {
                    HomeModuleShortcutsGrid(
                        uiPrefs = uiPrefs,
                        unresolvedSecurityAlerts = unresolvedSecurityAlerts,
                        pendingProposalsCount = pendingProposalsCount,
                        onNavigateShortcut = onNavigateShortcut
                    )
                }
            }

            // Customizable Home Gadgets (Raphaël mode only)
            if (!isWyatt && uiPrefs.enabledGadgets.isNotEmpty()) {
                item(key = "home_gadgets_section") {
                    HomeGadgetsSection(
                        enabledGadgets = uiPrefs.enabledGadgets,
                        topQuote = topQuote,
                        topNews = topNews,
                        primaryGoal = primaryGoal,
                        nextMusicTrack = nextMusicTrack,
                        memoryProfile = memoryProfile,
                        onNavigateShortcut = onNavigateShortcut
                    )
                }
            }

            if (messagesState is UiState.Loading) {
                item(key = "messages_loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (messagesState is UiState.Error) {
                item(key = "messages_error") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = messagesState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else if (messages.isEmpty()) {
                item(key = "empty_greeting") {
                    HazardEmptyGreetingCard(
                        speakerMode = if (isWyatt) SpeakerMode.WYATT else speakerMode,
                        lockedWyattMode = lockedWyattMode,
                        onSelectSpeakerMode = onSelectSpeakerMode
                    )
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onCopy = { clipboard.setText(AnnotatedString(msg.text)) },
                    onSpeak = {
                        if (ttsReady) {
                            tts.speak(msg.text, TextToSpeech.QUEUE_FLUSH, null, msg.id)
                        }
                    },
                    onConfirmYes = { onSendMessage("Oui, je valide cette action.", MissionTopic.fromWire(msg.topic)) },
                    onConfirmNo = { onSendMessage("Non, on n'agit pas encore, ajuste la proposition.", MissionTopic.fromWire(msg.topic)) },
                    onSaveToVault = {
                        val category = when (MissionTopic.fromWire(msg.topic)) {
                            MissionTopic.MUSIC -> CreationCategory.MUSIC
                            MissionTopic.PROMO -> CreationCategory.PROMO
                            MissionTopic.ADMIN -> CreationCategory.ADMIN
                            MissionTopic.PROJECTS -> CreationCategory.PROJECTS
                            MissionTopic.TRADING -> CreationCategory.TRADING
                            MissionTopic.WYATT_ADVENTURE -> CreationCategory.WYATT
                            MissionTopic.GENERAL ->
                                if (isWyatt) CreationCategory.WYATT else CreationCategory.PROJECTS
                        }
                        val title = msg.text.lineSequence().firstOrNull { it.isNotBlank() }?.take(60)
                            ?: "Note Hazard"
                        onSaveCreation(category, title, msg.text)
                    }
                )
            }

            if (streamingText != null) {
                item(key = "streaming_bubble") {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Hazard réfléchit...",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (streamingText.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = streamingText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        if (aiError != null || voiceNotice != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = aiError ?: voiceNotice.orEmpty(),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        onDismissError()
                        voiceNotice = null
                    }) {
                        Text("OK")
                    }
                }
            }
        }

        // Quick mission chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickChips.forEach { chip ->
                AssistChip(
                    onClick = { onSendMessage(chip.prompt, chip.topic) },
                    label = { Text(chip.label, style = MaterialTheme.typography.labelLarge) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                )
            }
        }

        // Sticky quick composer bar for one-handed bottom access while reading replies
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = ::triggerVoiceOrPerm,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isListening) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isListening) MaterialTheme.colorScheme.onError
                        else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_input_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isListening) "Arrêter la dictée" else "Parler à Hazard"
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            if (isWyatt) "Répondre à Hazard..."
                            else "Répondre vite à Hazard..."
                        )
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                FilledIconButton(
                    onClick = {
                        val toSend = inputText.trim()
                        if (toSend.isNotEmpty()) {
                            inputText = ""
                            onSendMessage(toSend, null)
                        }
                    },
                    enabled = inputText.isNotBlank() && !isGenerating,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Envoyer"
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeModuleShortcutsGrid(
    uiPrefs: UiPreferences,
    unresolvedSecurityAlerts: Int,
    pendingProposalsCount: Int,
    onNavigateShortcut: (String) -> Unit
) {
    val allCatalog = mapOf(
        "musique" to HomeModuleShortcut("musique", "Studio Musique", "Textes, Flow & Suno", Icons.Default.MusicNote),
        "finance" to HomeModuleShortcut("finance", "Finance", "Cours, Bot & Journal", Icons.AutoMirrored.Filled.TrendingUp),
        "plans" to HomeModuleShortcut("plans", "Plans", "Budget, Épargne & SaaS", Icons.Default.AccountBalanceWallet),
        "actualites" to HomeModuleShortcut("actualites", "Actualités", "Actu éco simplifiée", Icons.Default.Newspaper),
        "wyatt" to HomeModuleShortcut("wyatt", "Wyatt", "Passer en Mode 7 ans", Icons.Default.ChildCare),
        "securite" to HomeModuleShortcut(
            "securite",
            "Sécurité",
            if (unresolvedSecurityAlerts > 0) "$unresolvedSecurityAlerts alerte(s)" else "Bouclier & Accès",
            Icons.Default.Shield
        ),
        "propositions" to HomeModuleShortcut(
            "propositions",
            "Propositions",
            if (pendingProposalsCount > 0) "$pendingProposalsCount à valider" else "Amélioration IA",
            Icons.Default.AutoAwesome
        )
    )

    val orderedIds = (uiPrefs.moduleOrder + listOf("musique", "finance", "plans", "actualites", "wyatt", "securite", "propositions"))
        .distinct()
        .filter { it in allCatalog && it !in uiPrefs.hiddenModules }

    if (orderedIds.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Raccourcis rapides",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        orderedIds.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { id ->
                    val item = allCatalog.getValue(id)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateShortcut(item.id) }
                            .testTag("shortcut_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = item.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WyattOversizedShortcuts(
    onSendMessage: (String, MissionTopic?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Choisis ta super mission !",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        val cards = listOf(
            listOf(
                "🚀 Aventure Spatiale",
                "Cap sur Mars !",
                "Invente une mission secrète sur Mars où je pilote une fusée rigolote !",
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary
            ),
            listOf(
                "🔬 Labo Magique",
                "Science maison",
                "Explique-moi une expérience de science trop cool et sans danger avec de l'eau ou du papier !",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.secondary
            ),
            listOf(
                "🦖 Défi Rigolo",
                "Jeu du salon",
                "Donne-moi un défi de grand frère super marrant à faire dans le salon !",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.tertiary
            ),
            listOf(
                "🎵 Chanson Maboule",
                "Paroles drôles",
                "Inventons une chanson délirante sur un robot qui mange des pizzas !",
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.primary
            )
        )
        cards.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pair.forEach { entry ->
                    val title = entry[0] as String
                    val subtitle = entry[1] as String
                    val prompt = entry[2] as String
                    val bg = entry[3] as Color
                    val accent = entry[4] as Color
                    Card(
                        colors = CardDefaults.cardColors(containerColor = bg),
                        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(104.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onSendMessage(prompt, MissionTopic.WYATT_ADVENTURE) }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGadgetsSection(
    enabledGadgets: List<String>,
    topQuote: MarketQuote?,
    topNews: NewsArticle?,
    primaryGoal: SavingsGoal?,
    nextMusicTrack: CreationItem?,
    memoryProfile: MemoryProfile?,
    onNavigateShortcut: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Mes gadgets d'accueil",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if ("crypto" in enabledGadgets) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateShortcut("finance") }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Gadget Crypto Live", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        if (topQuote != null) {
                            Text(
                                text = "${topQuote.symbol} · ${String.format(Locale.FRANCE, "%.2f €", topQuote.priceEur)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text("Appuie pour charger tes cours en direct", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    if (topQuote != null) {
                        val up = topQuote.change24hPct >= 0
                        Text(
                            text = String.format(Locale.FRANCE, "%+.2f%%", topQuote.change24hPct),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (up) ProfitEmerald else RiskCoral
                        )
                    }
                }
            }
        }

        if ("news" in enabledGadgets) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateShortcut("actualites") }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Gadget Actu du jour", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    Text(
                        text = topNews?.title ?: "Ouvre Actualités pour le résumé éco de Hazard",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if ("goal" in enabledGadgets) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateShortcut("plans") }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Savings, contentDescription = null, tint = ProfitEmerald, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gadget Objectif du mois", style = MaterialTheme.typography.labelSmall, color = ProfitEmerald)
                    }
                    if (primaryGoal != null) {
                        val pct = (primaryGoal.savedEur / primaryGoal.targetEur.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
                        Text(
                            text = "${primaryGoal.title} : ${String.format(Locale.FRANCE, "%.0f / %.0f €", primaryGoal.savedEur, primaryGoal.targetEur)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(progress = { pct }, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text("Ajoute un objectif d'épargne dans Plans", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        if ("track" in enabledGadgets) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onNavigateShortcut("musique") }
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Gadget Prochain morceau", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    val trackLabel = nextMusicTrack?.title
                        ?: memoryProfile?.inProgress?.takeIf { it.isNotBlank() }
                        ?: "Crée un nouveau texte ou prompt Suno avec Hazard"
                    Text(
                        text = trackLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun HazardHeroSpeakerCard(
    speakerMode: SpeakerMode,
    lockedWyattMode: Boolean,
    activeTopic: MissionTopic,
    hasGroqAvailable: Boolean,
    preferFastGroq: Boolean,
    themeMode: String,
    onToggleFastGroq: () -> Unit,
    onToggleThemeDarkLight: () -> Unit,
    onOpenPersonalize: () -> Unit,
    onSelectSpeakerMode: (SpeakerMode) -> Unit,
    onNewSession: () -> Unit,
    onRequestMemoryExtract: () -> Unit
) {
    val isWyatt = speakerMode == SpeakerMode.WYATT || lockedWyattMode
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp)
            ) {
                Image(
                    painter = painterResource(
                        id = if (isWyatt) R.drawable.img_wyatt_adventure else R.drawable.img_hazard_studio
                    ),
                    contentDescription = if (isWyatt) "Atelier d'aventure de Wyatt" else "Studio de Hazard",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isWyatt) R.drawable.img_avatar_wyatt else R.drawable.img_avatar_raph
                        ),
                        contentDescription = "Badge actif",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isWyatt) "Mode Wyatt (7 ans)" else "Hazard · QG Raphaël",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isWyatt) "Grand frère, science & défis rigolos"
                            else "Focus : ${activeTopic.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onNewSession) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Nouvelle session",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (!isWyatt && !lockedWyattMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onSelectSpeakerMode(SpeakerMode.RAPHAEL) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (speakerMode == SpeakerMode.RAPHAEL)
                                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (speakerMode == SpeakerMode.RAPHAEL)
                                MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_raphael_button")
                    ) {
                        Text("C'est moi (Raph)")
                    }

                    Button(
                        onClick = { onSelectSpeakerMode(SpeakerMode.WYATT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_wyatt_button")
                    ) {
                        Text("C'est Wyatt")
                    }

                    OutlinedButton(
                        onClick = onRequestMemoryExtract,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("update_memory_from_chat_button")
                    ) {
                        Text("MàJ Fiche")
                    }
                }
            }

            if (!isWyatt) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onOpenPersonalize,
                        label = { Text("Personnaliser") },
                        leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.testTag("open_personalize_chip")
                    )
                    AssistChip(
                        onClick = onToggleThemeDarkLight,
                        label = { Text(if (themeMode == "dark") "Passer en Clair" else "Passer en Sombre") },
                        leadingIcon = {
                            Icon(
                                if (themeMode == "dark") Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("toggle_theme_chip")
                    )
                    if (hasGroqAvailable) {
                        FilterChip(
                            selected = preferFastGroq,
                            onClick = onToggleFastGroq,
                            label = { Text(if (preferFastGroq) "⚡ Groq Rapide ON" else "⚡ Groq Rapide") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HazardEmptyGreetingCard(
    speakerMode: SpeakerMode,
    lockedWyattMode: Boolean,
    onSelectSpeakerMode: (SpeakerMode) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = when (speakerMode) {
                    SpeakerMode.UNKNOWN -> "C'est toi Raph ou Wyatt ?"
                    SpeakerMode.WYATT -> "Salut Wyatt ! Prêt pour une mission de grand frère ou une invention complètement maboule ?"
                    SpeakerMode.RAPHAEL -> "On attaque quoi aujourd'hui Raph ? Un son Suno, un plan TikTok, une analyse trading propre ou un plan concret pour faire rentrer du cash ?"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (speakerMode == SpeakerMode.UNKNOWN && !lockedWyattMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { onSelectSpeakerMode(SpeakerMode.RAPHAEL) }) {
                        Text("C'est moi (Raph)")
                    }
                    OutlinedButton(onClick = { onSelectSpeakerMode(SpeakerMode.WYATT) }) {
                        Text("C'est Wyatt")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    onCopy: () -> Unit,
    onSpeak: () -> Unit,
    onConfirmYes: () -> Unit,
    onConfirmNo: () -> Unit,
    onSaveToVault: () -> Unit
) {
    val isUser = message.role == "user"
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (message.speakerMode == SpeakerMode.WYATT.wireValue) {
                                "HAZARD · GRAND FRÈRE"
                            } else {
                                "HAZARD · ${MissionTopic.fromWire(message.topic).label.uppercase()}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row {
                            IconButton(
                                onClick = onSpeak,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Écouter à voix haute",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copier le message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onSaveToVault,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkAdd,
                                    contentDescription = "Sauver dans l'Atelier",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor
                )

                if (!isUser && message.requiresConfirmation) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = "Action importante",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Action importante : j'attends ton OUI",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = onConfirmYes,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Oui, je valide")
                                }
                                OutlinedButton(
                                    onClick = onConfirmNo,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Non, ajuste")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
