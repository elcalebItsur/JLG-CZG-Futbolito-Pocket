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

enum class GameState { INICIO, TURNO_P1, TURNO_P2, PANTALLA_EPILOGO_P1, TIEMPO_EXTRA, FINISHED }

@Composable
fun FutbolitoGame() {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var ballPos by remember { mutableStateOf(Offset(0f, 0f)) }
    var ballVel by remember { mutableStateOf(Offset(0f, 0f)) }
    
    var p1Goals by remember { mutableStateOf(0) }
    var p2Goals by remember { mutableStateOf(0) }
    
    var gameState by remember { mutableStateOf(GameState.INICIO) }
    var timeLeft by remember { mutableStateOf(45) }

    // --- Cronómetro de Juego ---
    LaunchedEffect(gameState) {
        if (gameState == GameState.TURNO_P1 || gameState == GameState.TURNO_P2 || gameState == GameState.TIEMPO_EXTRA) {
            while (timeLeft > 0) {
                kotlinx.coroutines.delay(1000)
                timeLeft--
            }
            // Transiciones al terminar el tiempo
            when (gameState) {
                GameState.TURNO_P1 -> {
                    gameState = GameState.PANTALLA_EPILOGO_P1
                }
                GameState.TURNO_P2 -> {
                    if (p1Goals == p2Goals) {
                        gameState = GameState.TIEMPO_EXTRA
                        timeLeft = 15 // Tiempo extra de 15s según instrucciones
                    } else {
                        gameState = GameState.FINISHED
                    }
                }
                else -> gameState = GameState.FINISHED
            }
        }
    }

    val ballRadius = 25f
    val goalWidth = 180f
    
    // --- Posición inicial al centro ---
    LaunchedEffect(size) {
        if (size.width > 0 && ballPos == Offset(0f, 0f)) {
            ballPos = Offset(size.width / 2f, size.height / 2f)
        }
    }

    // --- Definición de Obstáculos (Diseño tipo Laberinto) ---
    val obstacles = remember(size) {
        if (size.width == 0) emptyList() else {
            val w = size.width.toFloat()
            val h = size.height.toFloat()
            val t = 20f
            listOf(
                Rect(Offset(w * 0.2f, h * 0.12f), Size(w * 0.6f, t)),
                Rect(Offset(w * 0.1f, h * 0.12f), Size(t, h * 0.1f)),
                Rect(Offset(w * 0.9f - t, h * 0.12f), Size(t, h * 0.1f)),
                Rect(Offset(30f, h * 0.25f), Size(w * 0.25f, t)),
                Rect(Offset(w * 0.75f - 30f, h * 0.25f), Size(w * 0.25f, t)),
                Rect(Offset(w * 0.5f - t/2, h * 0.2f), Size(t, h * 0.15f)),
                Rect(Offset(w * 0.25f, h * 0.35f), Size(t, h * 0.12f)),
                Rect(Offset(w * 0.75f - t, h * 0.35f), Size(t, h * 0.12f)),
                Rect(Offset(w * 0.35f, h * 0.42f), Size(w * 0.3f, t)),
                Rect(Offset(w * 0.4f, h * 0.5f - t/2), Size(w * 0.2f, t)),
                Rect(Offset(w * 0.5f - t/2, h * 0.45f), Size(t, h * 0.1f)),
                Rect(Offset(w * 0.15f, h * 0.58f), Size(w * 0.2f, t)),
                Rect(Offset(w * 0.65f, h * 0.58f), Size(w * 0.2f, t)),
                Rect(Offset(w * 0.5f - t/2, h * 0.65f), Size(t, h * 0.12f)),
                Rect(Offset(w * 0.3f, h * 0.75f), Size(w * 0.4f, t)),
                Rect(Offset(w * 0.1f, h * 0.75f), Size(t, h * 0.1f)),
                Rect(Offset(w * 0.9f - t, h * 0.75f), Size(t, h * 0.1f)),
                Rect(Offset(w * 0.25f, h * 0.88f), Size(w * 0.5f, t)),
                Rect(Offset(30f, h * 0.45f), Size(w * 0.15f, t)),
                Rect(Offset(w * 0.85f - 30f, h * 0.45f), Size(w * 0.15f, t)),
            )
        }
    }

    // --- Integración de Sensores (Solo activos durante el turno) ---
    val juegoActivo = gameState == GameState.TURNO_P1 || gameState == GameState.TURNO_P2 || gameState == GameState.TIEMPO_EXTRA
    if (isAccelerometerSensorAvailable() && juegoActivo) {
        val sensorValue by rememberAccelerometerSensorValueAsState()
        val (x, y, _) = sensorValue.value
        val sensitivity = 1.3f
        ballVel = Offset(ballVel.x - x * sensitivity, ballVel.y + y * sensitivity)
    }

    // --- Fricción ---
    ballVel = Offset(ballVel.x * 0.97f, ballVel.y * 0.97f)

    // --- Física ---
    LaunchedEffect(ballVel) {
        if (size.width > 0 && juegoActivo) {
            val width = size.width.toFloat()
            val height = size.height.toFloat()
            var nextX = ballPos.x + ballVel.x
            var nextY = ballPos.y + ballVel.y

            // Colisiones Obstáculos
            for (obs in obstacles) {
                if (nextX + ballRadius > obs.left && nextX - ballRadius < obs.right &&
                    nextY + ballRadius > obs.top && nextY - ballRadius < obs.bottom) {
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

            // Colisiones Bordes
            if (nextX - ballRadius < 30f) {
                nextX = 30f + ballRadius
                ballVel = Offset(-ballVel.x * 0.7f, ballVel.y)
            } else if (nextX + ballRadius > width - 30f) {
                nextX = width - 30f - ballRadius
                ballVel = Offset(-ballVel.x * 0.7f, ballVel.y)
            }

            // Porterías
            val goalXStart = (width - goalWidth) / 2
            val goalXEnd = (width + goalWidth) / 2
            
            // Portería superior (Objetivo)
            if (nextY - ballRadius < 30f) {
                if (nextX in goalXStart..goalXEnd) {
                    if (gameState == GameState.TURNO_P1) p1Goals++ else p2Goals++
                    nextX = width / 2; nextY = height / 2; ballVel = Offset(0f, 0f)
                } else {
                    nextY = 30f + ballRadius; ballVel = Offset(ballVel.x, -ballVel.y * 0.7f)
                }
            } 
            // Portería inferior (Objetivo)
            else if (nextY + ballRadius > height - 30f) {
                if (nextX in goalXStart..goalXEnd) {
                    if (gameState == GameState.TURNO_P1) p1Goals++ else p2Goals++
                    nextX = width / 2; nextY = height / 2; ballVel = Offset(0f, 0f)
                } else {
                    nextY = height - 30f - ballRadius; ballVel = Offset(ballVel.x, -ballVel.y * 0.7f)
                }
            }
            ballPos = Offset(nextX, nextY)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20))))
            .systemBarsPadding()
            .onSizeChanged { size = it }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width.toFloat(); val height = size.height.toFloat()
            // Dibujar campo base
            val stripes = 15; val stripeHeight = height / stripes
            for (i in 0 until stripes) { if (i % 2 == 0) drawRect(Color.White.copy(alpha = 0.05f), Offset(0f, i * stripeHeight), Size(width, stripeHeight)) }
            drawRect(Color.White.copy(alpha = 0.8f), Offset(30f, 30f), Size(width - 60f, height - 60f), style = Stroke(width = 8f))
            drawLine(Color.White.copy(alpha = 0.5f), Offset(30f, height / 2), Offset(width - 30f, height / 2), strokeWidth = 4f)
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                center = Offset(width / 2, height / 2),
                radius = 120f,
                style = Stroke(width = 4f)
            )
            
            // Porterías
            val goalXStart = (width - goalWidth) / 2
            drawRect(Color(0xFFE0E0E0), Offset(goalXStart, 10f), Size(goalWidth, 20f))
            drawRect(Color(0xFFE0E0E0), Offset(goalXStart, height - 30f), Size(goalWidth, 20f))

            // Obstáculos
            for (obs in obstacles) {
                drawRect(Brush.linearGradient(colors = listOf(Color(0xFFBDBDBD), Color(0xFF757575))), obs.topLeft, obs.size)
                drawRect(Color.White.copy(alpha = 0.3f), obs.topLeft, Size(obs.size.width, 4f))
            }

            // Pelota
            drawCircle(
                color = Color.Black.copy(alpha = 0.2f),
                center = ballPos + Offset(5f, 5f),
                radius = ballRadius
            )
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

        // --- HUD del Juego ---
        Column(modifier = Modifier.fillMaxSize()) {
            Scoreboard(p1Goals, p2Goals, timeLeft, gameState)
        }

        // --- Overlays de Estado ---
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (gameState) {
                GameState.INICIO -> {
                    OverlayInformativo(
                        titulo = "FUT-GOLIN",
                        mensaje = "Reglas: Cada jugador tiene 45s para anotar tantos goles como pueda.\n¿Quién inicia?",
                        botonText = "Iniciar Jugador 1",
                        onAction = { gameState = GameState.TURNO_P1; timeLeft = 45 }
                    )
                }
                GameState.PANTALLA_EPILOGO_P1 -> {
                    OverlayInformativo(
                        titulo = "¡Tiempo de P1 terminado!",
                        mensaje = "Jugador 1 anotó: $p1Goals goles.\nAhora es el turno del Jugador 2.",
                        botonText = "Iniciar Jugador 2",
                        onAction = { 
                            gameState = GameState.TURNO_P2
                            timeLeft = 45
                            ballPos = Offset(size.width / 2f, size.height / 2f)
                            ballVel = Offset(0f, 0f)
                        }
                    )
                }
                GameState.FINISHED -> {
                    val ganador = if (p1Goals > p2Goals) "JUGADOR 1" else if (p2Goals > p1Goals) "JUGADOR 2" else "EMPATE"
                    OverlayInformativo(
                        titulo = "¡JUEGO TERMINADO!",
                        mensaje = "P1: $p1Goals vs P2: $p2Goals\nGANADOR: $ganador",
                        botonText = "Reiniciar",
                        onAction = { 
                            gameState = GameState.INICIO
                            p1Goals = 0; p2Goals = 0; timeLeft = 45
                        }
                    )
                }
                GameState.TIEMPO_EXTRA -> {
                    // Texto flotante de tiempo extra
                    Text("¡TIEMPO EXTRA!", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 200.dp))
                }
                else -> {}
            }
        }
    }
}

@Composable
fun Scoreboard(p1: Int, p2: Int, time: Int, state: GameState) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoCard("JUGADOR 1", p1, state == GameState.TURNO_P1)
            TimerCard(time)
            InfoCard("JUGADOR 2", p2, state == GameState.TURNO_P2 || state == GameState.TIEMPO_EXTRA)
        }
        if (state == GameState.TURNO_P1 || state == GameState.TURNO_P2) {
             Text(
                 text = if (state == GameState.TURNO_P1) "ATACANDO: P1" else "ATACANDO: P2",
                 color = Color.Yellow,
                 fontWeight = FontWeight.Bold,
                 fontSize = 18.sp
             )
        }
    }
}

@Composable
fun InfoCard(name: String, goals: Int, active: Boolean) {
    Surface(
        color = if (active) Color(0xFFC0CA33) else Color.Black.copy(alpha = 0.6f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.padding(4.dp).width(120.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Text(name, color = Color.White, fontSize = 12.sp)
            Text("$goals GOLES", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TimerCard(time: Int) {
    Surface(
        color = Color.Red.copy(alpha = 0.8f),
        shape = androidx.compose.foundation.shape.CircleShape,
        modifier = Modifier.size(70.dp),
        border = Stroke(width = 2f).let { null } // Just for reference
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("$time s", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OverlayInformativo(titulo: String, mensaje: String, botonText: String, onAction: () -> Unit) {
    Surface(
        color = Color.Black.copy(alpha = 0.85f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        modifier = Modifier.padding(32.dp).fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(titulo, color = Color.Yellow, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(16.dp))
            Text(mensaje, color = Color.White, fontSize = 18.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            androidx.compose.material3.Button(onClick = onAction) {
                Text(botonText, fontSize = 20.sp)
            }
        }
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
