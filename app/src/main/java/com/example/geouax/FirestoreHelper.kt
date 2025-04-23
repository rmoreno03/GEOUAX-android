package com.example.geouax.firestore

import android.util.Log
import com.example.geouax.Punto
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreHelper {

    private val db = FirebaseFirestore.getInstance()

    // Función para obtener los puntos desde Firestore
    fun getAllPuntos(callback: (List<Punto>?) -> Unit) {
        val puntosCollection = db.collection("puntos_localizacion")

        // Sin usar select(), simplemente obtenemos todos los campos disponibles.
        puntosCollection
            .get()
            .addOnSuccessListener { result ->
                val puntos = mutableListOf<Punto>()
                for (document in result) {
                    // Asegúrate de que el documento contenga los campos adecuados
                    val id = document.getString("id") ?: ""
                    val nombre = document.getString("nombre") ?: ""
                    val descripcion = document.getString("descripcion") ?: ""
                    val latitud = document.getDouble("latitud") ?: 0.0
                    val longitud = document.getDouble("longitud") ?: 0.0

                    // Creamos un objeto Punto con los datos obtenidos
                    val punto = Punto(id, latitud, longitud, nombre, descripcion, "", null)
                    puntos.add(punto)
                }
                callback(puntos)  // Llamamos al callback con la lista de puntos
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreHelper", "Error getting documents: ", exception)
                callback(null)  // Llamamos al callback con null en caso de error
            }
    }
}

