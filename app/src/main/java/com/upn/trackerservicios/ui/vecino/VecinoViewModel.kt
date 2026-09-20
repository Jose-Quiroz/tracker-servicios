package com.upn.trackerservicios.ui.vecino

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Domicilio(
    val latitud: Double,
    val longitud: Double,
    val referencia: String
)

sealed class DomicilioState {
    object Cargando : DomicilioState()
    object SinDomicilio : DomicilioState()
    data class Registrado(val domicilio: Domicilio) : DomicilioState()
    data class Error(val mensaje: String) : DomicilioState()
}

class VecinoViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _estado = MutableStateFlow<DomicilioState>(DomicilioState.Cargando)
    val estado = _estado.asStateFlow()

    // READ: Consultar domicilio al iniciar
    fun cargarDomicilio() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _estado.value = DomicilioState.Error("Sesión no válida")
            return
        }

        _estado.value = DomicilioState.Cargando

        firestore.collection("usuarios").document(uid).get()
            .addOnSuccessListener { doc ->
                val lat = doc.getDouble("latitud")
                val lng = doc.getDouble("longitud")
                val ref = doc.getString("referencia") ?: ""

                if (lat != null && lng != null) {
                    _estado.value = DomicilioState.Registrado(
                        Domicilio(latitud = lat, longitud = lng, referencia = ref)
                    )
                } else {
                    _estado.value = DomicilioState.SinDomicilio
                }
            }
            .addOnFailureListener { e ->
                _estado.value = DomicilioState.Error(e.message ?: "Error al cargar domicilio")
            }
    }

    // Guardar o actualizar coordenada y referencia
    fun guardarDomicilio(punto: Point, referencia: String, onExito: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        val datos = hashMapOf(
            "latitud" to punto.latitude(),
            "longitud" to punto.longitude(),
            "referencia" to referencia,
            "domicilioRegistrado" to true
        )

        firestore.collection("usuarios").document(uid)
            .set(datos, SetOptions.merge())
            .addOnSuccessListener {
                _estado.value = DomicilioState.Registrado(
                    Domicilio(
                        latitud = punto.latitude(),
                        longitud = punto.longitude(),
                        referencia = referencia
                    )
                )
                onExito()
            }
            .addOnFailureListener { e ->
                _estado.value = DomicilioState.Error(e.message ?: "Error al guardar")
            }
    }

    // DELETE: Desvincular domicilio
    fun eliminarDomicilio(onExito: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "latitud" to FieldValue.delete(),
            "longitud" to FieldValue.delete(),
            "referencia" to FieldValue.delete(),
            "domicilioRegistrado" to false
        )

        firestore.collection("usuarios").document(uid).update(updates)
            .addOnSuccessListener {
                _estado.value = DomicilioState.SinDomicilio
                onExito()
            }
            .addOnFailureListener { e ->
                _estado.value = DomicilioState.Error(e.message ?: "Error al eliminar")
            }
    }
}