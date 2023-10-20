package com.github.cvzi.screenshottile.utils

import android.media.AudioManager
import android.media.ToneGenerator
import android.media.ToneGenerator.TONE_CDMA_ABBR_ALERT
import android.media.ToneGenerator.TONE_CDMA_ABBR_INTERCEPT
import android.media.ToneGenerator.TONE_CDMA_ABBR_REORDER
import android.media.ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE
import android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
import android.media.ToneGenerator.TONE_CDMA_ALERT_INCALL_LITE
import android.media.ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE
import android.media.ToneGenerator.TONE_CDMA_ANSWER
import android.media.ToneGenerator.TONE_CDMA_CALLDROP_LITE
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_INTERGROUP
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PAT3
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PAT5
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PAT6
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PAT7
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_PING_RING
import android.media.ToneGenerator.TONE_CDMA_CALL_SIGNAL_ISDN_SP_PRI
import android.media.ToneGenerator.TONE_CDMA_CONFIRM
import android.media.ToneGenerator.TONE_CDMA_DIAL_TONE_LITE
import android.media.ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK
import android.media.ToneGenerator.TONE_CDMA_HIGH_L
import android.media.ToneGenerator.TONE_CDMA_HIGH_PBX_L
import android.media.ToneGenerator.TONE_CDMA_HIGH_PBX_SLS
import android.media.ToneGenerator.TONE_CDMA_HIGH_PBX_SS
import android.media.ToneGenerator.TONE_CDMA_HIGH_PBX_SSL
import android.media.ToneGenerator.TONE_CDMA_HIGH_PBX_S_X4
import android.media.ToneGenerator.TONE_CDMA_HIGH_SLS
import android.media.ToneGenerator.TONE_CDMA_HIGH_SS
import android.media.ToneGenerator.TONE_CDMA_HIGH_SSL
import android.media.ToneGenerator.TONE_CDMA_HIGH_SS_2
import android.media.ToneGenerator.TONE_CDMA_HIGH_S_X4
import android.media.ToneGenerator.TONE_CDMA_INTERCEPT
import android.media.ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE
import android.media.ToneGenerator.TONE_CDMA_LOW_L
import android.media.ToneGenerator.TONE_CDMA_LOW_PBX_L
import android.media.ToneGenerator.TONE_CDMA_LOW_PBX_SLS
import android.media.ToneGenerator.TONE_CDMA_LOW_PBX_SS
import android.media.ToneGenerator.TONE_CDMA_LOW_PBX_SSL
import android.media.ToneGenerator.TONE_CDMA_LOW_PBX_S_X4
import android.media.ToneGenerator.TONE_CDMA_LOW_SLS
import android.media.ToneGenerator.TONE_CDMA_LOW_SS
import android.media.ToneGenerator.TONE_CDMA_LOW_SSL
import android.media.ToneGenerator.TONE_CDMA_LOW_SS_2
import android.media.ToneGenerator.TONE_CDMA_LOW_S_X4
import android.media.ToneGenerator.TONE_CDMA_MED_L
import android.media.ToneGenerator.TONE_CDMA_MED_PBX_L
import android.media.ToneGenerator.TONE_CDMA_MED_PBX_SLS
import android.media.ToneGenerator.TONE_CDMA_MED_PBX_SS
import android.media.ToneGenerator.TONE_CDMA_MED_PBX_SSL
import android.media.ToneGenerator.TONE_CDMA_MED_PBX_S_X4
import android.media.ToneGenerator.TONE_CDMA_MED_SLS
import android.media.ToneGenerator.TONE_CDMA_MED_SS
import android.media.ToneGenerator.TONE_CDMA_MED_SSL
import android.media.ToneGenerator.TONE_CDMA_MED_SS_2
import android.media.ToneGenerator.TONE_CDMA_MED_S_X4
import android.media.ToneGenerator.TONE_CDMA_NETWORK_BUSY
import android.media.ToneGenerator.TONE_CDMA_NETWORK_BUSY_ONE_SHOT
import android.media.ToneGenerator.TONE_CDMA_NETWORK_CALLWAITING
import android.media.ToneGenerator.TONE_CDMA_NETWORK_USA_RINGBACK
import android.media.ToneGenerator.TONE_CDMA_ONE_MIN_BEEP
import android.media.ToneGenerator.TONE_CDMA_PIP
import android.media.ToneGenerator.TONE_CDMA_PRESSHOLDKEY_LITE
import android.media.ToneGenerator.TONE_CDMA_REORDER
import android.media.ToneGenerator.TONE_CDMA_SIGNAL_OFF
import android.media.ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE
import android.media.ToneGenerator.TONE_DTMF_0
import android.media.ToneGenerator.TONE_DTMF_1
import android.media.ToneGenerator.TONE_DTMF_2
import android.media.ToneGenerator.TONE_DTMF_3
import android.media.ToneGenerator.TONE_DTMF_4
import android.media.ToneGenerator.TONE_DTMF_5
import android.media.ToneGenerator.TONE_DTMF_6
import android.media.ToneGenerator.TONE_DTMF_7
import android.media.ToneGenerator.TONE_DTMF_8
import android.media.ToneGenerator.TONE_DTMF_9
import android.media.ToneGenerator.TONE_DTMF_A
import android.media.ToneGenerator.TONE_DTMF_B
import android.media.ToneGenerator.TONE_DTMF_C
import android.media.ToneGenerator.TONE_DTMF_D
import android.media.ToneGenerator.TONE_DTMF_P
import android.media.ToneGenerator.TONE_DTMF_S
import android.media.ToneGenerator.TONE_PROP_ACK
import android.media.ToneGenerator.TONE_PROP_BEEP
import android.media.ToneGenerator.TONE_PROP_BEEP2
import android.media.ToneGenerator.TONE_PROP_NACK
import android.media.ToneGenerator.TONE_PROP_PROMPT
import android.media.ToneGenerator.TONE_SUP_BUSY
import android.media.ToneGenerator.TONE_SUP_CALL_WAITING
import android.media.ToneGenerator.TONE_SUP_CONFIRM
import android.media.ToneGenerator.TONE_SUP_CONGESTION
import android.media.ToneGenerator.TONE_SUP_CONGESTION_ABBREV
import android.media.ToneGenerator.TONE_SUP_DIAL
import android.media.ToneGenerator.TONE_SUP_ERROR
import android.media.ToneGenerator.TONE_SUP_INTERCEPT
import android.media.ToneGenerator.TONE_SUP_INTERCEPT_ABBREV
import android.media.ToneGenerator.TONE_SUP_PIP
import android.media.ToneGenerator.TONE_SUP_RADIO_ACK
import android.media.ToneGenerator.TONE_SUP_RADIO_NOTAVAIL
import android.media.ToneGenerator.TONE_SUP_RINGTONE
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.github.cvzi.screenshottile.App

class Sound {

    companion object {
        val allTones = hashMapOf(
            "CDMA_ABBR_ALERT" to TONE_CDMA_ABBR_ALERT,
            "CDMA_ABBR_INTERCEPT" to TONE_CDMA_ABBR_INTERCEPT,
            "CDMA_ABBR_REORDER" to TONE_CDMA_ABBR_REORDER,
            "CDMA_ALERT_AUTOREDIAL_LITE" to TONE_CDMA_ALERT_AUTOREDIAL_LITE,
            "CDMA_ALERT_CALL_GUARD" to TONE_CDMA_ALERT_CALL_GUARD,
            "CDMA_ALERT_INCALL_LITE" to TONE_CDMA_ALERT_INCALL_LITE,
            "CDMA_ALERT_NETWORK_LITE" to TONE_CDMA_ALERT_NETWORK_LITE,
            "CDMA_ANSWER" to TONE_CDMA_ANSWER,
            "CDMA_CALLDROP_LITE" to TONE_CDMA_CALLDROP_LITE,
            "CDMA_CALL_SIGNAL_ISDN_INTERGROUP" to TONE_CDMA_CALL_SIGNAL_ISDN_INTERGROUP,
            "CDMA_CALL_SIGNAL_ISDN_NORMAL" to TONE_CDMA_CALL_SIGNAL_ISDN_NORMAL,
            "CDMA_CALL_SIGNAL_ISDN_PAT3" to TONE_CDMA_CALL_SIGNAL_ISDN_PAT3,
            "CDMA_CALL_SIGNAL_ISDN_PAT5" to TONE_CDMA_CALL_SIGNAL_ISDN_PAT5,
            "CDMA_CALL_SIGNAL_ISDN_PAT6" to TONE_CDMA_CALL_SIGNAL_ISDN_PAT6,
            "CDMA_CALL_SIGNAL_ISDN_PAT7" to TONE_CDMA_CALL_SIGNAL_ISDN_PAT7,
            "CDMA_CALL_SIGNAL_ISDN_PING_RING" to TONE_CDMA_CALL_SIGNAL_ISDN_PING_RING,
            "CDMA_CALL_SIGNAL_ISDN_SP_PRI" to TONE_CDMA_CALL_SIGNAL_ISDN_SP_PRI,
            "CDMA_CONFIRM" to TONE_CDMA_CONFIRM,
            "CDMA_DIAL_TONE_LITE" to TONE_CDMA_DIAL_TONE_LITE,
            "CDMA_EMERGENCY_RINGBACK" to TONE_CDMA_EMERGENCY_RINGBACK,
            "CDMA_HIGH_L" to TONE_CDMA_HIGH_L,
            "CDMA_HIGH_PBX_L" to TONE_CDMA_HIGH_PBX_L,
            "CDMA_HIGH_PBX_SLS" to TONE_CDMA_HIGH_PBX_SLS,
            "CDMA_HIGH_PBX_SS" to TONE_CDMA_HIGH_PBX_SS,
            "CDMA_HIGH_PBX_SSL" to TONE_CDMA_HIGH_PBX_SSL,
            "CDMA_HIGH_PBX_S_X4" to TONE_CDMA_HIGH_PBX_S_X4,
            "CDMA_HIGH_SLS" to TONE_CDMA_HIGH_SLS,
            "CDMA_HIGH_SS" to TONE_CDMA_HIGH_SS,
            "CDMA_HIGH_SSL" to TONE_CDMA_HIGH_SSL,
            "CDMA_HIGH_SS_2" to TONE_CDMA_HIGH_SS_2,
            "CDMA_HIGH_S_X4" to TONE_CDMA_HIGH_S_X4,
            "CDMA_INTERCEPT" to TONE_CDMA_INTERCEPT,
            "CDMA_KEYPAD_VOLUME_KEY_LITE" to TONE_CDMA_KEYPAD_VOLUME_KEY_LITE,
            "CDMA_LOW_L" to TONE_CDMA_LOW_L,
            "CDMA_LOW_PBX_L" to TONE_CDMA_LOW_PBX_L,
            "CDMA_LOW_PBX_SLS" to TONE_CDMA_LOW_PBX_SLS,
            "CDMA_LOW_PBX_SS" to TONE_CDMA_LOW_PBX_SS,
            "CDMA_LOW_PBX_SSL" to TONE_CDMA_LOW_PBX_SSL,
            "CDMA_LOW_PBX_S_X4" to TONE_CDMA_LOW_PBX_S_X4,
            "CDMA_LOW_SLS" to TONE_CDMA_LOW_SLS,
            "CDMA_LOW_SS" to TONE_CDMA_LOW_SS,
            "CDMA_LOW_SSL" to TONE_CDMA_LOW_SSL,
            "CDMA_LOW_SS_2" to TONE_CDMA_LOW_SS_2,
            "CDMA_LOW_S_X4" to TONE_CDMA_LOW_S_X4,
            "CDMA_MED_L" to TONE_CDMA_MED_L,
            "CDMA_MED_PBX_L" to TONE_CDMA_MED_PBX_L,
            "CDMA_MED_PBX_SLS" to TONE_CDMA_MED_PBX_SLS,
            "CDMA_MED_PBX_SS" to TONE_CDMA_MED_PBX_SS,
            "CDMA_MED_PBX_SSL" to TONE_CDMA_MED_PBX_SSL,
            "CDMA_MED_PBX_S_X4" to TONE_CDMA_MED_PBX_S_X4,
            "CDMA_MED_SLS" to TONE_CDMA_MED_SLS,
            "CDMA_MED_SS" to TONE_CDMA_MED_SS,
            "CDMA_MED_SSL" to TONE_CDMA_MED_SSL,
            "CDMA_MED_SS_2" to TONE_CDMA_MED_SS_2,
            "CDMA_MED_S_X4" to TONE_CDMA_MED_S_X4,
            "CDMA_NETWORK_BUSY" to TONE_CDMA_NETWORK_BUSY,
            "CDMA_NETWORK_BUSY_ONE_SHOT" to TONE_CDMA_NETWORK_BUSY_ONE_SHOT,
            "CDMA_NETWORK_CALLWAITING" to TONE_CDMA_NETWORK_CALLWAITING,
            "CDMA_NETWORK_USA_RINGBACK" to TONE_CDMA_NETWORK_USA_RINGBACK,
            "CDMA_ONE_MIN_BEEP" to TONE_CDMA_ONE_MIN_BEEP,
            "CDMA_PIP" to TONE_CDMA_PIP,
            "CDMA_PRESSHOLDKEY_LITE" to TONE_CDMA_PRESSHOLDKEY_LITE,
            "CDMA_REORDER" to TONE_CDMA_REORDER,
            "CDMA_SIGNAL_OFF" to TONE_CDMA_SIGNAL_OFF,
            "CDMA_SOFT_ERROR_LITE" to TONE_CDMA_SOFT_ERROR_LITE,
            "DTMF_0" to TONE_DTMF_0,
            "DTMF_1" to TONE_DTMF_1,
            "DTMF_2" to TONE_DTMF_2,
            "DTMF_3" to TONE_DTMF_3,
            "DTMF_4" to TONE_DTMF_4,
            "DTMF_5" to TONE_DTMF_5,
            "DTMF_6" to TONE_DTMF_6,
            "DTMF_7" to TONE_DTMF_7,
            "DTMF_8" to TONE_DTMF_8,
            "DTMF_9" to TONE_DTMF_9,
            "DTMF_A" to TONE_DTMF_A,
            "DTMF_B" to TONE_DTMF_B,
            "DTMF_C" to TONE_DTMF_C,
            "DTMF_D" to TONE_DTMF_D,
            "DTMF_P" to TONE_DTMF_P,
            "DTMF_S" to TONE_DTMF_S,
            "PROP_ACK" to TONE_PROP_ACK,
            "PROP_BEEP" to TONE_PROP_BEEP,
            "PROP_BEEP2" to TONE_PROP_BEEP2,
            "PROP_NACK" to TONE_PROP_NACK,
            "PROP_PROMPT" to TONE_PROP_PROMPT,
            "SUP_BUSY" to TONE_SUP_BUSY,
            "SUP_CALL_WAITING" to TONE_SUP_CALL_WAITING,
            "SUP_CONFIRM" to TONE_SUP_CONFIRM,
            "SUP_CONGESTION" to TONE_SUP_CONGESTION,
            "SUP_CONGESTION_ABBREV" to TONE_SUP_CONGESTION_ABBREV,
            "SUP_DIAL" to TONE_SUP_DIAL,
            "SUP_ERROR" to TONE_SUP_ERROR,
            "SUP_INTERCEPT" to TONE_SUP_INTERCEPT,
            "SUP_INTERCEPT_ABBREV" to TONE_SUP_INTERCEPT_ABBREV,
            "SUP_PIP" to TONE_SUP_PIP,
            "SUP_RADIO_ACK" to TONE_SUP_RADIO_ACK,
            "SUP_RADIO_NOTAVAIL" to TONE_SUP_RADIO_NOTAVAIL,
            "SUP_RINGTONE" to TONE_SUP_RINGTONE,
        )

        private fun getTone(name: String): Int {
            var id = allTones[name]
            if (id != null) {
                return id
            }
            id = allTones[name.uppercase().replace("TONE_", "").replace(" ", "_")]
            if (id != null) {
                return id
            }
            return TONE_PROP_BEEP
        }

        val allAudioSinks = hashMapOf(
            "Media" to AudioManager.STREAM_MUSIC,
            "Alarm" to AudioManager.STREAM_ALARM,
            "Notification" to AudioManager.STREAM_NOTIFICATION,
            "Ring" to AudioManager.STREAM_RING,
            "Call" to AudioManager.STREAM_VOICE_CALL
        )
        const val defaultAudioSink = "Media"

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                allAudioSinks["Accessibility"] = AudioManager.STREAM_ACCESSIBILITY
            }
        }

        private fun selectedAudioSink(): Int {
            val sinkName = App.getInstance().prefManager.soundNotificationSink
            return allAudioSinks.getOrElse(sinkName) {
                allAudioSinks.getOrDefault(defaultAudioSink, AudioManager.STREAM_MUSIC)
            }
        }

        fun selectedToneName(): String {
            val toneName = App.getInstance().prefManager.soundNotificationTone
            if (toneName.startsWith("tone:")) {
                return toneName.substring(5)
            }
            return toneName
        }

        private fun selectedTone(): Int {
            val toneName = App.getInstance().prefManager.soundNotificationTone
            if (toneName.startsWith("tone:")) {
                val toneKey = toneName.substring(5)
                return getTone(toneKey)
            }
            return TONE_PROP_BEEP
        }

        fun playTone() {
            val toneDuration = App.getInstance().prefManager.soundNotificationDuration
            val toneGenerator = ToneGenerator(selectedAudioSink(), 100)
            toneGenerator.startTone(selectedTone(), toneDuration)
            Handler(Looper.getMainLooper()).postDelayed({
                toneGenerator.release()
            }, (toneDuration + 50).toLong())
        }
    }


}