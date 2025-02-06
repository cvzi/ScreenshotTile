package com.github.cvzi.screenshottile.assist

import android.content.Intent
import android.os.Bundle
import android.os.RemoteException
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import android.util.Log
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.ToastType
import com.github.cvzi.screenshottile.utils.getLocalizedString
import com.github.cvzi.screenshottile.utils.setUserLanguage
import com.github.cvzi.screenshottile.utils.toastMessage

/**
 * On Android 11 and lower: when the default assistant app is set, the assistant app is also used
 * to provide speech recognition. When an app requests speech recognition this service is started
 * and onStartListening is called. Instead of listening to the audio, this implementation
 * immediately returns the string "The default assistant app "ScreenshotTile" does not
 * offer speech recognition!" to the requesting app.
 *
 * On Android 12 and higher the speech recognition and the assistant app are separated by default
 * but the assistant app can still be selected manually as the speech recognition app.
 *
 * https://github.com/cvzi/ScreenshotTile/issues/190
 */
class MyRecognitionService : RecognitionService() {
    override fun onStartListening(intent: Intent, callback: Callback) {
        setUserLanguage()
        val errorMsg =
            "The default assistant app \"${getLocalizedString(R.string.app_name)}\" does not offer speech recognition!"

        try {
            callback.results(Bundle().apply {
                putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(errorMsg))
            })
        } catch (e: RemoteException) {
            Log.e("MyRecognitionService", "callback.results: RemoteException", e)
        }

        toastMessage("⚠️ $errorMsg", ToastType.ERROR)
    }

    override fun onCancel(callback: Callback) = Unit
    override fun onStopListening(callback: Callback) = Unit
}