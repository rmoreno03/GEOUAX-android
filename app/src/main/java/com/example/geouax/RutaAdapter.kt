package com.example.geouax

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.geouax.RutaAdapter.RutaViewHolder
import java.text.SimpleDateFormat
import java.util.*

class RutaAdapter(
    private val rutas: List<Ruta>,
    private val onRutaClick: (Ruta) -> Unit
) : RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ruta, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        val ruta = rutas[position]
        holder.bind(ruta)
    }

    override fun getItemCount(): Int = rutas.size

    inner class RutaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)
        private val tvDistancia: TextView = itemView.findViewById(R.id.tvDistancia)
        private val tvDuracion: TextView = itemView.findViewById(R.id.tvDuracion)

        fun bind(ruta: Ruta) {
            tvNombre.text = ruta.nombre
            tvDistancia.text = "Distancia: ${ruta.distanciaKm} km"
            tvDuracion.text = "Duración: ${ruta.duracionMin} min"

            itemView.setOnClickListener {
                onRutaClick(ruta)
            }
        }
    }
}

