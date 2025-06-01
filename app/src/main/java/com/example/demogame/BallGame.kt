package com.example.demogame

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

@Composable
fun BallGame() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    var accX by remember { mutableStateOf(0f) }
    var accY by remember { mutableStateOf(0f) }

    var posX by remember { mutableStateOf(500f) }
    var posY by remember { mutableStateOf(500f) }

    var speedX by remember { mutableStateOf(0f) }
    var speedY by remember { mutableStateOf(0f) }

    val radius = 60f
    val damping = 0.7f

    var canvasWidth by remember { mutableStateOf(1080f) }
    var canvasHeight by remember { mutableStateOf(1920f) }

    // Лабиринт — список препятствий
    val walls = remember {
        listOf(
            Rect(200f, 600f, 800f, 650f),
            Rect(400f, 900f, 450f, 1500f),
            Rect(100f, 1200f, 600f, 1250f),
        )
    }

    // Подключение акселерометра
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                accX = -event.values[0]
                accY = event.values[1]
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Игровой цикл
    LaunchedEffect(Unit) {
        while (true) {
            speedX += accX * 0.5f
            speedY += accY * 0.5f

            var newX = posX + speedX
            var newY = posY + speedY

            // Столкновение со стенками экрана
            if (newX <= radius) {
                newX = radius
                speedX = -speedX * damping
            } else if (newX >= canvasWidth - radius) {
                newX = canvasWidth - radius
                speedX = -speedX * damping
            }

            if (newY <= radius) {
                newY = radius
                speedY = -speedY * damping
            } else if (newY >= canvasHeight - radius) {
                newY = canvasHeight - radius
                speedY = -speedY * damping
            }

            // Столкновение с препятствиями
            for (wall in walls) {
                val ballRect = Rect(newX - radius, newY - radius, newX + radius, newY + radius)

                if (ballRect.overlaps(wall)) {
                    val overlapLeft = ballRect.right - wall.left
                    val overlapRight = wall.right - ballRect.left
                    val overlapTop = ballRect.bottom - wall.top
                    val overlapBottom = wall.bottom - ballRect.top

                    val minOverlap =
                        listOf(overlapLeft, overlapRight, overlapTop, overlapBottom).minOrNull()

                    when (minOverlap) {
                        overlapLeft -> {
                            newX = wall.left - radius
                            speedX = -speedX * damping
                        }

                        overlapRight -> {
                            newX = wall.right + radius
                            speedX = -speedX * damping
                        }

                        overlapTop -> {
                            newY = wall.top - radius
                            speedY = -speedY * damping
                        }

                        overlapBottom -> {
                            newY = wall.bottom + radius
                            speedY = -speedY * damping
                        }
                    }
                }
            }

            posX = newX
            posY = newY

            // Трение
            speedX *= 0.98f
            speedY *= 0.98f

            delay(16L)
        }
    }

    // Отрисовка
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    canvasWidth = it.width.toFloat()
                    canvasHeight = it.height.toFloat()
                }
        ) {
            // Стены
            walls.forEach {
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(it.left, it.top),
                    size = Size(it.width, it.height)
                )
            }

            // Мяч
            drawCircle(
                color = Color.Cyan,
                radius = radius,
                center = Offset(posX, posY)
            )
        }
    }
}