package com.upn.trackerservicios.ui.recolector

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelRecolectorScreen(
    mensaje: String,
    onCerrarSesion: () -> Unit,
    viewModel: RecolectorViewModel = viewModel()
) {
    val puntos by viewModel.puntos.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    val colorTematico = Color(0xFF059669)

    val puntosCompletados = puntos.count { it.completada }
    val progreso = if (puntos.isEmpty()) 0f else puntosCompletados.toFloat() / puntos.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ruta Recolección", fontWeight = FontWeight.Bold, color = colorTematico) },
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
            // Tarjeta de Progreso Operativo
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
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
                            text = "$puntosCompletados de ${puntos.size} zonas",
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
                text = "Puntos de acopio asignados",
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
                    Text("No hay zonas de recolección para hoy.", color = Color.Gray)
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

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = punto.zona,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B),
                    fontSize = 15.sp
                )
                Text(
                    text = if (punto.completada) "Zona Limpia" else "Pendiente",
                    color = if (punto.completada) Color(0xFF10B981) else Color.Gray,
                    fontSize = 13.sp
                )
            }

            Checkbox(
                checked = punto.completada,
                onCheckedChange = { onCambiarEstado(it) },
                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
            )
        }
    }
}