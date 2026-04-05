package com.dailychallenge.app.util

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri

object SoundHelper {
    fun playSuccessSound(context: Context) {
        val uri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (uri != null) {
            try {
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, uri)
                ringtone.play()
            } catch (_: Exception) { }
        }
    }
}
