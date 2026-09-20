package com.upn.trackerservicios.ui.recolector

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PuntoRecoleccion(
    val id: String = "",
    val orden: Int = 0,
    val zona: String = "",
    val completada: Boolean = false
)

class RecolectorViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _puntos = MutableStateFlow<List<PuntoRecoleccion>>(emptyList())
    val puntos = _puntos.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando = _cargando.asStateFlow()

    init {
        cargarRuta()
    }

    fun cargarRuta() {
        val uidChofer = auth.currentUser?.uid ?: return
        _cargando.value = true

        firestore.collection("usuarios").document(uidChofer)
            .collection("ruta_recoleccion")
            .orderBy("orden", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _cargando.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val lista = snapshot.documents.map { doc ->
                        PuntoRecoleccion(
                            id = doc.id,
                            orden = doc.getLong("orden")?.toInt() ?: 0,
                            zona = doc.getString("zona") ?: "",
                            completada = doc.getBoolean("completada") ?: false
                        )
                    }
                    _puntos.value = lista

                    // Si está vacío, genera datos de prueba
                    if (lista.isEmpty()) {
                        generarRutaDePrueba(uidChofer)
                    }
                }
                _cargando.value = false
            }
    }

    fun marcarPuntoCompletado(idPunto: String, completada: Boolean) {
        val uidChofer = auth.currentUser?.uid ?: return
        firestore.collection("usuarios").document(uidChofer)
            .collection("ruta_recoleccion").document(idPunto)
            .update("completada", completada)
    }

    private fun generarRutaDePrueba(uid: String) {
        val rutaPrueba = listOf(
            mapOf("orden" to 1, "zona" to "Av. Vía de Evitamiento - Cuadras 1 a 5", "completada" to false),
            mapOf("orden" to 2, "zona" to "Mercado Central - Contenedores Sur", "completada" to false),
            mapOf("orden" to 3, "zona" to "Urb. Ramón Castilla - Parque Principal", "completada" to false),
            mapOf("orden" to 4, "zona" to "Jr. Cruz de Piedra - Intersección Amalia Puga", "completada" to false)
        )

        val batch = firestore.batch()
        rutaPrueba.forEach { datos ->
            val docRef = firestore.collection("usuarios").document(uid).collection("ruta_recoleccion").document()
            batch.set(docRef, datos)
        }
        batch.commit()
    }
}