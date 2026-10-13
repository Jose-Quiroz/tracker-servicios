package com.upn.trackerservicios.ui.vecino

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CajaTrackerScreen(
    onCerrarSesion: () -> Unit,
    viewModel: VecinoViewModel = viewModel()
) {
    val context = LocalContext.current
    val estadoDomicilio by viewModel.estado.collectAsState()
    val vehiculosActivos by viewModel.vehiculosActivos.collectAsState()
    val alertaGeocerca by viewModel.alertaGeocerca.collectAsState()

    var mostrarPanelDomicilio by remember { mutableStateOf(false) }
    var mostrarDialogoReporte by remember { mutableStateOf(false) }
    var mostrarDialogoGeocerca by remember { mutableStateOf(false) }
    var servicioSeleccionado by remember { mutableStateOf("AGUA") }

    val colorPrincipal = Color(0xFF005B8F)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Permiso para notificaciones en Android 13+ (RF-10)
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        viewModel.cargarDomicilio()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // RF-05: Cada vez que cambia el Toggle ("AGUA" o "BASURA"), limpia y escucha solo esos vehículos
    LaunchedEffect(servicioSeleccionado) {
        viewModel.escucharVehiculosPorServicio(servicioSeleccionado)
    }

    // RF-10: Disparar notificación push local cuando el motor espacial detecta cruce de geocerca
    LaunchedEffect(alertaGeocerca) {
        alertaGeocerca?.let { mensaje ->
            mostrarNotificacionGeocerca(context, mensaje)
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            viewModel.limpiarAlertaGeocerca()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mi Perfil", color = colorPrincipal, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar menú", tint = colorPrincipal)
                    }
                }

                HorizontalDivider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text("Mi Domicilio") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mostrarPanelDomicilio = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Radar, contentDescription = null) },
                    label = { Text("Radio de Geocerca (100/300/500m)") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        if (estadoDomicilio is DomicilioState.Registrado) {
                            mostrarDialogoGeocerca = true
                        } else {
                            Toast.makeText(context, "Primero registra tu domicilio", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Warning, contentDescription = null) },
                    label = { Text("Solicitar Servicio / Emergencia") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mostrarDialogoReporte = true
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onCerrarSesion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                ) {
                    Text("Cerrar Sesión", color = Color.White)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "CajaTracker",
                            color = colorPrincipal,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Menú", tint = colorPrincipal)
                        }
                    },
                    actions = {
                        // Acceso rápido a RF-09 (Ajuste de Geocerca)
                        IconButton(onClick = {
                            if (estadoDomicilio is DomicilioState.Registrado) {
                                mostrarDialogoGeocerca = true
                            } else {
                                Toast.makeText(context, "Primero fija tu domicilio en el mapa", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Rounded.Radar, contentDescription = "Geocerca", tint = colorPrincipal)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xFFE8EDF8)) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Map, contentDescription = "Mapa", tint = colorPrincipal) },
                        label = { Text("Mapa", color = colorPrincipal) },
                        selected = !mostrarPanelDomicilio && !mostrarDialogoReporte && !mostrarDialogoGeocerca,
                        onClick = {
                            mostrarPanelDomicilio = false
                            mostrarDialogoReporte = false
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFC2E7FF))
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.LocationOn, contentDescription = "Domicilio", tint = colorPrincipal) },
                        label = { Text("Domicilio", color = colorPrincipal) },
                        selected = mostrarPanelDomicilio,
                        onClick = {
                            if (estadoDomicilio is DomicilioState.Registrado) {
                                mostrarPanelDomicilio = !mostrarPanelDomicilio
                            } else {
                                Toast.makeText(context, "Fija tu domicilio en la tarjeta inferior", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFC2E7FF))
                    )

                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Warning, contentDescription = "Solicitar", tint = colorPrincipal) },
                        label = { Text("Solicitar", color = colorPrincipal) },
                        selected = mostrarDialogoReporte,
                        onClick = {
                            if (estadoDomicilio is DomicilioState.Registrado) {
                                mostrarDialogoReporte = true
                            } else {
                                Toast.makeText(context, "Primero fija tu domicilio en el mapa", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFC2E7FF))
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                MapaInteractivo(
                    estadoDomicilio = estadoDomicilio,
                    vehiculosActivos = vehiculosActivos,
                    mostrarPanelDomicilio = mostrarPanelDomicilio,
                    onGuardar = { punto, direccion, referencia ->
                        viewModel.guardarDomicilio(punto, direccion, referencia) {
                            mostrarPanelDomicilio = false
                            Toast.makeText(context, "Dirección guardada: $direccion", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEliminar = {
                        viewModel.eliminarDomicilio {
                            mostrarPanelDomicilio = false
                            Toast.makeText(context, "Domicilio eliminado de las rutas", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // RF-05: Toggle superior de servicio (Agua Potable / Recojo de Basura)
                AnimatedVisibility(
                    visible = !mostrarPanelDomicilio && estadoDomicilio is DomicilioState.Registrado,
                    modifier = Modifier.align(Alignment.TopCenter)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFE0E5EC),
                            shadowElevation = 4.dp,
                            modifier = Modifier.height(42.dp)
                        ) {
                            Row {
                                Button(
                                    onClick = { servicioSeleccionado = "AGUA" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (servicioSeleccionado == "AGUA") Color(0xFF0075B0) else Color.Transparent
                                    ),
                                    elevation = null,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(
                                        "Agua Potable",
                                        color = if (servicioSeleccionado == "AGUA") Color.White else Color.DarkGray
                                    )
                                }
                                Button(
                                    onClick = { servicioSeleccionado = "BASURA" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (servicioSeleccionado == "BASURA") Color(0xFF059669) else Color.Transparent
                                    ),
                                    elevation = null,
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text(
                                        "Recojo de Basura",
                                        color = if (servicioSeleccionado == "BASURA") Color.White else Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.9f)
                        ) {
                            Text(
                                text = "Unidades en vivo (${servicioSeleccionado.lowercase()}): ${vehiculosActivos.size}",
                                fontSize = 12.sp,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // RF-09: Diálogo con Slider para configurar el Radio de Geocerca (100m, 300m, 500m)
    if (mostrarDialogoGeocerca && estadoDomicilio is DomicilioState.Registrado) {
        val radioInicial = (estadoDomicilio as DomicilioState.Registrado).domicilio.radioGeocerca
        val indiceInicial = when (radioInicial) {
            100 -> 0f
            500 -> 2f
            else -> 1f
        }
        var sliderIndex by remember { mutableFloatStateOf(indiceInicial) }
        val radioElegido = when (sliderIndex.roundToInt()) {
            0 -> 100
            2 -> 500
            else -> 300
        }

        AlertDialog(
            onDismissRequest = { mostrarDialogoGeocerca = false },
            title = {
                Text("Radio de Geocerca", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Recibirás una alerta automática cuando el camión entre a este perímetro alrededor de tu hogar (máximo 1 vez al día).",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Radio seleccionado: $radioElegido metros",
                        fontWeight = FontWeight.Bold,
                        color = colorPrincipal,
                        fontSize = 16.sp
                    )
                    Slider(
                        value = sliderIndex,
                        onValueChange = { sliderIndex = it },
                        valueRange = 0f..2f,
                        steps = 1, // Permite exactamente 3 posiciones: 0 (100m), 1 (300m), 2 (500m)
                        colors = SliderDefaults.colors(
                            thumbColor = colorPrincipal,
                            activeTrackColor = colorPrincipal
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("100 m", fontSize = 12.sp, color = Color.DarkGray)
                        Text("300 m", fontSize = 12.sp, color = Color.DarkGray)
                        Text("500 m", fontSize = 12.sp, color = Color.DarkGray)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.actualizarRadioGeocerca(radioElegido) {
                            Toast.makeText(context, "Geocerca actualizada a $radioElegido m", Toast.LENGTH_SHORT).show()
                            mostrarDialogoGeocerca = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrincipal)
                ) {
                    Text("Guardar Radio", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoGeocerca = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Modal de Solicitud de Servicio
    if (mostrarDialogoReporte) {
        var tipoReporte by remember { mutableStateOf(servicioSeleccionado) }
        var esEmergencia by remember { mutableStateOf(true) }
        var detalleExtra by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { mostrarDialogoReporte = false },
            title = { Text("Solicitar Servicio en mi Ubicación", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Tu ubicación registrada aparecerá en la hoja de ruta del chofer en tiempo real.",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tipoReporte == "AGUA",
                            onClick = { tipoReporte = "AGUA" },
                            label = { Text("Cisterna Agua") },
                            leadingIcon = { Icon(Icons.Rounded.WaterDrop, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        FilterChip(
                            selected = tipoReporte == "BASURA",
                            onClick = { tipoReporte = "BASURA" },
                            label = { Text("Recolector") },
                            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                    if (tipoReporte == "AGUA") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(checked = esEmergencia, onCheckedChange = { esEmergencia = it })
                            Text("Marcar como Solicitud de Emergencia (Prioridad Alta)", fontSize = 13.sp)
                        }
                    }
                    OutlinedTextField(
                        value = detalleExtra,
                        onValueChange = { detalleExtra = it },
                        label = { Text("Nota opcional (ej. Sin agua hace 3 días)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.enviarReporteServicio(tipoReporte, esEmergencia, detalleExtra) { _, msj ->
                            Toast.makeText(context, msj, Toast.LENGTH_SHORT).show()
                            mostrarDialogoReporte = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrincipal)
                ) {
                    Text("Enviar a Ruta", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoReporte = false }) { Text("Cancelar") }
            }
        )
    }
}

// RF-10: Lanzador de Notificación Push en el dispositivo al cruzar la Geocerca
fun mostrarNotificacionGeocerca(context: Context, mensaje: String) {
    val channelId = "canal_alertas_geocerca"
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Alertas de Geocerca CajaTracker",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("¡Unidad de servicio cerca de tu domicilio!")
        .setContentText(mensaje)
        .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .build()

    manager.notify(2002, notification)
}