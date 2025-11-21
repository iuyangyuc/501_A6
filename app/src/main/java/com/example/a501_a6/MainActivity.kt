package com.example.a501_a6

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a501_a6.ui.theme._501_A6Theme
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private const val NS_TO_S = 1f / 1_000_000_000f
private const val RAD_TO_DEG = (180f / Math.PI.toFloat())
private const val LEVEL_CLAMP = 60f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _501_A6Theme {
                SensorPlaygroundScreen()
            }
        }
    }
}

data class SensorState(
    val heading: Float,
    val roll: Float,
    val pitch: Float,
    val compassAvailable: Boolean,
    val levelAvailable: Boolean
)

@Composable
fun SensorPlaygroundScreen() {
    var simulate by remember { mutableStateOf(true) }
    val sensorState = rememberSensorState(simulate)
    val animatedHeading by animateFloatAsState(
        targetValue = sensorState.heading,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "heading"
    )
    val animatedRoll by animateFloatAsState(
        targetValue = sensorState.roll,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "roll"
    )
    val animatedPitch by animateFloatAsState(
        targetValue = sensorState.pitch,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "pitch"
    )

    val background = remember {
        Brush.verticalGradient(
            listOf(
                Color(0xFF050914),
                Color(0xFF0F1F3E),
                Color(0xFF0E4D64)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Magnetic Mischief Lab",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF9FFFCB),
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Spin the compass, chase the bubble — tilt, swirl, repeat.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFC8E6FF)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (simulate) "Simulation: Chaos Mode" else "Live Sensors",
                        color = Color(0xFF58D3FF),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (simulate) "Random gyro + compass data for easy testing" else "Using device sensors",
                        color = Color(0xFFC8E6FF),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(
                    checked = simulate,
                    onCheckedChange = { simulate = it }
                )
            }

            CompassCard(heading = animatedHeading, sensorsReady = sensorState.compassAvailable)
            LevelCard(
                roll = animatedRoll,
                pitch = animatedPitch,
                sensorsReady = sensorState.levelAvailable
            )
        }
    }
}

@Composable
fun CompassCard(heading: Float, sensorsReady: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1329).copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Compass",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF9FFFCB),
                fontWeight = FontWeight.Bold
            )
            if (!sensorsReady) {
                Text(
                    text = "Magnetometer + accelerometer not found on this device.",
                    color = Color(0xFFFFC4D6),
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                CompassDial(heading = heading)
            }

            Text(
                text = "Heading ${heading.toInt()}°",
                color = Color(0xFFC8E6FF),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompassDial(heading: Float) {
    val dialGradient = Brush.radialGradient(
        colors = listOf(Color(0xFF0EF7B5), Color(0xFF0B8FD1), Color(0xFF05122E)),
        center = Offset.Zero
    )

    Box(
        modifier = Modifier
            .size(260.dp)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    listOf(Color(0xFF9FFFCB), Color(0xFF58D3FF))
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2.4f
            val center = this.center

            drawCircle(
                brush = dialGradient,
                radius = radius,
                center = center,
                style = Stroke(width = 6f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius * 0.92f,
                center = center,
                style = Stroke(width = 12f)
            )

            for (degree in 0 until 360 step 30) {
                val thick = degree % 90 == 0
                val tickLength = if (thick) radius * 0.14f else radius * 0.08f
                val angleRad = Math.toRadians(degree.toDouble() - 90)
                val start = Offset(
                    x = center.x + cos(angleRad).toFloat() * (radius - tickLength),
                    y = center.y + sin(angleRad).toFloat() * (radius - tickLength)
                )
                val end = Offset(
                    x = center.x + cos(angleRad).toFloat() * radius,
                    y = center.y + sin(angleRad).toFloat() * radius
                )
                drawLine(
                    color = if (thick) Color(0xFF9FFFCB) else Color.White.copy(alpha = 0.35f),
                    start = start,
                    end = end,
                    strokeWidth = if (thick) 6f else 3f,
                    cap = StrokeCap.Round
                )
            }

            rotate(degrees = -heading, pivot = center) {
                val needleLength = radius * 0.85f
                val path = Path().apply {
                    moveTo(center.x, center.y - needleLength)
                    lineTo(center.x - 20f, center.y + 18f)
                    lineTo(center.x + 20f, center.y + 18f)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFF8A00), Color(0xFFFF1B6B))
                    )
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = 16f,
                    center = center
                )
            }
        }

        Text(
            text = "N",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Text(
            text = "S",
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Text(
            text = "W",
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterStart)
        )
        Text(
            text = "E",
            color = Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun LevelCard(roll: Float, pitch: Float, sensorsReady: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0B1329).copy(alpha = 0.92f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Digital Level",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFF9FFFCB),
                fontWeight = FontWeight.Bold
            )

            if (!sensorsReady) {
                Text(
                    text = "Gyroscope not found on this device.",
                    color = Color(0xFFFFC4D6),
                    style = MaterialTheme.typography.bodyMedium
                )
                return@Column
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                LevelBubble(roll = roll, pitch = pitch)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TiltStat(label = "Roll", value = roll)
                TiltStat(label = "Pitch", value = pitch)
            }
        }
    }
}

@Composable
fun LevelBubble(roll: Float, pitch: Float) {
    val bubbleRoll by animateFloatAsState(
        targetValue = roll,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "bubbleRoll"
    )
    val bubblePitch by animateFloatAsState(
        targetValue = pitch,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessLow),
        label = "bubblePitch"
    )

    Canvas(
        modifier = Modifier
            .size(220.dp)
            .padding(8.dp)
    ) {
        val center = this.center
        val radius = size.minDimension / 2.6f
        val bubbleRadius = size.minDimension * 0.09f

        drawCircle(
            color = Color.White.copy(alpha = 0.06f),
            radius = radius * 1.05f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.15f),
            radius = radius,
            center = center,
            style = Stroke(width = 10f)
        )

        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color.White.copy(alpha = 0.45f),
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 6f,
            cap = StrokeCap.Round
        )

        val normRoll = (bubbleRoll / LEVEL_CLAMP).coerceIn(-1f, 1f)
        val normPitch = (bubblePitch / LEVEL_CLAMP).coerceIn(-1f, 1f)
        val offset = Offset(
            x = normRoll * (radius - bubbleRadius),
            y = normPitch * (radius - bubbleRadius)
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF5DF7FF), Color(0xFF00B4D8), Color(0xFF072A40)),
                center = center + offset,
                radius = bubbleRadius * 1.4f
            ),
            radius = bubbleRadius,
            center = center + offset
        )
    }
}

@Composable
fun TiltStat(label: String, value: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            color = Color(0xFF58D3FF),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${value.toInt()}°",
            color = Color(0xFFC8E6FF),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun rememberSensorState(simulate: Boolean): SensorState {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    var heading by remember { mutableFloatStateOf(0f) }
    var roll by remember { mutableFloatStateOf(0f) }
    var pitch by remember { mutableFloatStateOf(0f) }
    var compassAvailable by remember { mutableStateOf(true) }
    var levelAvailable by remember { mutableStateOf(true) }

    if (simulate) {
        LaunchedEffect(Unit) {
            while (true) {
                heading = ((heading + Random.nextFloat() * 40f - 20f) + 360f) % 360f
                roll = (roll + Random.nextFloat() * 20f - 10f).coerceIn(-LEVEL_CLAMP, LEVEL_CLAMP)
                pitch = (pitch + Random.nextFloat() * 20f - 10f).coerceIn(-LEVEL_CLAMP, LEVEL_CLAMP)
                delay(700L)
            }
        }
        return SensorState(
            heading = heading,
            roll = roll,
            pitch = pitch,
            compassAvailable = true,
            levelAvailable = true
        )
    }

    DisposableEffect(sensorManager) {
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)

        var hasAccel = false
        var hasMag = false
        var lastGyroTimestamp = 0L

        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        compassAvailable = accelSensor != null && magSensor != null
        levelAvailable = gyroSensor != null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        event.values.copyInto(gravity)
                        hasAccel = true
                        updateHeading()
                    }

                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        event.values.copyInto(geomagnetic)
                        hasMag = true
                        updateHeading()
                    }

                    Sensor.TYPE_GYROSCOPE -> {
                        val now = event.timestamp
                        if (lastGyroTimestamp != 0L) {
                            val dt = (now - lastGyroTimestamp) * NS_TO_S
                            val deltaPitch = event.values[0] * dt * RAD_TO_DEG
                            val deltaRoll = event.values[1] * dt * RAD_TO_DEG
                            pitch = (pitch + deltaPitch).coerceIn(-LEVEL_CLAMP, LEVEL_CLAMP)
                            roll = (roll + deltaRoll).coerceIn(-LEVEL_CLAMP, LEVEL_CLAMP)
                        }
                        lastGyroTimestamp = now
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

            private fun updateHeading() {
                if (!compassAvailable || !hasAccel || !hasMag) return
                val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                if (success) {
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuth = (Math.toDegrees(orientationAngles[0].toDouble()).toFloat() + 360f) % 360f
                    heading = heading * 0.85f + azimuth * 0.15f
                }
            }
        }

        if (compassAvailable) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magSensor, SensorManager.SENSOR_DELAY_GAME)
        }
        if (levelAvailable) {
            sensorManager.registerListener(listener, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return SensorState(
        heading = heading,
        roll = roll,
        pitch = pitch,
        compassAvailable = compassAvailable,
        levelAvailable = levelAvailable
    )
}

@Preview(showBackground = true)
@Composable
fun DashboardPreview() {
    _501_A6Theme {
        SensorPlaygroundScreen()
    }
}
