package com.upn.trackerservicios.ui.vecino

import android.widget.Toast
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CajaTrackerScreen(
    onCerrarSesion: () -> Unit,
    viewModel: VecinoViewModel = viewModel()
) {
    val context = LocalContext.current
    val estadoDomicilio by viewModel.estado.collectAsState()
    var mostrarPanelDomicilio by remember { mutableStateOf(false) }
    val colorPrincipal = Color(0xFF005B8F)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Cargar el domicilio de Firestore al entrar
    LaunchedEffect(Unit) {
        viewModel.cargarDomicilio()
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
                    Text(
                        text = "Mi Perfil",
                        color = colorPrincipal,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar menú",
                            tint = colorPrincipal
                        )
                    }
                }

                HorizontalDivider()

                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                    label = { Text("Mis Datos") },
                    selected = false,
                    onClick = { /* TODO */ },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Rounded.Warning, contentDescription = null) },
                    label = { Text("Mis Reportes") },
                    selected = false,
                    onClick = { /* TODO */ },
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
                        // AQUÍ ESTÁ EL CAMBIO: El if envuelve al botón
                        if (estadoDomicilio is DomicilioState.Registrado) {
                            IconButton(onClick = {
                                mostrarPanelDomicilio = !mostrarPanelDomicilio
                            }) {
                                Icon(
                                    imageVector = if (mostrarPanelDomicilio) Icons.Rounded.Close else Icons.Rounded.LocationOn,
                                    contentDescription = "Panel Domicilio",
                                    tint = colorPrincipal
                                )
                            }
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
                        selected = true,
                        onClick = { },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFC2E7FF))
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = "Horarios", tint = colorPrincipal) },
                        label = { Text("Horarios", color = colorPrincipal) },
                        selected = false,
                        onClick = { }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Warning, contentDescription = "Reportar", tint = colorPrincipal) },
                        label = { Text("Reportar", color = colorPrincipal) },
                        selected = false,
                        onClick = { }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Notifications, contentDescription = "Alertas", tint = colorPrincipal) },
                        label = { Text("Alertas", color = colorPrincipal) },
                        selected = false,
                        onClick = { }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

                MapaInteractivo(
                    estadoDomicilio = estadoDomicilio,
                    mostrarPanelDomicilio = mostrarPanelDomicilio, // Le pasamos el estado del botón del header
                    onGuardar = { punto, referencia ->
                        viewModel.guardarDomicilio(punto, referencia) {
                            mostrarPanelDomicilio = false // Cerramos el panel al guardar
                            Toast.makeText(context, "Domicilio guardado con éxito", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEliminar = {
                        viewModel.eliminarDomicilio {
                            mostrarPanelDomicilio = false // Cerramos el panel al borrar
                            Toast.makeText(context, "Domicilio eliminado", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                // Selector rápido superior de servicio
                AnimatedVisibility(
                    visible = !mostrarPanelDomicilio && estadoDomicilio is DomicilioState.Registrado,
                    modifier = Modifier.align(Alignment.TopCenter)                ) {
                    // Selector rápido superior de servicio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFE0E5EC),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row {
                                Button(
                                    onClick = { },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(
                                            0xFF0075B0
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Text("Agua Potable", color = Color.White)
                                }
                                TextButton(onClick = { }) {
                                    Text("Recojo de Basura", color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}