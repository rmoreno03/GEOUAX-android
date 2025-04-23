package com.example.geouax

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat

// Clase para controlar el manejo de la ubicación en una aplicación Android.
class LocationControlador(
    private val context: Context
) {

    private lateinit var locationManager: LocationManager
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0

    // Listener para actualizaciones de ubicación
    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLat = location.latitude
            currentLon = location.longitude
        }

        @Deprecated("Deprecated in API 29")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {
            Toast.makeText(context, "GPS habilitado.", Toast.LENGTH_SHORT).show()
        }

        override fun onProviderDisabled(provider: String) {
            Toast.makeText(context, "GPS deshabilitado.", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkGPSEnabledAndStartUpdates() {
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Verifica permisos
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permisos de ubicación no otorgados.", Toast.LENGTH_SHORT).show()
            return
        }

        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (lastKnownLocation != null) {
            currentLat = lastKnownLocation.latitude
            currentLon = lastKnownLocation.longitude
        } else {
            Toast.makeText(context, "Esperando ubicación inicial...", Toast.LENGTH_SHORT).show()
        }

        // Configura actualizaciones de ubicación
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            2000L,
            1f,
            locationListener
        )
    }

    fun getCurrentLocation(): Location? {
        return if (currentLat != 0.0 && currentLon != 0.0) {
            Location("current").apply {
                latitude = currentLat
                longitude = currentLon
            }
        } else {
            null
        }
    }

    fun onDestroy() {
        if (::locationManager.isInitialized) {
            locationManager.removeUpdates(locationListener)
        }
    }
}

