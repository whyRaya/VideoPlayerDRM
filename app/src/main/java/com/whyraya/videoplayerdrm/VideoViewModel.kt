package com.whyraya.videoplayerdrm

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    val player: Player,
    private val drm: VideoDrm,
    private val metaDataReader: MetaDataReader
) : ViewModel() {

    private val videoUris = savedStateHandle.getStateFlow(KEY_VIDEO_URI, emptyList<Uri>())

    val videoItem = videoUris.map { uris ->
        uris.map { uri ->
            VideoItemDto(
                name = metaDataReader.getMetaDataFromUri(uri)?.name ?: "Untitled",
                contentUri = uri,
                mediaItem = MediaItem.fromUri(uri)
            )

        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(3500L), emptyList())

    init {
        player.prepare()
        player.playWhenReady = true
    }

    fun addVideoUri(uri: Uri) {
        savedStateHandle[KEY_VIDEO_URI] = videoUris.value + uri
        player.addMediaItem(MediaItem.fromUri(uri))
    }

    @UnstableApi
    fun playVideo(uri: Uri) {
        drm.getUriType(uri)
        val contentUri = videoItem.value.find { it.contentUri == uri }?.mediaItem ?: return

        player.setMediaItem(contentUri)
    }

    @UnstableApi
    fun playDashVideo(
        videoUrl: String,
        licenseUrl: String,
        isWidevine: Boolean = true
    ) = viewModelScope.launch(Dispatchers.IO) {
        try {
            if (drm.hasValidWidevineLicense().not()) {
                drm.requestWidevineLicense(videoUrl, licenseUrl)
            }
            launch(Dispatchers.Main) {
                player.setMediaItem(drm.buildMediaItem(videoUrl.toUri(), isWidevine))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
    }

    companion object {
        const val KEY_VIDEO_URI = "videoUris"
    }
}
