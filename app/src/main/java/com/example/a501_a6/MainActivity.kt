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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a501_a6.ui.theme._501_A6Theme
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _501_A6Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        BallMazeScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}

@Composable
fun BallMazeScreen(modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val gyroState by rememberGyroscopeTilt()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
        val obstacleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        val ballColor = MaterialTheme.colorScheme.secondary
        Text(
            text = if (gyroState.available) "Tilt your phone to roll the ball" else "Gyroscope not available",
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            var canvasSize by remember { mutableStateOf(Size.Zero) }
            val ballRadius = with(density) { 14.dp.toPx() }

            val startPosition = remember(canvasSize) {
                if (canvasSize == Size.Zero) Offset.Zero else Offset(ballRadius + 24f, ballRadius + 24f)
            }

            var position by remember(canvasSize) { mutableStateOf(startPosition) }
            var velocity by remember(canvasSize) { mutableStateOf(Offset.Zero) }
            val obstacles = remember(canvasSize) {
                if (canvasSize == Size.Zero) emptyList()
                else buildMaze(canvasSize.width, canvasSize.height)
            }
            val goal = remember(canvasSize) {
                if (canvasSize == Size.Zero) null
                else Rect(
                    offset = Offset(canvasSize.width * 0.78f, canvasSize.height * 0.78f),
                    size = Size(canvasSize.width * 0.16f, canvasSize.height * 0.14f)
                )
            }

            LaunchedEffect(canvasSize, gyroState.available) {
                if (canvasSize == Size.Zero) return@LaunchedEffect
                var lastNanos = 0L
                while (true) {
                    withFrameNanos { time ->
                        if (lastNanos == 0L) {
                            lastNanos = time
                            return@withFrameNanos
                        }
                        val dt = (time - lastNanos) / 1_000_000_000f
                        lastNanos = time

                        val accelScale = 900f
                        val damping = 0.985f
                        val tilt = gyroState.tilt

                        val acceleration = Offset(
                            x = tilt.x * accelScale,
                            y = tilt.y * accelScale
                        )

                        var newVelocity = Offset(
                            x = (velocity.x + acceleration.x * dt) * damping,
                            y = (velocity.y + acceleration.y * dt) * damping
                        )
                        var newPosition = Offset(
                            x = position.x + newVelocity.x * dt,
                            y = position.y + newVelocity.y * dt
                        )

                        newPosition = Offset(
                            x = newPosition.x.coerceIn(ballRadius, canvasSize.width - ballRadius),
                            y = newPosition.y.coerceIn(ballRadius, canvasSize.height - ballRadius)
                        )

                        val collided = obstacles.any { rect ->
                            circleIntersectsRect(newPosition, ballRadius, rect)
                        }
                        if (collided) {
                            newVelocity = Offset.Zero
                            newPosition = position
                        }

                        position = newPosition
                        velocity = newVelocity
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                    }
            ) {
                drawRect(
                    color = backgroundColor,
                    size = Size(size.width, size.height)
                )

                goal?.let { finish ->
                    drawRect(
                        color = Color(0xFFB9F6CA),
                        topLeft = finish.topLeft,
                        size = finish.size
                    )
                }

                obstacles.forEach { rect ->
                    drawRect(
                        color = obstacleColor,
                        topLeft = rect.topLeft,
                        size = rect.size
                    )
                }

                drawCircle(
                    color = ballColor,
                    radius = ballRadius,
                    center = position
                )

                drawRect(
                    color = Color.Black.copy(alpha = 0.3f),
                    size = Size(size.width, 3.dp.toPx()),
                    topLeft = Offset(0f, 0f)
                )
            }
        }
    }
}

@Composable
fun rememberGyroscopeTilt(): androidx.compose.runtime.State<GyroReading> {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val state = remember { mutableStateOf(GyroReading()) }

    DisposableEffect(sensorManager) {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        if (sensor == null) {
            state.value = GyroReading(available = false)
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                private var lastTimestamp = 0L
                private var pitch = 0f
                private var roll = 0f

                override fun onSensorChanged(event: SensorEvent) {
                    if (lastTimestamp == 0L) {
                        lastTimestamp = event.timestamp
                        return
                    }
                    val dt = (event.timestamp - lastTimestamp) / 1_000_000_000f
                    lastTimestamp = event.timestamp

                    // Integrate angular velocity to approximate tilt; clamp to avoid runaway drift.
                    pitch = (pitch + event.values[0] * dt).coerceIn(-1.5f, 1.5f)
                    roll = (roll + event.values[1] * dt).coerceIn(-1.5f, 1.5f)

                    state.value = GyroReading(
                        tilt = Offset(x = roll, y = -pitch),
                        available = true
                    )
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    return state
}

fun buildMaze(width: Float, height: Float): List<Rect> {
    val wallThickness = max(14f, width * 0.02f)
    val longWallLength = width * 0.7f
    val shortWallLength = height * 0.28f

    val walls = listOf(
        Rect(Offset(width * 0.1f, height * 0.22f), Size(longWallLength, wallThickness)),
        Rect(Offset(width * 0.1f, height * 0.5f), Size(longWallLength, wallThickness)),
        Rect(Offset(width * 0.1f, height * 0.78f), Size(longWallLength, wallThickness)),
        Rect(Offset(width * 0.35f, height * 0.22f), Size(wallThickness, shortWallLength)),
        Rect(Offset(width * 0.68f, height * 0.5f), Size(wallThickness, shortWallLength)),
        Rect(Offset(width * 0.42f, height * 0.65f), Size(wallThickness, shortWallLength))
    )
    return walls
}

fun circleIntersectsRect(center: Offset, radius: Float, rect: Rect): Boolean {
    val closestX = center.x.coerceIn(rect.left, rect.right)
    val closestY = center.y.coerceIn(rect.top, rect.bottom)
    val dx = center.x - closestX
    val dy = center.y - closestY
    return (dx * dx + dy * dy) < radius * radius
}

data class GyroReading(
    val tilt: Offset = Offset.Zero,
    val available: Boolean = true
)

@Preview(showBackground = true)
@Composable
fun GamePreview() {
    _501_A6Theme {
        BallMazeScreen()
    }
}
