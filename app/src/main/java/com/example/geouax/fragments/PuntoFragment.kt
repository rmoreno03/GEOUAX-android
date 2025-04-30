package com.example.geouax.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geouax.Punto
import com.example.geouax.R
import com.example.geouax.Ruta
import com.example.geouax.RutaAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp

class PuntoFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var recyclerViewRutas: RecyclerView
    private lateinit var layoutRutas: LinearLayout
    private lateinit var layoutFormulario: LinearLayout
    private lateinit var rutaAdapter: RutaAdapter
    private val rutasUsuario = mutableListOf<Ruta>()
    private val puntosUsuario = mutableListOf<Punto>()

    private var puntoInicioSeleccionado: Punto? = null
    private var puntoFinSeleccionado: Punto? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_punto, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        recyclerViewRutas = view.findViewById(R.id.recyclerViewRutas)
        layoutRutas = view.findViewById(R.id.layoutRutas)
        layoutFormulario = view.findViewById(R.id.layoutFormulario)
        val buttonCrearRuta: Button = view.findViewById(R.id.CrearnuevaRuta)
        val buttonSelectPuntos: Button = view.findViewById(R.id.buttonSelectPuntos)
        val buttonGuardarRuta: Button = view.findViewById(R.id.buttonGuardarRuta)
        val buttonCancelarRuta: Button = view.findViewById(R.id.buttonCancelarRuta)

        recyclerViewRutas.layoutManager = LinearLayoutManager(requireContext())
        rutaAdapter = RutaAdapter(rutasUsuario)

        recyclerViewRutas.adapter = rutaAdapter

        buttonCrearRuta.setOnClickListener {
            mostrarFormularioCreacionRuta()
        }

        buttonCancelarRuta.setOnClickListener {
            ocultarFormularioCreacionRuta()
        }

        buttonSelectPuntos.setOnClickListener {
            mostrarDialogoSeleccionarPunto("Selecciona punto de inicio")
        }

        buttonGuardarRuta.setOnClickListener {
            guardarRuta()
        }

        cargarRutasUsuario()
        cargarPuntosUsuario()
    }

    private fun cargarRutasUsuario() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("rutas")
            .get()
            .addOnSuccessListener { result ->
                rutasUsuario.clear()
                for (document in result) {
                    val ruta = document.toObject(Ruta::class.java).copy(id = document.id)
                    rutasUsuario.add(ruta)
                    Log.d("PuntoFragment", "Ruta cargada: $ruta") // Log de las rutas cargadas
                }
                rutaAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar rutas", Toast.LENGTH_SHORT).show()
            }

    }

    private fun cargarPuntosUsuario() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("puntos_localizacion")
            .whereEqualTo("usuarioCreador", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                puntosUsuario.clear()
                for (document in result) {
                    val punto = Punto(
                        id = document.id,
                        nombre = document.getString("nombre").orEmpty(),
                        descripcion = document.getString("descripcion").orEmpty(),
                        latitud = document.getDouble("latitud") ?: 0.0,
                        longitud = document.getDouble("longitud") ?: 0.0,
                        usuarioCreador = document.getString("usuarioCreador").orEmpty()
                    )
                    puntosUsuario.add(punto)
                }
            }
    }

    private fun mostrarDialogoSeleccionarPunto(titulo: String) {
        if (puntosUsuario.isEmpty()) {
            Toast.makeText(requireContext(), "No hay puntos disponibles", Toast.LENGTH_SHORT).show()
            return
        }

        val puntosNombres = puntosUsuario.map { it.nombre }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(titulo)
            .setItems(puntosNombres) { _, which ->
                val seleccionado = puntosUsuario[which]
                if (titulo.contains("inicio", true)) {
                    puntoInicioSeleccionado = seleccionado
                    mostrarDialogoSeleccionarPunto("Selecciona punto de fin")
                } else {
                    puntoFinSeleccionado = seleccionado
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun guardarRuta() {
        val nombreRuta = view?.findViewById<EditText>(R.id.textNombreRuta)?.text.toString()
        val tipoTransporte = view?.findViewById<Spinner>(R.id.spinnerTransportMode)?.selectedItem.toString()

        if (puntoInicioSeleccionado == null || puntoFinSeleccionado == null || nombreRuta.isBlank()) {
            Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val nuevaRuta = Ruta(
            nombre = nombreRuta,
            tipoRuta = tipoTransporte,
            puntos = listOf(puntoInicioSeleccionado!!, puntoFinSeleccionado!!),
            usuarioCreador = currentUser.uid,
            fechaCreacion = Timestamp.now()
        )

        db.collection("rutas")
            .add(nuevaRuta)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Ruta guardada", Toast.LENGTH_SHORT).show()
                ocultarFormularioCreacionRuta()
                cargarRutasUsuario()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun mostrarFormularioCreacionRuta() {
        layoutRutas.visibility = View.GONE // Ocultar la lista de rutas
        layoutFormulario.visibility = View.VISIBLE // Mostrar el formulario de creación
        view?.findViewById<Button>(R.id.buttonGuardarRuta)?.visibility = View.VISIBLE // Mostrar el botón "Guardar Ruta"
        view?.findViewById<Button>(R.id.buttonCancelarRuta)?.visibility = View.VISIBLE // Mostrar el botón "Cancelar Ruta"
        view?.findViewById<Button>(R.id.CrearnuevaRuta)?.visibility = View.GONE // Ocultar el botón "Generar Ruta"
        puntoInicioSeleccionado = null // Limpiar selección de punto de inicio
        puntoFinSeleccionado = null // Limpiar selección de punto de fin
    }


    private fun ocultarFormularioCreacionRuta() {
        layoutRutas.visibility = View.VISIBLE // Mostrar la lista de rutas
        layoutFormulario.visibility = View.GONE // Ocultar el formulario de creación
        view?.findViewById<EditText>(R.id.textNombreRuta)?.setText("") // Limpiar el campo "Nombre de la ruta"
        view?.findViewById<Button>(R.id.CrearnuevaRuta)?.visibility = View.VISIBLE // Volver a mostrar el botón "Generar Ruta"
    }

}

















