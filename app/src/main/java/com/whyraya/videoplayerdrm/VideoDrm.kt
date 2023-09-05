package com.whyraya.videoplayerdrm

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.dash.DashUtil
import androidx.media3.exoplayer.dash.DefaultDashChunkSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.DrmSession
import androidx.media3.exoplayer.drm.DrmSessionEventListener
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.drm.LocalMediaDrmCallback
import androidx.media3.exoplayer.drm.OfflineLicenseHelper
import java.io.IOException

@UnstableApi class VideoDrm(context: Context) {


    private val playerInfo by lazy {
        Util.getUserAgent(context, context.getString(R.string.app_name))
    }

    private val eventDispatcher by lazy {
        DrmSessionEventListener.EventDispatcher()
    }


    private val httpDataSourceFactory by lazy {
        DefaultHttpDataSource.Factory().apply {
            setUserAgent(playerInfo)
        }
    }

    private val dataSourceFactory by lazy {
        DefaultDataSource.Factory(context)
    }

    private val drmKeyManager by lazy {
        VideoDrmKeyManager(context, KEY_SETTINGS)
    }

    @Throws(
        IOException::class,
        InterruptedException::class,
        DrmSession.DrmSessionException::class
    )
    fun requestWidevineLicense(dashMediaInput: String, licenseUrl: String) {
        val dataSource = dataSourceFactory.createDataSource()
        val dashManifest = DashUtil.loadManifest(dataSource, Uri.parse(dashMediaInput))
        val drmInitData = DashUtil.loadFormatWithDrmInitData(dataSource, dashManifest.getPeriod(0))

        drmInitData?.let {
            val offlineLicenseHelper = OfflineLicenseHelper.newWidevineInstance(
                licenseUrl,
                httpDataSourceFactory,
                eventDispatcher
            )
            offlineLicenseHelper.downloadLicense(it).also { keySetId ->
                drmKeyManager.saveKeySetId(KEY_WIDEVINE, keySetId)
            }
        }
    }

    private fun getWidevineLicense() = drmKeyManager.getKeySetId(KEY_WIDEVINE)

    private fun getClearKeyLicense() = drmKeyManager.getKeySetId(KEY_CLEAR_KEY)

    fun hasValidWidevineLicense(): Boolean = try {
        getWidevineLicense()?.let { bytes ->
            OfflineLicenseHelper.newWidevineInstance(
                "",
                httpDataSourceFactory,
                eventDispatcher
            ).getLicenseDurationRemainingSec(bytes).also {
                return it.first > 0
            }
        }
        false
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }

    fun buildMediaItem(
        uri: Uri,
        isWidevine: Boolean = false
    ): MediaItem {
        val mediaItem = MediaItem.fromUri(uri)
        return when (val type = Util.inferContentType(uri)) {
            C.CONTENT_TYPE_DASH -> {
                mediaItem.createDashMediaItem(isWidevine)
            }
            C.CONTENT_TYPE_OTHER -> {
                mediaItem
            }
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }

    private fun MediaItem.createDashMediaItem(
        isWidevine: Boolean = false
    ): MediaItem {
        Log.e("Test101", "createDashMediaItem widevine $isWidevine")
        val dashMediaSource = DashMediaSource.Factory(
            DefaultDashChunkSource.Factory(dataSourceFactory),
            dataSourceFactory
        )
        val builder =  buildUpon().apply {
            if (isWidevine) {
                setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setKeySetId(getWidevineLicense())
                        .build()
                )
            } else {
                getClearKeyLicense()?.let { keySetId ->
                    val localCallBack = LocalMediaDrmCallback(keySetId)
                    val defaultDrmSessionManager = DefaultDrmSessionManager.Builder().apply {
                        setUuidAndExoMediaDrmProvider(C.CLEARKEY_UUID) { uuid ->
                            FrameworkMediaDrm.newInstance(uuid)
                        }
                    }.build(localCallBack)
                    dashMediaSource.setDrmSessionManagerProvider {
                        defaultDrmSessionManager
                    }
                }
            }
        }
        return dashMediaSource.createMediaSource(builder.build()).mediaItem
    }

    fun getUriType(uri: Uri) {
        when (val type = Util.inferContentType(uri)) {
            C.CONTENT_TYPE_DASH -> Log.e("getUriType","CONTENT_TYPE_DASH")
            C.CONTENT_TYPE_SS -> Log.e("getUriType","CONTENT_TYPE_SS")
            C.CONTENT_TYPE_HLS -> Log.e("getUriType","CONTENT_TYPE_HLS")
            C.CONTENT_TYPE_RTSP -> Log.e("getUriType","CONTENT_TYPE_RTSP")
            C.CONTENT_TYPE_OTHER -> Log.e("getUriType","CONTENT_TYPE_OTHER")
            else -> throw IllegalStateException("Unsupported type: $type")
        }
    }


    companion object {
        const val KEY_SETTINGS = "video_drm"
        const val KEY_WIDEVINE = "wv"
        const val KEY_CLEAR_KEY = "ck"
    }


}


