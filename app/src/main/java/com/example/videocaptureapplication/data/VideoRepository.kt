package com.example.videocaptureapplication.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.video.MediaStoreOutputOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VideoRepository(
    private val context: Context
) {

    companion object {
        private const val FOLDER = "Download/VideoCapture"
    }

    fun createOutputOptions(): MediaStoreOutputOptions {
        val fileName = "VID_" +
                SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.US
                ).format(Date()) +
                ".mp4"

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                FOLDER
            )
        }

        return MediaStoreOutputOptions.Builder(
            context.contentResolver,
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        )
            .setContentValues(values)
            .build()
    }

    fun getVideos(): List<VideoItem> {

        val videos = mutableListOf<VideoItem>()

        val collection =
            MediaStore.Downloads.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DURATION
        )

        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND " +
                    "${MediaStore.MediaColumns.MIME_TYPE} LIKE ?"

        val args = arrayOf(
            "$FOLDER%",
            "video/%"
        )

        val sortOrder =
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            args,
            sortOrder
        )?.use { cursor ->

            val idCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns._ID
                )

            val nameCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DISPLAY_NAME
                )

            val durCol =
                cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.DURATION
                )

            while (cursor.moveToNext()) {

                val id = cursor.getLong(idCol)

                videos.add(
                    VideoItem(
                        id = id,
                        name = cursor
                            .getString(nameCol)
                            .substringBeforeLast("."),
                        uri = ContentUris.withAppendedId(
                            collection,
                            id
                        ),
                        durationMs = cursor.getLong(durCol)
                    )
                )
            }
        }

        return videos
    }
    fun deleteVideo(video: VideoItem): Boolean {
        return try {
            context.contentResolver.delete(
                video.uri,
                null,
                null
            ) > 0
        } catch (e: Exception) {
            false
        }
    }
}