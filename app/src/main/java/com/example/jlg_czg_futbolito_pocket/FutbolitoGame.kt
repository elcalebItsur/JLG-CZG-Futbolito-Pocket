package com.example.jlg_czg_futbolito_pocket

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ricknout.composesensors.accelerometer.isAccelerometerSensorAvailable
import dev.ricknout.composesensors.accelerometer.rememberAccelerometerSensorValueAsState

@Composable
fun FutbolitoGame() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var ballPos by remember { mutableStateOf(Offset(200f, 200f)) }
    var ballVel by remember { mutableStateOf(Offset(0f, 0f)) }
    
    var homeScore by remember { mutableStateOf(0) }
    var visitorScore by remember { mutableStateOf(0) }

    // --- Constantes del Juego ---
    val ballRadius = 25f // Radio de la pelota
    val goalWidth = 180f  // Ancho de la portería
    
    // --- Definición de Obstáculos (Diseño tipo Laberinto Complejo) ---
    val obstacles = remember(size) {
        if (size.width == 0) emptyList() else {
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val t = 20f // grosor de las barras
            
            listOf(
                // --- Sector Superior ---
                Rect(Offset(w * 0.2f, h * 0.12f), Size(w * 0.6f, t)), // Barra central superior
                Rect(Offset(w * 0.1f, h * 0.12f), Size(t, h * 0.1f)),  // Lateral L
                Rect(Offset(w * 0.9f - t, h * 0.12f), Size(t, h * 0.1f)), // Lateral R
                
                // --- Laberinto Superior ---
                Rect(Offset(30f, h * 0.25f), Size(w * 0.25f, t)),
                Rect(Offset(w * 0.75f - 30f, h * 0.25f), Size(w * 0.25f, t)),
                Rect(Offset(w * 0.5f - t/2, h * 0.2f), Size(t, h * 0.15f)), // Barra vertical central top
                
                // --- Sector Medio-Superior ---
                Rect(Offset(w * 0.25f, h * 0.35f), Size(t, h * 0.12f)),
                Rect(Offset(w * 0.75f - t, h * 0.35f), Size(t, h * 0.12f)),
                Rect(Offset(w * 0.35f, h * 0.42f), Size(w * 0.3f, t)),
                
                // --- Centro (Círculo central con obstáculos tipo "T") ---
                Rect(Offset(w * 0.4f, h * 0.5f - t/2), Size(w * 0.2f, t)),
                Rect(Offset(w * 0.5f - t/2, h * 0.45f), Size(t, h * 0.1f)),
                
                // --- Sector Medio-Inferior ---
                Rect(Offset(w * 0.15f, h * 0.58f), Size(w * 0.2f, t)),
                Rect(Offset(w * 0.65f, h * 0.58f), Size(w * 0.2f, t)),
                Rect(Offset(w * 0.5f - t/2, h * 0.65f), Size(t, h * 0.12f)),
                
                // --- Laberinto Inferior ---
                Rect(Offset(w * 0.3f, h * 0.75f), Size(w * 0.4f, t)),
                Rect(Offset(w * 0.1f, h * 0.75f), Size(t, h * 0.1f)),
                Rect(Offset(w * 0.9f - t, h * 0.75f), Size(t, h * 0.1f)),
                
                // --- Defensas de Portería Inferior ---
                Rect(Offset(w * 0.25f, h * 0.88f), Size(w * 0.5f, t)),
                
                // --- Obstáculos laterales adicionales ---
                Rect(Offset(30f, h * 0.45f), Size(w * 0.15f, t)),
                Rect(Offset(w * 0.85f - 30f, h * 0.45f), Size(w * 0.15f, t)),
            )
        }
    }

    // --- Integración de Sensores ---
    if (isAccelerometerSensorAvailable()) {
        val sensorValue by rememberAccelerometerSensorValueAsState()
        val (x, y, _) = sensorValue.value
        
        // Sensibilidad de inclinación
        val sensitivity = 1.25f
        ballVel = Offset(
            x = ballVel.x - x * sensitivity,
            y = ballVel.y + y * sensitivity
        )
    }

    // --- Fricción (Desaceleración natural) ---
    ballVel = Offset(ballVel.x * 0.97f, ballVel.y * 0.97f)

    // --- Actualización de la Física ---
    LaunchedEffect(ballVel) {
        if (size.width > 0 && size.height > 0) {
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            
            var nextX = ballPos.x + ballVel.x
            var nextY = ballPos.y + ballVel.y

            // --- Colisiones con Obstáculos ---
            for (obs in obstacles) {
                // AABB (Caja Delimitadora) simplificada con la pelota
                if (nextX + ballRadius > obs.left && nextX - ballRadius < obs.right &&
                    nextY + ballRadius > obs.top && nextY - ballRadius < obs.bottom) {
                    
                    // Determinar el lado de la colisión para un rebote correcto
                    val fromLeft = ballPos.x + ballRadius <= obs.left
                    val fromRight = ballPos.x - ballRadius >= obs.right
                    val fromTop = ballPos.y + ballRadius <= obs.top
                    val fromBottom = ballPos.y - ballRadius >= obs.bottom

                    if (fromLeft || fromRight) {
                        ballVel = Offset(-ballVel.x * 0.7f, ballVel.y)
                        nextX = if (fromLeft) obs.left - ballRadius else obs.right + ballRadius
                    } else if (fromTop || fromBottom) {
                        ballVel = Offset(ballVel.x, -ballVel.y * 0.7f)
                        nextY = if (fromTop) obs.top - ballRadius else obs.bottom + ballRadius
                    }
                }
            }

            // --- Colisiones con Bordes de la Cancha ---
            if (nextX - ballRadius < 30f) {
                nextX = 30f + ballRadius
                ballVel = Offset(-ballVel.x * 0.7f, ballVel.y)
            } else if (nextX + ballRadius > width - 30f) {
                nextX = width - 30f - ballRadius
                ballVel = Offset(-ballVel.x * 0.7f, ballVel.y)
            }

            // --- Lógica de Porterías ---
            if (nextY - ballRadius < 30f) {
                val goalXStart = (width - goalWidth) / 2
                val goalXEnd = (width + goalWidth) / 2
                if (nextX in goalXStart..goalXEnd) {
                    visitorScore++
                    nextX = width / 2
                    nextY = height / 2
                    ballVel = Offset(0f, 0f)
                } else {
                    nextY = 30f + ballRadius
                    ballVel = Offset(ballVel.x, -ballVel.y * 0.7f)
                }
            } else if (nextY + ballRadius > height - 30f) {
                val goalXStart = (width - goalWidth) / 2
                val goalXEnd = (width + goalWidth) / 2
                if (nextX in goalXStart..goalXEnd) {
                    homeScore++
                    nextX = width / 2
                    nextY = height / 2
                    ballVel = Offset(0f, 0f)
                } else {
                    nextY = height - 30f - ballRadius
                    ballVel = Offset(ballVel.x, -ballVel.y * 0.7f)
                }
            }

            ballPos = Offset(nextX, nextY)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))
                )
            )
            .systemBarsPadding() // Evita el Notch y la barra de navegación
            .onSizeChanged { size = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width.toFloat()
            val height = size.height.toFloat()

            // Textura del Campo (Rayas de césped)
            val stripes = 15
            val stripeHeight = height / stripes
            for (i in 0 until stripes) {
                if (i % 2 == 0) {
                    drawRect(
                        color = Color.White.copy(alpha = 0.05f),
                        topLeft = Offset(0f, i * stripeHeight),
                        size = Size(width, stripeHeight)
                    )
                }
            }

            // Bordes Exteriores
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(30f, 30f),
                size = Size(width - 60f, height - 60f),
                style = Stroke(width = 8f)
            )
            
            // Línea de medio campo y círculo central
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(30f, height / 2),
                end = Offset(width - 30f, height / 2),
                strokeWidth = 4f
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                center = Offset(width / 2, height / 2),
                radius = 120f,
                style = Stroke(width = 4f)
            )

            // Porterías
            val goalXStart = (width - goalWidth) / 2
            drawRect(
                color = Color(0xFFE0E0E0),
                topLeft = Offset(goalXStart, 10f),
                size = Size(goalWidth, 20f)
            )
            drawRect(
                color = Color(0xFFE0E0E0),
                topLeft = Offset(goalXStart, height - 30f),
                size = Size(goalWidth, 20f)
            )

            // Dibujo de Obstáculos
            for (obs in obstacles) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFBDBDBD), Color(0xFF757575))
                    ),
                    topLeft = obs.topLeft,
                    size = obs.size
                )
                // Reflejos en las barras (Efecto metálico)
                drawRect(
                    color = Color.White.copy(alpha = 0.3f),
                    topLeft = obs.topLeft,
                    size = Size(obs.size.width, 4f)
                )
            }

            // Sombra de la Pelota
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                center = ballPos + Offset(5f, 5f),
                radius = ballRadius
            )

            // Dibujo de la Pelota
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFBDBDBD)),
                    center = ballPos - Offset(5f, 5f),
                    radius = ballRadius * 1.5f
                ),
                center = ballPos,
                radius = ballRadius
            )
        }

        // Interfaz del Marcador
        Scoreboard(homeScore, visitorScore)
    }
}

// Componente del Marcador
@Composable
fun Scoreboard(homeScore: Int, visitorScore: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 50.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "LOCAL: $homeScore",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
        Text(
            text = " - ",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "VISITA: $visitorScore",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
