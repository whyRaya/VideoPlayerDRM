package com.whyraya.videoplayerdrm

import android.net.Uri
import androidx.media3.common.MediaItem

data class VideoItemDto(
    val name: String,
    val contentUri: Uri,
    val mediaItem: MediaItem
)
