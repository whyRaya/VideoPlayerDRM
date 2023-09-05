package com.whyraya.videoplayerdrm

import android.app.Application
import android.net.Uri
import android.provider.MediaStore


class MetaDataReader(private val app: Application) {

    fun getMetaDataFromUri(contentUri: Uri): MetaData? {
        if (contentUri.scheme != "content") return null
        val fileName = app.contentResolver.query(
            contentUri,
            arrayOf(MediaStore.Video.VideoColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(MediaStore.Video.VideoColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(index)
        }
        return fileName?.let {
            MetaData(name = Uri.parse(it).lastPathSegment ?: return null)
        }
    }
}

data class MetaData(
    val name: String
)
