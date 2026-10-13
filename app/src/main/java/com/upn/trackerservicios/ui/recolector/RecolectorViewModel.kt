package com.upn.trackerservicios.ui.recolector

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PuntoRecoleccion(
    val id: String = "",
    val orden: Int = 0,
    val direccion: String = "",
    val zona: String = "",
    val completada: Boolean = false,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0
)

class RecolectorViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private val _puntos = MutableStateFlow<List<PuntoRecoleccion>>(emptyList())
    val puntos = _puntos.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando = _cargando.asStateFlow()

    init {
        cargarRuta()
    }

    fun cargarRuta() {
        _cargando.value = true
        listenerRegistration?.remove()

        listenerRegistration = firestore.collection("rutas_recoleccion")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _cargando.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val docsOrdenados = snapshot.documents.sortedBy {
                        it.getBoolean("completada") ?: false
                    }

                    val lista = docsOrdenados.mapIndexed { index, doc ->
                        PuntoRecoleccion(
                            id = doc.id,
                            orden = index + 1,
                            direccion = doc.getString("direccion") ?: "Dirección en mapa",
                            zona = doc.getString("zona") ?: "Sin referencia",
                            completada = doc.getBoolean("completada") ?: false,
                            latitud = doc.getDouble("latitud") ?: 0.0,
                            longitud = doc.getDouble("longitud") ?: 0.0
                        )
                    }
                    _puntos.value = lista
                }
                _cargando.value = false
            }
    }

    fun marcarPuntoCompletado(idPunto: String, completada: Boolean) {
        firestore.collection("rutas_recoleccion").document(idPunto)
            .update("completada", completada)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}