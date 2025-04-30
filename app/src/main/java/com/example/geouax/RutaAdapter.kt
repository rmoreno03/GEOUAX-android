package com.example.geouax

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RutaAdapter(private val rutas: List<Ruta>) : RecyclerView.Adapter<RutaAdapter.RutaViewHolder>() {

    inner class RutaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombreRuta: TextView = itemView.findViewById(R.id.tvNombreRuta)
        private val tvTipoRuta: TextView = itemView.findViewById(R.id.tvTipoRuta)
        private val tvDistancia: TextView = itemView.findViewById(R.id.tvDistancia)
        private val tvDuracion: TextView = itemView.findViewById(R.id.tvDuracion)

        fun bind(ruta: Ruta) {
            
            tvNombreRuta.text = "Nombre: ${ruta.nombre}"
            tvTipoRuta.text = "Tipo: ${ruta.tipoRuta}"
            tvDistancia.text = "Distancia: ${ruta.distanciaKm} km"
            tvDuracion.text = "Duración: ${ruta.duracionMin} min"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RutaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ruta, parent, false)
        return RutaViewHolder(view)
    }

    override fun onBindViewHolder(holder: RutaViewHolder, position: Int) {
        holder.bind(rutas[position])
    }

    override fun getItemCount(): Int = rutas.size
}



