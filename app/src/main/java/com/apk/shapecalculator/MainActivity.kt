package com.apk.shapecalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sqrt

// =====================================================
// LESSON: Interface — contract that classes must follow
// =====================================================
interface Drawable {
    fun draw(drawScope: DrawScope, topLeft: Offset, size: Float)
}

// =====================================================
// LESSON: Sealed class — restricted hierarchy (great for state)
// All subclasses must be defined in the same file
// =====================================================
sealed class Shape(
    val name: String,
    val color: Color
) : Drawable {

    // LESSON: Abstract — subclasses MUST implement these
    abstract fun area(): Double
    abstract fun perimeter(): Double

    // LESSON: Open — subclasses CAN override (but don't have to)
    open fun description(): String = "$name: area=${"%.2f".format(area())}"

    // =====================================================
    // LESSON: Data class inheriting from sealed class
    // =====================================================
    data class Circle(val radius: Double) : Shape("Circle", Color.Red) {
        override fun area(): Double = PI * radius * radius
        override fun perimeter(): Double = 2 * PI * radius

        override fun draw(drawScope: DrawScope, topLeft: Offset, size: Float) {
            drawScope.drawCircle(
                color = color,
                radius = size / 2,
                center = Offset(topLeft.x + size / 2, topLeft.y + size / 2)
            )
        }
    }

    data class Rectangle(
        val width: Double,
        val height: Double
    ) : Shape("Rectangle", Color.Blue) {
        override fun area(): Double = width * height
        override fun perimeter(): Double = 2 * (width + height)

        // LESSON: Overriding the open function
        override fun description(): String =
            "${super.description()}, ratio=${"%.2f".format(width / height)}"

        override fun draw(drawScope: DrawScope, topLeft: Offset, size: Float) {
            val ratio = (width / height).toFloat()
            drawScope.drawRect(
                color = color,
                topLeft = topLeft,
                size = Size(size * ratio.coerceIn(0.5f, 1.5f), size)
            )
        }
    }

    data class Triangle(
        val base: Double,
        val height: Double
    ) : Shape("Triangle", Color.Green) {
        override fun area(): Double = 0.5 * base * height

        override fun perimeter(): Double {
            val side = sqrt(((base / 2) * (base / 2)) + (height * height))
            return base + 2 * side
        }

        override fun draw(drawScope: DrawScope, topLeft: Offset, size: Float) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(topLeft.x + size / 2, topLeft.y)
                lineTo(topLeft.x + size, topLeft.y + size)
                lineTo(topLeft.x, topLeft.y + size)
                close()
            }
            drawScope.drawPath(path, color)
        }
    }

    // =====================================================
    // LESSON: Companion object — static-like members
    // =====================================================
    companion object {
        fun allShapeNames(): List<String> = listOf("Circle", "Rectangle", "Triangle")

        fun createDefault(type: String): Shape = when (type) {
            "Circle" -> Circle(5.0)
            "Rectangle" -> Rectangle(4.0, 6.0)
            "Triangle" -> Triangle(3.0, 4.0)
            else -> Circle(1.0)
        }
    }
}

// =====================================================
// LESSON: Object declaration — Singleton
// =====================================================
object ShapeAnalyzer {
    fun findLargest(shapes: List<Shape>): Shape? = shapes.maxByOrNull { it.area() }
    fun totalArea(shapes: List<Shape>): Double = shapes.sumOf { it.area() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ShapeCalculatorScreen() } }
    }
}

@Composable
fun ShapeCalculatorScreen() {
    val shapes = remember {
        mutableStateListOf<Shape>(
            Shape.Circle(5.0),
            Shape.Rectangle(4.0, 6.0),
            Shape.Triangle(3.0, 4.0)
        )
    }
    var selectedType by remember { mutableStateOf("Circle") }
    var param1 by remember { mutableStateOf("5") }
    var param2 by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
    ) {
        Spacer(Modifier.height(32.dp))
        Text("📐 Shape Calculator", fontSize = 24.sp)
        Spacer(Modifier.height(8.dp))

        // Shape type selector using companion object
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Shape.allShapeNames().forEach { name ->
                FilterChip(
                    selected = selectedType == name,
                    onClick = { selectedType = name },
                    label = { Text(name) }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = param1,
                onValueChange = { param1 = it },
                label = {
                    Text(
                        when (selectedType) {
                            "Circle" -> "Radius"
                            "Rectangle" -> "Width"
                            else -> "Base"
                        }
                    )
                },
                modifier = Modifier.weight(1f)
            )
            if (selectedType != "Circle") {
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = param2,
                    onValueChange = { param2 = it },
                    label = { Text("Height") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val p1 = param1.toDoubleOrNull() ?: return@Button
                val p2 = param2.toDoubleOrNull() ?: 0.0
                val newShape = when (selectedType) {
                    "Circle" -> Shape.Circle(p1)
                    "Rectangle" -> Shape.Rectangle(p1, p2)
                    "Triangle" -> Shape.Triangle(p1, p2)
                    else -> return@Button
                }
                shapes.add(newShape)
            }) { Text("Add") }
        }

        Spacer(Modifier.height(8.dp))

        // LESSON: Using Singleton object
        val largest = ShapeAnalyzer.findLargest(shapes)
        val totalArea = ShapeAnalyzer.totalArea(shapes)
        Text("Total area: ${"%.2f".format(totalArea)}")
        Text("Largest: ${largest?.description() ?: "None"}")

        // Draw shapes
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            shapes.forEachIndexed { index, shape ->
                // LESSON: Polymorphism — each shape draws itself
                shape.draw(
                    drawScope = this,
                    topLeft = Offset(index * 100f + 10f, 10f),
                    size = 80f
                )
            }
        }

        // Shape list
        shapes.forEach { shape ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // LESSON: when on sealed class — compiler checks all branches
                        val details = when (shape) {
                            is Shape.Circle    -> "r=${shape.radius}"
                            is Shape.Rectangle -> "${shape.width}×${shape.height}"
                            is Shape.Triangle  -> "b=${shape.base}, h=${shape.height}"
                            // No 'else' needed! Sealed class = exhaustive
                        }
                        Text("${shape.name}: $details")
                        Text(
                            "Area: ${"%.2f".format(shape.area())} | " +
                                    "Perimeter: ${"%.2f".format(shape.perimeter())}",
                            fontSize = 12.sp, color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}