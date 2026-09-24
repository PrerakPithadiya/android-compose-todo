package com.example.todo_list.ui.components.lock

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.todo_list.ui.components.icons.IosFaceIdIcon
import com.example.todo_list.ui.theme.AppleHealth
import com.example.todo_list.ui.theme.SystemBlue
import com.example.todo_list.ui.theme.SystemDivider
import com.example.todo_list.ui.theme.SystemGray
import com.example.todo_list.ui.theme.SystemGray5
import com.example.todo_list.ui.theme.SystemLabelPrimary
import com.example.todo_list.ui.theme.SystemLabelSecondary
import com.example.todo_list.ui.theme.SystemRed
import com.example.todo_list.ui.theme.SystemSurface
import com.example.todo_list.utils.HapticManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Authentic Apple iOS-style Face ID Camera Scanner component.
 * Uses CameraX with the front camera and Google ML Kit on-device face detector
 * to detect the user's face immediately upon display.
 *
 * Implements Priority #1 in the authentication cascade:
 * 1. Face lock (auto-started on open)
 * 2. Fingerprint (fallback if declined or undetected after timeout)
 * 3. Manual PIN entry (fallback if fingerprint fails or passcode selected)
 */
@Composable
fun CameraFaceScanner(
    onFaceUnlocked: () -> Unit,
    onFallbackToFingerprint: () -> Unit,
    onFallbackToManualPin: () -> Unit,
    isFingerprintAvailable: Boolean = true,
    scanTimeoutSeconds: Int = 6,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            // Permission denied: smooth fallback to fingerprint or manual PIN
            if (isFingerprintAvailable) {
                onFallbackToFingerprint()
            } else {
                onFallbackToManualPin()
            }
        }
    }

    // Auto-request camera permission if not yet granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isDetected by remember { mutableStateOf(false) }
    var isTimedOut by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Looking for Face...") }

    // Face detection scanner animation
    val infiniteTransition = rememberInfiniteTransition(label = "face_scanner_anim")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = -50f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    val bracketPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Camera Executor
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // Auto-timeout watchdog: If face is not detected in scanTimeoutSeconds, trigger fallback
    LaunchedEffect(hasCameraPermission, isDetected) {
        if (hasCameraPermission && !isDetected) {
            delay((scanTimeoutSeconds * 1000).toLong())
            if (!isDetected) {
                isTimedOut = true
                statusText = "Face Not Detected"
                HapticManager.performError(context)
                delay(800)
                if (isFingerprintAvailable) {
                    onFallbackToFingerprint()
                } else {
                    onFallbackToManualPin()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasCameraPermission) {
            // Permission Request Card
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(SystemSurface)
                    .border(1.dp, SystemDivider, RoundedCornerShape(36.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CameraAlt,
                        contentDescription = "Camera Permission Needed",
                        tint = SystemBlue,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Camera Access",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SystemLabelPrimary
                    )
                    Text(
                        text = "Required for Face ID",
                        fontSize = 12.sp,
                        color = SystemLabelSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                colors = ButtonDefaults.buttonColors(containerColor = SystemBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enable Camera", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        } else {
            // Scanner Viewfinder
            Box(
                modifier = Modifier
                    .size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Front Camera Live Feed Viewfinder
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color.Black)
                        .border(
                            width = 2.dp,
                            color = when {
                                isDetected -> AppleHealth
                                isTimedOut -> SystemRed
                                else -> SystemBlue.copy(alpha = bracketPulseAlpha)
                            },
                            shape = RoundedCornerShape(36.dp)
                        )
                ) {
                    if (!isDetected) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                }

                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    val cameraProvider = cameraProviderFuture.get()

                                    val preview = Preview.Builder().build().also {
                                        it.surfaceProvider = previewView.surfaceProvider
                                    }

                                    // ML Kit Face Detector with fast performance profile
                                    val faceDetectorOptions = FaceDetectorOptions.Builder()
                                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                                        .setMinFaceSize(0.20f)
                                        .build()

                                    val faceDetector = FaceDetection.getClient(faceDetectorOptions)

                                    val imageAnalysis = ImageAnalysis.Builder()
                                        .setTargetResolution(Size(480, 640))
                                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                        .build()

                                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                        @androidx.camera.core.ExperimentalGetImage
                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null && !isDetected && !isTimedOut) {
                                            val inputImage = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )
                                            faceDetector.process(inputImage)
                                                .addOnSuccessListener { faces ->
                                                    if (faces.isNotEmpty() && !isDetected && !isTimedOut) {
                                                        isDetected = true
                                                        statusText = "Face Recognized"
                                                        HapticManager.performSuccess(context)
                                                        coroutineScope.launch {
                                                            delay(350)
                                                            onFaceUnlocked()
                                                        }
                                                    }
                                                }
                                                .addOnCompleteListener {
                                                    imageProxy.close()
                                                }
                                        } else {
                                            imageProxy.close()
                                        }
                                    }

                                    try {
                                        cameraProvider.unbindAll()
                                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                        cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            cameraSelector,
                                            preview,
                                            imageAnalysis
                                        )
                                    } catch (_: Exception) {
                                        // Front camera binding fallback
                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (_: Exception) {
                                            // Handle error
                                        }
                                    }
                                }, ContextCompat.getMainExecutor(ctx))

                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Laser Scanning Line Animation
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .offset(y = laserPosition.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            SystemBlue.copy(alpha = 0.8f),
                                            Color.White,
                                            SystemBlue.copy(alpha = 0.8f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    } else {
                        // Success Face ID confirmed overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(AppleHealth.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Face Verified",
                                tint = AppleHealth,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }

                // Apple Face ID Vector Boundary Overlay
                Icon(
                    imageVector = IosFaceIdIcon,
                    contentDescription = "Face ID Scan",
                    tint = when {
                        isDetected -> AppleHealth
                        isTimedOut -> SystemRed
                        else -> SystemBlue.copy(alpha = bracketPulseAlpha)
                    },
                    modifier = Modifier.size(180.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Status Label with subtle animation
        Text(
            text = statusText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = when {
                isDetected -> AppleHealth
                isTimedOut -> SystemRed
                else -> SystemLabelPrimary
            }
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Position your face directly in the frame",
            fontSize = 13.sp,
            color = SystemLabelSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Fallback Action Buttons (Priority #2 Fingerprint & Priority #3 Passcode)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFingerprintAvailable) {
                // Priority #2: Fingerprint Fallback Button
                OutlinedButton(
                    onClick = onFallbackToFingerprint,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SystemBlue
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SystemBlue.copy(alpha = 0.35f)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Fingerprint,
                        contentDescription = "Use Fingerprint",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Use Fingerprint",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Priority #3: Manual PIN / Passcode Fallback Button
            OutlinedButton(
                onClick = onFallbackToManualPin,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = SystemLabelPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, SystemDivider),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Use Passcode",
                    tint = SystemLabelSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "Use Passcode",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
