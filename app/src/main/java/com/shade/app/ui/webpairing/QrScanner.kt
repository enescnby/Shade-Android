package com.shade.app.ui.webpairing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun QrScanner(
    modifier: Modifier = Modifier,
    onQrText: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onQrTextLatest by rememberUpdatedState(onQrText)

    val permissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val previewViewState = remember { mutableStateOf<PreviewView?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> permissionGranted.value = granted }
    )

    LaunchedEffect(Unit) {
        if (!permissionGranted.value) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!permissionGranted.value) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                Text("Kamera izni ver")
            }
        }
        return
    }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                previewViewState.value = this
            }
        },
        update = { previewViewState.value = it }
    )

    val previewView = previewViewState.value
    DisposableEffect(permissionGranted.value, lifecycleOwner, previewView) {
        if (permissionGranted.value && previewView != null) {
            bindCamera(
                context = context,
                previewView = previewView,
                onQrText = { onQrTextLatest(it) },
                lifecycleOwner = lifecycleOwner,
                permissionGranted = permissionGranted,
                analysisExecutor = analysisExecutor
            )
        }
        onDispose {
            tryUnbindAll(context)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
private fun bindCamera(
    context: Context,
    previewView: PreviewView,
    onQrText: (String) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    permissionGranted: MutableState<Boolean>,
    analysisExecutor: Executor
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)

    cameraProviderFuture.addListener(
        {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val delivered = AtomicBoolean(false)
            analysis.setAnalyzer(
                analysisExecutor,
                object : ImageAnalysis.Analyzer {
                    override fun analyze(imageProxy: ImageProxy) {
                        if (delivered.get()) {
                            imageProxy.close()
                            return
                        }
                        val mediaImage = imageProxy.image
                        if (mediaImage == null) {
                            imageProxy.close()
                            return
                        }
                        val image = InputImage.fromMediaImage(
                            mediaImage,
                            imageProxy.imageInfo.rotationDegrees
                        )
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val qr = barcodes.firstOrNull()?.rawValue
                                    ?: barcodes.firstOrNull()?.displayValue
                                if (!qr.isNullOrBlank() && delivered.compareAndSet(false, true)) {
                                    onQrText(qr)
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }
            )

            try {
                cameraProvider.unbindAll()
                if (!permissionGranted.value) return@addListener
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (_: Exception) {
                // UI tarafında ayrıca hata gösterilmiyor; analyzer hiç çalışmaz.
            }
        },
        mainExecutor
    )
}

private fun tryUnbindAll(context: Context) {
    val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener(
        { runCatching { future.get().unbindAll() } },
        mainExecutor
    )
}
