package com.example.geouax.firestore

import android.util.Log
import com.example.geouax.Punto
import com.example.geouax.Ruta
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

object FirestoreHelper {
    private const val TAG = "FirestoreHelper"
    private val db = FirebaseFirestore.getInstance()

    // Función para obtener los puntos desde Firestore
    fun getPuntosDelUsuario(usuarioId: String, callback: (List<Punto>?) -> Unit) {
        val puntosCollection = db.collection("puntos_localizacion")

        puntosCollection
            .whereEqualTo("usuarioCreador", usuarioId)  // Filtramos solo puntos del usuario
            .get()
            .addOnSuccessListener { result ->
                val puntos = mutableListOf<Punto>()
                for (document in result) {
                    try {
                        val id = document.id
                        val nombre = document.getString("nombre") ?: ""
                        val descripcion = document.getString("descripcion") ?: ""
                        val latitud = document.getDouble("latitud") ?: 0.0
                        val longitud = document.getDouble("longitud") ?: 0.0
                        val usuarioCreador = document.getString("usuarioCreador")
                        val fechaCreacion = document.getTimestamp("fechaCreacion")

                        val punto = Punto(id, latitud, longitud, nombre, descripcion, usuarioCreador, fechaCreacion)
                        puntos.add(punto)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando documento: ${document.id}", e)
                    }
                }
                callback(puntos)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error obteniendo puntos: ", exception)
                callback(null)
            }
    }


    // Función para obtener las rutas desde Firestore
    fun getAllRutas(callback: (List<Ruta>?) -> Unit) {
        val rutasCollection = db.collection("rutas")

        rutasCollection
            .get()
            .addOnSuccessListener { result ->
                val rutas = mutableListOf<Ruta>()
                for (document in result) {
                    try {
                        // Asegúrate de que el documento contenga los campos adecuados
                        val id = document.id
                        val nombre = document.getString("nombre") ?: ""
                        val distanciaKm = document.getDouble("distanciaKm") ?: 0.0
                        val duracionMin = document.getDouble("duracionMin") ?: 0.0
                        val tipoRuta = document.getString("tipoRuta") ?: "driving"
                        val usuarioCreador = document.getString("usuarioCreador") ?: ""
                        val fechaCreacion = document.getTimestamp("fechaCreacion")

                        // Obtenemos los IDs de los puntos
                        val puntosIds = document.get("puntosIds") as? List<String> ?: emptyList()

                        // Creamos una ruta inicial sin puntos
                        val ruta = Ruta(
                            id = id,
                            distanciaKm = distanciaKm,
                            duracionMin = duracionMin,
                            fechaCreacion = fechaCreacion,
                            nombre = nombre,
                            puntos = emptyList(), // Los puntos se cargarán después
                            tipoRuta = tipoRuta,
                            usuarioCreador = usuarioCreador
                        )

                        rutas.add(ruta)

                        // Si hay puntos, los cargamos para esta ruta
                        if (puntosIds.isNotEmpty()) {
                            cargarPuntosParaRuta(puntosIds) { puntos ->
                                if (puntos != null) {
                                    // Actualizamos la ruta con los puntos cargados
                                    val index = rutas.indexOfFirst { it.id == id }
                                    if (index >= 0) {
                                        rutas[index] = rutas[index].copy(puntos = puntos)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando documento de ruta: ${document.id}", e)
                    }
                }
                callback(rutas)  // Llamamos al callback con la lista de rutas
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error obteniendo rutas: ", exception)
                callback(null)  // Llamamos al callback con null en caso de error
            }
    }

    // Función para cargar los puntos de una ruta específica
    private fun cargarPuntosParaRuta(puntosIds: List<String>, callback: (List<Punto>?) -> Unit) {
        if (puntosIds.isEmpty()) {
            callback(emptyList())
            return
        }

        val puntosCollection = db.collection("puntos_localizacion")

        puntosCollection
            .whereIn("__name__", puntosIds)
            .get()
            .addOnSuccessListener { result ->
                val puntos = mutableListOf<Punto>()
                for (document in result) {
                    try {
                        val id = document.id
                        val nombre = document.getString("nombre") ?: ""
                        val descripcion = document.getString("descripcion") ?: ""
                        val latitud = document.getDouble("latitud") ?: 0.0
                        val longitud = document.getDouble("longitud") ?: 0.0
                        val usuarioCreador = document.getString("usuarioCreador")
                        val fechaCreacion = document.getTimestamp("fechaCreacion")

                        val punto = Punto(id, latitud, longitud, nombre, descripcion, usuarioCreador, fechaCreacion)
                        puntos.add(punto)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando punto: ${document.id}", e)
                    }
                }

                // Ordenamos los puntos según el orden de puntosIds
                val puntosOrdenados = puntosIds.mapNotNull { id ->
                    puntos.find { it.id == id }
                }

                callback(puntosOrdenados)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error cargando puntos para ruta: ", exception)
                callback(null)
            }
    }

    // Función para obtener una ruta específica por su ID
    fun getRutaById(rutaId: String, callback: (Ruta?) -> Unit) {
        db.collection("rutas")
            .document(rutaId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    try {
                        val id = document.id
                        val nombre = document.getString("nombre") ?: ""
                        val distanciaKm = document.getDouble("distanciaKm") ?: 0.0
                        val duracionMin = document.getDouble("duracionMin") ?: 0.0
                        val tipoRuta = document.getString("tipoRuta") ?: "driving"
                        val usuarioCreador = document.getString("usuarioCreador") ?: ""
                        val fechaCreacion = document.getTimestamp("fechaCreacion")

                        // Obtenemos los IDs de los puntos
                        val puntosIds = document.get("puntosIds") as? List<String> ?: emptyList()

                        // Creamos una ruta inicial sin puntos
                        val ruta = Ruta(
                            id = id,
                            distanciaKm = distanciaKm,
                            duracionMin = duracionMin,
                            fechaCreacion = fechaCreacion,
                            nombre = nombre,
                            puntos = emptyList(), // Los puntos se cargarán después
                            tipoRuta = tipoRuta,
                            usuarioCreador = usuarioCreador
                        )

                        // Si hay puntos, los cargamos para esta ruta
                        if (puntosIds.isNotEmpty()) {
                            cargarPuntosParaRuta(puntosIds) { puntos ->
                                if (puntos != null) {
                                    // Devolvemos la ruta con los puntos cargados
                                    callback(ruta.copy(puntos = puntos))
                                } else {
                                    callback(ruta)
                                }
                            }
                        } else {
                            callback(ruta)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando documento de ruta: ${document.id}", e)
                        callback(null)
                    }
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error obteniendo ruta por ID: ", exception)
                callback(null)
            }
    }

    // Función para guardar un nuevo punto
    fun guardarPunto(punto: Punto, callback: (Boolean, String?) -> Unit) {
        val puntoMap = hashMapOf(
            "nombre" to punto.nombre,
            "descripcion" to punto.descripcion,
            "latitud" to punto.latitud,
            "longitud" to punto.longitud,
            "usuarioCreador" to punto.usuarioCreador,
            "fechaCreacion" to punto.fechaCreacion
        )

        db.collection("puntos_localizacion")
            .add(puntoMap)
            .addOnSuccessListener { documentRef ->
                callback(true, documentRef.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar punto", e)
                callback(false, null)
            }
    }

    // Función para guardar una nueva ruta
    fun guardarRuta(ruta: Ruta, callback: (Boolean, String?) -> Unit) {
        val puntosIds = ruta.puntos.map { it.id }

        val rutaMap = hashMapOf(
            "nombre" to ruta.nombre,
            "distanciaKm" to ruta.distanciaKm,
            "duracionMin" to ruta.duracionMin,
            "tipoRuta" to ruta.tipoRuta,
            "fechaCreacion" to ruta.fechaCreacion,
            "usuarioCreador" to ruta.usuarioCreador,
            "puntosIds" to puntosIds
        )

        db.collection("rutas")
            .add(rutaMap)
            .addOnSuccessListener { documentRef ->
                callback(true, documentRef.id)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar ruta", e)
                callback(false, null)
            }
    }
}