package com.whyraya.videoplayerdrm

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64


class VideoDrmKeyManager(context: Context, settingsKey: String) {

    private val prefs: SharedPreferences = context.getSharedPreferences(settingsKey, 0)

    fun saveKeySetId(key: String, keySetId: ByteArray) {
        val encodedKey = Base64.encodeToString(key.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val encodedKeySetId = Base64.encodeToString(keySetId, Base64.NO_WRAP)
        prefs.edit()
            .putString(encodedKey, encodedKeySetId)
            .apply()
    }

    fun getKeySetId(key: String): ByteArray? {
        val encodedKey = Base64.encodeToString(key.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val encodedKeySetId = prefs.getString(encodedKey, null) ?: return null
        return Base64.decode(encodedKeySetId, 0)
    }

    fun remove(key: String) = prefs.edit()
        .remove(Base64.encodeToString(key.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
        .apply()
}
