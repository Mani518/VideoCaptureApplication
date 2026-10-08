package com.example.videocaptureapplication.capture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.videocaptureapplication.data.VideoRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

private fun hasAllPermissions(context: Context) = REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

@Composable
fun CaptureScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(hasAllPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionGranted = results.values.all { it }
        if (!permissionGranted) {
            Toast.makeText(context, "Camera and microphone permission required", Toast.LENGTH_SHORT).show()
            onClose()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    if (permissionGranted) {
        CaptureContent(onClose = onClose)
    } else {
        Box(Modifier.fillMaxSize().background(Color.Black))
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun CaptureContent(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val repository = remember { VideoRepository(context) }

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        }
    }

    // UI state
    var isRecording by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var isFlashOn by remember { mutableStateOf(false) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableIntStateOf(0) }       // 0 = off, 3 or 10
    var countdown by remember { mutableIntStateOf(0) }          // shown while timer is running
    var selectedZoom by remember { mutableStateOf(1f) }
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }

    // Zoom limits come from the camera. 0.5x only shows up on phones that have an ultra-wide lens.
    val zoomState by cameraController.zoomState.observeAsState()
    val minZoom = zoomState?.minZoomRatio ?: 1f
    val maxZoom = zoomState?.maxZoomRatio ?: 1f
    val zoomOptions = listOf(0.5f, 1f, 2f).filter { it >= minZoom - 0.01f && it <= maxZoom }

    DisposableEffect(lifecycleOwner) {
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            activeRecording?.stop()
            cameraController.unbind()
        }
    }

    fun startRecording() {
        val options = repository.createOutputOptions()
        activeRecording = cameraController.startRecording(
            options,
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(context)
        ) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    isRecording = true
                    isPaused = false
                }
                is VideoRecordEvent.Status -> {
                    elapsedSeconds = event.recordingStats.recordedDurationNanos / 1_000_000_000
                }
                is VideoRecordEvent.Pause -> isPaused = true
                is VideoRecordEvent.Resume -> isPaused = false
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    isPaused = false
                    activeRecording = null
                    elapsedSeconds = 0
                    if (event.hasError()) {
                        Toast.makeText(context, "Recording failed (${event.error})", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Saved to Downloads/VideoCapture", Toast.LENGTH_SHORT).show()
                        onClose()
                    }
                }
            }
        }
    }

    fun onRecordButtonClick() {
        if (countdown > 0) return           // timer already running

        if (isRecording) {
            activeRecording?.stop()
            return
        }

        if (timerSeconds == 0) {
            startRecording()
        } else {
            scope.launch {
                for (i in timerSeconds downTo 1) {
                    countdown = i
                    delay(1000)
                }
                countdown = 0
                startRecording()
            }
        }
    }

    fun toggleCamera() {
        isFrontCamera = !isFrontCamera
        cameraController.cameraSelector =
            if (isFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        // reset flash and zoom, front camera has neither
        isFlashOn = false
        selectedZoom = 1f
        cameraController.enableTorch(false)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            }
        )

        // ---- top bar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 8.dp, vertical = 32.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose, enabled = !isRecording) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            if (isRecording) {
                Text(
                    text = "%02d:%02d".format(elapsedSeconds / 60, elapsedSeconds % 60),
                    color = if (isPaused) Color.Yellow else Color.Red,
                    fontSize = 18.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Flash
                IconButton(onClick = {
                    isFlashOn = !isFlashOn
                    cameraController.enableTorch(isFlashOn)
                }) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = Color.White
                    )
                }

                // Timer: Off -> 3s -> 10s -> Off
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(enabled = !isRecording) {
                            timerSeconds = when (timerSeconds) {
                                0 -> 3
                                3 -> 10
                                else -> 0
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Timer, contentDescription = "Timer", tint = Color.White)
                    if (timerSeconds > 0) {
                        Text("${timerSeconds}s", color = Color.White, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // Front / back
                IconButton(onClick = { toggleCamera() }, enabled = !isRecording) {
                    Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Switch camera", tint = Color.White)
                }
            }
        }

        // ---- countdown ----
        if (countdown > 0) {
            Text(
                text = countdown.toString(),
                color = Color.White,
                fontSize = 96.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        // ---- bottom controls ----
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // zoom chips
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                zoomOptions.forEach { zoom ->
                    val selected = selectedZoom == zoom
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color.White else Color.Black.copy(alpha = 0.5f))
                            .clickable {
                                selectedZoom = zoom
                                cameraController.setZoomRatio(zoom)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (zoom) {
                                0.5f -> "0.5x"
                                1f -> "1x"
                                else -> "2x"
                            },
                            color = if (selected) Color.Black else Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.size(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Pause / Resume (only while recording)
                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                    if (isRecording) {
                        IconButton(onClick = {
                            if (isPaused) activeRecording?.resume() else activeRecording?.pause()
                        }) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = "Pause or resume",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.width(24.dp))

                // Start / Stop
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(8.dp)
                        .clip(if (isRecording) RoundedCornerShape(8.dp) else CircleShape)
                        .background(Color.Red)
                        .clickable { onRecordButtonClick() }
                )

                Spacer(Modifier.width(24.dp))
                Spacer(Modifier.size(56.dp))   // keeps the record button centered
            }
        }
    }
}
