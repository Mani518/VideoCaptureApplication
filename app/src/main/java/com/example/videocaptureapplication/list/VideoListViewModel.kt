package com.example.videocaptureapplication.list

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videocaptureapplication.data.VideoItem
import com.example.videocaptureapplication.data.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoListViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = VideoRepository(app)

    private val _videos = MutableStateFlow<List<VideoItem>>(emptyList())
    val videos: StateFlow<List<VideoItem>> = _videos

    fun loadVideos() {
        viewModelScope.launch {
            _videos.value = withContext(Dispatchers.IO) { repository.getVideos() }
        }
    }
    fun deleteVideo(video: VideoItem) {
        viewModelScope.launch {
            val deleted = withContext(Dispatchers.IO) {
                repository.deleteVideo(video)
            }

            if (deleted) {
                loadVideos()
            }
        }
    }
}
