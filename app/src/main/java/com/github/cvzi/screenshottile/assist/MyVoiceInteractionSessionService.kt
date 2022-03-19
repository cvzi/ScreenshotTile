package com.github.cvzi.screenshottile.assist

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Service that starts the sessions
 */
class MyVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(bundle: Bundle?): VoiceInteractionSession {
        return MyVoiceInteractionSession(this)
    }
}
