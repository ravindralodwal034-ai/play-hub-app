package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private enum class PhotoFilterPreset(val label: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    MARWADI_WARM("Warm Glow"),
    CYBER_COOL("Cyber Cool"),
    NOIR("B&W Noir"),
    VINTAGE("Vintage")
}

@Composable
fun InstantStudioDialog(
    onDismiss: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(0) } // 0 = Photo Editor, 1 = Video Player
    var sourceBitmap by remember { mutableStateOf<Bitmap?>(createSampleCanvasBitmap()) }
    var selectedPreset by remember { mutableStateOf(PhotoFilterPreset.ORIGINAL) }
    var brightness by remember { mutableFloatStateOf(0f) } // -80..80
    var contrast by remember { mutableFloatStateOf(1f) } // 0.6..1.8
    var saturation by remember { mutableFloatStateOf(1f) } // 0..2
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var captionText by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = loadScaledBitmapFromUri(context, uri)
                if (decoded != null) {
                    sourceBitmap = decoded
                    rotationDegrees = 0
                } else {
                    onShowMessage("Could not open selected photo")
                }
            }
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedVideoUri = uri
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(8.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoFixHigh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Built-In Instant Studio",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Edit photos & play videos with zero downloads",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close Studio")
                    }
                }

                // Mode Switcher
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    SegmentedButton(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("studio_tab_photo")
                    ) {
                        Text("Photo Editor")
                    }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("studio_tab_video")
                    ) {
                        Text("Video Player")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeTab == 0) {
                    val activeMatrix = remember(selectedPreset, brightness, contrast, saturation) {
                        buildAndroidColorMatrix(
                            preset = selectedPreset,
                            brightness = brightness,
                            contrast = contrast,
                            saturation = saturation
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Preview Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0xFF0A0F1F)),
                            contentAlignment = Alignment.Center
                        ) {
                            sourceBitmap?.let { bmp ->
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Photo preview",
                                    contentScale = ContentScale.Fit,
                                    colorFilter = ColorFilter.colorMatrix(
                                        androidx.compose.ui.graphics.ColorMatrix(activeMatrix.array)
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationZ = rotationDegrees.toFloat()
                                        }
                                )
                            }
                            if (captionText.isNotBlank()) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.58f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = captionText,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Action Buttons: Pick Photo, Rotate, Sample Reset
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("studio_pick_photo_button")
                            ) {
                                Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Photo")
                            }
                            OutlinedButton(
                                onClick = { rotationDegrees = (rotationDegrees + 90) % 360 },
                                modifier = Modifier.testTag("studio_rotate_button")
                            ) {
                                Icon(Icons.Rounded.RotateRight, contentDescription = "Rotate 90 degrees", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rotate")
                            }
                            OutlinedButton(
                                onClick = {
                                    sourceBitmap = createSampleCanvasBitmap()
                                    selectedPreset = PhotoFilterPreset.ORIGINAL
                                    brightness = 0f
                                    contrast = 1f
                                    saturation = 1f
                                    rotationDegrees = 0
                                    captionText = ""
                                }
                            ) {
                                Text("Reset")
                            }
                        }

                        // Filter Chips
                        Text("Filters", style = MaterialTheme.typography.labelLarge)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PhotoFilterPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = { selectedPreset = preset },
                                    label = { Text(preset.label) }
                                )
                            }
                        }

                        // Sliders Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Brightness (${brightness.roundToInt()})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = brightness,
                                    onValueChange = { brightness = it },
                                    valueRange = -80f..80f
                                )
                                Text(
                                    text = "Contrast (${String.format("%.1fx", contrast)})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = contrast,
                                    onValueChange = { contrast = it },
                                    valueRange = 0.6f..1.8f
                                )
                                Text(
                                    text = "Color Saturation (${String.format("%.1fx", saturation)})",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = saturation,
                                    onValueChange = { saturation = it },
                                    valueRange = 0f..2f
                                )
                            }
                        }

                        OutlinedTextField(
                            value = captionText,
                            onValueChange = { captionText = it },
                            label = { Text("Add Text Caption / Watermark") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            enabled = !isSaving && sourceBitmap != null,
                            onClick = {
                                val bmp = sourceBitmap ?: return@Button
                                isSaving = true
                                scope.launch {
                                    val savedMsg = saveEditedBitmapToGallery(
                                        context = context,
                                        source = bmp,
                                        colorMatrix = activeMatrix,
                                        rotationDegrees = rotationDegrees,
                                        caption = captionText.trim()
                                    )
                                    isSaving = false
                                    onShowMessage(savedMsg)
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("studio_save_photo_button")
                        ) {
                            Icon(Icons.Rounded.DownloadDone, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isSaving) "Saving Photo..." else "Save Edited Photo")
                        }
                    }
                } else {
                    // Instant Local Video Player Mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("studio_pick_video_button")
                            ) {
                                Icon(Icons.Rounded.VideoLibrary, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pick Phone Video")
                            }
                        }

                        val activeVideo = selectedVideoUri
                        if (activeVideo == null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Rounded.VideoLibrary,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Play Any Video Instantly",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap Pick Phone Video above to watch videos directly without downloading an external player app.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            AndroidView(
                                factory = { ctx ->
                                    VideoView(ctx).apply {
                                        val controller = MediaController(ctx)
                                        controller.setAnchorView(this)
                                        setMediaController(controller)
                                        setVideoURI(activeVideo)
                                        setOnPreparedListener { mp ->
                                            mp.isLooping = true
                                            start()
                                        }
                                    }
                                },
                                update = { view ->
                                    view.setVideoURI(activeVideo)
                                    view.start()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildAndroidColorMatrix(
    preset: PhotoFilterPreset,
    brightness: Float,
    contrast: Float,
    saturation: Float
): ColorMatrix {
    val result = ColorMatrix()

    when (preset) {
        PhotoFilterPreset.ORIGINAL -> Unit
        PhotoFilterPreset.VIVID -> {
            result.setSaturation(1.35f)
        }
        PhotoFilterPreset.MARWADI_WARM -> {
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, 14f,
                        0f, 1.05f, 0f, 0f, 6f,
                        0f, 0f, 0.85f, 0f, -6f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        PhotoFilterPreset.CYBER_COOL -> {
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        0.88f, 0f, 0f, 0f, -6f,
                        0f, 1.04f, 0f, 0f, 4f,
                        0f, 0f, 1.2f, 0f, 16f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        PhotoFilterPreset.NOIR -> {
            result.setSaturation(0f)
        }
        PhotoFilterPreset.VINTAGE -> {
            result.postConcat(
                ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
    }

    val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
    result.postConcat(satMatrix)

    val translate = (1f - contrast) * 128f + brightness
    val bcMatrix = ColorMatrix(
        floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        )
    )
    result.postConcat(bcMatrix)
    return result
}

private fun createSampleCanvasBitmap(): Bitmap {
    val width = 900
    val height = 600
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val bgPaint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(0xFF0A1432.toInt(), 0xFFD92B4B.toInt(), 0xFFFF9E00.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val orbPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x44FFFFFF
    }
    canvas.drawCircle(width * 0.78f, height * 0.25f, 140f, orbPaint)
    canvas.drawCircle(width * 0.2f, height * 0.78f, 180f, orbPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 54f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    canvas.drawText("Tube Studio", width / 2f, height / 2f - 10f, textPaint)
    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xEEFFFFFF.toInt()
        textSize = 30f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("Pick a photo or test filters live", width / 2f, height / 2f + 42f, subPaint)
    return bitmap
}

private suspend fun loadScaledBitmapFromUri(context: Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, opts)
        }
        var sample = 1
        while (opts.outWidth / sample > 1440 || opts.outHeight / sample > 1440) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}

private suspend fun saveEditedBitmapToGallery(
    context: Context,
    source: Bitmap,
    colorMatrix: ColorMatrix,
    rotationDegrees: Int,
    caption: String
): String = withContext(Dispatchers.IO) {
    try {
        val matrix = Matrix().apply {
            if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
        }
        val rotated = if (rotationDegrees != 0) {
            Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        } else {
            source
        }

        val output = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(colorMatrix)
        }
        canvas.drawBitmap(rotated, 0f, 0f, paint)

        if (caption.isNotBlank()) {
            val textSize = (output.width * 0.055f).coerceIn(28f, 96f)
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                this.textSize = textSize
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x99000000.toInt()
            }
            val padY = textSize * 0.55f
            val boxTop = output.height - textSize - padY * 2f
            canvas.drawRect(0f, boxTop, output.width.toFloat(), output.height.toFloat(), bgPaint)
            canvas.drawText(caption, output.width / 2f, output.height - padY, textPaint)
        }

        val fileName = "TubeStudio_${System.currentTimeMillis()}.png"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/TubeStore")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    output.compress(Bitmap.CompressFormat.PNG, 95, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                return@withContext "Saved to Pictures/TubeStore ($fileName)"
            }
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val file = File(dir, fileName)
        FileOutputStream(file).use { out ->
            output.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        "Saved edited photo ($fileName)"
    } catch (_: Exception) {
        "Unable to export photo right now"
    }
}
