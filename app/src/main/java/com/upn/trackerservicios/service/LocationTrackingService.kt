package com.upn.trackerservicios.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var tipoServicio: String = "AGUA" // "AGUA" o "BASURA"

    companion object {
        const val ACTION_START = "ACTION_START_TRACKING"
        const val ACTION_STOP = "ACTION_STOP_TRACKING"
        const val EXTRA_TIPO_SERVICIO = "EXTRA_TIPO_SERVICIO"
        const val CHANNEL_ID = "canal_tracking_gps_chofer"
        const val NOTIFICATION_ID = 1001

        var estaActivo: Boolean = false
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        crearCanalNotificacion()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                tipoServicio = intent.getStringExtra(EXTRA_TIPO_SERVICIO) ?: "AGUA"
                iniciarForegroundService()
                iniciarActualizacionesUbicacion()
                estaActivo = true
            }
            ACTION_STOP -> {
                detenerTracking()
                estaActivo = false
            }
        }
        return START_STICKY
    }

    private fun iniciarForegroundService() {
        val nombreRol = if (tipoServicio == "AGUA") "Camión Cisterna" else "Camión Recolector"
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CajaTracker • GPS Activo ($nombreRol)")
            .setContentText("Transmitiendo telemetría en tiempo real cada 6 segundos...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun iniciarActualizacionesUbicacion() {
        // RF-15: Intervalo entre 5 y 10 segundos (configurado en 6000 ms)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 6000L)
            .setMinUpdateIntervalMillis(5000L)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val uid = auth.currentUser?.uid ?: return

                val datosTelemetria = hashMapOf(
                    "uidChofer" to uid,
                    "tipo" to tipoServicio, // "AGUA" o "BASURA"
                    "latitud" to location.latitude,
                    "longitud" to location.longitude,
                    "rumbo" to location.bearing, // RF-06: Ángulo de rotación del vehículo
                    "velocidad" to location.speed,
                    "activo" to true,
                    "timestamp" to FieldValue.serverTimestamp()
                )

                firestore.collection("vehiculos_activos").document(uid)
                    .set(datosTelemetria, SetOptions.merge())
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun detenerTracking() {
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        val uid = auth.currentUser?.uid
        if (uid != null) {
            firestore.collection("vehiculos_activos").document(uid)
                .update("activo", false)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Seguimiento GPS de Vehículos",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}