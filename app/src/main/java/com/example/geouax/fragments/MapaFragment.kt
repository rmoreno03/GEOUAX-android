package com.example.geouax.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.geouax.firestore.FirestoreHelper

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var puntos: List<Punto> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        val lat = arguments?.getDouble("latitud", Double.NaN) ?: Double.NaN
        val lng = arguments?.getDouble("longitud", Double.NaN) ?: Double.NaN
        val nombre = arguments?.getString("nombre")
        val descripcion = arguments?.getString("descripcion")

        if (!lat.isNaN() && !lng.isNaN() && nombre != null) {
            // Modo punto único
            val puntoLatLng = LatLng(lat, lng)
            googleMap.addMarker(
                MarkerOptions()
                    .position(puntoLatLng)
                    .title(nombre)
                    .snippet(descripcion ?: "")
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(puntoLatLng, 15f))
        } else {
            // Modo general
            FirestoreHelper.getAllPuntos { puntosList ->
                if (puntosList.isNullOrEmpty()) {
                    Toast.makeText(requireContext(), "No hay puntos para mostrar", Toast.LENGTH_SHORT).show()
                    return@getAllPuntos
                }

                val puntosLimitados = puntosList.take(20) //

                puntosLimitados.forEachIndexed { index, punto ->
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::googleMap.isInitialized) {
            googleMap.clear() // 🧹 Libera los marcadores del mapa
        }
    }
}




