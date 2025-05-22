package com.example.geouax.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.view.*
import android.widget.*
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geouax.Punto
import com.example.geouax.R
import com.example.geouax.Ruta
import com.example.geouax.RutaAdapter
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.google.maps.android.PolyUtil
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL


class PuntoFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var layoutRutas: LinearLayout
    private lateinit var layoutFormulario: LinearLayout
    private val rutasUsuario = mutableListOf<Ruta>()
    private val puntosUsuario = mutableListOf<Punto>()
    private lateinit var layoutMisRutas: LinearLayout
    private lateinit var scrollTodasRutas: ScrollView
    private lateinit var scrollMisRutas: ScrollView

    private var puntoInicioSeleccionado: Punto? = null
    private var puntoFinSeleccionado: Punto? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_punto, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        layoutRutas = view.findViewById(R.id.layoutRutas)
        layoutFormulario = view.findViewById(R.id.layoutFormulario)
        val buttonCrearRuta: Button = view.findViewById(R.id.CrearnuevaRuta)
        val buttonSelectPuntos: Button = view.findViewById(R.id.buttonSelectPuntos)
        val buttonGuardarRuta: Button = view.findViewById(R.id.buttonGuardarRuta)
        val buttonCancelarRuta: Button = view.findViewById(R.id.buttonCancelarRuta)
        layoutMisRutas = view.findViewById(R.id.layoutMisRutas)
        scrollTodasRutas = view.findViewById(R.id.scrollTodasRutas)
        scrollMisRutas = view.findViewById(R.id.scrollMisRutas)

        val buttonTodasRutas: Button = view.findViewById(R.id.buttonTodasRutas)
        val buttonMisRutas: Button = view.findViewById(R.id.buttonMisRutas)

        buttonTodasRutas.setOnClickListener {
            scrollTodasRutas.visibility = View.VISIBLE
            scrollMisRutas.visibility = View.GONE
        }

        buttonMisRutas.setOnClickListener {
            scrollTodasRutas.visibility = View.GONE
            scrollMisRutas.visibility = View.VISIBLE
        }

        buttonCrearRuta.setOnClickListener { mostrarFormularioCreacionRuta() }
        buttonCancelarRuta.setOnClickListener { ocultarFormularioCreacionRuta() }
        buttonSelectPuntos.setOnClickListener { mostrarDialogoSeleccionarPunto("Selecciona punto de inicio") }
        buttonGuardarRuta.setOnClickListener { guardarRuta() }

        cargarRutasUsuario()
        cargarPuntosUsuario()
    }

    private fun cargarRutasUsuario() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        db.collection("rutas")
            .get()
            .addOnSuccessListener { result ->
                rutasUsuario.clear()
                layoutRutas.removeAllViews()
                layoutMisRutas.removeAllViews()

                val usuariosCache = mutableMapOf<String, String>()

                for (document in result) {
                    val ruta = document.toObject(Ruta::class.java).copy(id = document.id)
                    rutasUsuario.add(ruta)

                    val uidCreador = ruta.usuarioCreador

                    val agregarBoton = { nombreCreador: String ->
                        if (uidCreador == user.uid) {
                            agregarBotonRuta(ruta, nombreCreador, layoutMisRutas)
                        }
                        agregarBotonRuta(ruta, nombreCreador, layoutRutas)
                    }

                    if (usuariosCache.containsKey(uidCreador)) {
                        agregarBoton(usuariosCache[uidCreador]!!)
                    } else {
                        db.collection("usuarios").document(uidCreador)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val nombreCreador = userDoc.getString("nombre") ?: "Usuario desconocido"
                                usuariosCache[uidCreador] = nombreCreador
                                agregarBoton(nombreCreador)
                            }
                            .addOnFailureListener {
                                agregarBoton("Usuario desconocido")
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar rutas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun agregarBotonRuta(ruta: Ruta, nombreCreador: String, contenedor: LinearLayout) {
        val botonRuta = Button(ContextThemeWrapper(requireContext(), R.style.EstiloBotonRuta)).apply {
            val nombreRuta = ruta.nombre
            val creador = "Creado por: $nombreCreador"

            val spannable = SpannableString("$nombreRuta\n$creador")
            spannable.setSpan(
                RelativeSizeSpan(1.3f), 0, nombreRuta.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.black)), 0, nombreRuta.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.gray)), nombreRuta.length + 1, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannable.setSpan(
                RelativeSizeSpan(0.9f), nombreRuta.length + 1, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            text = spannable
            setPadding(40, 48, 40, 48)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            isAllCaps = false

            background = ContextCompat.getDrawable(requireContext(), R.drawable.boton_ruta_background)
            stateListAnimator = null
            elevation = 6f

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
            }

            setOnClickListener {
                mostrarRutaEnMapa(ruta)
            }
        }

        contenedor.addView(botonRuta)
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

        val origen = LatLng(puntoInicioSeleccionado!!.latitud, puntoInicioSeleccionado!!.longitud)
        val destino = LatLng(puntoFinSeleccionado!!.latitud, puntoFinSeleccionado!!.longitud)

        obtenerRutaYActualizar(origen, destino) { distanciaKm, duracionMin, puntosRuta ->
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@obtenerRutaYActualizar

            val nuevaRuta = Ruta(
                nombre = nombreRuta,
                tipoRuta = tipoTransporte,
                puntos = listOf(puntoInicioSeleccionado!!, puntoFinSeleccionado!!),
                usuarioCreador = currentUser.uid,
                fechaCreacion = Timestamp.now(),
                distanciaKm = distanciaKm,
                duracionMin = duracionMin
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
    }

    private fun obtenerRutaYActualizar(
        origen: LatLng,
        destino: LatLng,
        onResultado: (distanciaKm: Double, duracionMin: Double, puntos: List<LatLng>) -> Unit
    ) {
        val apiKey = "YOUR_GOOGLE_MAPS_API_KEY"
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${origen.latitude},${origen.longitude}" +
                "&destination=${destino.latitude},${destino.longitude}" +
                "&mode=driving&key=$apiKey"

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                val response = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                val route = json.getJSONArray("routes").getJSONObject(0)
                val leg = route.getJSONArray("legs").getJSONObject(0)

                val distancia = leg.getJSONObject("distance").getDouble("value") / 1000
                val duracion = leg.getJSONObject("duration").getDouble("value") / 60

                val puntosEncoded = route.getJSONObject("overview_polyline").getString("points")
                val puntosRuta = PolyUtil.decode(puntosEncoded)

                requireActivity().runOnUiThread {
                    onResultado(distancia, duracion, puntosRuta)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun mostrarRutaEnMapa(ruta: Ruta) {
        val args = Bundle().apply {
            putParcelable("ruta", ruta)
        }

        val mapaFragment = MapaFragment().apply {
            arguments = args
        }

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, mapaFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun mostrarFormularioCreacionRuta() {
        layoutFormulario.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.buttonGuardarRuta)?.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.buttonCancelarRuta)?.visibility = View.VISIBLE
        view?.findViewById<Button>(R.id.CrearnuevaRuta)?.visibility = View.GONE
        puntoInicioSeleccionado = null
        puntoFinSeleccionado = null
    }

    private fun ocultarFormularioCreacionRuta() {
        layoutRutas.visibility = View.VISIBLE
        layoutFormulario.visibility = View.GONE
        view?.findViewById<EditText>(R.id.textNombreRuta)?.setText("")
        view?.findViewById<Button>(R.id.CrearnuevaRuta)?.visibility = View.VISIBLE
    }
}


















