package com.example.geouax

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

data class Ruta(
    val id: String = "",
    var distanciaKm: Double = 0.0,
    var duracionMin: Double = 0.0,
    var fechaCreacion: Timestamp? = null,
    var nombre: String = "",
    var puntos: List<Punto> = emptyList(),
    var tipoRuta: String = "",
    var usuarioCreador: String = ""
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readParcelable<Timestamp>(Timestamp::class.java.classLoader),
        parcel.readString() ?: "",
        parcel.createTypedArrayList(Punto.CREATOR) ?: emptyList(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeDouble(distanciaKm)
        parcel.writeDouble(duracionMin)
        parcel.writeParcelable(fechaCreacion, flags)
        parcel.writeString(nombre)
        parcel.writeTypedList(puntos)
        parcel.writeString(tipoRuta)
        parcel.writeString(usuarioCreador)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Ruta> {
        override fun createFromParcel(parcel: Parcel): Ruta {
            return Ruta(parcel)
        }

        override fun newArray(size: Int): Array<Ruta?> {
            return arrayOfNulls(size)
        }
    }
}