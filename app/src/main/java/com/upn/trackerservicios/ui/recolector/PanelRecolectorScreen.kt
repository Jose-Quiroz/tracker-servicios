package com.upn.trackerservicios.ui.recolector

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.upn.trackerservicios.service.LocationTrackingService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelRecolectorScreen(
    mensaje: String,
    onCerrarSesion: () -> Unit,
    viewModel: RecolectorViewModel = viewModel()
) {
    val context = LocalContext.current
    val puntos by viewModel.puntos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    var gpsActivo by remember { mutableStateOf(LocationTrackingService.estaActivo) }

    val colorTematico = Color(0xFF059669)
    val puntosCompletados = puntos.count { it.completada }
    val progreso = if (puntos.isEmpty()) 0f else puntosCompletados.toFloat() / puntos.size

    val permisosGpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(LocationTrackingService.EXTRA_TIPO_SERVICIO, "BASURA")
            }
            ContextCompat.startForegroundService(context, intent)
            gpsActivo = true
            Toast.makeText(context, "Transmitiendo GPS de Recolector", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere permiso de ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta Recolección", fontWeight = FontWeight.Bold, color = colorTematico) },
                actions = {
                    IconButton(onClick = {
                        if (gpsActivo) {
                            context.startService(
                                Intent(context, LocationTrackingService::class.java).apply {
                                    action = LocationTrackingService.ACTION_STOP
                                }
                            )
                            gpsActivo = false
                        }
                        onCerrarSesion()
                    }) {
                        Icon(Icons.Rounded.Logout, contentDescription = "Salir", tint = Color(0xFFE53935))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // RF-15: Switch de Tracking GPS en Segundo Plano
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (gpsActivo) Color(0xFFECFDF5) else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (gpsActivo) colorTematico else Color(0xFFCBD5E1)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Rounded.GpsFixed,
                            contentDescription = null,
                            tint = if (gpsActivo) colorTematico else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (gpsActivo) "GPS en Segundo Plano Activo" else "Iniciar Transmisión GPS",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Envía posición cada 6s a los vecinos",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    Switch(
                        checked = gpsActivo,
                        onCheckedChange = { activar ->
                            if (activar) {
                                val listaPermisos = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    listaPermisos.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permisosGpsLauncher.launch(listaPermisos.toTypedArray())
                            } else {
                                context.startService(
                                    Intent(context, LocationTrackingService::class.java).apply {
                                        action = LocationTrackingService.ACTION_STOP
                                    }
                                )
                                gpsActivo = false
                            }
                        }
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Progreso de recolección", color = Color.Gray, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$puntosCompletados de ${puntos.size} domicilios/zonas",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF1E293B)
                        )
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = colorTematico)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = colorTematico,
                        trackColor = Color(0xFFE2E8F0),
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            Text(
                text = "Ubicaciones de vecinos asignadas",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorTematico)
                }
            } else if (puntos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aún no hay domicilios registrados por los vecinos.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(puntos) { punto ->
                        ItemPuntoRecoleccion(punto = punto, colorTematico = colorTematico) { completada ->
                            viewModel.marcarPuntoCompletado(punto.id, completada)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemPuntoRecoleccion(punto: PuntoRecoleccion, colorTematico: Color, onCambiarEstado: (Boolean) -> Unit) {
    val context = LocalContext.current
    val colorFondo = if (punto.completada) Color(0xFFF0FDF4) else Color.White
    val colorBorde = if (punto.completada) Color(0xFF10B981) else Color(0xFFCBD5E1)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colorFondo,
        border = androidx.compose.foundation.BorderStroke(1.dp, colorBorde),
        shadowElevation = if (punto.completada) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (punto.completada) Color(0xFF10B981) else colorTematico,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (punto.completada) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                } else {
                    Text("${punto.orden}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = punto.direccion.ifBlank { punto.zona },
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    fontSize = 15.sp
                )
                Text(
                    text = "Ref: ${punto.zona}",
                    color = Color(0xFF475569),
                    fontSize = 13.sp
                )
                Text(
                    text = if (punto.completada) "Recojo Completado" else "Pendiente de recojo",
                    color = if (punto.completada) Color(0xFF10B981) else Color.Gray,
                    fontSize = 12.sp
                )
            }

            if (!punto.completada && punto.latitud != 0.0) {
                IconButton(
                    onClick = {
                        try {
                            val gmmIntentUri = Uri.parse("google.navigation:q=${punto.latitud},${punto.longitud}&mode=d")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Google Maps no está instalado", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Navigation, contentDescription = "Navegar", tint = colorTematico)
                }
            }

            Checkbox(
                checked = punto.completada,
                onCheckedChange = { onCambiarEstado(it) },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
            )
        }
    }
}