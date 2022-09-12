package com.github.cvzi.screenshottile.assist

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.utils.ToastType
import com.github.cvzi.screenshottile.utils.toastMessage

class RecognitionServiceEmpty : RecognitionService() {
    override fun onStartListening(intent: Intent, callback: Callback) {
        val errorMsg =
            "The default assistant app \"${getString(R.string.app_name)}\" does not offer speech recognition!"

        callback.results(Bundle().apply {
            putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(errorMsg))
        })

        toastMessage("⚠️ $errorMsg", ToastType.ERROR)
    }

    override fun onCancel(callback: Callback) = Unit
    override fun onStopListening(callback: Callback) = Unit
}