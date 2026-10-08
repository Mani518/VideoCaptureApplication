package com.example.videocaptureapplication.list

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.videocaptureapplication.data.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoListScreen(
    onAddVideoClick: () -> Unit,
    onVideoClick: (VideoItem) -> Unit,
    viewModel: VideoListViewModel = viewModel()
) {

    val videos by viewModel.videos.collectAsState()


    LaunchedEffect(Unit) {
        viewModel.loadVideos()
    }


    var videoToDelete by remember {
        mutableStateOf<VideoItem?>(null)
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Text("My Videos")
                }
            )
        },

        floatingActionButton = {

            ExtendedFloatingActionButton(

                onClick = onAddVideoClick,

                icon = {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null
                    )
                },

                text = {
                    Text("Add Video Capture")
                }
            )
        }

    ) { padding ->

        if (videos.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = "No videos yet. Tap the button to record one."
                )
            }

        } else {

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),

                contentPadding = PaddingValues(12.dp)

            ) {

                items(
                    items = videos,
                    key = { it.id }
                ) { video ->

                    VideoRow(

                        video = video,

                        onClick = {
                            onVideoClick(video)
                        },

                        onDelete = {
                            videoToDelete = video
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }
            }
        }
    }

    videoToDelete?.let { video ->

        AlertDialog(

            onDismissRequest = {
                videoToDelete = null
            },

            title = {
                Text("Delete Video?")
            },

            text = {
                Text(
                    "Are you sure you want to delete \"${video.name}\"?"
                )
            },

            confirmButton = {

                TextButton(

                    onClick = {

                        viewModel.deleteVideo(video)

                        videoToDelete = null
                    }

                ) {

                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(

                    onClick = {
                        videoToDelete = null
                    }

                ) {

                    Text("Cancel")
                }
            }
        )
    }
}


@Composable
private fun VideoRow(
    video: VideoItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    val context = LocalContext.current


    val thumbnail by produceState<Bitmap?>(
        initialValue = null,
        key1 = video.uri
    ) {

        value = withContext(Dispatchers.IO) {

            loadThumbnail(
                context = context,
                uri = video.uri
            )
        }
    }

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(
                onClick = onClick
            )
            .padding(8.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {


        Box(

            modifier = Modifier
                .size(
                    width = 110.dp,
                    height = 70.dp
                )
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .background(Color.Black),

            contentAlignment = Alignment.Center
        ) {

            thumbnail?.let { bitmap ->

                Image(

                    bitmap = bitmap.asImageBitmap(),

                    contentDescription = video.name,

                    contentScale = ContentScale.Crop,

                    modifier = Modifier.fillMaxSize()
                )
            }


            Icon(

                imageVector = Icons.Default.PlayCircle,

                contentDescription = null,

                tint = Color.White.copy(
                    alpha = 0.85f
                )
            )
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )


        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(

                text = video.name,

                style = MaterialTheme.typography.titleMedium,

                maxLines = 1,

                overflow = TextOverflow.Ellipsis
            )

            Text(

                text = video.durationText,

                style = MaterialTheme.typography.bodySmall
            )
        }

        IconButton(
            onClick = onDelete
        ) {

            Icon(

                imageVector = Icons.Default.Delete,

                contentDescription = "Delete ${video.name}",

                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun loadThumbnail(
    context: Context,
    uri: Uri
): Bitmap? {

    return try {

        context.contentResolver.loadThumbnail(
            uri,
            Size(320, 200),
            null
        )

    } catch (e: Exception) {

        try {

            val retriever =
                MediaMetadataRetriever()

            retriever.setDataSource(
                context,
                uri
            )

            val frame =
                retriever.getFrameAtTime(0)

            retriever.release()

            frame

        } catch (e2: Exception) {

            null
        }
    }
}