package com.upn.trackerservicios.ui.auth

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Cargando : AuthState()
    data class Exito(val rol: String) : AuthState()
    data class Error(val mensaje: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState = _uiState.asStateFlow()

    fun iniciarSesion(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthState.Error("Por favor completa todos los campos")
            return
        }

        _uiState.value = AuthState.Cargando

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid == null) {
                    _uiState.value = AuthState.Error("Error al obtener ID de usuario")
                    return@addOnSuccessListener
                }

                firestore.collection("usuarios").document(uid).get()
                    .addOnSuccessListener { document ->
                        val rol = document.getString("rol") ?: "VECINO"
                        _uiState.value = AuthState.Exito(rol)
                    }
                    .addOnFailureListener {
                        _uiState.value = AuthState.Error("Error al consultar perfil")
                    }
            }
            .addOnFailureListener { error ->
                val mensaje = when (error) {
                    is FirebaseAuthInvalidUserException -> "El correo no está registrado"
                    is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
                    else -> "Error de autenticación. Verifica tus datos."
                }
                _uiState.value = AuthState.Error(mensaje)
            }
    }

    fun registrar(email: String, password: String) {
        _uiState.value = AuthState.Cargando
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { _uiState.value = AuthState.Exito("VECINO") }
            .addOnFailureListener { _uiState.value = AuthState.Error(it.message ?: "Error desconocido") }
    }

    fun cerrarSesion() {
        auth.signOut()
        _uiState.value = AuthState.Idle
    }

    fun hayUsuarioActivo(): Boolean = auth.currentUser != null

    fun verificarSesion() {
        val usuario = auth.currentUser
        if (usuario != null) {
            _uiState.value = AuthState.Cargando

            firestore.collection("usuarios").document(usuario.uid).get()
                .addOnSuccessListener { document ->
                    val rol = document.getString("rol") ?: "VECINO"
                    _uiState.value = AuthState.Exito(rol)
                }
                .addOnFailureListener {
                    _uiState.value = AuthState.Idle
                }
        }
    }
}