package com.example.geouax.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.geouax.Punto
import com.example.geouax.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import com.example.geouax.Ruta
import com.example.geouax.firestore.FirestoreHelper
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.Timestamp


class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private var puntosRuta: List<Punto> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.mapa_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.clear()

        // Obtener la Ruta desde los argumentos (ya es Parcelable)
        val ruta = arguments?.getParcelable<Ruta>("ruta")

        if (ruta != null && ruta.puntos.isNotEmpty()) {
            puntosRuta = ruta.puntos
            val boundsBuilder = LatLngBounds.builder()

            // Agregar marcadores
            puntosRuta.forEachIndexed { index, punto ->
                val latLng = LatLng(punto.latitud, punto.longitud)
                val title = when (index) {
                    0 -> "Inicio"
                    puntosRuta.lastIndex -> "Fin"
                    else -> "Punto ${index + 1}"
                }

                googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(title)
                        .snippet(punto.descripcion)
                )
                boundsBuilder.include(latLng)
            }

            // Dibujar la línea de la ruta
            val latLngs = puntosRuta.map { LatLng(it.latitud, it.longitud) }
            googleMap.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .width(10f)
                    .color(R.color.teal_700) // puedes personalizar el color aquí
            )

            // Ajustar cámara al conjunto de puntos
            val bounds = boundsBuilder.build()
            googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } else {
            mostrarPuntosFirebase()
        }

        // Escuchar clics para agregar nuevos puntos
        googleMap.setOnMapClickListener { latLng ->
            mostrarDialogoAgregarPunto(latLng.latitude, latLng.longitude)
        }
    }

    private fun mostrarPuntosFirebase() {
        FirestoreHelper.getAllPuntos { puntosList ->
            if (puntosList.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "No hay puntos para mostrar", Toast.LENGTH_SHORT).show()
                return@getAllPuntos
            }

            puntosList.take(20).forEachIndexed { index, punto ->
                val latLng = LatLng(punto.latitud, punto.longitud)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(punto.nombre)
                        .snippet(punto.descripcion)
                )
                if (index == 0) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12f))
                }
            }
        }
    }

    private fun mostrarDialogoAgregarPunto(latitud: Double, longitud: Double) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Agregar Punto de Localización")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputNombre = EditText(requireContext()).apply { hint = "Nombre del punto" }
        val inputDescripcion = EditText(requireContext()).apply { hint = "Descripción del punto" }
        val inputLatitud = EditText(requireContext()).apply {
            hint = "Latitud"
            setText(latitud.toString())
            isEnabled = false
        }
        val inputLongitud = EditText(requireContext()).apply {
            hint = "Longitud"
            setText(longitud.toString())
            isEnabled = false
        }

        layout.addView(inputNombre)
        layout.addView(inputDescripcion)
        layout.addView(inputLatitud)
        layout.addView(inputLongitud)

        builder.setView(layout)

        builder.setPositiveButton("Agregar") { _, _ ->
            val nombre = inputNombre.text.toString()
            val descripcion = inputDescripcion.text.toString()

            if (nombre.isNotEmpty() && descripcion.isNotEmpty()) {
                agregarPuntoLocalizacion(nombre, descripcion, latitud, longitud)
            } else {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun agregarPuntoLocalizacion(nombre: String, descripcion: String, latitud: Double, longitud: Double) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUID = currentUser?.uid

        if (currentUID == null) {
            Toast.makeText(requireContext(), "Debes iniciar sesión para agregar puntos.", Toast.LENGTH_SHORT).show()
            return
        }

        val punto = hashMapOf(
            "nombre" to nombre,
            "descripcion" to descripcion,
            "latitud" to latitud,
            "longitud" to longitud,
            "usuarioCreador" to currentUID,
            "fechaCreacion" to com.google.firebase.Timestamp.now()
        )

        db.collection("puntos_localizacion")
            .add(punto)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Punto agregado correctamente.", Toast.LENGTH_SHORT).show()
                googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(latitud, longitud))
                        .title(nombre)
                        .snippet(descripcion)
                )
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al agregar punto: $e", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::googleMap.isInitialized) googleMap.clear()
    }
}



