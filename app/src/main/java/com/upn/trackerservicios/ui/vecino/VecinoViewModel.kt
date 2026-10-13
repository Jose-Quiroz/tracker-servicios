package com.upn.trackerservicios.ui.vecino

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

data class Domicilio(
    val latitud: Double,
    val longitud: Double,
    val direccion: String,
    val referencia: String,
    val radioGeocerca: Int = 300 // RF-09: Por defecto 300m (100, 300 o 500)
)

data class VehiculoTelemetria(
    val id: String,
    val tipo: String, // "AGUA" o "BASURA"
    val latitud: Double,
    val longitud: Double,
    val rumbo: Float
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
    private var vehiculosListener: ListenerRegistration? = null

    private val _estado = MutableStateFlow<DomicilioState>(DomicilioState.Cargando)
    val estado = _estado.asStateFlow()

    // RF-05 y RF-06: Lista de vehículos activos del servicio seleccionado
    private val _vehiculosActivos = MutableStateFlow<List<VehiculoTelemetria>>(emptyList())
    val vehiculosActivos = _vehiculosActivos.asStateFlow()

    // RF-10: Evento para disparar notificación cuando un camión entra al radio de geocerca
    private val _alertaGeocerca = MutableStateFlow<String?>(null)
    val alertaGeocerca = _alertaGeocerca.asStateFlow()

    private var ultimaFechaAlertaServidor: String = ""

    fun limpiarAlertaGeocerca() {
        _alertaGeocerca.value = null
    }

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
                val dir = doc.getString("direccion") ?: "Calle sin nombre detectado"
                val ref = doc.getString("referencia") ?: ""
                val radio = doc.getLong("radioGeocerca")?.toInt() ?: 300
                ultimaFechaAlertaServidor = doc.getString("ultimaAlertaFecha") ?: ""

                if (lat != null && lng != null) {
                    _estado.value = DomicilioState.Registrado(
                        Domicilio(
                            latitud = lat,
                            longitud = lng,
                            direccion = dir,
                            referencia = ref,
                            radioGeocerca = radio
                        )
                    )
                } else {
                    _estado.value = DomicilioState.SinDomicilio
                }
            }
            .addOnFailureListener { e ->
                _estado.value = DomicilioState.Error(e.message ?: "Error al cargar domicilio")
            }
    }

    // RF-05 y RF-06: Escuchar solo los vehículos activos del servicio elegido ("AGUA" o "BASURA")
    fun escucharVehiculosPorServicio(tipoServicio: String) {
        vehiculosListener?.remove()
        _vehiculosActivos.value = emptyList() // Limpia los marcadores anteriores al cambiar el toggle

        vehiculosListener = firestore.collection("vehiculos_activos")
            .whereEqualTo("tipo", tipoServicio)
            .whereEqualTo("activo", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val lista = snapshot.documents.mapNotNull { doc ->
                    val lat = doc.getDouble("latitud") ?: return@mapNotNull null
                    val lng = doc.getDouble("longitud") ?: return@mapNotNull null
                    val rumbo = doc.getDouble("rumbo")?.toFloat() ?: 0f
                    val tipo = doc.getString("tipo") ?: tipoServicio
                    VehiculoTelemetria(
                        id = doc.id,
                        tipo = tipo,
                        latitud = lat,
                        longitud = lng,
                        rumbo = rumbo
                    )
                }
                _vehiculosActivos.value = lista

                // RF-10: Evaluar si algún vehículo entró en la geocerca del vecino
                evaluarGeocercaConVehiculos(lista)
            }
    }

    // RF-09: Guardar configuración del radio de geocerca (100, 300 o 500 m) en el perfil del usuario
    fun actualizarRadioGeocerca(nuevoRadio: Int, onExito: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val estadoActual = _estado.value
        if (estadoActual !is DomicilioState.Registrado) return

        firestore.collection("usuarios").document(uid)
            .set(mapOf("radioGeocerca" to nuevoRadio), SetOptions.merge())
            .addOnSuccessListener {
                _estado.value = DomicilioState.Registrado(
                    estadoActual.domicilio.copy(radioGeocerca = nuevoRadio)
                )
                onExito()
            }
    }

    // RF-10: Motor de Evaluación Espacial (Fórmula Haversine + Control de no repetir en el mismo día)
    private fun evaluarGeocercaConVehiculos(vehiculos: List<VehiculoTelemetria>) {
        val estadoActual = _estado.value
        if (estadoActual !is DomicilioState.Registrado) return
        val dom = estadoActual.domicilio
        val uid = auth.currentUser?.uid ?: return

        val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (ultimaFechaAlertaServidor == hoy) return // Ya se notificó hoy a este vecino

        for (v in vehiculos) {
            val distanciaMetros = calcularDistanciaMetros(
                dom.latitud, dom.longitud,
                v.latitud, v.longitud
            )
            if (distanciaMetros <= dom.radioGeocerca) {
                ultimaFechaAlertaServidor = hoy
                val nombreCamion = if (v.tipo == "AGUA") "El camión cisterna" else "El camión recolector"
                _alertaGeocerca.value = "¡$nombreCamion entró a tu geocerca (${distanciaMetros.toInt()} m de tu hogar)!"

                // Registrar en el backend que ya se envió la alerta hoy para no repetirla
                firestore.collection("usuarios").document(uid)
                    .set(mapOf("ultimaAlertaFecha" to hoy), SetOptions.merge())
                break
            }
        }
    }

    private fun calcularDistanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6371000.0 // metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radioTierra * c
    }

    fun guardarDomicilio(punto: Point, direccion: String, referencia: String, onExito: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val radioActual = (_estado.value as? DomicilioState.Registrado)?.domicilio?.radioGeocerca ?: 300

        val datosUsuario = hashMapOf(
            "latitud" to punto.latitude(),
            "longitud" to punto.longitude(),
            "direccion" to direccion,
            "referencia" to referencia,
            "radioGeocerca" to radioActual,
            "domicilioRegistrado" to true
        )

        val batch = firestore.batch()
        val userRef = firestore.collection("usuarios").document(uid)
        batch.set(userRef, datosUsuario, SetOptions.merge())

        val cisternaRef = firestore.collection("rutas_cisterna").document(uid)
        batch.set(
            cisternaRef,
            hashMapOf(
                "uidVecino" to uid,
                "direccion" to direccion,
                "referencia" to referencia,
                "latitud" to punto.latitude(),
                "longitud" to punto.longitude(),
                "completada" to false,
                "esEmergencia" to false,
                "timestamp" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )

        val recolectorRef = firestore.collection("rutas_recoleccion").document(uid)
        batch.set(
            recolectorRef,
            hashMapOf(
                "uidVecino" to uid,
                "direccion" to direccion,
                "zona" to referencia,
                "latitud" to punto.latitude(),
                "longitud" to punto.longitude(),
                "completada" to false,
                "timestamp" to FieldValue.serverTimestamp()
            ),
            SetOptions.merge()
        )

        batch.commit()
            .addOnSuccessListener {
                _estado.value = DomicilioState.Registrado(
                    Domicilio(
                        latitud = punto.latitude(),
                        longitud = punto.longitude(),
                        direccion = direccion,
                        referencia = referencia,
                        radioGeocerca = radioActual
                    )
                )
                onExito()
            }
            .addOnFailureListener { e ->
                _estado.value = DomicilioState.Error(e.message ?: "Error al guardar")
            }
    }

    fun enviarReporteServicio(
        tipoServicio: String,
        esEmergencia: Boolean,
        detalleExtra: String,
        onResultado: (Boolean, String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return
        val estadoActual = _estado.value
        if (estadoActual !is DomicilioState.Registrado) {
            onResultado(false, "Primero debes registrar tu domicilio en el mapa")
            return
        }

        val dom = estadoActual.domicilio
        val textoReferencia = if (detalleExtra.isNotBlank()) {
            "${dom.referencia} • Nota: $detalleExtra"
        } else {
            dom.referencia
        }

        if (tipoServicio == "AGUA") {
            val datos = hashMapOf(
                "uidVecino" to uid,
                "direccion" to dom.direccion,
                "referencia" to textoReferencia,
                "latitud" to dom.latitud,
                "longitud" to dom.longitud,
                "completada" to false,
                "esEmergencia" to esEmergencia,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("rutas_cisterna").document(uid)
                .set(datos, SetOptions.merge())
                .addOnSuccessListener { onResultado(true, "Solicitud enviada al camión cisterna") }
                .addOnFailureListener { onResultado(false, "Error al enviar solicitud") }
        } else {
            val datos = hashMapOf(
                "uidVecino" to uid,
                "direccion" to dom.direccion,
                "zona" to textoReferencia,
                "latitud" to dom.latitud,
                "longitud" to dom.longitud,
                "completada" to false,
                "timestamp" to FieldValue.serverTimestamp()
            )
            firestore.collection("rutas_recoleccion").document(uid)
                .set(datos, SetOptions.merge())
                .addOnSuccessListener { onResultado(true, "Punto reportado al camión recolector") }
                .addOnFailureListener { onResultado(false, "Error al enviar reporte") }
        }
    }

    fun eliminarDomicilio(onExito: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        val updates = hashMapOf<String, Any>(
            "latitud" to FieldValue.delete(),
            "longitud" to FieldValue.delete(),
            "direccion" to FieldValue.delete(),
            "referencia" to FieldValue.delete(),
            "domicilioRegistrado" to false
        )

        val batch = firestore.batch()
        batch.update(firestore.collection("usuarios").document(uid), updates)
        batch.delete(firestore.collection("rutas_cisterna").document(uid))
        batch.delete(firestore.collection("rutas_recoleccion").document(uid))

        batch.commit()
            .addOnSuccessListener {
                _estado.value = DomicilioState.SinDomicilio
                onExito()
            }
            .addOnFailureListener { e ->
                _estado.value = DomicilioState.Error(e.message ?: "Error al eliminar")
            }
    }

    override fun onCleared() {
        super.onCleared()
        vehiculosListener?.remove()
    }
}