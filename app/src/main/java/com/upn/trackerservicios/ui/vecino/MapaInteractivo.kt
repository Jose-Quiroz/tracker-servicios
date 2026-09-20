package com.upn.trackerservicios.ui.vecino

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditLocationAlt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.ViewAnnotation
import com.mapbox.maps.viewannotation.geometry
import com.mapbox.maps.viewannotation.viewAnnotationOptions

@Composable
fun MapaInteractivo(
    estadoDomicilio: DomicilioState,
    mostrarPanelDomicilio: Boolean, // Controlado por el botón del Header
    onGuardar: (Point, String) -> Unit,
    onEliminar: () -> Unit
) {
    val context = LocalContext.current
    val cajamarca = Point.fromLngLat(-78.50027, -7.16378)

    val mapViewportState = rememberMapViewportState {
        setCameraOptions { center(cajamarca); zoom(15.0); pitch(0.0) }
    }

    var referenciaInput by remember { mutableStateOf("") }
    var mostrarDialogoBorrar by remember { mutableStateOf(false) }

    // Estado interno para saber si estamos en la vista de edición o de lectura
    var modoEdicionInterno by remember { mutableStateOf(false) }

    val esNuevoRegistro = estadoDomicilio is DomicilioState.SinDomicilio
    // La tarjeta se muestra si no tiene casa, o si tocó el botón del header
    val mostrarTarjeta = esNuevoRegistro || mostrarPanelDomicilio
    // El puntero rojo se muestra si no tiene casa o si le dio a "Cambiar"
    val estaEnModoEdicion = esNuevoRegistro || modoEdicionInterno

    // Si el usuario cierra el panel desde el header, reseteamos el modo edición
    LaunchedEffect(mostrarPanelDomicilio) {
        if (!mostrarPanelDomicilio) modoEdicionInterno = false
    }

    LaunchedEffect(estadoDomicilio) {
        if (estadoDomicilio is DomicilioState.Registrado) {
            referenciaInput = estadoDomicilio.domicilio.referencia
            val puntoCasa = Point.fromLngLat(estadoDomicilio.domicilio.longitud, estadoDomicilio.domicilio.latitud)
            mapViewportState.setCameraOptions { center(puntoCasa); zoom(16.0) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapboxMap(modifier = Modifier.fillMaxSize(), mapViewportState = mapViewportState) {
            if (estadoDomicilio is DomicilioState.Registrado) {
                val puntoCasa = Point.fromLngLat(estadoDomicilio.domicilio.longitud, estadoDomicilio.domicilio.latitud)
                ViewAnnotation(options = viewAnnotationOptions { geometry(puntoCasa); allowOverlap(true) }) {
                    Icon(imageVector = Icons.Rounded.Home, contentDescription = "Domicilio", tint = Color(0xFF005B8F), modifier = Modifier.size(48.dp))
                }
            }
        }

        if (estaEnModoEdicion && mostrarTarjeta) {
            Icon(
                imageVector = Icons.Rounded.LocationOn, contentDescription = "Puntero", tint = Color(0xFFE53935),
                modifier = Modifier.align(Alignment.Center).padding(bottom = 28.dp).size(56.dp)
            )
        }

        // --- TARJETA CON ANIMACIÓN Y PROTECCIÓN DE TECLADO ---
        AnimatedVisibility(
            visible = mostrarTarjeta,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .imePadding() // <-- ESTA LÍNEA EVITA QUE EL TECLADO TAPE LA TARJETA
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                    if (estadoDomicilio is DomicilioState.Cargando) {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF005B8F))
                        }
                    } else if (estaEnModoEdicion) {
                        // === VISTA DE FORMULARIO ===
                        Text(
                            text = if (esNuevoRegistro) "Fijar Domicilio" else "Actualizar Domicilio",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "Mueve el mapa para colocar el pin en tu casa e ingresa una referencia.",
                            color = Color.Gray, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = referenciaInput,
                            onValueChange = { referenciaInput = it },
                            label = { Text("Referencia") },
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
                                ) { Text("Cancelar",color=Color.DarkGray) }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Button(
                                onClick = {
                                    val centerPoint = mapViewportState.cameraState?.center
                                    if (centerPoint != null) {
                                        if (referenciaInput.isBlank()) Toast.makeText(context, "Ingresa una referencia", Toast.LENGTH_SHORT).show()
                                        else {
                                            onGuardar(centerPoint, referenciaInput)
                                            modoEdicionInterno = false
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
                                Text(text = "Mi Domicilio",color = Color.Black, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(text = estadoDomicilio.domicilio.referencia, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF475569))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { modoEdicionInterno = true }, // Activa el formulario
                                modifier = Modifier.weight(1f).height(46.dp), shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(imageVector = Icons.Rounded.EditLocationAlt, tint = Color.DarkGray, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cambiar",color=Color.DarkGray, fontSize = 13.sp)
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
                TextButton(onClick = { mostrarDialogoBorrar = false; onEliminar() }) { Text("Eliminar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoBorrar = false }) { Text("Cancelar") }
            }
        )
    }
}