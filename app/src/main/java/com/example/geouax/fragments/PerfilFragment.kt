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
import androidx.core.content.ContextCompat

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

        // Aplicar animaciones
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in)
        profileImageView.startAnimation(fadeIn)

        // Configurar listeners
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
        }

        // Actualizar UI según estado de autenticación
        actualizarVista()
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
            emailInputLayout.error = "Por favor, ingrese un correo electrónico válido"
            esValido = false
        } else {
            emailInputLayout.error = null
        }

        if (password.isEmpty()) {
            passwordInputLayout.error = "Por favor, ingrese su contraseña"
            esValido = false
        } else if (password.length < 6) {
            passwordInputLayout.error = "La contraseña debe tener al menos 6 caracteres"
            esValido = false
        } else {
            passwordInputLayout.error = null
        }

        return esValido
    }

    private fun actualizarVista() {
        val user = auth.currentUser

        if (user != null) {
            // Usuario autenticado
            statusTextView.text = "Bienvenido, ${user.email}"

            // Configurar visibilidad
            emailInputLayout.visibility = View.GONE
            passwordInputLayout.visibility = View.GONE
            loginButton.visibility = View.GONE
            registerButton.visibility = View.GONE
            logoutButton.visibility = View.VISIBLE

            // Animación para el avatar
            val rotateAnimation = AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left)
            profileImageView.startAnimation(rotateAnimation)
        } else {
            // Usuario no autenticado
            statusTextView.text = "Inicia sesión para continuar"

            // Configurar visibilidad
            emailInputLayout.visibility = View.VISIBLE
            passwordInputLayout.visibility = View.VISIBLE
            loginButton.visibility = View.VISIBLE
            registerButton.visibility = View.VISIBLE
            logoutButton.visibility = View.GONE

            // Limpiar campos
            emailEditText.text?.clear()
            passwordEditText.text?.clear()
            emailInputLayout.error = null
            passwordInputLayout.error = null
        }
    }

    private fun mostrarMensaje(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_SHORT).show()
    }

    private fun mostrarError(mensaje: String) {
        Toast.makeText(requireContext(), mensaje, Toast.LENGTH_LONG).show()
    }

    private fun mostrarCargando(mostrar: Boolean) {
        if (mostrar) {
            loginButton.isEnabled = false
            registerButton.isEnabled = false
            // Aquí podrías mostrar un ProgressBar si lo añades al layout
        } else {
            loginButton.isEnabled = true
            registerButton.isEnabled = true
            // Aquí podrías ocultar el ProgressBar
        }
    }
}