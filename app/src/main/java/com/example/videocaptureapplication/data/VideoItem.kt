package com.example.videocaptureapplication.data

import android.net.Uri

data class VideoItem(
    val id: Long,
    val name: String,
    val uri: Uri,
    val durationMs: Long
) {
    val durationText: String
        get() {
            val totalSec = durationMs / 1000
            return "%02d:%02d".format(totalSec / 60, totalSec % 60)
        }
}
