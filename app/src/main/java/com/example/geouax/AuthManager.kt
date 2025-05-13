package com.example.geouax

import android.content.Context
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Clase para centralizar la gestión de autenticación en la aplicación
 */
object AuthManager {
    private val auth = FirebaseAuth.getInstance()

    // Lista de listeners para cambios en el estado de autenticación
    private val authStateListeners = mutableListOf<(Boolean) -> Unit>()

    init {
        // Configurar listener global para cambios en autenticación
        auth.addAuthStateListener { firebaseAuth ->
            val isLoggedIn = firebaseAuth.currentUser != null
            // Notificar a todos los listeners registrados
            authStateListeners.forEach { it.invoke(isLoggedIn) }
        }
    }

    /**
     * Verifica si hay un usuario autenticado actualmente
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Obtiene el ID del usuario actual, o null si no hay usuario autenticado
     */
    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Obtiene el email del usuario actual, o null si no hay usuario autenticado
     */
    fun getCurrentUserEmail(): String? {
        return auth.currentUser?.email
    }

    /**
     * Obtiene el objeto FirebaseUser actual, o null si no hay usuario autenticado
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Inicia sesión con email y contraseña
     *
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param onSuccess Callback llamado cuando el inicio de sesión es exitoso
     * @param onFailure Callback llamado cuando ocurre un error, con el mensaje de error
     */
    fun loginUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Error desconocido al iniciar sesión")
            }
    }

    /**
     * Registra un nuevo usuario con email y contraseña
     *
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param onSuccess Callback llamado cuando el registro es exitoso
     * @param onFailure Callback llamado cuando ocurre un error, con el mensaje de error
     */
    fun registerUser(email: String, password: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onFailure(exception.message ?: "Error desconocido al registrar usuario")
            }
    }

    /**
     * Cierra la sesión del usuario actual
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Registra un listener para cambios en el estado de autenticación
     *
     * @param listener Función que será llamada cuando cambie el estado de autenticación
     */
    fun addAuthStateListener(listener: (Boolean) -> Unit) {
        authStateListeners.add(listener)
        // Notificar inmediatamente el estado actual
        listener(isUserLoggedIn())
    }

    /**
     * Elimina un listener previamente registrado
     *
     * @param listener Listener a eliminar
     */
    fun removeAuthStateListener(listener: (Boolean) -> Unit) {
        authStateListeners.remove(listener)
    }

    /**
     * Muestra un diálogo de error de autenticación
     *
     * @param context Contexto para mostrar el mensaje
     * @param message Mensaje de error a mostrar
     */
    fun showAuthError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}