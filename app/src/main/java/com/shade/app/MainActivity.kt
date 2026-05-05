package com.shade.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.messaging.FirebaseMessaging
import com.shade.app.data.repository.AppPrefsRepository
import com.shade.app.security.KeyVaultManager
import com.shade.app.ui.audit.SecurityAuditScreen
import com.shade.app.ui.auth.AuthScreen
import com.shade.app.ui.chat.ChatScreen
import com.shade.app.ui.contacts.ContactsScreen
import com.shade.app.ui.home.HomeScreen
import com.shade.app.ui.lock.LockScreen
import com.shade.app.ui.myprofile.MyProfileScreen
import com.shade.app.ui.qr.QrScannerScreen
import com.shade.app.ui.navigation.Screen
import com.shade.app.ui.qr.QrScreen
import com.shade.app.ui.theme.ShadeTheme
import com.shade.app.ui.user.ProfileScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SHADE_NAV"

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var keyVaultManager: KeyVaultManager
    @Inject lateinit var appPrefsRepository: AppPrefsRepository

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d("FCM", "Notification permission granted: $isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        Log.d(TAG, "MainActivity onCreate")
        askNotificationPermission()

        val pendingChatId   = intent?.getStringExtra("chatId")
        val pendingChatName = intent?.getStringExtra("chatName")

        setContent {
            // ── Theme state ────────────────────────────────────────────────────
            val isDark by appPrefsRepository.isDarkTheme.collectAsState(initial = true)

            // ── App-lock state ─────────────────────────────────────────────────
            var isLocked by remember { mutableStateOf(false) }
            var lockError by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                val enabled = appPrefsRepository.isAppLockEnabled.first()
                val hasPin  = appPrefsRepository.hasPin()
                if (enabled && hasPin) isLocked = true
            }

            ShadeTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLocked) {
                        LockScreen(
                            pinError = lockError,
                            onPinComplete = { pin ->
                                lifecycleScope.launch {
                                    if (appPrefsRepository.verifyPin(pin)) {
                                        isLocked  = false
                                        lockError = false
                                    } else {
                                        lockError = true
                                    }
                                }
                            },
                            onBiometricRequest = {
                                showBiometric(
                                    onSuccess = { isLocked = false; lockError = false },
                                    onError   = { lockError = false }
                                )
                            }
                        )
                    } else {
                        AppNavigation(
                            pendingChatId   = pendingChatId,
                            pendingChatName = pendingChatName,
                            isDarkTheme     = isDark,
                            onToggleTheme   = {
                                lifecycleScope.launch {
                                    appPrefsRepository.setDarkTheme(!isDark)
                                }
                            }
                        )
                    }
                }
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                lifecycleScope.launch { keyVaultManager.saveFcmToken(task.result) }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showBiometric(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val mgr = BiometricManager.from(this)
        if (mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            onError("Biyometrik kullanılamıyor")
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
                override fun onAuthenticationFailed() {}
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Shade'i Aç")
            .setSubtitle("Kimliğinizi doğrulayın")
            .setNegativeButtonText("PIN Kullan")
            .build()
        prompt.authenticate(info)
    }
}

@Composable
fun AppNavigation(
    pendingChatId: String?   = null,
    pendingChatName: String? = null,
    isDarkTheme: Boolean     = true,
    onToggleTheme: () -> Unit = {}
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

    NavHost(navController = navController, startDestination = Screen.Auth.route) {

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
                    navController.navigate(Screen.Chat.createRoute(chatId, chatName))
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.Contacts.route)
                },
                onLogout = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onSecurityAuditClick = {
                    navController.navigate(Screen.SecurityAudit.route)
                },
                onQrClick = {
                    navController.navigate(Screen.Qr.route)
                },
                onMyProfileClick = {
                    navController.navigate(Screen.MyProfile.route)
                },
                onQrScannerClick = {
                    navController.navigate(Screen.QrScanner.route)
                },
                isDarkTheme   = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }

        composable(Screen.Contacts.route) {
            Log.d(TAG, "→ Contacts ekranı")
            ContactsScreen(
                onBackClick = { navController.popBackStack() },
                onContactClick = { shadeId, displayName ->
                    navController.navigate(Screen.Chat.createRoute(shadeId, displayName))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("chatId")   { type = NavType.StringType },
                navArgument("chatName") { type = NavType.StringType }
            )
        ) {
            Log.d(TAG, "→ Chat ekranı")
            ChatScreen(
                onBackClick    = { navController.popBackStack() },
                onProfileClick = { shadeId ->
                    navController.navigate(Screen.Profile.createRoute(shadeId))
                }
            )
        }

        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("shadeId") { type = NavType.StringType })
        ) {
            Log.d(TAG, "→ Profile ekranı")
            ProfileScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.SecurityAudit.route) {
            Log.d(TAG, "→ SecurityAudit ekranı")
            SecurityAuditScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Qr.route) {
            Log.d(TAG, "→ QR ekranı")
            QrScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.MyProfile.route) {
            Log.d(TAG, "→ MyProfile ekranı")
            MyProfileScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.QrScanner.route) {
            Log.d(TAG, "→ QrScanner ekranı")
            QrScannerScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
