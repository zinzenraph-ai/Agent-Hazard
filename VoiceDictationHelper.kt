package com.example.voice

import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

object VoiceDictationHelper {

    fun createFrenchSpeechIntent(): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle à Hazard...")
        }

    fun mergeDictatedText(existing: String, dictated: String): String {
        val cleanSpoken = dictated.trim()
        if (cleanSpoken.isEmpty()) return existing
        val cleanExisting = existing.trimEnd()
        return if (cleanExisting.isEmpty()) cleanSpoken else "$cleanExisting $cleanSpoken"
    }

    fun mapSpeechErrorToFrench(errorCode: Int): String = when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Problème audio avec le microphone."
        SpeechRecognizer.ERROR_CLIENT -> "Erreur interne de dictée, réessaie."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission micro requise pour la dictée vocale."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Réseau indisponible pour la reconnaissance vocale."
        SpeechRecognizer.ERROR_NO_MATCH -> "Je n'ai pas compris, essaie de parler plus près du micro."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Le micro est déjà occupé, appuie à nouveau."
        SpeechRecognizer.ERROR_SERVER -> "Service vocal momentanément indisponible."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Aucune voix détectée. Appuie sur le micro et parle."
        else -> "Dictée vocale interrompue, essaie encore."
    }
}
