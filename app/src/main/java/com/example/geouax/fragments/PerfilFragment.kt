package com.example.geouax.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.geouax.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import android.widget.ImageView
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class PerfilFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var registerButton: MaterialButton
    private lateinit var logoutButton: MaterialButton
    private lateinit var statusTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var buttonAchievements: MaterialButton
    private lateinit var layoutAchievements: LinearLayout
    private lateinit var progressBar1: ProgressBar
    private lateinit var progressBar2: ProgressBar
    private lateinit var profileCardView: CardView
    private lateinit var db: FirebaseFirestore
    private lateinit var tickPuntos: TextView
    private lateinit var tickRutas: TextView
    private lateinit var layoutBarraRutaLarga: LinearLayout
    private lateinit var progressBar3: ProgressBar
    private lateinit var tickRutaLarga: TextView

    // Listener para cuando cambia el estado de autenticación
    private var authStateListener: ((Boolean) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        emailEditText = view.findViewById(R.id.editTextEmail)
        passwordEditText = view.findViewById(R.id.editTextPassword)
        loginButton = view.findViewById(R.id.buttonLogin)
        registerButton = view.findViewById(R.id.buttonRegister)
        logoutButton = view.findViewById(R.id.buttonLogout)
        statusTextView = view.findViewById(R.id.textViewStatus)
        profileImageView = view.findViewById(R.id.imageViewProfile)
        emailInputLayout = view.findViewById(R.id.textInputLayoutEmail)
        passwordInputLayout = view.findViewById(R.id.textInputLayoutPassword)
        buttonAchievements = view.findViewById(R.id.buttonAchievements)
        // Inicializar layoutAchievements - Este era el error principal
        layoutAchievements = view.findViewById(R.id.layoutLogros)
        progressBar1 = view.findViewById(R.id.progressBar1)
        progressBar2 = view.findViewById(R.id.progressBar2)
        profileCardView = view.findViewById(R.id.cardViewProfile)
        db = FirebaseFirestore.getInstance()
        tickPuntos = view.findViewById(R.id.tickPuntos)
        tickRutas = view.findViewById(R.id.tickRutas)
        layoutBarraRutaLarga = view.findViewById(R.id.layoutBarraRutaLarga)
        progressBar3 = view.findViewById(R.id.progressBar3)
        tickRutaLarga = view.findViewById(R.id.tickRutaLarga)

        buttonAchievements.setOnClickListener {
            mostrarLogros()
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            loginUser(email, password)
        }

        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            registerUser(email, password)
        }

        logoutButton.setOnClickListener {
            auth.signOut()
            mostrarMensaje("Sesión cerrada correctamente")
            actualizarVista()
            // Notificar al listener que el usuario cerró sesión
            authStateListener?.invoke(false)
        }

        actualizarVista()
    }

    private fun obtenerRutaLarga(user: FirebaseUser, callback: (Boolean) -> Unit) {
        db.collection("rutas")
            .whereEqualTo("usuarioCreador", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                val tieneRutaLarga = documents.any {
                    val distancia = it.getDouble("distanciaKm") ?: 0.0
                    distancia > 10.0
                }
                callback(tieneRutaLarga)
            }
            .addOnFailureListener {
                mostrarError("Error al verificar rutas largas")
                callback(false)
            }
    }

    private fun mostrarLogros() {
        // Verificar si layoutAchievements está inicializado
        if (!::layoutAchievements.isInitialized) {
            mostrarError("Error: No se pudo mostrar logros")
            return
        }

        layoutAchievements.visibility = View.VISIBLE
        profileCardView.visibility = View.GONE

        val user = auth.currentUser
        if (user != null) {
            obtenerPuntosDeUbicacion(user) { puntos ->
                progressBar1.max = 10
                progressBar1.progress = puntos
                tickPuntos.text = if (puntos >= 10) "✅" else ""
            }

            obtenerRutasCreadas(user) { rutas ->
                progressBar2.max = 3
                progressBar2.progress = rutas
                tickRutas.text = if (rutas >= 3) "✅" else ""
            }
            obtenerRutaLarga(user) { tieneRutaLarga ->
                progressBar3.max = 1
                progressBar3.progress = if (tieneRutaLarga) 1 else 0
                tickRutaLarga.text = if (tieneRutaLarga) "✅" else ""
            }
        }
    }

    private fun obtenerPuntosDeUbicacion(user: FirebaseUser, callback: (Int) -> Unit) {
        db.collection("puntos_localizacion")
            .whereEqualTo("usuarioCreador", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.size())
            }
            .addOnFailureListener {
                mostrarError("Error al obtener puntos de localización")
                callback(0)
            }
    }

    private fun obtenerRutasCreadas(user: FirebaseUser, callback: (Int) -> Unit) {
        db.collection("rutas")
            .whereEqualTo("usuarioCreador", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                callback(documents.size())
            }
            .addOnFailureListener {
                mostrarError("Error al obtener rutas")
                callback(0)
            }
    }

    private fun actualizarVista() {
        val user = auth.currentUser
        if (user != null) {
            statusTextView.text = "Bienvenido, ${user.email}"

            // Cargar avatar si está disponible
            val photoUrl = user.photoUrl
            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .circleCrop()
                    .into(profileImageView)
            }

            emailInputLayout.visibility = View.GONE
            passwordInputLayout.visibility = View.GONE
            loginButton.visibility = View.GONE
            registerButton.visibility = View.GONE
            logoutButton.visibility = View.VISIBLE
            buttonAchievements.visibility = View.VISIBLE
            profileCardView.visibility = View.VISIBLE

            // Verificar si layoutAchievements está inicializado antes de usarlo
            if (::layoutAchievements.isInitialized) {
                layoutAchievements.visibility = View.GONE
            }

            // Notificar al listener que el usuario inició sesión
            authStateListener?.invoke(true)
        } else {
            statusTextView.text = "Inicia sesión para continuar"

            emailInputLayout.visibility = View.VISIBLE
            passwordInputLayout.visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
            registerButton.visibility = View.VISIBLE
            logoutButton.visibility = View.GONE
            buttonAchievements.visibility = View.GONE
            profileCardView.visibility = View.VISIBLE

            // Verificar si layoutAchievements está inicializado antes de usarlo
            if (::layoutAchievements.isInitialized) {
                layoutAchievements.visibility = View.GONE
            }

            // Notificar al listener que el usuario cerró sesión
            authStateListener?.invoke(false)
        }
    }

    private fun loginUser(email: String, password: String) {
        if (validarCampos(email, password)) {
            mostrarCargando(true)
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    mostrarCargando(false)
                    if (task.isSuccessful) {
                        mostrarMensaje("¡Inicio de sesión exitoso!")
                        actualizarVista()
                    } else {
                        mostrarError("Error al iniciar sesión: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun registerUser(email: String, password: String) {
        if (validarCampos(email, password)) {
            mostrarCargando(true)
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    mostrarCargando(false)
                    if (task.isSuccessful) {
                        mostrarMensaje("Usuario registrado correctamente")
                        actualizarVista()
                    } else {
                        mostrarError("Error al registrar: ${task.exception?.message}")
                    }
                }
        }
    }

    private fun validarCampos(email: String, password: String): Boolean {
        var esValido = true
        if (email.isEmpty()) {
            emailInputLayout.error = "Por favor, ingrese su correo electrónico"
            esValido = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "Correo no válido"
            esValido = false
        } else {
            emailInputLayout.error = null
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Por favor, ingrese su contraseña"
            esValido = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "Mínimo 6 caracteres"
            esValido = false
        } else {
            passwordInputLayout.error = null
        }

        return esValido
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarError(error: String) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
    }

    private fun mostrarCargando(mostrar: Boolean) {
        loginButton.isEnabled = !mostrar
        registerButton.isEnabled = !mostrar
        logoutButton.isEnabled = !mostrar
    }

    // Métodos para la integración con MapaFragment

    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    // Método para establecer un listener para cambios en el estado de autenticación
    fun setAuthStateListener(listener: (Boolean) -> Unit) {
        this.authStateListener = listener
        // Notificar el estado actual
        listener(isUserLoggedIn())
    }

    // Método para obtener la instancia de FirebaseAuth
    fun getAuthInstance(): FirebaseAuth {
        return auth
    }
}