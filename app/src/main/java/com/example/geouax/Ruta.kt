package com.example.geouax

import com.google.firebase.Timestamp
import java.io.Serializable

data class Ruta(
    val id: String = "",
    var distanciaKm: Double = 0.0,
    var duracionMin: Double = 0.0,
    var fechaCreacion: Timestamp? = null,
    var nombre: String = "",
    var puntos: List<Punto> = emptyList(),
    var tipoRuta: String = "",
    var usuarioCreador: String = ""
) : Serializable
