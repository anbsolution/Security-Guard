package com.securityguard.app.core.audio
import android.content.Context
object AudioManager {
    var bgmEnabled=false
    var sfxEnabled=true
    var masterMuted=false
    private var bgm: BgmManager?=null
    fun init(context: Context){ bgm=BgmManager(context.applicationContext) }
    fun setBgm(enabled:Boolean){ bgmEnabled=enabled; if(enabled&&!masterMuted) bgm?.play() else bgm?.pause() }
    fun setMasterMuted(muted:Boolean){ masterMuted=muted; if(muted) bgm?.pause() else if(bgmEnabled) bgm?.play() }
    fun emit(event: AudioEvent){ if(!sfxEnabled||masterMuted) return; SfxManager.play(event) }
}
