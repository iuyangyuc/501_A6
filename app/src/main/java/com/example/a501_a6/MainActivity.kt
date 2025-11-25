package com.example.a501_a6

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.a501_a6.ui.theme._501_A6Theme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            _501_A6Theme {
                TrailMapScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrailMapScreen() {
    val trailPoints = remember {
        listOf(
            LatLng(37.8010, -122.4789),
            LatLng(37.7990, -122.4750),
            LatLng(37.7972, -122.4705),
            LatLng(37.7950, -122.4680),
            LatLng(37.7935, -122.4645)
        )
    }

    val parkOutline = remember {
        listOf(
            LatLng(37.7992, -122.4715),
            LatLng(37.7984, -122.4684),
            LatLng(37.7963, -122.4689),
            LatLng(37.7968, -122.4720)
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(trailPoints[2], 15f)
    }

    var overlayInfo by remember { mutableStateOf(R.string.trail_info) }
    var trailColor by remember { mutableStateOf(Color(0xFF2E7D32)) }
    var trailWidth by remember { mutableStateOf(12f) }
    var parkColor by remember { mutableStateOf(Color(0xFF00897B)) }
    var parkStrokeWidth by remember { mutableStateOf(8f) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(R.string.map_screen_title)) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Text(
                    text = stringResource(overlayInfo),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            GoogleMap(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true, compassEnabled = true)
            ) {
                Polyline(
                    points = trailPoints,
                    color = trailColor,
                    width = trailWidth,
                    jointType = JointType.ROUND,
                    clickable = true,
                    onClick = { overlayInfo = R.string.trail_info }
                )

                Polygon(
                    points = parkOutline,
                    strokeColor = parkColor,
                    fillColor = parkColor.copy(alpha = 0.25f),
                    strokeWidth = parkStrokeWidth,
                    clickable = true,
                    onClick = { overlayInfo = R.string.park_info }
                )

                Marker(
                    state = MarkerState(position = trailPoints.first()),
                    title = "Trailhead"
                )
                Marker(
                    state = MarkerState(position = trailPoints.last()),
                    title = "Scenic Overlook"
                )
            }

            ControlSection(
                title = "Trail appearance",
                selectedColor = trailColor,
                onColorSelected = { trailColor = it },
                width = trailWidth,
                widthRange = 6f..24f,
                onWidthChange = { trailWidth = it }
            )

            ControlSection(
                title = "Park outline",
                selectedColor = parkColor,
                onColorSelected = { parkColor = it },
                width = parkStrokeWidth,
                widthRange = 4f..18f,
                onWidthChange = { parkStrokeWidth = it }
            )
        }
    }
}

@Composable
private fun ControlSection(
    title: String,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    width: Float,
    widthRange: ClosedFloatingPointRange<Float>,
    onWidthChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        ColorPickerRow(selectedColor = selectedColor, onColorSelected = onColorSelected)
        WidthSlider(width = width, widthRange = widthRange, onWidthChange = onWidthChange)
    }
}

@Composable
private fun ColorPickerRow(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    val colorOptions = remember {
        listOf(
            Color(0xFF2E7D32),
            Color(0xFF1565C0),
            Color(0xFFC62828),
            Color(0xFF8E24AA),
            Color(0xFFFF8F00)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        colorOptions.forEach { colorOption ->
            Surface(
                modifier = Modifier
                    .size(44.dp)
                    .border(
                        width = if (colorOption == selectedColor) 3.dp else 1.dp,
                        color = if (colorOption == selectedColor) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = CircleShape
                    ),
                color = colorOption,
                shape = CircleShape,
                onClick = { onColorSelected(colorOption) }
            ) {}
        }
    }
}

@Composable
private fun WidthSlider(
    width: Float,
    widthRange: ClosedFloatingPointRange<Float>,
    onWidthChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Width", style = MaterialTheme.typography.labelLarge)
            Text(text = "${width.toInt()} px", style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = width,
            onValueChange = onWidthChange,
            valueRange = widthRange,
            steps = 6
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrailMapPreview() {
    _501_A6Theme {
        TrailMapScreen()
    }
}
