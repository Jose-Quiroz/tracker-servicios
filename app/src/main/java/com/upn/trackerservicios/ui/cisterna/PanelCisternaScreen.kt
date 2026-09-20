package com.upn.trackerservicios.ui.cisterna

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelCisternaScreen(
    mensaje: String,
    onCerrarSesion: () -> Unit,
    viewModel: CisternaViewModel = viewModel()
) {
    val paradas by viewModel.paradas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    val colorPrincipal = Color(0xFF006B99)
    val paradasCompletadas = paradas.count { it.completada }
    val progreso = if (paradas.isEmpty()) 0f else paradasCompletadas.toFloat() / paradas.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta Cisterna", fontWeight = FontWeight.Bold, color = colorPrincipal) },
                actions = {
                    IconButton(onClick = onCerrarSesion) {
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
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                text = "Próximas paradas",
                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E293B),
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
                        ItemParada(parada = parada, colorPrincipal = colorPrincipal) { completada ->
                            viewModel.marcarParadaCompletada(parada.id, completada)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ItemParada(parada: Parada, colorPrincipal: Color, onCambiarEstado: (Boolean) -> Unit) {
    val context = LocalContext.current

    val colorBorde = when {
        parada.completada -> Color(0xFF10B981) // Verde
        parada.esEmergencia -> Color(0xFFEF4444) // Rojo
        else -> Color(0xFFCBD5E1) // Gris
    }

    val colorFondo = when {
        parada.completada -> Color(0xFFECFDF5) // Fondo verde claro
        parada.esEmergencia -> Color(0xFFFEF2F2) // Fondo rojo claro
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
            // Número
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

            // Textos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = parada.referencia,
                    fontWeight = FontWeight.Bold, color = Color(0xFF1E293B), fontSize = 15.sp
                )

                // Etiqueta de estado
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    if (parada.esEmergencia && !parada.completada) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Solicitud de Emergencia", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Text(
                            text = if (parada.completada) "Entregado" else "Pendiente",
                            color = if (parada.completada) Color(0xFF10B981) else Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Botón para trazar ruta
            if (!parada.completada && parada.latitud != 0.0) {
                IconButton(
                    onClick = {
                        try {
                            // Abre Google Maps en modo navegación hacia las coordenadas
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

            // Checkbox
            Checkbox(
                checked = parada.completada,
                onCheckedChange = { onCambiarEstado(it) },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
            )
        }
    }
}