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
import com.google.firebase.auth.FirebaseUser

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Inicializar vistas
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
        layoutAchievements = view.findViewById(R.id.layoutAchievements)
        progressBar1 = view.findViewById(R.id.progressBar1)
        progressBar2 = view.findViewById(R.id.progressBar2)
        profileCardView = view.findViewById(R.id.cardViewProfile)

        // Botón de logros
        buttonAchievements.setOnClickListener {
            mostrarLogros()
        }

        // Botón login
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            loginUser(email, password)
        }

        // Botón registro
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            registerUser(email, password)
        }

        // Botón logout
        logoutButton.setOnClickListener {
            auth.signOut()
            mostrarMensaje("Sesión cerrada correctamente")
            actualizarVista()
        }

        actualizarVista()
    }

    private fun mostrarLogros() {
        // Cambia visibilidad entre perfil y logros
        layoutAchievements.visibility = View.VISIBLE
        profileCardView.visibility = View.GONE

        val user = auth.currentUser
        if (user != null) {
            val puntos = obtenerPuntosDeUbicacion(user)
            val rutas = obtenerRutasCreadas(user)

            progressBar1.max = 10
            progressBar2.max = 3

            progressBar1.progress = puntos
            progressBar2.progress = rutas
        }
    }

    private fun obtenerPuntosDeUbicacion(user: FirebaseUser): Int {
        // Aquí puedes conectar con Firestore o base de datos
        return 5 // Simulación
    }

    private fun obtenerRutasCreadas(user: FirebaseUser): Int {
        // Aquí puedes conectar con Firestore o base de datos
        return 2 // Simulación
    }

    private fun actualizarVista() {
        val user = auth.currentUser
        if (user != null) {
            statusTextView.text = "Bienvenido, ${user.email}"

            emailInputLayout.visibility = View.GONE
            passwordInputLayout.visibility = View.GONE
            loginButton.visibility = View.GONE
            registerButton.visibility = View.GONE
            logoutButton.visibility = View.VISIBLE
            buttonAchievements.visibility = View.VISIBLE
            profileCardView.visibility = View.VISIBLE
            layoutAchievements.visibility = View.GONE
        } else {
            statusTextView.text = "Inicia sesión para continuar"

            emailInputLayout.visibility = View.VISIBLE
            passwordInputLayout.visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
            registerButton.visibility = View.VISIBLE
            logoutButton.visibility = View.GONE
            buttonAchievements.visibility = View.GONE
            profileCardView.visibility = View.VISIBLE
            layoutAchievements.visibility = View.GONE
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
}