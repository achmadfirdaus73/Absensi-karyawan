package com.example.biometrics

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class AudioFeedbackHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isEnabled = true

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val indonesian = Locale("id", "ID")
                val result = tts?.setLanguage(indonesian)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setPitch(1.0f)
                tts?.setSpeechRate(1.05f)
                isTtsReady = true
            } else {
                Log.e("AudioFeedbackHelper", "TextToSpeech initialization failed")
            }
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun speak(text: String) {
        if (!isEnabled || !isTtsReady) return
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AttendanceAudioId")
        } catch (e: Exception) {
            Log.e("AudioFeedbackHelper", "Error speaking text: ${e.message}")
        }
    }

    fun playSuccessChime() {
        if (!isEnabled) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } catch (e: Exception) {
            Log.e("AudioFeedbackHelper", "Error playing chime: ${e.message}")
        }
    }

    fun playErrorChime() {
        if (!isEnabled) return
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
            toneGen.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        } catch (e: Exception) {
            Log.e("AudioFeedbackHelper", "Error playing error chime: ${e.message}")
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("AudioFeedbackHelper", "Error shutting down TTS", e)
        }
    }
}
