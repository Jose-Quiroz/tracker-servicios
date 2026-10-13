package com.upn.trackerservicios.ui.cisterna

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
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
fun PanelCisternaScreen(
    mensaje: String,
    onCerrarSesion: () -> Unit,
    viewModel: CisternaViewModel = viewModel()
) {
    val context = LocalContext.current
    val paradas by viewModel.paradas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    var gpsActivo by remember { mutableStateOf(LocationTrackingService.estaActivo) }
    var idParadaParaFoto by remember { mutableStateOf<String?>(null) }

    val colorPrincipal = Color(0xFF006B99)
    val paradasCompletadas = paradas.count { it.completada }
    val progreso = if (paradas.isEmpty()) 0f else paradasCompletadas.toFloat() / paradas.size

    // RF-15: Launcher de permisos de Ubicación para el Foreground Service
    val permisosGpsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) {
            val intent = Intent(context, LocationTrackingService::class.java).apply {
                action = LocationTrackingService.ACTION_START
                putExtra(LocationTrackingService.EXTRA_TIPO_SERVICIO, "AGUA")
            }
            ContextCompat.startForegroundService(context, intent)
            gpsActivo = true
            Toast.makeText(context, "Transmitiendo GPS en segundo plano", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Se requiere permiso de ubicación para el tracking", Toast.LENGTH_SHORT).show()
        }
    }

    // RF-13: Launcher para capturar foto con la cámara
    val tomarFotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val idParada = idParadaParaFoto
        if (bitmap != null && idParada != null) {
            viewModel.subirFotoYCompletarParada(idParada, bitmap) { _, msj ->
                Toast.makeText(context, msj, Toast.LENGTH_LONG).show()
            }
        }
        idParadaParaFoto = null
    }

    // RF-13: Launcher para permiso de Cámara
    val permisoCamaraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            tomarFotoLauncher.launch()
        } else {
            Toast.makeText(context, "Permiso de cámara requerido para registrar la entrega", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta Cisterna", fontWeight = FontWeight.Bold, color = colorPrincipal) },
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
            // RF-15: Tarjeta de Control de Tracking GPS en Segundo Plano
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (gpsActivo) Color(0xFFE0F2FE) else Color.White,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (gpsActivo) colorPrincipal else Color(0xFFCBD5E1)
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
                            tint = if (gpsActivo) colorPrincipal else Color.Gray
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

            // Tarjeta de Progreso
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Progreso del recorrido", color = Color.Gray, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$paradasCompletadas de ${paradas.size} entregas",
                            fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B)
                        )
                        Icon(Icons.Rounded.WaterDrop, contentDescription = null, tint = colorPrincipal)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progreso },
                        modifier = Modifier.fillMaxWidth().height(10.dp),
                        color = colorPrincipal, trackColor = Color(0xFFE2E8F0), strokeCap = StrokeCap.Round
                    )
                }
            }

            Text(
                text = "Próximas paradas (Requiere foto de entrega)",
                fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorPrincipal)
                }
            } else if (paradas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay ruta asignada para hoy.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(paradas) { parada ->
                        ItemParada(
                            parada = parada,
                            colorPrincipal = colorPrincipal,
                            onTomarFotoEntrega = {
                                idParadaParaFoto = parada.id
                                permisoCamaraLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onDesmarcar = {
                                viewModel.marcarParadaCompletada(parada.id, false)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ItemParada(
    parada: Parada,
    colorPrincipal: Color,
    onTomarFotoEntrega: () -> Unit,
    onDesmarcar: () -> Unit
) {
    val context = LocalContext.current

    val colorBorde = when {
        parada.completada -> Color(0xFF10B981)
        parada.esEmergencia -> Color(0xFFEF4444)
        else -> Color(0xFFCBD5E1)
    }

    val colorFondo = when {
        parada.completada -> Color(0xFFECFDF5)
        parada.esEmergencia -> Color(0xFFFEF2F2)
        else -> Color.White
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = colorFondo,
        border = androidx.compose.foundation.BorderStroke(if (parada.esEmergencia && !parada.completada) 2.dp else 1.dp, colorBorde),
        shadowElevation = if (parada.completada) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (parada.completada) Color(0xFF10B981) else if (parada.esEmergencia) Color(0xFFEF4444) else colorPrincipal,
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (parada.completada) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                } else {
                    Text("${parada.orden}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parada.direccion.ifBlank { parada.referencia },
                    fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 15.sp
                )
                Text(
                    text = "Ref: ${parada.referencia}",
                    color = Color(0xFF475569), fontSize = 13.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    if (parada.esEmergencia && !parada.completada) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Solicitud de Emergencia", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        val textoEstado = if (parada.completada) {
                            if (parada.tieneEvidenciaFoto) "Atendida • Foto JPEG (${parada.pesoFotoKb} KB)" else "Atendida"
                        } else {
                            "Pendiente de captura"
                        }
                        Text(
                            text = textoEstado,
                            color = if (parada.completada) Color(0xFF10B981) else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = if (parada.completada) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }

            if (!parada.completada && parada.latitud != 0.0) {
                IconButton(
                    onClick = {
                        try {
                            val gmmIntentUri = Uri.parse("google.navigation:q=${parada.latitud},${parada.longitud}&mode=d")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Google Maps no está instalado", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Navigation, contentDescription = "Navegar", tint = colorPrincipal)
                }
            }

            // RF-13: Botón de Cámara para capturar foto y atender parada
            if (!parada.completada) {
                FilledIconButton(
                    onClick = onTomarFotoEntrega,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = colorPrincipal)
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = "Capturar entrega", tint = Color.White)
                }
            } else {
                IconButton(onClick = onDesmarcar) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = "Completada", tint = Color(0xFF10B981))
                }
            }
        }
    }
}