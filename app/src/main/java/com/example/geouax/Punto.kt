package com.example.geouax

import com.google.firebase.Timestamp
import java.io.Serializable

// Modelo de datos
data class Punto(
    val id: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val nombre: String = "",
    val descripcion: String = "",
    val usuarioCreador: String = "",
    val fechaCreacion: Timestamp? = null,
    val fotos: List<String>? = null,
    var distancia: String = "",
    var categoria: String = "muy lejos",
    var semaforo: Int = R.drawable.semaforo_red,
) : Serializable
