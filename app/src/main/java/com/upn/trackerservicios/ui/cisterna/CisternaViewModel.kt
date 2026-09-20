package com.upn.trackerservicios.ui.cisterna

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Parada(
    val id: String = "",
    val orden: Int = 0,
    val referencia: String = "",
    val completada: Boolean = false,
    val esEmergencia: Boolean = false, // Etiqueta de emergencia
    val latitud: Double = 0.0,         // Coordenadas para navegación
    val longitud: Double = 0.0
)

class CisternaViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _paradas = MutableStateFlow<List<Parada>>(emptyList())
    val paradas = _paradas.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando = _cargando.asStateFlow()

    init {
        cargarRuta()
    }

    fun cargarRuta() {
        val uidChofer = auth.currentUser?.uid ?: return
        _cargando.value = true

        firestore.collection("usuarios").document(uidChofer)
            .collection("rutas")
            .orderBy("orden", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _cargando.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lista = snapshot.documents.map { doc ->
                        Parada(
                            id = doc.id,
                            orden = doc.getLong("orden")?.toInt() ?: 0,
                            referencia = doc.getString("referencia") ?: "",
                            completada = doc.getBoolean("completada") ?: false,
                            esEmergencia = doc.getBoolean("esEmergencia") ?: false,
                            latitud = doc.getDouble("latitud") ?: 0.0,
                            longitud = doc.getDouble("longitud") ?: 0.0
                        )
                    }
                    _paradas.value = lista

                    if (lista.isEmpty()) {
                        generarRutaDePrueba(uidChofer)
                    }
                }
                _cargando.value = false
            }
    }

    fun marcarParadaCompletada(idParada: String, completada: Boolean) {
        val uidChofer = auth.currentUser?.uid ?: return
        firestore.collection("usuarios").document(uidChofer)
            .collection("rutas").document(idParada)
            .update("completada", completada)
    }

    private fun generarRutaDePrueba(uid: String) {
        val rutaPrueba = listOf(
            mapOf("orden" to 1, "referencia" to "Jr. Los Conquistadores 450", "completada" to false, "esEmergencia" to false, "latitud" to -7.16378, "longitud" to -78.50027),
            mapOf("orden" to 2, "referencia" to "Av. Los Héroes 102 - Grifo", "completada" to false, "esEmergencia" to true, "latitud" to -7.16500, "longitud" to -78.50200), // ESTA ES EMERGENCIA
            mapOf("orden" to 3, "referencia" to "Urb. El Ingenio Mz. B Lt. 4", "completada" to false, "esEmergencia" to false, "latitud" to -7.16100, "longitud" to -78.49800),
            mapOf("orden" to 4, "referencia" to "Condominio Las Rosas", "completada" to false, "esEmergencia" to true, "latitud" to -7.16800, "longitud" to -78.49500) // ESTA ES EMERGENCIA
        )

        val batch = firestore.batch()
        rutaPrueba.forEach { datos ->
            val docRef = firestore.collection("usuarios").document(uid).collection("rutas").document()
            batch.set(docRef, datos)
        }
        batch.commit()
    }
}