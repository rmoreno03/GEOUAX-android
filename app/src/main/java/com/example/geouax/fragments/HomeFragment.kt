package com.example.geouax.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geouax.LocationControlador
import com.example.geouax.Punto
import com.example.geouax.PuntoAdapter
import com.example.geouax.R
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private val puntosList = mutableListOf<Punto>()
    private lateinit var adapter: PuntoAdapter
    private lateinit var locationHandler: LocationControlador
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = PuntoAdapter(puntosList) { puntoSeleccionado ->
            // Navegar a MapaFragment con los datos individuales
            val bundle = Bundle().apply {
                putDouble("latitud", puntoSeleccionado.latitud)
                putDouble("longitud", puntoSeleccionado.longitud)
                putString("nombre", puntoSeleccionado.nombre)
                putString("descripcion", puntoSeleccionado.descripcion)
            }
            findNavController().navigate(R.id.action_homeFragment_to_mapaFragment, bundle)
        }
        recyclerView.adapter = adapter

        locationHandler = LocationControlador(requireContext())

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        } else {
            locationHandler.checkGPSEnabledAndStartUpdates()
        }

        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                leerPuntosLocalizacion { puntos ->
                    puntosList.clear()
                    puntosList.addAll(puntos)
                    actualizarDistanciasYNotificar()
                }
                handler.postDelayed(this, 5000)
            }
        }
        handler.post(runnable)

        view.findViewById<Button>(R.id.btnAdd)?.setOnClickListener {
            mostrarDialogoAgregarPunto()
        }

        view.findViewById<Button>(R.id.btnRead)?.setOnClickListener {
            leerPuntosLocalizacion { puntos ->
                puntosList.clear()
                puntosList.addAll(puntos)
                actualizarDistanciasYNotificar()
            }
        }

        view.findViewById<Button>(R.id.btnUpdate)?.setOnClickListener {
            mostrarDialogoSeleccionarPunto { puntoSeleccionado ->
                mostrarDialogoActualizarPunto(puntoSeleccionado)
            }
        }

        view.findViewById<Button>(R.id.btnDelete)?.setOnClickListener {
            mostrarDialogoSeleccionarPunto { puntoSeleccionado ->
                eliminarPuntoLocalizacion(puntoSeleccionado.id)
            }
        }
    }

    private fun actualizarDistanciasYNotificar() {
        val currentLocation = locationHandler.getCurrentLocation()
        currentLocation?.let {
            adapter.updateDistances(it.latitude, it.longitude)
        } ?: Toast.makeText(requireContext(), "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
    }

    private fun mostrarDialogoAgregarPunto() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Agregar Punto de Localización")
        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val inputId = EditText(requireContext()).apply { hint = "Nombre del punto" }
        val inputLatitud = EditText(requireContext()).apply {
            hint = "Latitud"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        val inputLongitud = EditText(requireContext()).apply {
            hint = "Longitud"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        layout.addView(inputId)
        layout.addView(inputLatitud)
        layout.addView(inputLongitud)
        builder.setView(layout)

        builder.setPositiveButton("Agregar") { _, _ ->
            val id = inputId.text.toString()
            val latitud = inputLatitud.text.toString().toDoubleOrNull()
            val longitud = inputLongitud.text.toString().toDoubleOrNull()
            if (id.isNotEmpty() && latitud != null && longitud != null) {
                agregarPuntoLocalizacion(id, latitud, longitud)
            } else {
                Toast.makeText(requireContext(), "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoSeleccionarPunto(callback: (Punto) -> Unit) {
        val puntosNombres = puntosList.map { it.nombre }.toTypedArray()
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Selecciona un punto")
        builder.setItems(puntosNombres) { _, which ->
            callback(puntosList[which])
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun mostrarDialogoActualizarPunto(punto: Punto) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Actualizar Punto")

        val layout = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }

        val inputLatitud = EditText(requireContext()).apply {
            hint = "Latitud"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(punto.latitud.toString())
        }
        val inputLongitud = EditText(requireContext()).apply {
            hint = "Longitud"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
            setText(punto.longitud.toString())
        }

        layout.addView(inputLatitud)
        layout.addView(inputLongitud)
        builder.setView(layout)

        builder.setPositiveButton("Actualizar") { _, _ ->
            val nuevaLatitud = inputLatitud.text.toString().toDoubleOrNull()
            val nuevaLongitud = inputLongitud.text.toString().toDoubleOrNull()
            if (nuevaLatitud != null && nuevaLongitud != null) {
                actualizarPuntoLocalizacion(punto.id, nuevaLatitud, nuevaLongitud)
            } else {
                Toast.makeText(requireContext(), "Por favor, introduce valores válidos.", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun leerPuntosLocalizacion(callback: (List<Punto>) -> Unit) {
        db.collection("puntos_localizacion")
            .get()
            .addOnSuccessListener { result ->
                val puntos = result.map { document ->
                    Punto(
                        id = document.id,
                        nombre = document.get("nombre")?.toString() ?: "",
                        descripcion = document.get("descripcion")?.toString() ?: "",
                        latitud = document.getDouble("latitud") ?: 0.0,
                        longitud = document.getDouble("longitud") ?: 0.0,
                        usuarioCreador = document.get("usuarioCreador")?.toString() ?: ""
                    )
                }
                callback(puntos)
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al leer datos: $e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun agregarPuntoLocalizacion(nombre: String, latitud: Double, longitud: Double) {
        val punto = mapOf(
            "nombre" to nombre,
            "descripcion" to "Descripción por defecto",
            "latitud" to latitud,
            "longitud" to longitud,
            "usuarioCreador" to "anónimo"
        )
        db.collection("puntos_localizacion")
            .add(punto)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Punto agregado.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al agregar: $e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun actualizarPuntoLocalizacion(documentId: String, nuevaLatitud: Double, nuevaLongitud: Double) {
        db.collection("puntos_localizacion").document(documentId)
            .update("latitud", nuevaLatitud, "longitud", nuevaLongitud)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Punto actualizado.", Toast.LENGTH_SHORT).show()
                leerPuntosLocalizacion { puntos ->
                    puntosList.clear()
                    puntosList.addAll(puntos)
                    actualizarDistanciasYNotificar()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al actualizar: $e", Toast.LENGTH_SHORT).show()
            }
    }

    private fun eliminarPuntoLocalizacion(documentId: String) {
        db.collection("puntos_localizacion").document(documentId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Punto eliminado.", Toast.LENGTH_SHORT).show()
                leerPuntosLocalizacion { puntos ->
                    puntosList.clear()
                    puntosList.addAll(puntos)
                    actualizarDistanciasYNotificar()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al eliminar: $e", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(runnable)
        locationHandler.onDestroy()
    }
}



