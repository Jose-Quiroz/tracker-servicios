package com.upn.trackerservicios.ui.cisterna

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

data class Parada(
    val id: String = "",
    val orden: Int = 0,
    val direccion: String = "",
    val referencia: String = "",
    val completada: Boolean = false,
    val esEmergencia: Boolean = false,
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    val tieneEvidenciaFoto: Boolean = false,
    val pesoFotoKb: Int = 0,
)

class CisternaViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private val _paradas = MutableStateFlow<List<Parada>>(emptyList())
    val paradas = _paradas.asStateFlow()

    private val _cargando = MutableStateFlow(true)
    val cargando = _cargando.asStateFlow()

    init {
        cargarRuta()
    }

    fun cargarRuta() {
        _cargando.value = true
        listenerRegistration?.remove()

        listenerRegistration = firestore.collection("rutas_cisterna")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _cargando.value = false
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val docsOrdenados = snapshot.documents.sortedWith(
                        compareBy<com.google.firebase.firestore.DocumentSnapshot> {
                            it.getBoolean("completada") ?: false
                        }.thenByDescending {
                            it.getBoolean("esEmergencia") ?: false
                        }
                    )

                    val lista = docsOrdenados.mapIndexed { index, doc ->
                        Parada(
                            id = doc.id,
                            orden = index + 1,
                            direccion = doc.getString("direccion") ?: "Dirección en mapa",
                            referencia = doc.getString("referencia") ?: "Sin referencia",
                            completada = doc.getBoolean("completada") ?: false,
                            esEmergencia = doc.getBoolean("esEmergencia") ?: false,
                            latitud = doc.getDouble("latitud") ?: 0.0,
                            longitud = doc.getDouble("longitud") ?: 0.0,
                            tieneEvidenciaFoto = !doc.getString("evidenciaFotoJpeg").isNullOrBlank(),
                            pesoFotoKb = doc.getLong("pesoFotoKb")?.toInt() ?: 0
                        )
                    }
                    _paradas.value = lista
                }
                _cargando.value = false
            }
    }

    // RF-13: Comprimir foto a JPEG <= 1 MB, subir al backend y marcar la parada como atendida
    fun subirFotoYCompletarParada(
        idParada: String,
        bitmapOriginal: Bitmap,
        onResultado: (Boolean, String) -> Unit
    ) {
        try {
            // 1. Redimensionar de forma segura manteniendo proporción para asegurar velocidad y peso <= 1MB
            val maxLado = 1024
            val ratio = minOf(
                maxLado.toFloat() / bitmapOriginal.width,
                maxLado.toFloat() / bitmapOriginal.height,
                1.0f
            )
            val anchoFinal = (bitmapOriginal.width * ratio).toInt()
            val altoFinal = (bitmapOriginal.height * ratio).toInt()
            val bitmapEscala = Bitmap.createScaledBitmap(bitmapOriginal, anchoFinal, altoFinal, true)

            // 2. Comprimir en formato JPEG garantizando estrictamente <= 1 MB (1,048,576 bytes)
            var calidad = 85
            var bytesJpeg: ByteArray
            do {
                val outputStream = ByteArrayOutputStream()
                bitmapEscala.compress(Bitmap.CompressFormat.JPEG, calidad, outputStream)
                bytesJpeg = outputStream.toByteArray()
                calidad -= 10
            } while (bytesJpeg.size > 1_048_576 && calidad > 20)

            val pesoKb = (bytesJpeg.size / 1024).coerceAtLeast(1)
            val jpegBase64 = "data:image/jpeg;base64," + Base64.encodeToString(bytesJpeg, Base64.NO_WRAP)
            val datosActualizacion = mapOf(
                "completada" to true,
                "evidenciaFotoJpeg" to jpegBase64,
                "pesoFotoKb" to pesoKb,
                "fechaEntrega" to FieldValue.serverTimestamp()
            )

            firestore.collection("rutas_cisterna").document(idParada)
                .update(datosActualizacion)
                .addOnSuccessListener {
                    onResultado(true, "Evidencia JPEG subida ($pesoKb KB ≤ 1 MB). ¡Parada atendida!")
                }
                .addOnFailureListener { e ->
                    onResultado(false, "Error al subir evidencia: ${e.message}")
                }
        } catch (e: Exception) {
            onResultado(false, "Error al comprimir imagen: ${e.message}")
        }
    }

    fun marcarParadaCompletada(idParada: String, completada: Boolean) {
        firestore.collection("rutas_cisterna").document(idParada)
            .update("completada", completada)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}