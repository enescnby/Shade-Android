package com.shade.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.shade.app.security.KeyVaultManager
import com.shade.app.ui.auth.AuthScreen
import com.shade.app.ui.chat.ChatScreen
import com.shade.app.ui.home.HomeScreen
import com.shade.app.ui.navigation.Screen
import com.shade.app.ui.theme.ShadeTheme
import com.shade.app.ui.user.ProfileScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var keyVaultManager: KeyVaultManager
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("FCM", "Notification permission granted: $isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        askNotificationPermission()

        val pendingChatId = intent?.getStringExtra("chatId")
        val pendingChatName = intent?.getStringExtra("chatName")

        setContent {
            ShadeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM", "Token: ${task.result}")
                    keyVaultManager.saveFcmToken(token)
                } else {
                    Log.e("FCM", "Token alınamadı", task.exception)
                }
            }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun AppNavigation(
    pendingChatId: String? = null,
    pendingChatName: String? = null
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingChatId) {
        if (pendingChatId != null && pendingChatName != null) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Auth.route) {inclusive = true}
            }
            navController.navigate(Screen.Chat.createRoute(pendingChatId, pendingChatName))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            AuthScreen(
                viewModel = hiltViewModel(),
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onChatClick = { chatId, chatName ->
                    navController.navigate(Screen.Chat.createRoute(chatId, chatName))
                },
                onNavigateToContacts = {
                    // Rehber ekranı eklendiğinde buraya gelecek
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("chatName") { type = NavType.StringType }
            )
        ) {
            ChatScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onProfileClick = { shadeId ->
                    navController.navigate(Screen.Profile.createRoute(shadeId))
                }
            )
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("shadeId") { type = NavType.StringType })
        ) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
