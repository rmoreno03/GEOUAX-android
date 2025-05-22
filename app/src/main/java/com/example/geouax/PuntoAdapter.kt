package com.example.geouax

import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class PuntoAdapter(
    private val puntos: List<Punto>,
    private val onItemClick: (Punto) -> Unit
) : RecyclerView.Adapter<PuntoAdapter.PuntoViewHolder>() {

    private var latActual: Double? = null
    private var lngActual: Double? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuntoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_punto_localizacion, parent, false)
        return PuntoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PuntoViewHolder, position: Int) {
        val punto = puntos[position]
        holder.bind(punto)

        // Escuchar clic en el item completo
        holder.itemView.setOnClickListener {
            onItemClick(punto)
        }
    }

    override fun getItemCount(): Int = puntos.size

    fun updateDistances(lat: Double, lng: Double) {
        latActual = lat
        lngActual = lng
        notifyDataSetChanged()
    }

    inner class PuntoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nombreTextView: TextView = itemView.findViewById(R.id.nombreTextView)
        private val latitudTextView: TextView = itemView.findViewById(R.id.latitudTextView)
        private val longitudTextView: TextView = itemView.findViewById(R.id.longitudTextView)
        private val distanciaTextView: TextView = itemView.findViewById(R.id.distanceText)
        private val semaforoView: View = itemView.findViewById(R.id.semaforo)


        fun bind(punto: Punto) {
            nombreTextView.text = punto.nombre
            latitudTextView.text = "Latitud: ${punto.latitud}"
            longitudTextView.text = "Longitud: ${punto.longitud}"

            val lat = latActual
            val lng = lngActual

            if (lat != null && lng != null) {
                val distancia = calcularDistancia(lat, lng, punto.latitud, punto.longitud)
                distanciaTextView.text = String.format("%.2f km", distancia)

                val background = when {
                    distancia < 1 -> R.drawable.semaforo_green
                    distancia < 5 -> R.drawable.semaforo_yellow
                    else -> R.drawable.semaforo_red
                }
                semaforoView.setBackgroundResource(background)
            } else {
                distanciaTextView.text = "Distancia desconocida"
                semaforoView.setBackgroundResource(R.drawable.semaforo_gray)
            }
        }


        private fun calcularDistancia(
            lat1: Double, lon1: Double, lat2: Double, lon2: Double
        ): Double {
            val earthRadius = 6371.0 // kilómetros
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2.0) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2.0)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c
        }
    }
}

