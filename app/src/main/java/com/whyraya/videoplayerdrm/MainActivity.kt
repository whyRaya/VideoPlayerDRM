package com.whyraya.videoplayerdrm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.whyraya.videoplayerdrm.theme.MyComposeAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@UnstableApi
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyComposeAppTheme(dynamicColor = false) {
                VideoPlayerContent()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    @UnstableApi
    @Preview
    fun VideoPlayerContent() {
        val viewModel = hiltViewModel<VideoViewModel>()
        val videoItem by viewModel.videoItem.collectAsState()

        var lifecycle by remember {
            mutableStateOf(Lifecycle.Event.ON_CREATE)
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                lifecycle = event
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        val selectVideoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
            onResult = { uri ->
                uri?.let(viewModel::addVideoUri)
            }
        )
        var textVideoUrl by remember {
            mutableStateOf("https://cdn.bitmovin.com/content/assets/art-of-motion_drm/mpds/11331.mpd")
        }
        var textVideoLicense by remember {
            mutableStateOf("https://cwip-shaka-proxy.appspot.com/no_auth")
        }
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp, 56.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        PlayerView(context).also {
                            it.player = viewModel.player
                        }
                    },
                    update = {
                        when (lifecycle) {
                            Lifecycle.Event.ON_PAUSE -> {
                                it.onPause()
                                it.player?.pause()
                            }

                            Lifecycle.Event.ON_RESUME -> {
                                it.onResume()
                            }

                            else -> Unit
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16 / 9f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    singleLine = true,
                    value = textVideoUrl,
                    onValueChange = { textVideoUrl = it },
                    label = { Text("Video URL") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    singleLine = true,
                    value = textVideoLicense,
                    onValueChange = { textVideoLicense = it },
                    label = { Text("License URL") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                IconButton(
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
                    onClick = {
                    viewModel.playDashVideo(textVideoUrl, textVideoLicense)
                }) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        tint = Color.Blue,
                        contentDescription = "Select video",
                        modifier = Modifier.height(56.dp).width(56.dp)
                    )
                }
                IconButton(onClick = {
                    selectVideoLauncher.launch("video/mp4")
                }) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        tint = Color.Green,
                        contentDescription = "Select video"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(videoItem) { item ->
                        Text(
                            text = item.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playVideo(item.contentUri)
                                }
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
