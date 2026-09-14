package com.securityguard.app.core.audio
import android.content.Context
import android.media.MediaPlayer
import com.securityguard.app.R
class BgmManager(private val context: Context){ private var player: MediaPlayer?=null
 fun play(){ if(player==null) player=MediaPlayer.create(context,R.raw.bgm_ambient)?.apply{isLooping=true;setVolume(.18f,.18f)}; player?.start() }
 fun pause(){ player?.pause() }
 fun release(){ player?.release();player=null }
}
