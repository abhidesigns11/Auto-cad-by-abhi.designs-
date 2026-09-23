package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

enum class Solid3DShapeType(val label: String, val iconName: String) {
    BOX("Block / Plate", "CropSquare"),
    CYLINDER("Cylinder / Tank", "RadioButtonUnchecked"),
    HOLLOW_PIPE("Pipe / Nozzle Shell", "DonutLarge"),
    SPHERE("Dished Head / Sphere", "Brightness1"),
    CONE("Hopper Discharge Cone", "ChangeHistory"),
    TORUS("Torispherical Ring", "Toll")
}

enum class Solid3DBooleanOp(val label: String, val symbol: String) {
    UNION("Union (+ Add)", "➕"),
    SUBTRACT("Subtract (- Cutout)", "➖"),
    INTERSECT("Intersect (Overlap)", "✖")
}

data class Solid3DPart(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Part 1",
    val shapeType: Solid3DShapeType = Solid3DShapeType.BOX,
    val booleanOp: Solid3DBooleanOp = Solid3DBooleanOp.UNION,
    val posX: Float = 0f,
    val posY: Float = 0f,
    val posZ: Float = 0f,
    val dimX: Float = 200f, // Width or Radius
    val dimY: Float = 200f, // Height / Length
    val dimZ: Float = 200f, // Depth or Inner Wall
    val rotationDeg: Float = 0f,
    val color: Color = Color(0xFFCBD5E1),
    val isWireframeOnly: Boolean = false
)

object Manual3DSolidEngine {

    fun generateMeshFromParts(parts: List<Solid3DPart>): IsoMesh {
        val lines = mutableListOf<Line3D>()
        val faces = mutableListOf<Face3D>()

        // 3D Origin Axes
        lines.add(Line3D(Point3D(-250f, 0f, 0f), Point3D(250f, 0f, 0f), Color(0xFFEF4444), 2.5f)) // X = Red
        lines.add(Line3D(Point3D(0f, -250f, 0f), Point3D(0f, 250f, 0f), Color(0xFF10B981), 2.5f)) // Y = Green
        lines.add(Line3D(Point3D(0f, 0f, -250f), Point3D(0f, 0f, 250f), Color(0xFF38BDF8), 2.5f)) // Z = Blue

        for (part in parts) {
            when (part.shapeType) {
                Solid3DShapeType.BOX -> {
                    val halfW = part.dimX * 0.5f
                    val halfH = part.dimY * 0.5f
                    val halfD = part.dimZ * 0.5f
                    val cx = part.posX
                    val cy = part.posY
                    val cz = part.posZ

                    val isCutout = part.booleanOp == Solid3DBooleanOp.SUBTRACT
                    val faceColor = if (isCutout) Color(0x99EF4444) else part.color
                    val edgeColor = if (isCutout) Color(0xFFDC2626) else Color(0xFF475569)

                    // 8 Vertices
                    val f_bl = Point3D(cx - halfW, cy - halfH, cz + halfD)
                    val f_br = Point3D(cx + halfW, cy - halfH, cz + halfD)
                    val f_tr = Point3D(cx + halfW, cy + halfH, cz + halfD)
                    val f_tl = Point3D(cx - halfW, cy + halfH, cz + halfD)

                    val b_bl = Point3D(cx - halfW, cy - halfH, cz - halfD)
                    val b_br = Point3D(cx + halfW, cy - halfH, cz - halfD)
                    val b_tr = Point3D(cx + halfW, cy + halfH, cz - halfD)
                    val b_tl = Point3D(cx - halfW, cy + halfH, cz - halfD)

                    faces.add(Face3D(listOf(f_bl, f_br, f_tr, f_tl), faceColor))
                    faces.add(Face3D(listOf(b_br, b_bl, b_tl, b_tr), faceColor * 0.85f))
                    faces.add(Face3D(listOf(f_tl, f_tr, b_tr, b_tl), faceColor * 1.15f))
                    faces.add(Face3D(listOf(b_bl, b_br, f_br, f_bl), faceColor * 0.7f))
                    faces.add(Face3D(listOf(b_bl, f_bl, f_tl, b_tl), faceColor * 0.9f))
                    faces.add(Face3D(listOf(f_br, b_br, b_tr, f_tr), faceColor * 0.95f))

                    // Edges
                    listOf(
                        f_bl to f_br, f_br to f_tr, f_tr to f_tl, f_tl to f_bl,
                        b_bl to b_br, b_br to b_tr, b_tr to b_tl, b_tl to b_bl,
                        f_bl to b_bl, f_br to b_br, f_tr to b_tr, f_tl to b_tl
                    ).forEach { (p1, p2) ->
                        lines.add(Line3D(p1, p2, edgeColor, 1.8f))
                    }
                }

                Solid3DShapeType.CYLINDER, Solid3DShapeType.HOLLOW_PIPE -> {
                    val radius = part.dimX * 0.5f
                    val height = part.dimY
                    val cx = part.posX
                    val cy = part.posY
                    val cz = part.posZ
                    val isCutout = part.booleanOp == Solid3DBooleanOp.SUBTRACT
                    val faceColor = if (isCutout) Color(0x99EF4444) else part.color
                    val edgeColor = if (isCutout) Color(0xFFDC2626) else Color(0xFF475569)

                    val steps = 16
                    val topRing = mutableListOf<Point3D>()
                    val botRing = mutableListOf<Point3D>()

                    for (i in 0 until steps) {
                        val ang = (i.toFloat() / steps) * 2.0 * Math.PI
                        val x = (cx + radius * Math.cos(ang)).toFloat()
                        val z = (cz + radius * Math.sin(ang)).toFloat()
                        topRing.add(Point3D(x, cy + height * 0.5f, z))
                        botRing.add(Point3D(x, cy - height * 0.5f, z))
                    }

                    // Side faces
                    for (i in 0 until steps) {
                        val next = (i + 1) % steps
                        faces.add(
                            Face3D(
                                listOf(botRing[i], botRing[next], topRing[next], topRing[i]),
                                faceColor * (0.8f + (i % 4) * 0.08f)
                            )
                        )
                        lines.add(Line3D(botRing[i], botRing[next], edgeColor, 1.5f))
                        lines.add(Line3D(topRing[i], topRing[next], edgeColor, 1.5f))
                    }

                    // Caps if solid cylinder
                    if (part.shapeType == Solid3DShapeType.CYLINDER) {
                        faces.add(Face3D(topRing, faceColor * 1.1f))
                        faces.add(Face3D(botRing.reversed(), faceColor * 0.75f))
                    }
                }

                Solid3DShapeType.CONE -> {
                    val topR = part.dimX * 0.5f
                    val botR = (part.dimZ * 0.5f).coerceAtLeast(10f)
                    val height = part.dimY
                    val cx = part.posX
                    val cy = part.posY
                    val cz = part.posZ
                    val isCutout = part.booleanOp == Solid3DBooleanOp.SUBTRACT
                    val faceColor = if (isCutout) Color(0x99EF4444) else part.color
                    val edgeColor = if (isCutout) Color(0xFFDC2626) else Color(0xFF475569)

                    val steps = 16
                    val topRing = mutableListOf<Point3D>()
                    val botRing = mutableListOf<Point3D>()

                    for (i in 0 until steps) {
                        val ang = (i.toFloat() / steps) * 2.0 * Math.PI
                        val xTop = (cx + topR * Math.cos(ang)).toFloat()
                        val zTop = (cz + topR * Math.sin(ang)).toFloat()
                        val xBot = (cx + botR * Math.cos(ang)).toFloat()
                        val zBot = (cz + botR * Math.sin(ang)).toFloat()
                        topRing.add(Point3D(xTop, cy + height * 0.5f, zTop))
                        botRing.add(Point3D(xBot, cy - height * 0.5f, zBot))
                    }

                    for (i in 0 until steps) {
                        val next = (i + 1) % steps
                        faces.add(
                            Face3D(
                                listOf(botRing[i], botRing[next], topRing[next], topRing[i]),
                                faceColor * (0.85f + (i % 3) * 0.1f)
                            )
                        )
                        lines.add(Line3D(botRing[i], botRing[next], edgeColor, 1.5f))
                        lines.add(Line3D(topRing[i], topRing[next], edgeColor, 1.5f))
                    }
                    faces.add(Face3D(topRing, faceColor * 1.15f))
                    faces.add(Face3D(botRing.reversed(), faceColor * 0.7f))
                }

                Solid3DShapeType.SPHERE, Solid3DShapeType.TORUS -> {
                    // Dished head sphere approximation
                    val radius = part.dimX * 0.5f
                    val cx = part.posX
                    val cy = part.posY
                    val cz = part.posZ
                    val isCutout = part.booleanOp == Solid3DBooleanOp.SUBTRACT
                    val faceColor = if (isCutout) Color(0x99EF4444) else part.color

                    val latSteps = 6
                    val lonSteps = 12
                    for (i in 0 until latSteps) {
                        val lat0 = Math.PI * (-0.5 + i.toDouble() / latSteps)
                        val z0 = (sin(lat0) * radius).toFloat()
                        val zr0 = (cos(lat0) * radius).toFloat()

                        val lat1 = Math.PI * (-0.5 + (i + 1.toDouble()) / latSteps)
                        val z1 = (sin(lat1) * radius).toFloat()
                        val zr1 = (cos(lat1) * radius).toFloat()

                        for (j in 0 until lonSteps) {
                            val lon0 = 2 * Math.PI * j / lonSteps
                            val x0 = (cos(lon0) * zr0).toFloat()
                            val y0 = (sin(lon0) * zr0).toFloat()

                            val lon1 = 2 * Math.PI * (j + 1) / lonSteps
                            val x1 = (cos(lon1) * zr0).toFloat()
                            val y1 = (sin(lon1) * zr0).toFloat()

                            val x2 = (cos(lon1) * zr1).toFloat()
                            val y2 = (sin(lon1) * zr1).toFloat()

                            val x3 = (cos(lon0) * zr1).toFloat()
                            val y3 = (sin(lon0) * zr1).toFloat()

                            val p0 = Point3D(cx + x0, cy + y0, cz + z0)
                            val p1 = Point3D(cx + x1, cy + y1, cz + z0)
                            val p2 = Point3D(cx + x2, cy + y2, cz + z1)
                            val p3 = Point3D(cx + x3, cy + y3, cz + z1)

                            faces.add(Face3D(listOf(p0, p1, p2, p3), faceColor))
                            lines.add(Line3D(p0, p1, Color(0xFF64748B), 1f))
                        }
                    }
                }
            }
        }

        return IsoMesh(lines, faces)
    }

    private operator fun Color.times(factor: Float): Color {
        return Color(
            red = (this.red * factor).coerceIn(0f, 1f),
            green = (this.green * factor).coerceIn(0f, 1f),
            blue = (this.blue * factor).coerceIn(0f, 1f),
            alpha = this.alpha
        )
    }
}
