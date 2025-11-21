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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.a501_a6.ui.theme._501_A6Theme
import kotlin.math.pow
import kotlinx.coroutines.delay

private const val SEA_LEVEL_PRESSURE_HPA = 1013.25f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _501_A6Theme {
                AltimeterScreen()
            }
        }
    }
}

@Composable
fun AltimeterScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val pressureSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) }

    var sensorPressure by remember { mutableStateOf<Float?>(null) }
    var simulatedPressure by remember { mutableStateOf(SEA_LEVEL_PRESSURE_HPA) }
    var useLiveSensor by remember { mutableStateOf(pressureSensor != null) }
    var autoSimulate by remember { mutableStateOf(true) }

    DisposableEffect(useLiveSensor, pressureSensor) {
        if (useLiveSensor && pressureSensor != null) {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.values.isNotEmpty()) {
                        sensorPressure = event.values[0]
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_GAME)
            onDispose { sensorManager.unregisterListener(listener) }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(autoSimulate) {
        if (!autoSimulate) return@LaunchedEffect
        var decreasing = true
        while (autoSimulate) {
            simulatedPressure = (simulatedPressure + if (decreasing) -0.8f else 0.8f).coerceIn(870f, 1035f)
            if (simulatedPressure <= 870f || simulatedPressure >= 1035f) {
                decreasing = !decreasing
            }
            delay(350L)
        }
    }

    val activePressure by remember {
        derivedStateOf {
            if (useLiveSensor && sensorPressure != null) {
                sensorPressure!!
            } else {
                simulatedPressure
            }
        }
    }
    val altitudeMeters = pressureToAltitude(activePressure)
    val animatedAltitude by animateFloatAsState(
        targetValue = altitudeMeters,
        label = "altitudeAnimation"
    )
    val backgroundColor by remember {
        derivedStateOf { backgroundColorForAltitude(animatedAltitude) }
    }

    Scaffold { padding ->
        AltimeterContent(
            altitudeMeters = animatedAltitude,
            pressureHpa = activePressure,
            sensorAvailable = pressureSensor != null,
            useSensor = useLiveSensor,
            simulateAutomatically = autoSimulate,
            simulatedPressure = simulatedPressure,
            onUseSensorChange = { desired ->
                useLiveSensor = desired && pressureSensor != null
            },
            onSimulatedPressureChange = { newPressure ->
                simulatedPressure = newPressure
            },
            onSimulateAutomaticallyChange = { autoSimulate = it },
            modifier = Modifier
                .padding(padding)
                .background(backgroundColor)
                .fillMaxSize()
        )
    }
}

@Composable
private fun AltimeterContent(
    altitudeMeters: Float,
    pressureHpa: Float,
    sensorAvailable: Boolean,
    useSensor: Boolean,
    simulateAutomatically: Boolean,
    simulatedPressure: Float,
    onUseSensorChange: (Boolean) -> Unit,
    onSimulatedPressureChange: (Float) -> Unit,
    onSimulateAutomaticallyChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Altimeter",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "${formatAltitude(altitudeMeters)} m",
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Pressure: ${formatPressure(pressureHpa)} hPa",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = if (sensorAvailable && useSensor) {
                "Using device pressure sensor"
            } else if (sensorAvailable) {
                "Simulation is driving the altimeter"
            } else {
                "No barometer detected, using simulation"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Use device sensor",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Barometer input when available",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = useSensor && sensorAvailable,
                        onCheckedChange = { onUseSensorChange(it) },
                        enabled = sensorAvailable
                    )
                }

                if (!useSensor || !sensorAvailable) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simulated pressure: ${formatPressure(simulatedPressure)} hPa",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = simulatedPressure,
                        onValueChange = {
                            onSimulatedPressureChange(it)
                        },
                        valueRange = 870f..1040f
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto simulate climb/descent",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Oscillates pressure to watch the UI react in real time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Switch(
                            checked = simulateAutomatically,
                            onCheckedChange = onSimulateAutomaticallyChange
                        )
                    }
                }
            }
        }
    }
}

private fun pressureToAltitude(pressure: Float): Float {
    return 44330f * (1f - (pressure / SEA_LEVEL_PRESSURE_HPA).pow(1f / 5.255f))
}

private fun backgroundColorForAltitude(altitudeMeters: Float): Color {
    val normalized = (altitudeMeters / 4000f).coerceIn(0f, 1f)
    val lightness = 0.82f - normalized * 0.45f
    val saturation = 0.35f + normalized * 0.15f
    return Color.hsl(210f, saturation, lightness)
}

private fun formatPressure(pressure: Float): String = String.format("%.2f", pressure)

private fun formatAltitude(altitude: Float): String = String.format("%.1f", altitude)

@Preview(showBackground = true)
@Composable
fun AltimeterPreview() {
    _501_A6Theme {
        AltimeterContent(
            altitudeMeters = 1250.5f,
            pressureHpa = 891.2f,
            sensorAvailable = true,
            useSensor = false,
            simulateAutomatically = true,
            simulatedPressure = 900f,
            onUseSensorChange = {},
            onSimulatedPressureChange = {},
            onSimulateAutomaticallyChange = {},
            modifier = Modifier
                .background(backgroundColorForAltitude(1250.5f))
                .fillMaxSize()
        )
    }
}
