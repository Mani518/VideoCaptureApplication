package com.example.videocaptureapplication

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.videocaptureapplication.capture.CaptureScreen
import com.example.videocaptureapplication.list.VideoListScreen
import com.example.videocaptureapplication.player.VideoPlayerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

private object Routes {
    const val LIST = "list"
    const val CAPTURE = "capture"
    const val PLAYER = "player/{uri}/{name}"

    fun player(uri: Uri, name: String) = "player/${Uri.encode(uri.toString())}/${Uri.encode(name)}"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            VideoListScreen(
                onAddVideoClick = { navController.navigate(Routes.CAPTURE) },
                onVideoClick = { video ->
                    navController.navigate(Routes.player(video.uri, video.name))
                }
            )
        }

        composable(Routes.CAPTURE) {
            CaptureScreen(onClose = { navController.popBackStack() })
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { entry ->
            val uri = Uri.parse(entry.arguments?.getString("uri").orEmpty())
            val name = entry.arguments?.getString("name").orEmpty()
            VideoPlayerScreen(
                videoUri = uri,
                title = name,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
