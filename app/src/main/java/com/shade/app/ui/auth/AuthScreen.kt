package com.shade.app.ui.auth

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shade.app.R
import kotlinx.coroutines.launch

enum class AuthStep {
    WELCOME, LOGIN, REGISTER
}

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.Authenticated -> {
                onAuthSuccess()
            }
            is AuthUiState.Success -> {
                if ((uiState as AuthUiState.Success).mnemonic.isEmpty()) {
                    onAuthSuccess()
                }
            }
            else -> {}
        }
    }

    if (uiState is AuthUiState.Authenticated) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        AuthScreenContent(
            uiState = uiState,
            onLogin = { shadeId, mnemonic ->
                viewModel.login(shadeId, mnemonic, "Android Device", "dummy_fcm")
            },
            onRegister = {
                viewModel.register("Android Device", "dummy_fcm")
            },
            onResetUiState = {
                viewModel.resetUiState()
            },
            onAuthSuccess = onAuthSuccess
        )
    }
}

@Composable
fun AuthScreenContent(
    uiState: AuthUiState,
    onLogin: (String, List<String>) -> Unit,
    onRegister: () -> Unit,
    onResetUiState: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    var currentStep by rememberSaveable { mutableStateOf(AuthStep.WELCOME) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentStep) {
        onResetUiState()
    }

    BackHandler(enabled = currentStep != AuthStep.WELCOME) {
        currentStep = AuthStep.WELCOME
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF000000),
                                Color(0xFF10002B),
                                Color(0xFF000000)
                            )
                        )
                    )
            )

            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState != AuthStep.WELCOME) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut())
                    }.using(
                        SizeTransform(clip = false)
                    )
                },
                label = "auth_transition"
            ) { step ->
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (step) {
                        AuthStep.WELCOME -> WelcomeLayout(onNavigate = { currentStep = it })
                        AuthStep.LOGIN -> LoginLayout(
                            uiState = uiState,
                            onLogin = onLogin,
                            onBack = { currentStep = AuthStep.WELCOME }
                        )
                        AuthStep.REGISTER -> RegisterLayout(
                            uiState = uiState,
                            onRegister = onRegister,
                            onBack = { currentStep = AuthStep.WELCOME },
                            snackbarHostState = snackbarHostState,
                            onAuthSuccess = onAuthSuccess
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomeLayout(onNavigate: (AuthStep) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(if (isLandscape) 20.dp else 40.dp))

        // Logo Section
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (isLandscape) 80.dp else 110.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Logo",
                    modifier = Modifier.size(if (isLandscape) 60.dp else 90.dp),
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = if (isLandscape) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 6.sp
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 24.dp else 40.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(if (isLandscape) 0.7f else 0.95f)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.welcome_greeting),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            InfoBubble(
                text = stringResource(R.string.privacy_motto),
                icon = Icons.Default.Lock
            )
            InfoBubble(
                text = stringResource(R.string.no_personal_data),
                icon = Icons.Default.VisibilityOff
            )
            InfoBubble(
                text = stringResource(R.string.free_anonymous),
                icon = Icons.Default.AccountCircle
            )
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 32.dp else 60.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigate(AuthStep.LOGIN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.login), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { onNavigate(AuthStep.REGISTER) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.5.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.register), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoBubble(text: String, icon: ImageVector) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun LoginLayout(
    uiState: AuthUiState,
    onLogin: (String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    var shadeIdInput by remember { mutableStateOf("") }
    var mnemonicInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.login),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = shadeIdInput,
                onValueChange = { shadeIdInput = it },
                label = { Text(stringResource(R.string.shade_id)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = mnemonicInput,
                onValueChange = { mnemonicInput = it },
                label = { Text(stringResource(R.string.mnemonic_label)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.mnemonic_placeholder), color = Color.DarkGray) },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val mnemonicList = mnemonicInput.trim().split("\\s+".toRegex())
                    onLogin(shadeIdInput, mnemonicList)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.login), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(uiState.message.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun RegisterLayout(
    uiState: AuthUiState,
    onRegister: () -> Unit,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, 
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.register),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = stringResource(R.string.register_notice),
                textAlign = TextAlign.Center,
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = {
                    if (uiState is AuthUiState.Success) {
                        onAuthSuccess()
                    }
                }
            ) {
                Text("I understand, Continue")
            }

            if (uiState !is AuthUiState.Success) {
                Button(
                    onClick = onRegister,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(stringResource(R.string.register_safely), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (uiState) {
                is AuthUiState.Success -> {
                    SuccessSection(uiState, snackbarHostState)
                }
                is AuthUiState.Error -> {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(uiState.message.asString(), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SuccessSection(state: AuthUiState.Success, snackbarHostState: SnackbarHostState) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            state.message.asString(),
            color = Color(0xFF4CAF50), 
            fontWeight = FontWeight.Bold, 
            textAlign = TextAlign.Center
        )

        state.shadeId?.let { id ->
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        scope.launch {
                            val clipData = ClipData.newPlainText("shadeId", id)
                            clipboard.setClipEntry(ClipEntry(clipData))
                            snackbarHostState.showSnackbar(context.getString(R.string.id_copied))
                        }
                    },
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.shade_id),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.LightGray
                        )
                        Text(
                            text = id,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy ID",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (state.mnemonic.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.save_mnemonic_warning),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                    .padding(12.dp)
            ) {
                items(state.mnemonic) { word ->
                    Surface(
                        modifier = Modifier.padding(4.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = word,
                            modifier = Modifier.padding(8.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val textToCopy = state.mnemonic.joinToString(" ")
                    scope.launch {
                        val clipData = ClipData.newPlainText("mnemonic", textToCopy)
                        clipboard.setClipEntry(ClipEntry(clipData))
                        snackbarHostState.showSnackbar(context.getString(R.string.mnemonic_copied))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.copy_mnemonic), color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AuthScreenPreview() {
    AuthScreenContent(
        uiState = AuthUiState.Idle,
        onLogin = { _, _ -> },
        onRegister = {},
        onResetUiState = {},
        onAuthSuccess = {}
    )
}
