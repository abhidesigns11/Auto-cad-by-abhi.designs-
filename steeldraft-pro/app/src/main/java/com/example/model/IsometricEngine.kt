package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Point3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Point3D(x * scalar, y * scalar, z * scalar)
}

data class Line3D(
    val p1: Point3D,
    val p2: Point3D,
    val color: Color = Color(0xFFE2E8F0),
    val strokeWidth: Float = 2f
)

data class Face3D(
    val vertices: List<Point3D>,
    val baseColor: Color = Color(0xFFCBD5E1),
    val isDoubleSided: Boolean = true
) {
    fun calculateNormal(): Point3D {
        if (vertices.size < 3) return Point3D(0f, 0f, 1f)
        val v1 = vertices[1] - vertices[0]
        val v2 = vertices[2] - vertices[0]
        val nx = v1.y * v2.z - v1.z * v2.y
        val ny = v1.z * v2.x - v1.x * v2.z
        val nz = v1.x * v2.y - v1.y * v2.x
        val len = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        return Point3D(nx / len, ny / len, nz / len)
    }

    fun centerDepth(rotatedVertices: List<Point3D>): Float {
        if (rotatedVertices.isEmpty()) return 0f
        return rotatedVertices.map { it.z }.average().toFloat()
    }
}

data class IsoMesh(
    val lines: List<Line3D> = emptyList(),
    val faces: List<Face3D> = emptyList()
)

object IsometricEngine {

    // Default isometric angles: 30° elevation, 45° azimuth
    const val DEFAULT_YAW = 45f
    const val DEFAULT_PITCH = 30f

    fun rotatePoint(p: Point3D, yawDeg: Float, pitchDeg: Float): Point3D {
        val yawRad = Math.toRadians(yawDeg.toDouble())
        val pitchRad = Math.toRadians(pitchDeg.toDouble())

        // Rotate around Y axis (yaw)
        val cosY = cos(yawRad).toFloat()
        val sinY = sin(yawRad).toFloat()
        val x1 = p.x * cosY + p.z * sinY
        val y1 = p.y
        val z1 = -p.x * sinY + p.z * cosY

        // Rotate around X axis (pitch)
        val cosP = cos(pitchRad).toFloat()
        val sinP = sin(pitchRad).toFloat()
        val x2 = x1
        val y2 = y1 * cosP - z1 * sinP
        val z2 = y1 * sinP + z1 * cosP

        return Point3D(x2, y2, z2)
    }

    fun projectToScreen(p: Point3D, centerX: Float, centerY: Float, scale: Float): Offset {
        // Orthographic CAD projection
        return Offset(
            x = centerX + p.x * scale,
            y = centerY - p.y * scale
        )
    }

    fun buildModelMesh(
        type: IsometricModelType,
        customEntities: List<CadEntity> = emptyList(),
        extrudeDepth: Float = 300f
    ): IsoMesh {
        return when (type) {
            IsometricModelType.MBBR_STP_TANK -> buildMbbrTankMesh()
            IsometricModelType.SS_CYCLONE_HOPPER -> buildCycloneHopperMesh()
            IsometricModelType.SS_MOBILE_TRAY_RACK -> buildMobileTrayRackMesh()
            IsometricModelType.SS_DOUBLE_SINK_TABLE -> buildDoubleSinkTableMesh()
            IsometricModelType.SS_LOCKER_CABINET -> buildLockerCabinetMesh()
            IsometricModelType.CUSTOM_3D_GENERATED -> {
                if (customEntities.isNotEmpty()) {
                    buildMeshFrom2DEntities(customEntities, extrudeDepth)
                } else {
                    buildMbbrTankMesh()
                }
            }
        }
    }

    fun buildMeshFrom2DEntities(entities: List<CadEntity>, extrudeDepth: Float = 300f): IsoMesh {
        if (entities.isEmpty()) {
            val axisLines = listOf(
                Line3D(Point3D(-200f, 0f, 0f), Point3D(200f, 0f, 0f), Color(0xFF38BDF8), 2f),
                Line3D(Point3D(0f, 0f, -200f), Point3D(0f, 0f, 200f), Color(0xFF10B981), 2f),
                Line3D(Point3D(0f, 0f, 0f), Point3D(0f, 200f, 0f), Color(0xFFF59E0B), 2f)
            )
            return IsoMesh(axisLines, emptyList())
        }

        // Compute centroid of non-annotation 2D entities
        val shapeEntities = entities.filter {
            it is CadRect || it is CadCircle || it is CadLine || it is CadNozzleComponent || it is CadHopperCone
        }
        val targetEntities = if (shapeEntities.isNotEmpty()) shapeEntities else entities

        var sumX = 0.0
        var sumY = 0.0
        var count = 0
        for (e in targetEntities) {
            val c = CadTransformEngine.getEntityCenter(e)
            sumX += c.x
            sumY += c.y
            count++
        }
        val avgX = if (count > 0) (sumX / count).toFloat() else 0f
        val avgY = if (count > 0) (sumY / count).toFloat() else 0f

        val lines = mutableListOf<Line3D>()
        val faces = mutableListOf<Face3D>()

        val ssColorTop = Color(0xFFE2E8F0)
        val ssColorFront = Color(0xFFCBD5E1)
        val ssColorSide = Color(0xFF94A3B8)
        val ssWireframe = Color(0xFF475569)

        val halfD = extrudeDepth * 0.5f

        for (e in targetEntities) {
            when (e) {
                is CadRect -> {
                    val depth = if (e.plateThicknessMm > 0) (e.plateThicknessMm * 4f).coerceIn(20f, 800f) else extrudeDepth
                    val hD = depth * 0.5f
                    val x1 = e.x - avgX
                    val x2 = (e.x + e.width) - avgX
                    val yTop = avgY - e.y
                    val yBot = avgY - (e.y + e.height)

                    val f_bl = Point3D(x1, yBot, hD)
                    val f_br = Point3D(x2, yBot, hD)
                    val f_tr = Point3D(x2, yTop, hD)
                    val f_tl = Point3D(x1, yTop, hD)

                    val b_bl = Point3D(x1, yBot, -hD)
                    val b_br = Point3D(x2, yBot, -hD)
                    val b_tr = Point3D(x2, yTop, -hD)
                    val b_tl = Point3D(x1, yTop, -hD)

                    faces.add(Face3D(listOf(f_tl, f_tr, f_br, f_bl), ssColorFront))
                    faces.add(Face3D(listOf(b_tr, b_tl, b_bl, b_br), ssColorFront))
                    faces.add(Face3D(listOf(b_tl, b_tr, f_tr, f_tl), ssColorTop))
                    faces.add(Face3D(listOf(f_bl, f_br, b_br, b_bl), ssColorSide))
                    faces.add(Face3D(listOf(b_tl, f_tl, f_bl, b_bl), ssColorSide))
                    faces.add(Face3D(listOf(f_tr, b_tr, b_br, f_br), ssColorSide))

                    lines.add(Line3D(f_tl, f_tr, ssWireframe, 2f))
                    lines.add(Line3D(f_tr, f_br, ssWireframe, 2f))
                    lines.add(Line3D(f_br, f_bl, ssWireframe, 2f))
                    lines.add(Line3D(f_bl, f_tl, ssWireframe, 2f))

                    lines.add(Line3D(b_tl, b_tr, ssWireframe, 1.5f))
                    lines.add(Line3D(b_tr, b_br, ssWireframe, 1.5f))
                    lines.add(Line3D(b_br, b_bl, ssWireframe, 1.5f))
                    lines.add(Line3D(b_bl, b_tl, ssWireframe, 1.5f))

                    lines.add(Line3D(f_tl, b_tl, ssWireframe, 1.5f))
                    lines.add(Line3D(f_tr, b_tr, ssWireframe, 1.5f))
                    lines.add(Line3D(f_br, b_br, ssWireframe, 1.5f))
                    lines.add(Line3D(f_bl, b_bl, ssWireframe, 1.5f))
                }

                is CadCircle -> {
                    val cx3d = e.cx - avgX
                    val cy3d = avgY - e.cy
                    val r = e.radius
                    val depth = if (e.isPcd) 15f else extrudeDepth * 0.8f
                    val hD = depth * 0.5f

                    val segments = 24
                    val frontPts = mutableListOf<Point3D>()
                    val backPts = mutableListOf<Point3D>()

                    for (i in 0 until segments) {
                        val angle = (2.0 * Math.PI * i / segments)
                        val px = (cx3d + r * cos(angle)).toFloat()
                        val py = (cy3d + r * sin(angle)).toFloat()
                        frontPts.add(Point3D(px, py, hD))
                        backPts.add(Point3D(px, py, -hD))
                    }

                    faces.add(Face3D(frontPts, if (e.isPcd) Color(0xFF94A3B8) else ssColorTop))
                    faces.add(Face3D(backPts.reversed(), if (e.isPcd) Color(0xFF94A3B8) else ssColorTop))

                    for (i in 0 until segments) {
                        val next = (i + 1) % segments
                        faces.add(
                            Face3D(
                                listOf(frontPts[i], frontPts[next], backPts[next], backPts[i]),
                                if (i % 2 == 0) ssColorFront else ssColorSide
                            )
                        )
                        lines.add(Line3D(frontPts[i], frontPts[next], ssWireframe, 1.5f))
                        lines.add(Line3D(backPts[i], backPts[next], ssWireframe, 1.2f))
                        if (i % 6 == 0) {
                            lines.add(Line3D(frontPts[i], backPts[i], ssWireframe, 1.2f))
                        }
                    }
                }

                is CadLine -> {
                    val x1 = e.x1 - avgX
                    val y1 = avgY - e.y1
                    val x2 = e.x2 - avgX
                    val y2 = avgY - e.y2

                    val p1_f = Point3D(x1, y1, halfD)
                    val p2_f = Point3D(x2, y2, halfD)
                    val p1_b = Point3D(x1, y1, -halfD)
                    val p2_b = Point3D(x2, y2, -halfD)

                    faces.add(Face3D(listOf(p1_f, p2_f, p2_b, p1_b), Color(0xFFCBD5E1)))

                    lines.add(Line3D(p1_f, p2_f, Color(0xFF38BDF8), 2.5f))
                    lines.add(Line3D(p1_b, p2_b, Color(0xFF0284C7), 2f))
                    lines.add(Line3D(p1_f, p1_b, Color(0xFF64748B), 1.5f))
                    lines.add(Line3D(p2_f, p2_b, Color(0xFF64748B), 1.5f))
                }

                is CadNozzleComponent -> {
                    val cx3d = e.cx - avgX
                    val cy3d = avgY - e.cy
                    val rad = Math.toRadians(e.angleDeg.toDouble())
                    val neckLen = e.neckLengthMm
                    val flangeR = e.flangeDiaMm * 0.5f

                    val startP = Point3D(cx3d, cy3d, 0f)
                    val endP = Point3D(
                        (cx3d + neckLen * cos(rad)).toFloat(),
                        (cy3d + neckLen * sin(rad)).toFloat(),
                        0f
                    )

                    lines.add(Line3D(startP, endP, Color(0xFFF59E0B), 5f))

                    val segments = 16
                    val flangePts = mutableListOf<Point3D>()
                    for (i in 0 until segments) {
                        val a = (2.0 * Math.PI * i / segments)
                        val offsetU = (flangeR * cos(a)).toFloat()
                        val offsetV = (flangeR * sin(a)).toFloat()
                        flangePts.add(Point3D(endP.x + offsetU * (-sin(rad)).toFloat(), endP.y + offsetU * cos(rad).toFloat(), offsetV))
                    }
                    faces.add(Face3D(flangePts, Color(0xFFF59E0B)))
                    for (i in 0 until segments) {
                        lines.add(Line3D(flangePts[i], flangePts[(i + 1) % segments], Color(0xFFB45309), 2f))
                    }
                }

                is CadHopperCone -> {
                    val topCx = (e.topX + e.topWidth * 0.5f) - avgX
                    val topCy = avgY - e.topY
                    val botCy = avgY - (e.topY + e.height)
                    val topR = e.topWidth * 0.5f
                    val botR = e.bottomWidth * 0.5f

                    val segs = 20
                    val topPts = mutableListOf<Point3D>()
                    val botPts = mutableListOf<Point3D>()

                    for (i in 0 until segs) {
                        val a = (2.0 * Math.PI * i / segs)
                        topPts.add(Point3D((topCx + topR * cos(a)).toFloat(), topCy, (topR * sin(a)).toFloat()))
                        botPts.add(Point3D((topCx + botR * cos(a)).toFloat(), botCy, (botR * sin(a)).toFloat()))
                    }

                    for (i in 0 until segs) {
                        val next = (i + 1) % segs
                        faces.add(Face3D(listOf(topPts[i], topPts[next], botPts[next], botPts[i]), if (i % 2 == 0) ssColorFront else ssColorSide))
                        lines.add(Line3D(topPts[i], topPts[next], ssWireframe, 1.5f))
                        lines.add(Line3D(botPts[i], botPts[next], ssWireframe, 1.5f))
                    }
                }

                else -> {}
            }
        }

        return IsoMesh(lines, faces)
    }

    // 1. MBBR STP Tank & Settling Unit (Image 1)
    private fun buildMbbrTankMesh(): IsoMesh {
        val lines = mutableListOf<Line3D>()
        val faces = mutableListOf<Face3D>()

        val ssColorTop = Color(0xFFE2E8F0)
        val ssColorFront = Color(0xFFCBD5E1)
        val ssColorSide = Color(0xFF94A3B8)
        val ssHopperColor = Color(0xFFA0AEC0)

        // Main Tank: 1000 x 1000 x 1000 mm. Origin at center (-300, 0, 0)
        val mW = 500f
        val mD = 500f
        val mH = 500f
        val mX = -280f

        val b0 = Point3D(mX - mW * 0.5f, 0f, -mD * 0.5f)
        val b1 = Point3D(mX + mW * 0.5f, 0f, -mD * 0.5f)
        val b2 = Point3D(mX + mW * 0.5f, 0f, mD * 0.5f)
        val b3 = Point3D(mX - mW * 0.5f, 0f, mD * 0.5f)

        val t0 = Point3D(mX - mW * 0.5f, mH, -mD * 0.5f)
        val t1 = Point3D(mX + mW * 0.5f, mH, -mD * 0.5f)
        val t2 = Point3D(mX + mW * 0.5f, mH, mD * 0.5f)
        val t3 = Point3D(mX - mW * 0.5f, mH, mD * 0.5f)

        // Main Tank Faces
        faces.add(Face3D(listOf(t0, t1, t2, t3), ssColorTop)) // Top
        faces.add(Face3D(listOf(t3, t2, b2, b3), ssColorFront)) // Front (+Z)
        faces.add(Face3D(listOf(t1, t0, b0, b1), ssColorSide)) // Back (-Z)
        faces.add(Face3D(listOf(t0, t3, b3, b0), ssColorSide)) // Left (-X)

        // Settling Tank: 800 x 800 x 800 mm (attached to right of main tank)
        val sW = 400f
        val sD = 400f
        val sStraightH = 250f
        val sHopperH = 180f
        val sX = mX + mW * 0.5f + sW * 0.5f

        val st0 = Point3D(sX - sW * 0.5f, mH, -sD * 0.5f)
        val st1 = Point3D(sX + sW * 0.5f, mH, -sD * 0.5f)
        val st2 = Point3D(sX + sW * 0.5f, mH, sD * 0.5f)
        val st3 = Point3D(sX - sW * 0.5f, mH, sD * 0.5f)

        val sm0 = Point3D(sX - sW * 0.5f, mH - sStraightH, -sD * 0.5f)
        val sm1 = Point3D(sX + sW * 0.5f, mH - sStraightH, -sD * 0.5f)
        val sm2 = Point3D(sX + sW * 0.5f, mH - sStraightH, sD * 0.5f)
        val sm3 = Point3D(sX - sW * 0.5f, mH - sStraightH, sD * 0.5f)

        // Settling Top Face
        faces.add(Face3D(listOf(st0, st1, st2, st3), ssColorTop))
        // Settling Straight Sides
        faces.add(Face3D(listOf(st3, st2, sm2, sm3), ssColorFront))
        faces.add(Face3D(listOf(st2, st1, sm1, sm2), ssColorSide))

        // Settling Conical Hopper Bottom (tapering to bottom drain nozzle)
        val drainPoint = Point3D(sX, mH - sStraightH - sHopperH, 0f)
        faces.add(Face3D(listOf(sm3, sm2, drainPoint), ssHopperColor))
        faces.add(Face3D(listOf(sm2, sm1, drainPoint), ssColorSide))
        faces.add(Face3D(listOf(sm1, sm0, drainPoint), ssHopperColor))
        faces.add(Face3D(listOf(sm0, sm3, drainPoint), ssColorFront))

        // Nozzles (Pipes & Flanges in Cyan/Silver)
        // 1. Inlet Nozzle on Main Tank Left
        lines.add(Line3D(Point3D(mX - mW * 0.5f, mH - 60f, 0f), Point3D(mX - mW * 0.5f - 50f, mH - 60f, 0f), Color(0xFF38BDF8), 4f))
        lines.add(Line3D(Point3D(mX - mW * 0.5f - 50f, mH - 90f, 0f), Point3D(mX - mW * 0.5f - 50f, mH - 30f, 0f), Color(0xFF38BDF8), 6f))

        // 2. Outlet Nozzle on Settling Tank Right
        lines.add(Line3D(Point3D(sX + sW * 0.5f, mH - 60f, 0f), Point3D(sX + sW * 0.5f + 50f, mH - 60f, 0f), Color(0xFF38BDF8), 4f))
        lines.add(Line3D(Point3D(sX + sW * 0.5f + 50f, mH - 90f, 0f), Point3D(sX + sW * 0.5f + 50f, mH - 30f, 0f), Color(0xFF38BDF8), 6f))

        // 3. Overflow Nozzle between tanks
        lines.add(Line3D(Point3D(mX + mW * 0.5f - 20f, mH - 50f, 0f), Point3D(sX - sW * 0.5f + 20f, mH - 50f, 0f), Color(0xFF38BDF8), 5f))

        // 4. Drain Nozzle on hopper bottom
        lines.add(Line3D(drainPoint, Point3D(drainPoint.x, drainPoint.y - 40f, 0f), Color(0xFF38BDF8), 5f))
        lines.add(Line3D(Point3D(drainPoint.x - 25f, drainPoint.y - 40f, 0f), Point3D(drainPoint.x + 25f, drainPoint.y - 40f, 0f), Color(0xFF38BDF8), 7f))

        return IsoMesh(lines, faces)
    }

    // 2. SS Pressure Cyclone & Hopper Vessel (Image 2)
    private fun buildCycloneHopperMesh(): IsoMesh {
        val lines = mutableListOf<Line3D>()
        val faces = mutableListOf<Face3D>()

        val ssColorTop = Color(0xFFE2E8F0)
        val ssColorBody = Color(0xFFCBD5E1)
        val ssColorDark = Color(0xFF94A3B8)

        // Cylindrical Shell: Ø400, Height: 300, Conical Bottom: 200, Top Flange Ø430
        val numSegments = 16
        val r = 200f
        val rFlange = 215f
        val shellH = 260f
        val coneH = 200f
        val bottomR = 35f

        val topPoints = mutableListOf<Point3D>()
        val bottomShellPoints = mutableListOf<Point3D>()
        val coneBottomPoints = mutableListOf<Point3D>()

        for (i in 0 until numSegments) {
            val angle = i * 2.0 * Math.PI / numSegments
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()

            topPoints.add(Point3D(r * cosA, shellH, r * sinA))
            bottomShellPoints.add(Point3D(r * cosA, 0f, r * sinA))
            coneBottomPoints.add(Point3D(bottomR * cosA, -coneH, bottomR * sinA))
        }

        // Top Flange Cover
        faces.add(Face3D(topPoints, ssColorTop))

        // Shell Cylindrical Faces
        for (i in 0 until numSegments) {
            val next = (i + 1) % numSegments
            val shade = if (i in 3..9) ssColorBody else ssColorDark
            faces.add(Face3D(listOf(topPoints[i], topPoints[next], bottomShellPoints[next], bottomShellPoints[i]), shade))
            // Conical Bottom Faces
            faces.add(Face3D(listOf(bottomShellPoints[i], bottomShellPoints[next], coneBottomPoints[next], coneBottomPoints[i]), ssColorDark))
        }

        // Top 50 NB Nozzle
        lines.add(Line3D(Point3D(0f, shellH, 0f), Point3D(0f, shellH + 70f, 0f), Color(0xFF38BDF8), 5f))
        lines.add(Line3D(Point3D(-40f, shellH + 70f, 0f), Point3D(40f, shellH + 70f, 0f), Color(0xFF38BDF8), 8f))

        // Hinged Inspection Door on top cover
        lines.add(Line3D(Point3D(40f, shellH + 5f, 40f), Point3D(140f, shellH + 5f, 100f), Color(0xFFF59E0B), 4f))

        // Support Brackets (4 pads)
        val padW = 50f
        lines.add(Line3D(Point3D(-r, 100f, 0f), Point3D(-r - padW, 100f, 0f), Color(0xFFE2E8F0), 6f))
        lines.add(Line3D(Point3D(r, 100f, 0f), Point3D(r + padW, 100f, 0f), Color(0xFFE2E8F0), 6f))
        lines.add(Line3D(Point3D(0f, 100f, -r), Point3D(0f, 100f, -r - padW), Color(0xFFE2E8F0), 6f))
        lines.add(Line3D(Point3D(0f, 100f, r), Point3D(0f, 100f, r + padW), Color(0xFFE2E8F0), 6f))

        return IsoMesh(lines, faces)
    }

    // 3. SS Mobile Tray Rack / Trolley (Image 3)
    private fun buildMobileTrayRackMesh(): IsoMesh {
        val lines = mutableListOf<Line3D>()
        val faces = mutableListOf<Face3D>()

        val w = 380f
        val d = 400f
        val h = 600f

        // 4 Uprights (Tubular Frame)
        val col = Color(0xFFE2E8F0)
        lines.add(Line3D(Point3D(-w * 0.5f, 0f, -d * 0.5f), Point3D(-w * 0.5f, h, -d * 0.5f), col, 4f))
        lines.add(Line3D(Point3D(w * 0.5f, 0f, -d * 0.5f), Point3D(w * 0.5f, h, -d * 0.5f), col, 4f))
        lines.add(Line3D(Point3D(w * 0.5f, 0f, d * 0.5f), Point3D(w * 0.5f, h, d * 0.5f), col, 4f))
        lines.add(Line3D(Point3D(-w * 0.5f, 0f, d * 0.5f), Point3D(-w * 0.5f, h, d * 0.5f), col, 4f))

        // Center upright divider
        lines.add(Line3D(Point3D(0f, 0f, -d * 0.5f), Point3D(0f, h, -d * 0.5f), col, 3f))
        lines.add(Line3D(Point3D(0f, 0f, d * 0.5f), Point3D(0f, h, d * 0.5f), col, 3f))

        // Top canopy & Bottom base sheet
        val t0 = Point3D(-w * 0.5f, h, -d * 0.5f)
        val t1 = Point3D(w * 0.5f, h, -d * 0.5f)
        val t2 = Point3D(w * 0.5f, h, d * 0.5f)
        val t3 = Point3D(-w * 0.5f, h, d * 0.5f)
        faces.add(Face3D(listOf(t0, t1, t2, t3), Color(0xFFCBD5E1)))

        val b0 = Point3D(-w * 0.5f, 0f, -d * 0.5f)
        val b1 = Point3D(w * 0.5f, 0f, -d * 0.5f)
        val b2 = Point3D(w * 0.5f, 0f, d * 0.5f)
        val b3 = Point3D(-w * 0.5f, 0f, d * 0.5f)
        faces.add(Face3D(listOf(b0, b1, b2, b3), Color(0xFF94A3B8)))

        // 12 Tier runners on both sides
        val numTiers = 12
        for (i in 0 until numTiers) {
            val y = 40f + i * (h - 80f) / numTiers
            // Left bay runners
            lines.add(Line3D(Point3D(-w * 0.5f, y, -d * 0.5f), Point3D(-w * 0.5f, y, d * 0.5f), Color(0xFF94A3B8), 2f))
            lines.add(Line3D(Point3D(0f, y, -d * 0.5f), Point3D(0f, y, d * 0.5f), Color(0xFF94A3B8), 2f))

            // Right bay runners
            lines.add(Line3D(Point3D(w * 0.5f, y, -d * 0.5f), Point3D(w * 0.5f, y, d * 0.5f), Color(0xFF94A3B8), 2f))
        }

        // Side 16" shelf
        val sW = 120f
        val sY1 = 200f
        val sY2 = 360f
        lines.add(Line3D(Point3D(-w * 0.5f, sY1, 0f), Point3D(-w * 0.5f - sW, sY1, 0f), col, 3f))
        lines.add(Line3D(Point3D(-w * 0.5f, sY2, 0f), Point3D(-w * 0.5f - sW, sY2, 0f), col, 3f))
        lines.add(Line3D(Point3D(-w * 0.5f - sW, sY1, 0f), Point3D(-w * 0.5f - sW, sY2, 0f), col, 3f))

        // Castor Wheels (4 wheels)
        val wheelColor = Color(0xFF334155)
        lines.add(Line3D(Point3D(-w * 0.5f, 0f, -d * 0.5f), Point3D(-w * 0.5f, -50f, -d * 0.5f), wheelColor, 6f))
        lines.add(Line3D(Point3D(w * 0.5f, 0f, -d * 0.5f), Point3D(w * 0.5f, -50f, -d * 0.5f), wheelColor, 6f))
        lines.add(Line3D(Point3D(w * 0.5f, 0f, d * 0.5f), Point3D(w * 0.5f, -50f, d * 0.5f), wheelColor, 6f))
        lines.add(Line3D(Point3D(-w * 0.5f, 0f, d * 0.5f), Point3D(-w * 0.5f, -50f, d * 0.5f), wheelColor, 6f))

        return IsoMesh(lines, faces)
    }

    // 4. SS Double Sink Table (Image 4)
    private fun buildDoubleSinkTableMesh(): IsoMesh {
        val lines = mutableListOf<Line3D>()
        val faces = mutableListOf<Face3D>()

        val w = 550f
        val d = 320f
        val h = 320f
        val splashH = 70f

        // Table Top Face
        val t0 = Point3D(-w * 0.5f, h, -d * 0.5f)
        val t1 = Point3D(w * 0.5f, h, -d * 0.5f)
        val t2 = Point3D(w * 0.5f, h, d * 0.5f)
        val t3 = Point3D(-w * 0.5f, h, d * 0.5f)
        faces.add(Face3D(listOf(t0, t1, t2, t3), Color(0xFFE2E8F0)))

        // Rear Splashback Upstand
        val sp0 = Point3D(-w * 0.5f, h + splashH, -d * 0.5f)
        val sp1 = Point3D(w * 0.5f, h + splashH, -d * 0.5f)
        faces.add(Face3D(listOf(sp0, sp1, t1, t0), Color(0xFFCBD5E1)))

        // 2 Deep Pressed Sinks
        val bW = 200f
        val bD = 200f
        val bDepth = 120f

        // Sink 1 (Left)
        val b1Cx = -w * 0.26f
        faces.add(Face3D(listOf(
            Point3D(b1Cx - bW * 0.5f, h - bDepth, -bD * 0.5f),
            Point3D(b1Cx + bW * 0.5f, h - bDepth, -bD * 0.5f),
            Point3D(b1Cx + bW * 0.5f, h - bDepth, bD * 0.5f),
            Point3D(b1Cx - bW * 0.5f, h - bDepth, bD * 0.5f)
        ), Color(0xFF64748B)))

        // Sink 2 (Right)
        val b2Cx = w * 0.26f
        faces.add(Face3D(listOf(
            Point3D(b2Cx - bW * 0.5f, h - bDepth, -bD * 0.5f),
            Point3D(b2Cx + bW * 0.5f, h - bDepth, -bD * 0.5f),
            Point3D(b2Cx + bW * 0.5f, h - bDepth, bD * 0.5f),
            Point3D(b2Cx - bW * 0.5f, h - bDepth, bD * 0.5f)
        ), Color(0xFF64748B)))

        // 4 Tubular Legs
        val legCol = Color(0xFFCBD5E1)
        lines.add(Line3D(Point3D(-w * 0.45f, h, -d * 0.45f), Point3D(-w * 0.45f, 0f, -d * 0.45f), legCol, 5f))
        lines.add(Line3D(Point3D(w * 0.45f, h, -d * 0.45f), Point3D(w * 0.45f, 0f, -d * 0.45f), legCol, 5f))
        lines.add(Line3D(Point3D(w * 0.45f, h, d * 0.45f), Point3D(w * 0.45f, 0f, d * 0.45f), legCol, 5f))
        lines.add(Line3D(Point3D(-w * 0.45f, h, d * 0.45f), Point3D(-w * 0.45f, 0f, d * 0.45f), legCol, 5f))

        // Under-frame Cross H-Bracing
        val brY = 80f
        lines.add(Line3D(Point3D(-w * 0.45f, brY, -d * 0.45f), Point3D(-w * 0.45f, brY, d * 0.45f), legCol, 3f))
        lines.add(Line3D(Point3D(w * 0.45f, brY, -d * 0.45f), Point3D(w * 0.45f, brY, d * 0.45f), legCol, 3f))
        lines.add(Line3D(Point3D(-w * 0.45f, brY, 0f), Point3D(w * 0.45f, brY, 0f), legCol, 3f))

        return IsoMesh(lines, faces)
    }

    // 5. SS Shoes & Apron Locker (Image 5)
    private fun buildLockerCabinetMesh(): IsoMesh {
        val lines = mutableListOf<Line3D>()
        val faces = mutableListOf<Face3D>()

        val w = 450f
        val d = 260f
        val h = 550f
        val slopeH = 70f

        // Locker Front Face
        faces.add(Face3D(listOf(
            Point3D(-w * 0.5f, h, d * 0.5f),
            Point3D(w * 0.5f, h, d * 0.5f),
            Point3D(w * 0.5f, 0f, d * 0.5f),
            Point3D(-w * 0.5f, 0f, d * 0.5f)
        ), Color(0xFFCBD5E1)))

        // Sloped Top Face (Slanted down towards front)
        faces.add(Face3D(listOf(
            Point3D(-w * 0.5f, h + slopeH, -d * 0.5f),
            Point3D(w * 0.5f, h + slopeH, -d * 0.5f),
            Point3D(w * 0.5f, h, d * 0.5f),
            Point3D(-w * 0.5f, h, d * 0.5f)
        ), Color(0xFFE2E8F0)))

        // Side Face
        faces.add(Face3D(listOf(
            Point3D(w * 0.5f, h + slopeH, -d * 0.5f),
            Point3D(w * 0.5f, 0f, -d * 0.5f),
            Point3D(w * 0.5f, 0f, d * 0.5f),
            Point3D(w * 0.5f, h, d * 0.5f)
        ), Color(0xFF94A3B8)))

        // 3 Door compartments divider lines on front
        val bayW = w / 3f
        lines.add(Line3D(Point3D(-w * 0.5f + bayW, 0f, d * 0.5f + 1f), Point3D(-w * 0.5f + bayW, h, d * 0.5f + 1f), Color(0xFF475569), 2f))
        lines.add(Line3D(Point3D(-w * 0.5f + bayW * 2f, 0f, d * 0.5f + 1f), Point3D(-w * 0.5f + bayW * 2f, h, d * 0.5f + 1f), Color(0xFF475569), 2f))

        // Apron / Shoe Divider line
        lines.add(Line3D(Point3D(-w * 0.5f, h * 0.35f, d * 0.5f + 1f), Point3D(w * 0.5f, h * 0.35f, d * 0.5f + 1f), Color(0xFF475569), 2f))

        // Skirting feet
        lines.add(Line3D(Point3D(-w * 0.45f, 0f, d * 0.5f), Point3D(-w * 0.45f, -40f, d * 0.5f), Color(0xFF334155), 6f))
        lines.add(Line3D(Point3D(w * 0.45f, 0f, d * 0.5f), Point3D(w * 0.45f, -40f, d * 0.5f), Color(0xFF334155), 6f))

        return IsoMesh(lines, faces)
    }
}
