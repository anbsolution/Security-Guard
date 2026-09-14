package com.securityguard.app.core.audio
import android.media.AudioManager
import android.media.ToneGenerator
object SfxManager { private var tone:ToneGenerator?=null
 fun play(event:AudioEvent){ tone=ToneGenerator(AudioManager.STREAM_NOTIFICATION,80); tone?.startTone(when(event){AudioEvent.Error,AudioEvent.RoundLate->ToneGenerator.TONE_PROP_NACK; AudioEvent.Success,AudioEvent.CheckIn,AudioEvent.CheckOut,AudioEvent.RoundStart,AudioEvent.CheckpointComplete,AudioEvent.RoundComplete,AudioEvent.BackupComplete,AudioEvent.RestoreComplete->ToneGenerator.TONE_PROP_ACK; else->ToneGenerator.TONE_PROP_BEEP}),120) }
}
