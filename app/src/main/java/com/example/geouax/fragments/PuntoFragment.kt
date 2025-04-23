package com.example.geouax.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geouax.Punto
import com.example.geouax.PuntoAdapter
import com.example.geouax.R
import com.google.firebase.firestore.FirebaseFirestore

class PuntoFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PuntoAdapter
    private val puntos = mutableListOf<Punto>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_punto, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewPuntos)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = PuntoAdapter(puntos) { punto ->
            // Puedes reemplazar este comportamiento por navegar a VacioFragment si lo deseas
            Toast.makeText(requireContext(), "Punto clickeado: ${punto.nombre}", Toast.LENGTH_SHORT).show()
        }

        recyclerView.adapter = adapter

        cargarPuntosDesdeFirebase()

        return view
    }

    private fun cargarPuntosDesdeFirebase() {
        db.collection("puntos_localizacion")
            .get()
            .addOnSuccessListener { result ->
                puntos.clear()
                for (document in result) {
                    val punto = Punto(
                        id = document.id,
                        latitud = document.getDouble("latitud") ?: 0.0,
                        longitud = document.getDouble("longitud") ?: 0.0,
                        nombre = document.getString("nombre") ?: "Sin nombre",
                        descripcion = document.getString("descripcion") ?: "",
                        usuarioCreador = document.getString("usuarioCreador") ?: "Desconocido",
                        fechaCreacion = document.getTimestamp("fechaCreacion"),
                        fotos = document.get("fotos") as? List<String>
                    )
                    puntos.add(punto)
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al cargar puntos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}



