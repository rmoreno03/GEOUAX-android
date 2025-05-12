package com.example.geouax

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.Timestamp

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
    var semaforo: Int = R.drawable.semaforo_red
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readParcelable<Timestamp>(Timestamp::class.java.classLoader),
        parcel.createStringArrayList(),
        parcel.readString() ?: "",
        parcel.readString() ?: "muy lejos",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeDouble(latitud)
        parcel.writeDouble(longitud)
        parcel.writeString(nombre)
        parcel.writeString(descripcion)
        parcel.writeString(usuarioCreador)
        parcel.writeParcelable(fechaCreacion, flags)
        parcel.writeStringList(fotos)
        parcel.writeString(distancia)
        parcel.writeString(categoria)
        parcel.writeInt(semaforo)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Punto> {
        override fun createFromParcel(parcel: Parcel): Punto {
            return Punto(parcel)
        }

        override fun newArray(size: Int): Array<Punto?> {
            return arrayOfNulls(size)
        }
    }
}