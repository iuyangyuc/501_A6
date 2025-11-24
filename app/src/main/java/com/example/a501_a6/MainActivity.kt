package com.example.a501_a6

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.a501_a6.ui.theme._501_A6Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

private const val THRESHOLD_DB = 85f
private const val MAX_DISPLAY_DB = 100f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _501_A6Theme {
                SoundMeterApp()
            }
        }
    }
}

@Composable
fun SoundMeterApp() {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isListening by remember { mutableStateOf(false) }
    var currentDb by remember { mutableFloatStateOf(0f) }
    var peakDb by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasMicPermission = granted
            if (!granted) {
                isListening = false
                statusMessage = "Microphone permission is required to measure sound."
            } else {
                statusMessage = null
                isListening = true
                peakDb = 0f
            }
        }

    val alertActive = currentDb >= THRESHOLD_DB

    ManageAudioRecording(
        isListening = isListening,
        hasMicPermission = hasMicPermission,
        onLevelMeasured = { decibel ->
            currentDb = decibel
            if (decibel.isFinite()) {
                peakDb = max(peakDb, decibel)
            }
        },
        onError = { message ->
            statusMessage = message
            isListening = false
        }
    )

    Surface(modifier = Modifier.fillMaxSize()) {
        val barColor by animateColorAsState(
            targetValue = when {
                alertActive -> MaterialTheme.colorScheme.error
                currentDb > THRESHOLD_DB * 0.7f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            label = "meterColor"
        )

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sound Meter",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = String.format("%.1f dB", currentDb),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            val progress = (currentDb / MAX_DISPLAY_DB).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Peak: ${"%.1f".format(peakDb)} dB")
                Text("Alert at: ${"%.0f".format(THRESHOLD_DB)} dB")
            }
            AnimatedVisibility(visible = alertActive) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Noise threshold exceeded!",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            statusMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    if (!hasMicPermission) {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    } else {
                        isListening = !isListening
                        if (isListening) {
                            peakDb = 0f
                            statusMessage = null
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isListening) "Stop Listening" else "Start Listening")
            }
        }
    }
}

@Composable
private fun ManageAudioRecording(
    isListening: Boolean,
    hasMicPermission: Boolean,
    onLevelMeasured: (Float) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(isListening, hasMicPermission) {
        if (!isListening || !hasMicPermission) {
            onLevelMeasured(0f)
            return@LaunchedEffect
        }

        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            onError("Microphone permission denied.")
            onLevelMeasured(0f)
            return@LaunchedEffect
        }

        val config = try {
            createAudioRecord()
        } catch (e: SecurityException) {
            onError("Microphone permission denied.")
            onLevelMeasured(0f)
            return@LaunchedEffect
        } ?: run {
            onError("Unable to access microphone.")
            return@LaunchedEffect
        }
        val (recorder, bufferSize) = config
        val buffer = ShortArray(bufferSize)

        try {
            recorder.startRecording()
            // Capture amplitude off the main thread to keep UI responsive.
            withContext(Dispatchers.Default) {
                while (isActive) {
                    val read = recorder.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val decibel = buffer.toDecibel(read)
                        withContext(Dispatchers.Main) {
                            onLevelMeasured(decibel)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            onError("Microphone permission denied.")
        } catch (e: IllegalStateException) {
            onError("Recording failed.")
        } finally {
            try {
                recorder.stop()
            } catch (_: IllegalStateException) {
            }
            recorder.release()
        }
    }
}

@RequiresPermission(Manifest.permission.RECORD_AUDIO)
private fun createAudioRecord(): Pair<AudioRecord, Int>? {
    val sampleRate = 44100
    val channel = AudioFormat.CHANNEL_IN_MONO
    val encoding = AudioFormat.ENCODING_PCM_16BIT
    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
    if (minBufferSize <= 0) return null

    val bufferSize = minBufferSize * 2
    val record = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        channel,
        encoding,
        bufferSize
    )
    if (record.state != AudioRecord.STATE_INITIALIZED) {
        record.release()
        return null
    }
    return record to bufferSize
}

private fun ShortArray.toDecibel(read: Int): Float {
    var sum = 0L
    for (i in 0 until read) {
        val sample = this[i].toInt()
        sum += sample * sample
    }
    if (sum <= 0 || read <= 0) return 0f

    val mean = sum / read.toFloat()
    val rms = sqrt(mean)
    val decibel = if (rms > 0f) 20f * log10(rms) else 0f
    return decibel.coerceAtLeast(0f)
}

@Preview(showBackground = true)
@Composable
fun SoundMeterPreview() {
    _501_A6Theme {
        SoundMeterApp()
    }
}
