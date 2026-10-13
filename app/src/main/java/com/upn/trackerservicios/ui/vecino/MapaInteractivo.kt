package com.upn.trackerservicios.ui.vecino

import android.content.Context
import android.location.Geocoder
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolygonAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.*

@Composable
fun MapaInteractivo(
    estadoDomicilio: DomicilioState,
    vehiculosActivos: List<VehiculoTelemetria>,
    mostrarPanelDomicilio: Boolean,
    onGuardar: (Point, String, String) -> Unit,
    onEliminar: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cajamarca = Point.fromLngLat(-78.50027, -7.16378)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions { center(cajamarca); zoom(15.0); pitch(0.0) }
    }

    var referenciaInput by remember { mutableStateOf("") }
    var mostrarDialogoBorrar by remember { mutableStateOf(false) }
    var modoEdicionInterno by remember { mutableStateOf(false) }
    var obteniendoDireccion by remember { mutableStateOf(false) }

    val esNuevoRegistro = estadoDomicilio is DomicilioState.SinDomicilio
    val mostrarTarjeta = esNuevoRegistro || mostrarPanelDomicilio
    val estaEnModoEdicion = esNuevoRegistro || modoEdicionInterno

    LaunchedEffect(mostrarPanelDomicilio) {
        if (!mostrarPanelDomicilio) modoEdicionInterno = false
    }

    LaunchedEffect(estadoDomicilio) {
        if (estadoDomicilio is DomicilioState.Registrado) {
            referenciaInput = estadoDomicilio.domicilio.referencia
            val puntoCasa = Point.fromLngLat(estadoDomicilio.domicilio.longitud, estadoDomicilio.domicilio.latitud)
            mapViewportState.setCameraOptions { center(puntoCasa); zoom(15.5) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(modifier = Modifier.fillMaxSize(), mapViewportState = mapViewportState) {

            // 1. RF-02 y RF-09: Marcador del Domicilio + Círculo de Geocerca en metros reales
            if (estadoDomicilio is DomicilioState.Registrado) {
                val dom = estadoDomicilio.domicilio
                val puntoCasa = Point.fromLngLat(dom.longitud, dom.latitud)
                val puntosCirculo = remember(dom.latitud, dom.longitud, dom.radioGeocerca) {
                    generarPoligonoCirculo(dom.latitud, dom.longitud, dom.radioGeocerca.toDouble())
                }

                // Círculo de Geocerca (RF-09)
                PolygonAnnotation(points = listOf(puntosCirculo)) {
                    fillColor = Color(0x330075B0)
                    fillOutlineColor = Color(0xFF005B8F)
                }

                // Ícono del hogar
                ViewAnnotation(options = viewAnnotationOptions { geometry(puntoCasa); allowOverlap(true) }) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "Domicilio",
                        tint = Color(0xFF005B8F),
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            // 2. RF-05 y RF-06: Renderizado de vehículos en tiempo real con transición suave y rotación
            vehiculosActivos.forEach { vehiculo ->
                key(vehiculo.id) {
                    val latAnimada by animateFloatAsState(
                        targetValue = vehiculo.latitud.toFloat(),
                        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
                        label = "lat"
                    )
                    val lngAnimada by animateFloatAsState(
                        targetValue = vehiculo.longitud.toFloat(),
                        animationSpec = tween(durationMillis = 1500, easing = LinearEasing),
                        label = "lng"
                    )
                    val rumboAnimado by animateFloatAsState(
                        targetValue = vehiculo.rumbo,
                        animationSpec = tween(durationMillis = 800, easing = LinearEasing),
                        label = "rumbo"
                    )

                    val puntoVehiculo = Point.fromLngLat(lngAnimada.toDouble(), latAnimada.toDouble())
                    val colorVehiculo = if (vehiculo.tipo == "AGUA") Color(0xFF006B99) else Color(0xFF059669)

                    ViewAnnotation(
                        options = viewAnnotationOptions {
                            geometry(puntoVehiculo)
                            allowOverlap(true)
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .graphicsLayer { rotationZ = rumboAnimado }
                                .background(colorVehiculo, CircleShape)
                                .border(2.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (vehiculo.tipo == "AGUA") Icons.Rounded.LocalShipping else Icons.Rounded.DeleteOutline,
                                contentDescription = "Vehículo en ruta",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }

        if (estaEnModoEdicion && mostrarTarjeta) {
            Icon(
                imageVector = Icons.Rounded.LocationOn,
                contentDescription = "Puntero",
                tint = Color(0xFFE53935),
                modifier = Modifier.align(Alignment.Center).padding(bottom = 28.dp).size(56.dp)
            )
        }

        AnimatedVisibility(
            visible = mostrarTarjeta,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding()
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    if (estadoDomicilio is DomicilioState.Cargando || obteniendoDireccion) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF005B8F))
                        }
                    } else if (estaEnModoEdicion) {
                        Text(
                            text = if (esNuevoRegistro) "Fijar Domicilio" else "Actualizar Domicilio",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "La dirección de la calle se detectará automáticamente con el pin.",
                            color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = referenciaInput,
                            onValueChange = { referenciaInput = it },
                            label = { Text("Referencia (ej. Casa portón marrón)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = Color(0xFF006B99),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = Color(0xFF006B99),
                                unfocusedLabelColor = Color(0xFF64748B),
                                focusedTextColor = Color(0xFF1E293B),
                                unfocusedTextColor = Color(0xFF1E293B)
                            )
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (!esNuevoRegistro) {
                                OutlinedButton(
                                    onClick = { modoEdicionInterno = false },
                                    modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp)
                                ) { Text("Cancelar", color = Color.DarkGray) }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = {
                                    val centerPoint = mapViewportState.cameraState?.center
                                    if (centerPoint != null) {
                                        if (referenciaInput.isBlank()) {
                                            Toast.makeText(context, "Ingresa una referencia", Toast.LENGTH_SHORT).show()
                                        } else {
                                            obteniendoDireccion = true
                                            scope.launch {
                                                val direccionDetectada = obtenerDireccionDesdeCoordenadas(
                                                    context,
                                                    centerPoint.latitude(),
                                                    centerPoint.longitude()
                                                )
                                                obteniendoDireccion = false
                                                onGuardar(centerPoint, direccionDetectada, referenciaInput)
                                                modoEdicionInterno = false
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006B99))
                            ) { Text(if (esNuevoRegistro) "Guardar" else "Actualizar", color = Color.White) }
                        }
                    } else if (estadoDomicilio is DomicilioState.Registrado) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Rounded.Home, contentDescription = null, tint = Color(0xFF005B8F), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = estadoDomicilio.domicilio.direccion.ifBlank { "Mi Domicilio" },
                                    color = Color(0xFF1E293B),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ref: ${estadoDomicilio.domicilio.referencia} • Geocerca: ${estadoDomicilio.domicilio.radioGeocerca}m",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { modoEdicionInterno = true },
                                modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.EditLocationAlt, tint = Color.DarkGray, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cambiar", color = Color.DarkGray, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = { mostrarDialogoBorrar = true },
                                modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Icon(imageVector = Icons.Rounded.Delete, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Eliminar", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoBorrar = false },
            title = { Text("¿Eliminar domicilio?") },
            text = { Text("Tu ubicación registrada se borrará del sistema.") },
            confirmButton = {
                TextButton(onClick = { mostrarDialogoBorrar = false; onEliminar() }) {
                    Text("Eliminar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoBorrar = false }) { Text("Cancelar") }
            }
        )
    }
}

// Genera los vértices geográficos para dibujar el círculo exacto de la geocerca en metros (RF-09)
fun generarPoligonoCirculo(latitud: Double, longitud: Double, radioMetros: Double, lados: Int = 64): List<Point> {
    val puntos = mutableListOf<Point>()
    val radioTierra = 6371000.0
    val latRad = Math.toRadians(latitud)
    val lonRad = Math.toRadians(longitud)
    val distAngular = radioMetros / radioTierra

    for (i in 0..lados) {
        val angulo = 2.0 * Math.PI * i / lados
        val pLatRad = asin(sin(latRad) * cos(distAngular) + cos(latRad) * sin(distAngular) * cos(angulo))
        val pLonRad = lonRad + atan2(
            sin(angulo) * sin(distAngular) * cos(latRad),
            cos(distAngular) - sin(latRad) * sin(pLatRad)
        )
        puntos.add(Point.fromLngLat(Math.toDegrees(pLonRad), Math.toDegrees(pLatRad)))
    }
    return puntos
}

@Suppress("DEPRECATION")
suspend fun obtenerDireccionDesdeCoordenadas(context: Context, lat: Double, lng: Double): String {
    return withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale("es", "PE"))
            val direcciones = geocoder.getFromLocation(lat, lng, 1)
            if (!direcciones.isNullOrEmpty()) {
                val dir = direcciones[0]
                val calle = dir.thoroughfare ?: ""
                val numero = dir.subThoroughfare ?: ""
                val urbanizacion = dir.subLocality ?: ""

                when {
                    calle.isNotBlank() && numero.isNotBlank() -> "$calle $numero"
                    calle.isNotBlank() -> calle
                    urbanizacion.isNotBlank() -> urbanizacion
                    else -> dir.getAddressLine(0)?.split(",")?.firstOrNull() ?: "Ubicación en mapa"
                }
            } else {
                "Ubicación (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
            }
        } catch (e: Exception) {
            "Ubicación (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lng)})"
        }
    }
}