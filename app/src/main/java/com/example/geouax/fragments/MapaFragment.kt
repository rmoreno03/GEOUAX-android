package com.example.geouax.fragments

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.geouax.Punto
import com.example.geouax.R
import com.example.geouax.Ruta
import com.example.geouax.AuthManager
import com.example.geouax.firestore.FirestoreHelper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import kotlin.math.roundToInt

class MapaFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MapaFragment"
        private const val DIRECTIONS_API_KEY = "AIzaSyAP1RpRJ_1Kerbwxabm0wvcYKE1dz0aspw"
    }

    private lateinit var googleMap: GoogleMap
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var puntosRuta: List<Punto> = emptyList()
    private var marcadores: MutableList<Marker> = mutableListOf()
    private var polylines: MutableList<Polyline> = mutableListOf()
    private var puntoInicio: Punto? = null
    private var puntoDestino: Punto? = null
    private lateinit var fabRuta: FloatingActionButton
    private lateinit var fabLimpiar: FloatingActionButton
    private lateinit var fabPuntos: FloatingActionButton
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Variable para controlar el estado de autenticación
    private var isUserLoggedIn = false

    // Listener para cambios en la autenticación
    private val authStateListener: (Boolean) -> Unit = { isLoggedIn ->
        isUserLoggedIn = isLoggedIn
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Verificar estado inicial de autenticación
        isUserLoggedIn = AuthManager.isUserLoggedIn()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.mapa_activity, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar los FABs
        fabRuta = view.findViewById(R.id.fabCalcularRuta)
        fabLimpiar = view.findViewById(R.id.fabLimpiar)
        fabPuntos = view.findViewById(R.id.fabVerPuntos)

        // Configurar los listeners de los FABs
        fabRuta.setOnClickListener {
            if (!isUserLoggedIn) {
                mostrarDialogoIniciarSesion()
                return@setOnClickListener
            }

            if (puntoInicio != null && puntoDestino != null) {
                calcularRuta(puntoInicio!!, puntoDestino!!)
            } else {
                mostrarDialogoSeleccionarPuntos()
            }
        }

        fabLimpiar.setOnClickListener {
            limpiarMapa()
        }

        fabPuntos.setOnClickListener {
            mostrarPuntosFirebase()
        }

        // Registrar listener para cambios en autenticación
        AuthManager.addAuthStateListener(authStateListener)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.clear()

        // Aplicar configuración del mapa
        configurarMapa()

        // Obtener la Ruta desde los argumentos (ya es Parcelable)
        val ruta = arguments?.getParcelable<Ruta>("ruta")

        if (ruta != null && ruta.puntos.isNotEmpty()) {
            mostrarRutaExistente(ruta)
        } else {
            // Si no hay ruta en los argumentos, mostrar puntos de Firebase
            mostrarPuntosFirebase()
        }

        // Escuchar clics para agregar nuevos puntos
        googleMap.setOnMapClickListener { latLng ->
            if (!isUserLoggedIn) {
                mostrarDialogoIniciarSesion()
                return@setOnMapClickListener
            }
            mostrarDialogoAgregarPunto(latLng.latitude, latLng.longitude)
        }

        // Escuchar clics en marcadores para seleccionarlos como inicio o destino
        googleMap.setOnMarkerClickListener { marker ->
            // Si el usuario no está logueado y quiere interactuar con marcadores para rutas
            if (!isUserLoggedIn) {
                mostrarDialogoIniciarSesion()
                return@setOnMarkerClickListener true
            }
            mostrarDialogoSeleccionarPuntoRuta(marker)
            true
        }
    }

    private fun configurarMapa() {
        with(googleMap) {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMyLocationButtonEnabled = true
            uiSettings.isMapToolbarEnabled = true

            try {
                isMyLocationEnabled = true
            } catch (e: SecurityException) {
                Log.e(TAG, "Error al habilitar la ubicación del usuario: ${e.message}")
            }

            // Establecer tipo de mapa
            mapType = GoogleMap.MAP_TYPE_NORMAL

            // Zoom inicial en España
            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(40.416775, -3.703790), 6f))
        }
    }

    private fun mostrarRutaExistente(ruta: Ruta) {
        puntosRuta = ruta.puntos
        val boundsBuilder = LatLngBounds.builder()

        // Limpiar el mapa primero
        limpiarMapa()

        // Agregar marcadores
        puntosRuta.forEachIndexed { index, punto ->
            val latLng = LatLng(punto.latitud, punto.longitud)
            val title = when (index) {
                0 -> "Inicio: ${punto.nombre}"
                puntosRuta.lastIndex -> "Fin: ${punto.nombre}"
                else -> "${punto.nombre} (Punto ${index + 1})"
            }

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(title)
                    .snippet(punto.descripcion)
                    .icon(
                        when (index) {
                            0 -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            puntosRuta.lastIndex -> BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )

                            else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                        }
                    )
            )

            if (marker != null) {
                marcadores.add(marker)
                marker.tag = punto
            }

            boundsBuilder.include(latLng)
        }

        // Dibujar la línea de la ruta si tiene más de un punto
        if (puntosRuta.size > 1) {
            val latLngs = puntosRuta.map { LatLng(it.latitud, it.longitud) }
            val polyline = googleMap.addPolyline(
                PolylineOptions()
                    .addAll(latLngs)
                    .width(10f)
                    .color(resources.getColor(R.color.teal_700, null))
                    .geodesic(true)
            )
            polylines.add(polyline)

            // Mostrar información de la ruta
            mostrarInformacionRuta(ruta)
        }

        // Ajustar cámara al conjunto de puntos
        try {
            val bounds = boundsBuilder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error al crear bounds para la cámara: ${e.message}")
            // Si solo hay un punto, hacer zoom a ese punto
            if (puntosRuta.isNotEmpty()) {
                val punto = puntosRuta.first()
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(punto.latitud, punto.longitud), 15f
                    )
                )
            }
        }
    }

    private fun mostrarInformacionRuta(ruta: Ruta) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_ruta_info, null)

        val tvNombre = bottomSheetView.findViewById<TextView>(R.id.tvNombreRuta)
        val tvDistancia = bottomSheetView.findViewById<TextView>(R.id.tvDistancia)
        val tvDuracion = bottomSheetView.findViewById<TextView>(R.id.tvDuracion)
        val tvTipo = bottomSheetView.findViewById<TextView>(R.id.tvTipoRuta)
        val btnGuardar = bottomSheetView.findViewById<Button>(R.id.btnGuardarRuta)

        tvNombre.text = ruta.nombre
        tvDistancia.text = "Distancia: ${formatearDistancia(ruta.distanciaKm)} km"
        tvDuracion.text = "Duración: ${formatearDuracion(ruta.duracionMin)} min"
        tvTipo.text = "Tipo: ${ruta.tipoRuta}"

        // Mostrar o ocultar el botón de guardar según si el usuario ha iniciado sesión
        if (!isUserLoggedIn) {
            btnGuardar.visibility = View.GONE
        } else {
            btnGuardar.visibility = View.VISIBLE
            btnGuardar.setOnClickListener {
                bottomSheetDialog.dismiss()
                // Si la ruta ya existe en Firebase, no guardarla de nuevo
                if (ruta.id.isEmpty()) {
                    mostrarDialogoGuardarRuta(ruta)
                }
            }
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()
    }

    private fun mostrarPuntosFirebase() {
        // Mostrar un diálogo de carga
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Cargando puntos")
            .setMessage("Obteniendo puntos de localización...")
            .setCancelable(false)
            .create()

        loadingDialog.show()

        FirestoreHelper.getAllPuntos { puntosList ->
            loadingDialog.dismiss()

            if (puntosList.isNullOrEmpty()) {
                mostrarMensaje("No hay puntos para mostrar")
                return@getAllPuntos
            }

            // Limpiar marcadores anteriores pero conservar puntos seleccionados
            limpiarMarcadoresNoSeleccionados()

            val boundsBuilder = LatLngBounds.builder()
            var addedPoints = false

            puntosList.forEach { punto ->
                // Verificar si el punto ya está como marcador seleccionado
                val yaExiste = marcadores.any {
                    val puntoMarker = it.tag as? Punto
                    puntoMarker?.id == punto.id
                }

                if (!yaExiste) {
                    val latLng = LatLng(punto.latitud, punto.longitud)
                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(punto.nombre)
                            .snippet(punto.descripcion)
                    )

                    if (marker != null) {
                        marker.tag = punto
                        marcadores.add(marker)
                        boundsBuilder.include(latLng)
                        addedPoints = true
                    }
                }
            }

            // Ajustar la cámara para mostrar todos los puntos
            if (addedPoints) {
                try {
                    val bounds = boundsBuilder.build()
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Error al crear bounds para la cámara: ${e.message}")
                    // Si hay puntos, hacer zoom al primer punto
                    if (puntosList.isNotEmpty()) {
                        val punto = puntosList.first()
                        googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(punto.latitud, punto.longitud), 12f
                            )
                        )
                    }
                }
            }
        }
    }

    private fun mostrarDialogoAgregarPunto(latitud: Double, longitud: Double) {
        // Verificar si el usuario ha iniciado sesión
        if (!isUserLoggedIn) {
            mostrarDialogoIniciarSesion()
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Agregar Punto de Localización")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputNombre = EditText(requireContext()).apply { hint = "Nombre del punto" }
        val inputDescripcion =
            EditText(requireContext()).apply { hint = "Descripción del punto" }
        val inputLatitud = EditText(requireContext()).apply {
            hint = "Latitud"
            setText(latitud.toString())
            isEnabled = false
        }
        val inputLongitud = EditText(requireContext()).apply {
            hint = "Longitud"
            setText(longitud.toString())
            isEnabled = false
        }

        layout.addView(inputNombre)
        layout.addView(inputDescripcion)
        layout.addView(inputLatitud)
        layout.addView(inputLongitud)

        builder.setView(layout)

        builder.setPositiveButton("Agregar") { _, _ ->
            val nombre = inputNombre.text.toString()
            val descripcion = inputDescripcion.text.toString()

            if (nombre.isNotEmpty() && descripcion.isNotEmpty()) {
                agregarPuntoLocalizacion(nombre, descripcion, latitud, longitud)
            } else {
                mostrarMensaje("Por favor, completa todos los campos.")
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun agregarPuntoLocalizacion(
        nombre: String,
        descripcion: String,
        latitud: Double,
        longitud: Double
    ) {
        val currentUser = auth.currentUser
        val currentUID = currentUser?.uid

        if (currentUID == null) {
            mostrarMensaje("Debes iniciar sesión para agregar puntos.")
            mostrarDialogoIniciarSesion()
            return
        }

        val punto = hashMapOf(
            "nombre" to nombre,
            "descripcion" to descripcion,
            "latitud" to latitud,
            "longitud" to longitud,
            "usuarioCreador" to currentUID,
            "fechaCreacion" to com.google.firebase.Timestamp.now()
        )

        // Mostrar diálogo de carga
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Guardando punto")
            .setMessage("Guardando punto de localización...")
            .setCancelable(false)
            .create()

        loadingDialog.show()

        db.collection("puntos_localizacion")
            .add(punto)
            .addOnSuccessListener { documentRef ->
                loadingDialog.dismiss()
                mostrarMensaje("Punto agregado correctamente.")

                // Crear objeto Punto
                val nuevoPunto = Punto(
                    documentRef.id,
                    latitud,
                    longitud,
                    nombre,
                    descripcion,
                    currentUID,
                    com.google.firebase.Timestamp.now()
                )

                // Agregar marcador
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(latitud, longitud))
                        .title(nombre)
                        .snippet(descripcion)
                )

                if (marker != null) {
                    marker.tag = nuevoPunto
                    marcadores.add(marker)

                    // Preguntar si desea usar este punto como inicio o destino
                    mostrarDialogoSeleccionarPuntoRuta(marker)
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                mostrarMensaje("Error al agregar punto: $e")
            }
    }

    private fun mostrarDialogoSeleccionarPuntoRuta(marker: Marker) {
        // Verificar si el usuario ha iniciado sesión
        if (!isUserLoggedIn) {
            mostrarDialogoIniciarSesion()
            return
        }

        val punto = marker.tag as? Punto ?: return

        val options = arrayOf(
            "Seleccionar como punto de inicio",
            "Seleccionar como punto de destino",
            "Ver detalles",
            "Cancelar"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("¿Qué deseas hacer?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> seleccionarPuntoInicio(punto, marker)
                    1 -> seleccionarPuntoDestino(punto, marker)
                    2 -> mostrarDetallesPunto(punto)
                    // 3 es Cancelar, no hacemos nada
                }
            }
            .show()
    }

    private fun seleccionarPuntoInicio(punto: Punto, marker: Marker) {
        // Restablecer el icono del punto de inicio anterior si existe
        marcadores.find { (it.tag as? Punto) == puntoInicio }
            ?.setIcon(BitmapDescriptorFactory.defaultMarker())

        // Establecer el nuevo punto de inicio
        puntoInicio = punto
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))

        mostrarMensaje("Punto de inicio establecido: ${punto.nombre}")

        // Si ya hay un punto de destino, preguntar si calcular la ruta
        if (puntoDestino != null) {
            AlertDialog.Builder(requireContext())
                .setTitle("Calcular ruta")
                .setMessage("¿Deseas calcular la ruta entre los puntos seleccionados?")
                .setPositiveButton("Sí") { _, _ ->
                    calcularRuta(puntoInicio!!, puntoDestino!!)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun seleccionarPuntoDestino(punto: Punto, marker: Marker) {
        // Restablecer el icono del punto de destino anterior si existe
        marcadores.find { (it.tag as? Punto) == puntoDestino }
            ?.setIcon(BitmapDescriptorFactory.defaultMarker())

        // Establecer el nuevo punto de destino
        puntoDestino = punto
        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))

        mostrarMensaje("Punto de destino establecido: ${punto.nombre}")

        // Si ya hay un punto de inicio, preguntar si calcular la ruta
        if (puntoInicio != null) {
            AlertDialog.Builder(requireContext())
                .setTitle("Calcular ruta")
                .setMessage("¿Deseas calcular la ruta entre los puntos seleccionados?")
                .setPositiveButton("Sí") { _, _ ->
                    calcularRuta(puntoInicio!!, puntoDestino!!)
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun mostrarDetallesPunto(punto: Punto) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Detalles del punto")

        val mensaje = """
            Nombre: ${punto.nombre}
            Descripción: ${punto.descripcion}
            Latitud: ${punto.latitud}
            Longitud: ${punto.longitud}
        """.trimIndent()

        builder.setMessage(mensaje)
        builder.setPositiveButton("OK", null)
        builder.show()
    }

    private fun mostrarDialogoSeleccionarPuntos() {
        // Verificar si el usuario ha iniciado sesión
        if (!isUserLoggedIn) {
            mostrarDialogoIniciarSesion()
            return
        }

        if (marcadores.size < 2) {
            mostrarMensaje("Necesitas al menos dos puntos para calcular una ruta.")
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Seleccionar puntos para la ruta")

        val puntosDisponibles = marcadores.mapNotNull { it.tag as? Punto }
        val nombresPuntos = puntosDisponibles.map { it.nombre }.toTypedArray()

        // Selección para punto de inicio
        builder.setItems(nombresPuntos) { _, inicioIndex ->
            puntoInicio = puntosDisponibles[inicioIndex]

            // Buscar el marcador correspondiente y cambiar su icono
            marcadores.find { (it.tag as? Punto) == puntoInicio }?.setIcon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
            )

            // Mostrar diálogo para seleccionar punto de destino
            AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar punto de destino")
                .setItems(nombresPuntos) { _, destinoIndex ->
                    if (inicioIndex == destinoIndex) {
                        mostrarMensaje("El punto de inicio y destino no pueden ser el mismo.")
                        return@setItems
                    }

                    puntoDestino = puntosDisponibles[destinoIndex]

                    // Buscar el marcador correspondiente y cambiar su icono
                    marcadores.find { (it.tag as? Punto) == puntoDestino }?.setIcon(
                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )

                    // Calcular la ruta
                    calcularRuta(puntoInicio!!, puntoDestino!!)
                }
                .show()
        }

        builder.show()
    }

    private fun calcularRuta(inicio: Punto, destino: Punto) {
        // Verificar si el usuario ha iniciado sesión
        if (!isUserLoggedIn) {
            mostrarDialogoIniciarSesion()
            return
        }

        // Mostrar diálogo de carga
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Calculando ruta")
            .setMessage("Obteniendo la mejor ruta entre los puntos...")
            .setCancelable(false)
            .create()

        loadingDialog.show()

        // Limpiar polylines anteriores
        polylines.forEach { it.remove() }
        polylines.clear()

        // Lanzar coroutine para la petición a la API
        coroutineScope.launch {
            try {
                val rutaCalculada = obtenerRutaDesdeAPI(inicio, destino)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()

                    if (rutaCalculada != null) {
                        // Crear nueva ruta con los puntos
                        val nuevaRuta = Ruta(
                            id = "",
                            distanciaKm = rutaCalculada.distanciaKm,
                            duracionMin = rutaCalculada.duracionMin,
                            fechaCreacion = com.google.firebase.Timestamp.now(),
                            nombre = "Ruta de ${inicio.nombre} a ${destino.nombre}",
                            puntos = listOf(inicio, destino),
                            tipoRuta = "driving",
                            usuarioCreador = auth.currentUser?.uid ?: ""
                        )

                        // Añadir polyline
                        val polyline = googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(rutaCalculada.puntos)
                                .width(10f)
                                .color(resources.getColor(R.color.teal_700, null))
                                .geodesic(true)
                        )
                        polylines.add(polyline)

                        // Mostrar información y ajustar cámara
                        mostrarInformacionRuta(nuevaRuta)
                        ajustarCamaraARuta(rutaCalculada.puntos)
                    } else {
                        mostrarMensaje("No se pudo calcular la ruta entre los puntos seleccionados.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    mostrarMensaje("Error al calcular la ruta: ${e.message}")
                    Log.e(TAG, "Error al calcular ruta", e)
                }
            }
        }
    }

    data class RutaCalculada(
        val puntos: List<LatLng>,
        val distanciaKm: Double,
        val duracionMin: Double
    )

    private suspend fun obtenerRutaDesdeAPI(inicio: Punto, destino: Punto): RutaCalculada? =
        withContext(Dispatchers.IO) {
            try {
                val origen = "${inicio.latitud},${inicio.longitud}"
                val dest = "${destino.latitud},${destino.longitud}"

                val urlString = "https://maps.googleapis.com/maps/api/directions/json" +
                        "?origin=$origen" +
                        "&destination=$dest" +
                        "&mode=driving" +
                        "&key=$DIRECTIONS_API_KEY"

                Log.d(TAG, "Request URL: $urlString") // 🟢 Log importante

                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString = connection.inputStream.bufferedReader().use { it.readText() }

                    Log.d(TAG, "API Response: $responseString") // 🟢 Log clave para saber qué responde la API

                    val jsonResponse = JSONObject(responseString)
                    val status = jsonResponse.getString("status")
                    Log.d(TAG, "API status: $status") // 🟢 Log para saber si es OK o error

                    if (status == "OK") {
                        val routes = jsonResponse.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val overviewPolyline = route.getJSONObject("overview_polyline")
                            val encodedPolyline = overviewPolyline.getString("points")

                            val puntos = decodificarPolyline(encodedPolyline)
                            Log.d(TAG, "Polyline decodificado: ${puntos.size} puntos") // 🟢 Verificar si hay puntos

                            val legs = route.getJSONArray("legs")
                            var distanciaTotal = 0.0
                            var duracionTotal = 0.0

                            for (i in 0 until legs.length()) {
                                val leg = legs.getJSONObject(i)
                                distanciaTotal += leg.getJSONObject("distance").getDouble("value")
                                duracionTotal += leg.getJSONObject("duration").getDouble("value")
                            }

                            val distanciaKm = distanciaTotal / 1000.0
                            val duracionMin = duracionTotal / 60.0

                            Log.d(TAG, "Ruta OK. Distancia: $distanciaKm km, Duración: $duracionMin min")

                            return@withContext RutaCalculada(puntos, distanciaKm, duracionMin)
                        } else {
                            Log.e(TAG, "No se encontraron rutas en la respuesta JSON.")
                        }
                    } else {
                        Log.e(TAG, "La API devolvió un status inválido: $status")
                    }
                } else {
                    Log.e(TAG, "HTTP error code: $responseCode")
                }

                null
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener ruta desde API", e)
                null
            }
        }


    private fun decodificarPolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }


    private fun ajustarCamaraARuta(puntos: List<LatLng>) {
        if (puntos.isEmpty()) return

        val boundsBuilder = LatLngBounds.builder()
        puntos.forEach { boundsBuilder.include(it) }

        try {
            val bounds = boundsBuilder.build()
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Error al crear bounds para la cámara: ${e.message}")
        }
    }

    private fun mostrarDialogoGuardarRuta(ruta: Ruta) {
        // Verificar si el usuario ha iniciado sesión
        if (!isUserLoggedIn) {
            mostrarDialogoIniciarSesion()
            return
        }

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Guardar ruta")

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val inputNombre = EditText(requireContext()).apply {
            hint = "Nombre de la ruta"
            setText(ruta.nombre)
        }

        val tiposRuta = arrayOf("driving", "walking", "bicycling")
        val tipoActual = tiposRuta.indexOf(ruta.tipoRuta).takeIf { it >= 0 } ?: 0

        layout.addView(inputNombre)

        builder.setView(layout)

        builder.setPositiveButton("Guardar") { _, _ ->
            val nombre = inputNombre.text.toString()

            if (nombre.isNotEmpty()) {
                guardarRuta(ruta.copy(nombre = nombre))
            } else {
                mostrarMensaje("Por favor, introduce un nombre para la ruta.")
            }
        }

        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun guardarRuta(ruta: Ruta) {
        val currentUser = auth.currentUser
        val currentUID = currentUser?.uid

        if (currentUID == null) {
            mostrarMensaje("Debes iniciar sesión para guardar rutas.")
            mostrarDialogoIniciarSesion()
            return
        }

        // Mostrar diálogo de carga
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setTitle("Guardando ruta")
            .setMessage("Guardando la ruta en la base de datos...")
            .setCancelable(false)
            .create()

        loadingDialog.show()

        val rutaMap = hashMapOf(
            "nombre" to ruta.nombre,
            "distanciaKm" to ruta.distanciaKm,
            "duracionMin" to ruta.duracionMin,
            "tipoRuta" to ruta.tipoRuta,
            "fechaCreacion" to com.google.firebase.Timestamp.now(),
            "usuarioCreador" to currentUID,
            "puntosIds" to listOf(
                puntoInicio?.id ?: "",
                puntoDestino?.id ?: ""
            )
        )

        db.collection("rutas")
            .add(rutaMap)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                mostrarMensaje("Ruta guardada correctamente.")
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                mostrarMensaje("Error al guardar la ruta: $e")
            }
    }

    // Método para mostrar diálogo de inicio de sesión
    private fun mostrarDialogoIniciarSesion() {
        AlertDialog.Builder(requireContext())
            .setTitle("Inicio de sesión requerido")
            .setMessage("Debes iniciar sesión para utilizar esta función. ¿Deseas ir a la pantalla de inicio de sesión?")
            .setPositiveButton("Ir a inicio de sesión") { _, _ ->
                // Cambiar al fragment de perfil para iniciar sesión
                val fragmentManager = requireActivity().supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(R.id.fragmentContainer, PerfilFragment())
                transaction.addToBackStack(null)
                transaction.commit()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun limpiarMapa() {
        // Limpiar todos los elementos del mapa
        googleMap.clear()
        marcadores.clear()
        polylines.clear()

        // Resetear puntos seleccionados
        puntoInicio = null
        puntoDestino = null
    }

    private fun limpiarMarcadoresNoSeleccionados() {
        // Conservar puntos seleccionados
        val marcadoresSeleccionados = marcadores.filter { marker ->
            val punto = marker.tag as? Punto
            punto == puntoInicio || punto == puntoDestino
        }

        // Limpiar el mapa y volver a agregar solo los marcadores seleccionados
        googleMap.clear()
        polylines.clear()
        marcadores.clear()

        marcadoresSeleccionados.forEach { marker ->
            val punto = marker.tag as Punto
            val latLng = LatLng(punto.latitud, punto.longitud)
            val nuevoMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(marker.title)
                    .snippet(marker.snippet)
                    .icon(
                        when (punto) {
                            puntoInicio -> BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_GREEN
                            )

                            puntoDestino -> BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_RED
                            )

                            else -> BitmapDescriptorFactory.defaultMarker()
                        }
                    )
            )

            if (nuevoMarker != null) {
                nuevoMarker.tag = punto
                marcadores.add(nuevoMarker)
            }
        }
    }

    private fun formatearDistancia(distanciaKm: Double): String {
        return DecimalFormat("0.00").format(distanciaKm)
    }

    private fun formatearDuracion(duracionMin: Double): String {
        val horas = (duracionMin / 60).toInt()
        val minutos = (duracionMin % 60).roundToInt()

        return if (horas > 0) {
            "$horas h $minutos min"
        } else {
            "$minutos min"
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
        // Eliminar listener de autenticación
        AuthManager.removeAuthStateListener(authStateListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        coroutineScope.cancel()
        if (::googleMap.isInitialized) googleMap.clear()
    }

    // Método para verificar y actualizar el estado de autenticación
    fun actualizarEstadoAutenticacion() {
        isUserLoggedIn = FirebaseAuth.getInstance().currentUser != null
    }
}
