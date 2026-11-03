package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.SitePhoto
import com.example.ui.components.CameraBottomControlsBar
import com.example.ui.components.CameraPhotoPreviewDialog
import com.example.ui.components.CameraTagRow
import com.example.ui.components.CameraTelemetryHudCard
import com.example.ui.components.CameraTopBar
import com.example.ui.theme.TelecomBlueAccent
import com.example.ui.theme.TelecomNavyPrimary
import com.example.ui.viewmodel.SiteAccessViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraScreen(
    viewModel: SiteAccessViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    BackHandler {
        onClose()
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var cameraInitError by remember { mutableStateOf<String?>(null) }

    val flashAlpha = remember { Animatable(0f) }

    var liveTimeString by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        while (true) {
            liveTimeString = sdf.format(Date())
            delay(1000)
        }
    }

    val siteId by viewModel.siteId.collectAsState()
    val operator by viewModel.operator.collectAsState()
    val status by viewModel.status.collectAsState()
    val gpsData by viewModel.gpsData.collectAsState()
    val gsmData by viewModel.gsmData.collectAsState()
    val photos by viewModel.photos.collectAsState()
    val isProcessingPhoto by viewModel.isProcessingPhoto.collectAsState()
    val activeTag by viewModel.activeCameraTag.collectAsState()
    val lastCapturedPhoto by viewModel.lastCapturedPhoto.collectAsState()

    var previewPhoto by remember { mutableStateOf<SitePhoto?>(null) }

    val photoTags = listOf(
        "Site Entrance",
        "Tower Structure",
        "Shelter & Lock",
        "Security Guard Post",
        "Padlock Status",
        "Access Blockage",
        "Antennas & Dishes",
        "Power & Generator"
    )

    fun takePhoto() {
        val capture = imageCapture
        if (capture == null) {
            viewModel.addSimulatedPhoto(activeTag)
            return
        }

        scope.launch {
            flashAlpha.snapTo(0.85f)
            flashAlpha.animateTo(0f, animationSpec = tween(durationMillis = 200))
        }

        val executor = ContextCompat.getMainExecutor(context)
        capture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val rawBitmap = imageProxy.toBitmap()
                    val rotatedBitmap = if (rotationDegrees != 0) {
                        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                        Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                    } else {
                        rawBitmap
                    }
                    imageProxy.close()

                    viewModel.addPhotoBitmap(rotatedBitmap, activeTag)
                }

                override fun onError(exception: ImageCaptureException) {
                    viewModel.addSimulatedPhoto(activeTag)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen")
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(TelecomNavyPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera Permission",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Camera access is required to capture site evidence with GPS coordinates, GSM cell info, and timestamps burned into the photo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = TelecomNavyPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_camera_permission_button")
                ) {
                    Text("Grant Camera Permission", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.addSimulatedPhoto(activeTag) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("simulate_capture_no_permission")
                ) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Field Test Simulation Mode", color = Color.White)
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("cancel_camera_button")
                ) {
                    Text("Cancel and Return", color = Color.Gray)
                }
            }
        } else {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val provider = cameraProviderFuture.get()
                            cameraProvider = provider

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()
                            imageCapture = capture

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            provider.unbindAll()
                            boundCamera = provider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                            cameraInitError = null
                        } catch (e: Exception) {
                            cameraInitError = e.localizedMessage ?: "Camera initialization error"
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            if (cameraInitError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = TelecomBlueAccent,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "CameraX Virtual Viewfinder",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Live telemetry HUD active. Tap Shutter to capture & burn GPS coordinates and timestamp.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeColor = Color.White.copy(alpha = 0.45f)
                val reticleColor = Color(0xFF38BDF8).copy(alpha = 0.85f)
                val strokeW = 3f

                val w = size.width
                val h = size.height

                val boxLeft = w * 0.08f
                val boxTop = h * 0.18f
                val boxRight = w * 0.92f
                val boxBottom = h * 0.65f
                val cornerLength = 40f

                drawLine(reticleColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLength, boxTop), strokeWidth = strokeW * 2)
                drawLine(reticleColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLength), strokeWidth = strokeW * 2)

                drawLine(reticleColor, Offset(boxRight, boxTop), Offset(boxRight - cornerLength, boxTop), strokeWidth = strokeW * 2)
                drawLine(reticleColor, Offset(boxRight, boxTop), Offset(boxRight, boxTop + cornerLength), strokeWidth = strokeW * 2)

                drawLine(reticleColor, Offset(boxLeft, boxBottom), Offset(boxLeft + cornerLength, boxBottom), strokeWidth = strokeW * 2)
                drawLine(reticleColor, Offset(boxLeft, boxBottom), Offset(boxLeft, boxBottom - cornerLength), strokeWidth = strokeW * 2)

                drawLine(reticleColor, Offset(boxRight, boxBottom), Offset(boxRight - cornerLength, boxBottom), strokeWidth = strokeW * 2)
                drawLine(reticleColor, Offset(boxRight, boxBottom), Offset(boxRight, boxBottom - cornerLength), strokeWidth = strokeW * 2)

                val cx = w / 2f
                val cy = (boxTop + boxBottom) / 2f
                drawLine(strokeColor, Offset(cx - 24f, cy), Offset(cx + 24f, cy), strokeWidth = strokeW)
                drawLine(strokeColor, Offset(cx, cy - 24f), Offset(cx, cy + 24f), strokeWidth = strokeW)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                CameraTopBar(
                    liveTime = liveTimeString,
                    isTorchEnabled = isTorchEnabled,
                    onClose = onClose,
                    onToggleTorch = {
                        val newTorch = !isTorchEnabled
                        isTorchEnabled = newTorch
                        boundCamera?.cameraControl?.enableTorch(newTorch)
                    },
                    onFlipCamera = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                )

                CameraTagRow(
                    tags = photoTags,
                    selectedTag = activeTag,
                    onSelectTag = { viewModel.setActiveCameraTag(it) }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                CameraTelemetryHudCard(
                    siteId = siteId,
                    operator = operator,
                    activeTag = activeTag,
                    status = status,
                    gpsData = gpsData,
                    gsmData = gsmData,
                    liveTime = liveTimeString
                )

                CameraBottomControlsBar(
                    photosCount = photos.size,
                    lastPhoto = lastCapturedPhoto ?: photos.lastOrNull(),
                    isProcessing = isProcessingPhoto,
                    onTakePhoto = { takePhoto() },
                    onOpenPreview = {
                        val photoToPreview = lastCapturedPhoto ?: photos.lastOrNull()
                        if (photoToPreview != null) {
                            previewPhoto = photoToPreview
                        }
                    },
                    onDone = onClose
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = flashAlpha.value))
            )
        }

        previewPhoto?.let { photo ->
            CameraPhotoPreviewDialog(
                photo = photo,
                onDismiss = { previewPhoto = null }
            )
        }
    }
}
