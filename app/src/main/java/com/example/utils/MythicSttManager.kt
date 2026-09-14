package com.example.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class MythicSttManager(
    private val context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onStateChanged: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    fun init() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("MythicSttManager", "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("MythicSttManager", "Beginning of speech")
                }

                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    Log.d("MythicSttManager", "End of speech")
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                        SpeechRecognizer.ERROR_SERVER -> "Error from server"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                        else -> "Didn't understand, please try again."
                    }
                    Log.e("MythicSttManager", "Error: $errorMessage")
                    onError(errorMessage)
                    stopListening()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches[0])
                    }
                    stopListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            onError("Speech recognition is not available on this device")
        }
    }

    fun startListening() {
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
            isListening = true
            onStateChanged(true)
        }
    }

    fun stopListening() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
            onStateChanged(false)
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
