package com.securityguard.app.core.haptics
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
object HapticManager { var enabled=true; private var vibrator:Vibrator?=null
 fun init(context:Context){ vibrator=if(android.os.Build.VERSION.SDK_INT>=31) context.getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }
 fun click(){ if(enabled) vibrator?.vibrate(VibrationEffect.createOneShot(28,VibrationEffect.DEFAULT_AMPLITUDE)) }
 fun success(){ if(enabled) vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0,35,45,60),-1)) }
}
