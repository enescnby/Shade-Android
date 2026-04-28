package com.shade.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.shade.app.security.KeyVaultManager
import kotlinx.coroutines.launch
import com.shade.app.ui.audit.SecurityAuditScreen
import com.shade.app.ui.auth.AuthScreen
import com.shade.app.ui.chat.ChatScreen
import com.shade.app.ui.contacts.ContactsScreen
import com.shade.app.ui.home.HomeScreen
import com.shade.app.ui.navigation.Screen
import com.shade.app.ui.theme.ShadeTheme
import com.shade.app.ui.user.ProfileScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "SHADE_NAV"

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
        // Ekran görüntüsü ve ekran kaydını engelle (gizlilik uygulaması için zorunlu)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        Log.d(TAG, "MainActivity onCreate")

        askNotificationPermission()

        val pendingChatId = intent?.getStringExtra("chatId")
        val pendingChatName = intent?.getStringExtra("chatName")

        setContent {
            ShadeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        pendingChatId = pendingChatId,
                        pendingChatName = pendingChatName
                    )
                }
            }
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM", "Token: $token")
                    lifecycleScope.launch {
                        keyVaultManager.saveFcmToken(token)
                    }
                } else {
                    Log.e("FCM", "Token alınamadı", task.exception)
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
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
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
            navController.navigate(Screen.Chat.createRoute(pendingChatId, pendingChatName))
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Auth.route
    ) {
        composable(Screen.Auth.route) {
            Log.d(TAG, "→ Auth ekranı")
            AuthScreen(
                viewModel = hiltViewModel(),
                onAuthSuccess = {
                    Log.d(TAG, "Auth başarılı → Home ekranına geçiliyor")
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            Log.d(TAG, "→ Home ekranı")
            HomeScreen(
                onChatClick = { chatId, chatName ->
                    Log.d(TAG, "Home → Chat: chatId=$chatId, chatName=$chatName")
                    navController.navigate(Screen.Chat.createRoute(chatId, chatName))
                },
                onNavigateToContacts = {
                    Log.d(TAG, "Home → Contacts ekranına geçiliyor")
                    navController.navigate(Screen.Contacts.route)
                },
                onLogout = {
                    Log.d(TAG, "Çıkış yapıldı → Auth ekranına dönülüyor")
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSecurityAuditClick = {
                    Log.d(TAG, "Home → Güvenlik Günlüğü")
                    navController.navigate(Screen.SecurityAudit.route)
                }
            )
        }

        composable(Screen.Contacts.route) {
            Log.d(TAG, "→ Contacts ekranı")
            ContactsScreen(
                onBackClick = {
                    Log.d(TAG, "Contacts → geri (Home)")
                    navController.popBackStack()
                },
                onContactClick = { shadeId, displayName ->
                    Log.d(TAG, "Contacts → Chat: shadeId=$shadeId, name=$displayName")
                    navController.navigate(Screen.Chat.createRoute(shadeId, displayName))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId") { type = NavType.StringType },
                navArgument("chatName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            val chatName = backStackEntry.arguments?.getString("chatName")
            Log.d(TAG, "→ Chat ekranı: chatId=$chatId, chatName=$chatName")
            ChatScreen(
                onBackClick = {
                    Log.d(TAG, "Chat → geri")
                    navController.popBackStack()
                },
                onProfileClick = { shadeId ->
                    Log.d(TAG, "Chat → Profile: shadeId=$shadeId")
                    navController.navigate(Screen.Profile.createRoute(shadeId))
                }
            )
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("shadeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val shadeId = backStackEntry.arguments?.getString("shadeId")
            Log.d(TAG, "→ Profile ekranı: shadeId=$shadeId")
            ProfileScreen(
                onBackClick = {
                    Log.d(TAG, "Profile → geri")
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.SecurityAudit.route) {
            Log.d(TAG, "→ SecurityAudit ekranı")
            SecurityAuditScreen(
                onBackClick = {
                    Log.d(TAG, "SecurityAudit → geri")
                    navController.popBackStack()
                }
            )
        }
    }
}
