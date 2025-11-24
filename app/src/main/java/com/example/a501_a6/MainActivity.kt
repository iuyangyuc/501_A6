package com.example.a501_a6

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.a501_a6.ui.theme._501_A6Theme
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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
                    LocationMapScreen()
                }
            }
        }
    }
}

@Composable
@SuppressLint("MissingPermission") // Permission gated in logic before enabling location
fun LocationMapScreen() {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var hasLocationPermission by remember { mutableStateOf(hasLocationPermission(context)) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var addressText by remember { mutableStateOf(context.getString(R.string.location_permission_rationale)) }
    var customMarkers by remember { mutableStateOf<List<CustomMarker>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { results ->
            hasLocationPermission = results.any { it.value }
        }
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            addressText = context.getString(R.string.fetching_location)
            val location = fetchPreciseLocation(context, fusedLocationClient)
            userLocation = location

            if (location != null) {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(location, 15f)
                )
                addressText = reverseGeocode(context, location)
            } else {
                addressText = "Unable to retrieve your location. Make sure location is enabled."
            }
        } else {
            userLocation = null
            addressText = context.getString(R.string.location_permission_rationale)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Location Information",
            style = MaterialTheme.typography.titleLarge
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission),
                onMapClick = { latLng ->
                    customMarkers = customMarkers + CustomMarker(
                        title = "Custom marker ${customMarkers.size + 1}",
                        position = latLng
                    )
                }
            ) {
                userLocation?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = "You are here",
                        snippet = addressText
                    )
                }
                customMarkers.forEach { marker ->
                    Marker(
                        state = MarkerState(position = marker.position),
                        title = marker.title,
                        snippet = formatCoordinates(marker.position)
                    )
                }
            }
        }

        Text(
            text = "Address",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = addressText,
            style = MaterialTheme.typography.bodyLarge
        )

        if (!hasLocationPermission) {
            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Enable location")
            }
        } else {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp),
                text = "Tap anywhere on the map to place a custom marker.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private data class CustomMarker(
    val title: String,
    val position: LatLng
)

private suspend fun fetchPreciseLocation(
    context: Context,
    fusedLocationProviderClient: FusedLocationProviderClient
): LatLng? {
    if (!hasLocationPermission(context)) return null

    val currentLocation = try {
        suspendCancellableCoroutine<LatLng?> { continuation ->
            val token = CancellationTokenSource()
            fusedLocationProviderClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                token.token
            ).addOnSuccessListener { location ->
                continuation.resume(location?.toLatLng())
            }.addOnFailureListener {
                continuation.resume(null)
            }.addOnCanceledListener {
                continuation.resume(null)
            }

            continuation.invokeOnCancellation {
                token.cancel()
            }
        }
    } catch (securityException: SecurityException) {
        return null
    }

    if (currentLocation != null) return currentLocation

    return try {
        suspendCancellableCoroutine { continuation ->
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    continuation.resume(location?.toLatLng())
                }
                .addOnFailureListener {
                    continuation.resume(null)
                }
                .addOnCanceledListener {
                    continuation.resume(null)
                }
        }
    } catch (securityException: SecurityException) {
        null
    }
}

private suspend fun reverseGeocode(
    context: Context,
    latLng: LatLng
): String = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) {
        return@withContext formatCoordinates(latLng)
    }

    suspendCancellableCoroutine { continuation ->
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        val addressLine = addresses.firstOrNull()?.getAddressLine(0)
                        continuation.resume(addressLine ?: formatCoordinates(latLng))
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(formatCoordinates(latLng))
                    }
                }
            )
        } catch (error: Throwable) {
            continuation.resume(formatCoordinates(latLng))
        }
    }
}

private fun formatCoordinates(latLng: LatLng): String =
    "Lat: %.5f, Lng: %.5f".format(Locale.US, latLng.latitude, latLng.longitude)

private fun Location.toLatLng(): LatLng = LatLng(latitude, longitude)
