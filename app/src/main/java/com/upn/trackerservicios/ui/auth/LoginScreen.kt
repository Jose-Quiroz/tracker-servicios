package com.upn.trackerservicios.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginExitoso: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var rolSeleccionado by remember { mutableStateOf("VECINO") }
    var pasoActual by remember { mutableStateOf(1) } // 1: Selección de Rol, 2: Credenciales

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val colorPrimario = Color(0xFF006B99)
    val colorSuperficie = Color(0xFFE8EDF8)
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.verificarSesion()
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthState.Exito) {
            onLoginExitoso((uiState as AuthState.Exito).rol)
        }
    }

    Scaffold(containerColor = Color.White) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding() // Eleva la interfaz cuando sube el teclado
                .verticalScroll(scrollState) // Permite desplazarse si la pantalla queda corta
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Encabezado
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = "Logo",
                tint = colorPrimario,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "CajaTracker",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorPrimario
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Selección de Perfil
            AnimatedVisibility(visible = pasoActual == 1) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Acceso al Sistema",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C1E)
                    )
                    Text(
                        text = "Seleccione su perfil de usuario para continuar.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    TarjetaOpcionRol(
                        titulo = "Vecino",
                        icono = Icons.Rounded.Person,
                        seleccionado = rolSeleccionado == "VECINO",
                        onClick = { rolSeleccionado = "VECINO" },
                        colorPrimario = colorPrimario,
                        colorFondo = colorSuperficie
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TarjetaOpcionRol(
                        titulo = "Chofer Cisterna",
                        icono = Icons.Rounded.LocalShipping,
                        seleccionado = rolSeleccionado == "CISTERNA",
                        onClick = { rolSeleccionado = "CISTERNA" },
                        colorPrimario = colorPrimario,
                        colorFondo = colorSuperficie
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TarjetaOpcionRol(
                        titulo = "Chofer Recolección",
                        icono = Icons.Rounded.DeleteOutline,
                        seleccionado = rolSeleccionado == "RECOLECTOR",
                        onClick = { rolSeleccionado = "RECOLECTOR" },
                        colorPrimario = colorPrimario,
                        colorFondo = colorSuperficie
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Button(
                        onClick = { pasoActual = 2 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorPrimario)
                    ) {
                        Text("Continuar", color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.Rounded.ArrowForward, tint = Color.White, contentDescription = null)
                    }
                }
            }

            // Credenciales
            AnimatedVisibility(visible = pasoActual == 2) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ingreso como $rolSeleccionado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1C1E)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = colorPrimario,
                            unfocusedBorderColor = Color(0xFF64748B),
                            focusedLabelColor = colorPrimario,
                            unfocusedLabelColor = Color(0xFF475569)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1E293B),
                            unfocusedTextColor = Color(0xFF1E293B),
                            focusedBorderColor = colorPrimario,
                            unfocusedBorderColor = Color(0xFF64748B),
                            focusedLabelColor = colorPrimario,
                            unfocusedLabelColor = Color(0xFF475569)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (uiState is AuthState.Cargando) {
                        CircularProgressIndicator(color = colorPrimario)
                    } else {
                        Button(
                            onClick = { viewModel.iniciarSesion(email, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimario)
                        ) {
                            Text("Iniciar Sesión", color = Color.White, fontSize = 16.sp)
                        }
                    }

                    if (uiState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = (uiState as AuthState.Error).mensaje,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(onClick = { pasoActual = 1 }) {
                        Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = null, tint = colorPrimario)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cambiar de perfil", color = colorPrimario)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaOpcionRol(
    titulo: String,
    icono: ImageVector,
    seleccionado: Boolean,
    onClick: () -> Unit,
    colorPrimario: Color,
    colorFondo: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = if (seleccionado) colorFondo else Color.Transparent,
        border = BorderStroke(
            width = if (seleccionado) 2.dp else 1.dp,
            color = if (seleccionado) colorPrimario else Color(0xFFDCE2E9)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = seleccionado,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = colorPrimario)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = titulo,
                fontSize = 16.sp,
                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal,
                color = Color(0xFF1E293B),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = if (seleccionado) colorPrimario else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}