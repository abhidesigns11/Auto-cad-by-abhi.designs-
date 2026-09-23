package com.example.ui.canvas

import android.graphics.Paint
import android.graphics.PathEffect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.CadEntityEditorDock
import com.example.ui.components.DimensionMeasurementDock
import com.example.ui.components.LineMeasurementDock
import kotlin.math.*

@Composable
fun CadCanvas2D(
    project: CadProject,
    currentTool: CadTool,
    isOrthoEnabled: Boolean,
    isSnapGridEnabled: Boolean,
    gridSizeMm: Float,
    canvasTheme: CanvasThemeMode,
    onEntityAdded: (CadEntity) -> Unit,
    onEntityUpdated: (CadEntity) -> Unit = {},
    onEntityDeleted: (String) -> Unit,
    onCoordinateChanged: (Float, Float) -> Unit,
    onMeasureResult: (Float, Float) -> Unit, // distance, angle
    modifier: Modifier = Modifier
) {
    // Viewport transform (World mm to Screen pixels)
    var panOffset by remember { mutableStateOf(Offset(200f, 200f)) }
    var zoomScale by remember { mutableFloatStateOf(0.45f) } // pixels per mm

    // Selection & Manipulation state
    var selectedEntityId by remember { mutableStateOf<String?>(null) }
    var dragInitialEntity by remember { mutableStateOf<CadEntity?>(null) }
    var dragRotateBaseAngle by remember { mutableFloatStateOf(0f) }

    // Active drawing temporary points
    var draftStartPoint by remember { mutableStateOf<Offset?>(null) }
    var currentCursorPoint by remember { mutableStateOf<Offset?>(null) }
    var stagedDimP1 by remember { mutableStateOf<Offset?>(null) }
    var stagedDimP2 by remember { mutableStateOf<Offset?>(null) }
    var activePolylinePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val bgColor = when (canvasTheme) {
        CanvasThemeMode.AUTOCAD_DARK -> Color(0xFF131822)
        CanvasThemeMode.ENGINEERING_WHITE -> Color(0xFFFBFBFB)
        CanvasThemeMode.BLUEPRINT_CYAN -> Color(0xFF091E3A)
    }

    val gridColorMinor = when (canvasTheme) {
        CanvasThemeMode.AUTOCAD_DARK -> Color(0x15FFFFFF)
        CanvasThemeMode.ENGINEERING_WHITE -> Color(0x18000000)
        CanvasThemeMode.BLUEPRINT_CYAN -> Color(0x2038BDF8)
    }

    val gridColorMajor = when (canvasTheme) {
        CanvasThemeMode.AUTOCAD_DARK -> Color(0x35FFFFFF)
        CanvasThemeMode.ENGINEERING_WHITE -> Color(0x30000000)
        CanvasThemeMode.BLUEPRINT_CYAN -> Color(0x4038BDF8)
    }

    // Helper functions: Screen <-> World Coordinate Transformations
    fun screenToWorld(screenOffset: Offset): Offset {
        val wx = (screenOffset.x - panOffset.x) / zoomScale
        val wy = (screenOffset.y - panOffset.y) / zoomScale
        return Offset(wx, wy)
    }

    fun worldToScreen(worldOffset: Offset): Offset {
        val sx = panOffset.x + worldOffset.x * zoomScale
        val sy = panOffset.y + worldOffset.y * zoomScale
        return Offset(sx, sy)
    }

    fun applySnappingAndOrtho(worldPt: Offset, originPt: Offset?): Offset {
        var x = worldPt.x
        var y = worldPt.y

        // Grid Snap
        if (isSnapGridEnabled && gridSizeMm > 0f) {
            x = (round(x / gridSizeMm) * gridSizeMm)
            y = (round(y / gridSizeMm) * gridSizeMm)
        }

        // Ortho mode lock relative to start point (0, 90, 180, 270 deg)
        if (isOrthoEnabled && originPt != null) {
            val dx = abs(x - originPt.x)
            val dy = abs(y - originPt.y)
            if (dx > dy) {
                y = originPt.y
            } else {
                x = originPt.x
            }
        }
        return Offset(x, y)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentTool) {
                if (currentTool == CadTool.PAN_ZOOM) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.08f, 5.0f)
                        panOffset += pan
                    }
                }
            }
            .pointerInput(currentTool, isOrthoEnabled, isSnapGridEnabled, gridSizeMm, project) {
                if (currentTool != CadTool.PAN_ZOOM) {
                    detectDragGestures(
                        onDragStart = { startScreenOffset ->
                            val rawWorld = screenToWorld(startScreenOffset)
                            val snappedWorld = applySnappingAndOrtho(rawWorld, null)
                            draftStartPoint = snappedWorld
                            currentCursorPoint = snappedWorld
                            onCoordinateChanged(snappedWorld.x, snappedWorld.y)

                            if (currentTool == CadTool.SELECT || currentTool == CadTool.ROTATE) {
                                val hit = findClosestEntity(snappedWorld, project.entities)
                                if (hit != null) {
                                    selectedEntityId = hit.id
                                    dragInitialEntity = hit
                                    dragRotateBaseAngle = 0f
                                } else {
                                    selectedEntityId = null
                                    dragInitialEntity = null
                                }
                            } else if (currentTool == CadTool.ERASE) {
                                // Find closest entity within threshold
                                val hitEntity = findClosestEntity(snappedWorld, project.entities)
                                if (hitEntity != null) {
                                    onEntityDeleted(hitEntity.id)
                                    if (selectedEntityId == hitEntity.id) selectedEntityId = null
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val rawWorld = screenToWorld(change.position)
                            val snappedWorld = applySnappingAndOrtho(rawWorld, draftStartPoint)
                            currentCursorPoint = snappedWorld
                            onCoordinateChanged(snappedWorld.x, snappedWorld.y)

                            if (currentTool == CadTool.SELECT && dragInitialEntity != null) {
                                val start = draftStartPoint ?: snappedWorld
                                val dx = snappedWorld.x - start.x
                                val dy = snappedWorld.y - start.y
                                val moved = CadTransformEngine.translateEntity(dragInitialEntity!!, dx, dy)
                                onEntityUpdated(moved)
                            } else if (currentTool == CadTool.ROTATE && dragInitialEntity != null) {
                                val start = draftStartPoint ?: snappedWorld
                                val center = CadTransformEngine.getEntityCenter(dragInitialEntity!!)
                                val a1 = (atan2(start.y - center.y, start.x - center.x) * 180.0 / PI).toFloat()
                                val a2 = (atan2(snappedWorld.y - center.y, snappedWorld.x - center.x) * 180.0 / PI).toFloat()
                                var deltaDeg = a2 - a1
                                if (isOrthoEnabled) {
                                    deltaDeg = (round(deltaDeg / 15f) * 15f)
                                }
                                dragRotateBaseAngle = deltaDeg
                                val rotated = CadTransformEngine.rotateEntity(dragInitialEntity!!, deltaDeg, center)
                                onEntityUpdated(rotated)
                            } else {
                                draftStartPoint?.let { start ->
                                    val dist = sqrt((snappedWorld.x - start.x).pow(2) + (snappedWorld.y - start.y).pow(2))
                                    val angleDeg = (atan2(snappedWorld.y - start.y, snappedWorld.x - start.x) * 180.0 / PI).toFloat()
                                    onMeasureResult(dist, angleDeg)
                                }
                            }
                        },
                        onDragEnd = {
                            if (dragInitialEntity != null) {
                                dragInitialEntity = project.entities.find { it.id == selectedEntityId }
                            }
                            val start = draftStartPoint
                            val end = currentCursorPoint

                            if (start != null && end != null) {
                                when (currentTool) {
                                    CadTool.LINE -> {
                                        if (distBetween(start, end) > 2f) {
                                            onEntityAdded(
                                                CadLine(x1 = start.x, y1 = start.y, x2 = end.x, y2 = end.y, layerId = "0")
                                            )
                                        }
                                    }
                                    CadTool.CENTERLINE -> {
                                        if (distBetween(start, end) > 2f) {
                                            onEntityAdded(
                                                CadLine(x1 = start.x, y1 = start.y, x2 = end.x, y2 = end.y, strokeType = StrokeType.CENTER_LINE, layerId = "CENTER")
                                            )
                                        }
                                    }
                                    CadTool.RECTANGLE -> {
                                        val minX = min(start.x, end.x)
                                        val minY = min(start.y, end.y)
                                        val width = abs(end.x - start.x)
                                        val height = abs(end.y - start.y)
                                        if (width > 2f && height > 2f) {
                                            onEntityAdded(
                                                CadRect(x = minX, y = minY, width = width, height = height, plateThicknessMm = 5f, layerId = "0")
                                            )
                                        }
                                    }
                                    CadTool.CIRCLE -> {
                                        val radius = distBetween(start, end)
                                        if (radius > 2f) {
                                            onEntityAdded(
                                                CadCircle(cx = start.x, cy = start.y, radius = radius, layerId = "0")
                                            )
                                        }
                                    }
                                    CadTool.DIMENSION -> {
                                        stagedDimP1 = start
                                        stagedDimP2 = end
                                        if (distBetween(start, end) > 4f) {
                                            val dx = abs(end.x - start.x)
                                            val dy = abs(end.y - start.y)
                                            val dimType = if (dy >= dx) DimensionType.LINEAR_VERTICAL else DimensionType.LINEAR_HORIZONTAL
                                            val measured = if (dimType == DimensionType.LINEAR_VERTICAL) dy else dx
                                            val offset = if (dimType == DimensionType.LINEAR_VERTICAL) -80f else -80f
                                            onEntityAdded(
                                                CadDimension(
                                                    type = dimType,
                                                    x1 = start.x,
                                                    y1 = start.y,
                                                    x2 = end.x,
                                                    y2 = end.y,
                                                    offsetDistance = offset,
                                                    textOverride = "%.0f".format(measured)
                                                )
                                            )
                                        }
                                    }
                                    CadTool.NOZZLE_STAMP -> {
                                        // Stamp a standard 50 NB / 2" flanged nozzle at tap
                                        onEntityAdded(
                                            CadNozzleComponent(
                                                cx = start.x,
                                                cy = start.y,
                                                sizeNb = 50,
                                                flangeDiaMm = 80f,
                                                neckLengthMm = 60f,
                                                label = "50 NB NOZZLE"
                                            )
                                        )
                                    }
                                    CadTool.CONE_HOPPER -> {
                                        val w = abs(end.x - start.x).coerceAtLeast(150f)
                                        val h = abs(end.y - start.y).coerceAtLeast(100f)
                                        onEntityAdded(
                                            CadHopperCone(
                                                topX = min(start.x, end.x),
                                                topY = min(start.y, end.y),
                                                topWidth = w,
                                                bottomWidth = w * 0.2f,
                                                height = h,
                                                wallThicknessMm = 5f
                                            )
                                        )
                                    }
                                    CadTool.LEADER_NOTE -> {
                                        onEntityAdded(
                                            CadLeader(
                                                targetX = start.x,
                                                targetY = start.y,
                                                elbowX = end.x,
                                                elbowY = end.y,
                                                text = "INLET NOZZLE - 1\""
                                            )
                                        )
                                    }
                                    CadTool.TEXT_NOTE -> {
                                        onEntityAdded(
                                            CadText(
                                                x = start.x,
                                                y = start.y,
                                                text = "PLATE THICKNESS - 5 MM",
                                                textHeightMm = 20f,
                                                isBold = true
                                            )
                                        )
                                    }
                                    else -> {}
                                }
                            }
                            draftStartPoint = null
                            currentCursorPoint = null
                        },
                        onDragCancel = {
                            draftStartPoint = null
                            currentCursorPoint = null
                        }
                    )
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Draw Background
            drawRect(bgColor)

            // 2. Draw CAD Grid
            drawCadGrid(
                canvasW = canvasW,
                canvasH = canvasH,
                panOffset = panOffset,
                zoomScale = zoomScale,
                minorGridMm = 20f,
                majorGridMm = 100f,
                minorColor = gridColorMinor,
                majorColor = gridColorMajor
            )

            // 3. Draw World Origin Axes (Red X, Green Y like AutoCAD)
            drawOriginAxes(panOffset, zoomScale)

            // 4. Draw Layer Entities
            val layerMap = project.layers.associateBy { it.id }

            for (entity in project.entities) {
                val layer = layerMap[entity.layerId] ?: project.layers.first()
                if (!layer.isVisible) continue

                val baseColor = if (canvasTheme == CanvasThemeMode.ENGINEERING_WHITE) {
                    when (entity.layerId) {
                        "DIM" -> Color(0xFFDC2626) // Red dimensions
                        "CENTER" -> Color(0xFF059669) // Green centerlines
                        "NOZZLE" -> Color(0xFF0284C7) // Cyan/Blue nozzles
                        "TEXT" -> Color(0xFFD97706) // Amber notes
                        else -> Color(0xFF0F172A) // Crisp black outline
                    }
                } else {
                    layer.composeColor
                }

                when (entity) {
                    is CadLine -> {
                        val s1 = worldToScreen(Offset(entity.x1, entity.y1))
                        val s2 = worldToScreen(Offset(entity.x2, entity.y2))
                        val strokeW = (entity.strokeWidthMm * zoomScale).coerceAtLeast(1.5f)
                        val stroke = when (entity.strokeType) {
                            StrokeType.CONTINUOUS -> Stroke(width = strokeW)
                            StrokeType.CENTER_LINE -> Stroke(
                                width = strokeW,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(30f, 10f, 8f, 10f), 0f)
                            )
                            StrokeType.DASHED_HIDDEN -> Stroke(
                                width = strokeW,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                            )
                        }
                        drawLine(color = baseColor, start = s1, end = s2, strokeWidth = strokeW, pathEffect = stroke.pathEffect)
                    }
                    is CadRect -> {
                        val sTopLeft = worldToScreen(Offset(entity.x, entity.y))
                        val sW = entity.width * zoomScale
                        val sH = entity.height * zoomScale
                        val strokeW = (1f * zoomScale).coerceIn(1.5f, 5f)
                        drawRect(
                            color = baseColor,
                            topLeft = sTopLeft,
                            size = Size(sW, sH),
                            style = Stroke(width = strokeW)
                        )
                        // If plate thickness indicated, draw inner offset
                        if (entity.plateThicknessMm > 0f) {
                            val innerThk = entity.plateThicknessMm * zoomScale
                            if (sW > innerThk * 2 && sH > innerThk * 2) {
                                drawRect(
                                    color = baseColor.copy(alpha = 0.6f),
                                    topLeft = Offset(sTopLeft.x + innerThk, sTopLeft.y + innerThk),
                                    size = Size(sW - innerThk * 2, sH - innerThk * 2),
                                    style = Stroke(width = 1f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                )
                            }
                        }
                    }
                    is CadCircle -> {
                        val sCenter = worldToScreen(Offset(entity.cx, entity.cy))
                        val sR = entity.radius * zoomScale
                        val strokeW = (1f * zoomScale).coerceIn(1.5f, 5f)
                        val pathEffect = if (entity.isPcd) androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 8f, 6f, 8f), 0f) else null
                        drawCircle(
                            color = baseColor,
                            radius = sR,
                            center = sCenter,
                            style = Stroke(width = strokeW, pathEffect = pathEffect)
                        )
                    }
                    is CadArc -> {
                        val sCenter = worldToScreen(Offset(entity.cx, entity.cy))
                        val sR = entity.radius * zoomScale
                        val strokeW = (1f * zoomScale).coerceIn(1.5f, 5f)
                        drawArc(
                            color = baseColor,
                            startAngle = entity.startAngleDeg,
                            sweepAngle = entity.sweepAngleDeg,
                            useCenter = false,
                            topLeft = Offset(sCenter.x - sR, sCenter.y - sR),
                            size = Size(sR * 2, sR * 2),
                            style = Stroke(width = strokeW)
                        )
                    }
                    is CadDimension -> {
                        drawCadDimension(
                            dim = entity,
                            worldToScreen = ::worldToScreen,
                            zoomScale = zoomScale,
                            color = if (canvasTheme == CanvasThemeMode.ENGINEERING_WHITE) Color(0xFFDC2626) else Color(0xFFEF4444)
                        )
                    }
                    is CadLeader -> {
                        drawCadLeader(
                            leader = entity,
                            worldToScreen = ::worldToScreen,
                            zoomScale = zoomScale,
                            color = if (canvasTheme == CanvasThemeMode.ENGINEERING_WHITE) Color(0xFF0F172A) else Color(0xFF38BDF8)
                        )
                    }
                    is CadText -> {
                        val sPos = worldToScreen(Offset(entity.x, entity.y))
                        val nativePaint = Paint().apply {
                            color = if (canvasTheme == CanvasThemeMode.ENGINEERING_WHITE) 0xFF0F172A.toInt() else 0xFFF59E0B.toInt()
                            textSize = (entity.textHeightMm * zoomScale).coerceAtLeast(14f)
                            isAntiAlias = true
                            isFakeBoldText = entity.isBold
                        }
                        drawContext.canvas.nativeCanvas.drawText(entity.text, sPos.x, sPos.y, nativePaint)
                    }
                    is CadNozzleComponent -> {
                        drawCadNozzle(
                            nozzle = entity,
                            worldToScreen = ::worldToScreen,
                            zoomScale = zoomScale,
                            color = if (canvasTheme == CanvasThemeMode.ENGINEERING_WHITE) Color(0xFF0284C7) else Color(0xFF38BDF8)
                        )
                    }
                    is CadHopperCone -> {
                        val s1 = worldToScreen(Offset(entity.topX, entity.topY))
                        val s2 = worldToScreen(Offset(entity.topX + entity.topWidth, entity.topY))
                        val bApexX = entity.topX + entity.topWidth * 0.5f
                        val bApexY = entity.topY + entity.height
                        val s3 = worldToScreen(Offset(bApexX + entity.bottomWidth * 0.5f, bApexY))
                        val s4 = worldToScreen(Offset(bApexX - entity.bottomWidth * 0.5f, bApexY))

                        drawLine(baseColor, s1, s4, strokeWidth = 2f)
                        drawLine(baseColor, s2, s3, strokeWidth = 2f)
                        drawLine(baseColor, s4, s3, strokeWidth = 2f)
                    }
                }
            }

            // 4b. Draw AutoCAD Grips & Transform Highlight for Selected Entity
            val selId = selectedEntityId
            if (selId != null) {
                val selected = project.entities.find { it.id == selId }
                if (selected != null) {
                    drawSelectedEntityGrips(
                        entity = selected,
                        worldToScreen = ::worldToScreen,
                        zoomScale = zoomScale,
                        isRotateMode = currentTool == CadTool.ROTATE,
                        currentAngleDeg = dragRotateBaseAngle
                    )
                }
            }

            // 5. Draw Active Tool Preview / Rubber-banding
            val dStart = draftStartPoint
            val dCurr = currentCursorPoint
            if (dStart != null && dCurr != null) {
                val sStart = worldToScreen(dStart)
                val sCurr = worldToScreen(dCurr)
                val previewColor = Color(0xFF38BDF8)

                when (currentTool) {
                    CadTool.LINE, CadTool.CENTERLINE, CadTool.MEASURE_TAPE -> {
                        drawLine(
                            color = previewColor,
                            start = sStart,
                            end = sCurr,
                            strokeWidth = 2.5f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                        )
                    }
                    CadTool.RECTANGLE -> {
                        val minX = min(sStart.x, sCurr.x)
                        val minY = min(sStart.y, sCurr.y)
                        val w = abs(sCurr.x - sStart.x)
                        val h = abs(sCurr.y - sStart.y)
                        drawRect(
                            color = previewColor,
                            topLeft = Offset(minX, minY),
                            size = Size(w, h),
                            style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        )
                    }
                    CadTool.CIRCLE -> {
                        val r = distBetween(sStart, sCurr)
                        drawCircle(
                            color = previewColor,
                            radius = r,
                            center = sStart,
                            style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                        )
                    }
                    CadTool.DIMENSION -> {
                        val dx = abs(dCurr.x - dStart.x)
                        val dy = abs(dCurr.y - dStart.y)
                        val previewType = if (dy >= dx) DimensionType.LINEAR_VERTICAL else DimensionType.LINEAR_HORIZONTAL
                        val previewDim = CadDimension(
                            type = previewType,
                            x1 = dStart.x,
                            y1 = dStart.y,
                            x2 = dCurr.x,
                            y2 = dCurr.y,
                            offsetDistance = -60f,
                            textOverride = "%.0f mm".format(if (previewType == DimensionType.LINEAR_VERTICAL) dy else dx)
                        )
                        drawCadDimension(previewDim, ::worldToScreen, zoomScale, Color(0xFFEF4444))
                    }
                    else -> {}
                }

                // Live dynamic measurement HUD near cursor
                val dist = distBetween(dStart, dCurr)
                val hudText = if (currentTool == CadTool.DIMENSION) {
                    val dx = abs(dCurr.x - dStart.x)
                    val dy = abs(dCurr.y - dStart.y)
                    if (dy >= dx) "SIZE: %.1f mm (Vertical)".format(dy) else "SIZE: %.1f mm (Horizontal)".format(dx)
                } else {
                    val angleDeg = (atan2(dCurr.y - dStart.y, dCurr.x - dStart.x) * 180.0 / PI).toFloat()
                    "LENGTH: %.1f mm | ANGLE: %.1f°".format(dist, angleDeg)
                }
                val hudPaint = Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 24f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val bgPaint = Paint().apply {
                    color = 0xEE0F172A.toInt()
                    style = Paint.Style.FILL
                }
                val borderPaint = Paint().apply {
                    color = if (currentTool == CadTool.DIMENSION) 0xFFEF4444.toInt() else 0xFF38BDF8.toInt()
                    style = Paint.Style.STROKE
                    strokeWidth = 3f
                }
                val hudX = sCurr.x + 20f
                val hudY = sCurr.y - 30f
                val textW = hudPaint.measureText(hudText)
                drawContext.canvas.nativeCanvas.drawRoundRect(hudX - 12f, hudY - 26f, hudX + textW + 12f, hudY + 10f, 8f, 8f, bgPaint)
                drawContext.canvas.nativeCanvas.drawRoundRect(hudX - 12f, hudY - 26f, hudX + textW + 12f, hudY + 10f, 8f, 8f, borderPaint)
                drawContext.canvas.nativeCanvas.drawText(hudText, hudX, hudY, hudPaint)
            }

            // 6. Draw Crosshair Cursor (AutoCAD Crosshair)
            val cur = currentCursorPoint
            if (cur != null) {
                val sCur = worldToScreen(cur)
                val crosshairColor = Color(0x9938BDF8)
                val boxSize = 14f

                // Horizontal crosshair line
                drawLine(crosshairColor, Offset(0f, sCur.y), Offset(canvasW, sCur.y), strokeWidth = 1f)
                // Vertical crosshair line
                drawLine(crosshairColor, Offset(sCur.x, 0f), Offset(sCur.x, canvasH), strokeWidth = 1f)
                // Center pickbox
                drawRect(
                    color = Color.White,
                    topLeft = Offset(sCur.x - boxSize * 0.5f, sCur.y - boxSize * 0.5f),
                    size = Size(boxSize, boxSize),
                    style = Stroke(width = 1.2f)
                )
            }
        }

        // 7. Interactive Docks for Entity Editing / Rotate, Line by Measurement and Show Size
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            val selected = project.entities.find { it.id == selectedEntityId }
            if (selected != null) {
                CadEntityEditorDock(
                    selectedEntity = selected,
                    onEntityUpdated = onEntityUpdated,
                    onEntityDeleted = { id ->
                        onEntityDeleted(id)
                        selectedEntityId = null
                    },
                    onEntityDuplicated = { dup ->
                        onEntityAdded(dup)
                        selectedEntityId = dup.id
                    },
                    onDeselect = { selectedEntityId = null }
                )
            } else if (currentTool == CadTool.LINE || currentTool == CadTool.CENTERLINE) {
                LineMeasurementDock(
                    currentStartX = draftStartPoint?.x ?: currentCursorPoint?.x ?: 1000f,
                    currentStartY = draftStartPoint?.y ?: currentCursorPoint?.y ?: 650f,
                    onAddLine = { lengthMm, angleDeg ->
                        val start = draftStartPoint ?: currentCursorPoint ?: Offset(1000f, 650f)
                        val rad = Math.toRadians(angleDeg.toDouble())
                        val endX = (start.x + lengthMm * Math.cos(rad)).toFloat()
                        val endY = (start.y + lengthMm * Math.sin(rad)).toFloat()
                        val layer = if (currentTool == CadTool.CENTERLINE) "CENTER" else "0"
                        val stroke = if (currentTool == CadTool.CENTERLINE) StrokeType.CENTER_LINE else StrokeType.CONTINUOUS

                        onEntityAdded(
                            CadLine(
                                x1 = start.x,
                                y1 = start.y,
                                x2 = endX,
                                y2 = endY,
                                strokeType = stroke,
                                layerId = layer
                            )
                        )
                        // Advance to chain next segment seamlessly
                        draftStartPoint = Offset(endX, endY)
                        currentCursorPoint = Offset(endX, endY)
                        onCoordinateChanged(endX, endY)
                    }
                )
            } else if (currentTool == CadTool.DIMENSION) {
                DimensionMeasurementDock(
                    startPoint = stagedDimP1 ?: draftStartPoint,
                    endPoint = stagedDimP2 ?: currentCursorPoint,
                    onPlaceDimension = { type, offsetDist, prefix ->
                        val p1 = stagedDimP1 ?: draftStartPoint
                        val p2 = stagedDimP2 ?: currentCursorPoint
                        if (p1 != null && p2 != null) {
                            val dx = abs(p2.x - p1.x)
                            val dy = abs(p2.y - p1.y)
                            val measured = when (type) {
                                DimensionType.LINEAR_VERTICAL -> dy
                                DimensionType.LINEAR_HORIZONTAL -> dx
                                else -> distBetween(p1, p2)
                            }
                            onEntityAdded(
                                CadDimension(
                                    type = type,
                                    x1 = p1.x,
                                    y1 = p1.y,
                                    x2 = p2.x,
                                    y2 = p2.y,
                                    offsetDistance = offsetDist,
                                    prefix = prefix,
                                    textOverride = "$prefix%.0f".format(measured)
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun DrawScope.drawCadGrid(
    canvasW: Float,
    canvasH: Float,
    panOffset: Offset,
    zoomScale: Float,
    minorGridMm: Float,
    majorGridMm: Float,
    minorColor: Color,
    majorColor: Color
) {
    val minorScreenStep = minorGridMm * zoomScale
    val majorScreenStep = majorGridMm * zoomScale

    if (minorScreenStep >= 8f) {
        val startX = (panOffset.x % minorScreenStep) - minorScreenStep
        var curX = startX
        while (curX <= canvasW + minorScreenStep) {
            drawLine(minorColor, Offset(curX, 0f), Offset(curX, canvasH), strokeWidth = 0.6f)
            curX += minorScreenStep
        }

        val startY = (panOffset.y % minorScreenStep) - minorScreenStep
        var curY = startY
        while (curY <= canvasH + minorScreenStep) {
            drawLine(minorColor, Offset(0f, curY), Offset(canvasW, curY), strokeWidth = 0.6f)
            curY += minorScreenStep
        }
    }

    if (majorScreenStep >= 20f) {
        val startX = (panOffset.x % majorScreenStep) - majorScreenStep
        var curX = startX
        while (curX <= canvasW + majorScreenStep) {
            drawLine(majorColor, Offset(curX, 0f), Offset(curX, canvasH), strokeWidth = 1.2f)
            curX += majorScreenStep
        }

        val startY = (panOffset.y % majorScreenStep) - majorScreenStep
        var curY = startY
        while (curY <= canvasH + majorScreenStep) {
            drawLine(majorColor, Offset(0f, curY), Offset(canvasW, curY), strokeWidth = 1.2f)
            curY += majorScreenStep
        }
    }
}

private fun DrawScope.drawOriginAxes(panOffset: Offset, zoomScale: Float) {
    val axisLength = 60f
    // X Axis (Red)
    drawLine(Color(0xFFEF4444), panOffset, Offset(panOffset.x + axisLength, panOffset.y), strokeWidth = 2.5f)
    // Y Axis (Green)
    drawLine(Color(0xFF10B981), panOffset, Offset(panOffset.x, panOffset.y - axisLength), strokeWidth = 2.5f)

    // Center Origin Marker
    drawCircle(Color.White, radius = 3.5f, center = panOffset)
}

private fun DrawScope.drawCadDimension(
    dim: CadDimension,
    worldToScreen: (Offset) -> Offset,
    zoomScale: Float,
    color: Color
) {
    val s1 = worldToScreen(Offset(dim.x1, dim.y1))
    val s2 = worldToScreen(Offset(dim.x2, dim.y2))
    val offsetPx = dim.offsetDistance * zoomScale
    val overshootPx = (8f * zoomScale).coerceIn(4f, 16f)

    val nativePaint = Paint().apply {
        this.color = color.hashCode()
        textSize = (13f * zoomScale).coerceIn(12f, 24f)
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    when (dim.type) {
        DimensionType.LINEAR_VERTICAL -> {
            val distMm = abs(dim.y2 - dim.y1)
            val displayText = dim.textOverride ?: "${dim.prefix}%.0f${dim.suffix}".format(distMm)

            val refX = if (dim.offsetDistance < 0) min(dim.x1, dim.x2) else max(dim.x1, dim.x2)
            val sDimX = worldToScreen(Offset(refX, 0f)).x + offsetPx
            val sMinY = min(s1.y, s2.y)
            val sMaxY = max(s1.y, s2.y)

            // Extension lines (Horizontal witness lines)
            val extOvershoot = if (dim.offsetDistance < 0) -overshootPx else overshootPx
            drawLine(color.copy(alpha = 0.55f), s1, Offset(sDimX + extOvershoot, s1.y), strokeWidth = 1.2f)
            drawLine(color.copy(alpha = 0.55f), s2, Offset(sDimX + extOvershoot, s2.y), strokeWidth = 1.2f)

            // Dimension line (Vertical)
            drawLine(color, Offset(sDimX, sMinY), Offset(sDimX, sMaxY), strokeWidth = 1.6f)

            // Arrowheads pointing towards ends
            drawArrowHead(Offset(sDimX, sMinY), Offset(sDimX, sMinY + 15f), color)
            drawArrowHead(Offset(sDimX, sMaxY), Offset(sDimX, sMaxY - 15f), color)

            // Dimension Text rotated -90 degrees along vertical line (matches blueprint standard)
            val midY = (sMinY + sMaxY) * 0.5f
            drawContext.canvas.nativeCanvas.save()
            drawContext.canvas.nativeCanvas.translate(sDimX - 8f, midY)
            drawContext.canvas.nativeCanvas.rotate(-90f)
            drawContext.canvas.nativeCanvas.drawText(displayText, 0f, 0f, nativePaint)
            drawContext.canvas.nativeCanvas.restore()
        }

        DimensionType.LINEAR_HORIZONTAL -> {
            val distMm = abs(dim.x2 - dim.x1)
            val displayText = dim.textOverride ?: "${dim.prefix}%.0f${dim.suffix}".format(distMm)

            val refY = if (dim.offsetDistance < 0) min(dim.y1, dim.y2) else max(dim.y1, dim.y2)
            val sDimY = worldToScreen(Offset(0f, refY)).y + offsetPx
            val sMinX = min(s1.x, s2.x)
            val sMaxX = max(s1.x, s2.x)

            // Extension lines (Vertical witness lines)
            val extOvershoot = if (dim.offsetDistance < 0) -overshootPx else overshootPx
            drawLine(color.copy(alpha = 0.55f), s1, Offset(s1.x, sDimY + extOvershoot), strokeWidth = 1.2f)
            drawLine(color.copy(alpha = 0.55f), s2, Offset(s2.x, sDimY + extOvershoot), strokeWidth = 1.2f)

            // Dimension line (Horizontal)
            drawLine(color, Offset(sMinX, sDimY), Offset(sMaxX, sDimY), strokeWidth = 1.6f)

            // Arrowheads pointing towards ends
            drawArrowHead(Offset(sMinX, sDimY), Offset(sMinX + 15f, sDimY), color)
            drawArrowHead(Offset(sMaxX, sDimY), Offset(sMaxX - 15f, sDimY), color)

            // Dimension Text centered above horizontal line
            val midX = (sMinX + sMaxX) * 0.5f
            drawContext.canvas.nativeCanvas.drawText(displayText, midX, sDimY - 7f, nativePaint)
        }

        else -> {
            // ALIGNED / DIAMETER
            val dx = s2.x - s1.x
            val dy = s2.y - s1.y
            val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
            val nx = -dy / len
            val ny = dx / len

            val dimP1 = Offset(s1.x + nx * offsetPx, s1.y + ny * offsetPx)
            val dimP2 = Offset(s2.x + nx * offsetPx, s2.y + ny * offsetPx)

            // Extension lines
            drawLine(color.copy(alpha = 0.55f), s1, Offset(dimP1.x + nx * overshootPx, dimP1.y + ny * overshootPx), strokeWidth = 1.2f)
            drawLine(color.copy(alpha = 0.55f), s2, Offset(dimP2.x + nx * overshootPx, dimP2.y + ny * overshootPx), strokeWidth = 1.2f)

            // Dimension Line
            drawLine(color, dimP1, dimP2, strokeWidth = 1.6f)

            // Arrowheads
            drawArrowHead(dimP1, dimP2, color)
            drawArrowHead(dimP2, dimP1, color)

            // Dimension Text
            val mid = Offset((dimP1.x + dimP2.x) * 0.5f, (dimP1.y + dimP2.y) * 0.5f)
            val displayText = dim.textOverride ?: "${dim.prefix}%.0f${dim.suffix}".format(len / zoomScale)
            drawContext.canvas.nativeCanvas.drawText(displayText, mid.x, mid.y - 7f, nativePaint)
        }
    }
}

private fun DrawScope.drawArrowHead(tip: Offset, from: Offset, color: Color) {
    val dx = from.x - tip.x
    val dy = from.y - tip.y
    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(0.001f)
    val uX = dx / len
    val uY = dy / len

    val arrowLength = 10f
    val arrowWidth = 4f

    val leftX = tip.x + uX * arrowLength - uY * arrowWidth
    val leftY = tip.y + uY * arrowLength + uX * arrowWidth

    val rightX = tip.x + uX * arrowLength + uY * arrowWidth
    val rightY = tip.y + uY * arrowLength - uX * arrowWidth

    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(leftX, leftY)
        lineTo(rightX, rightY)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawCadLeader(
    leader: CadLeader,
    worldToScreen: (Offset) -> Offset,
    zoomScale: Float,
    color: Color
) {
    val sTarget = worldToScreen(Offset(leader.targetX, leader.targetY))
    val sElbow = worldToScreen(Offset(leader.elbowX, leader.elbowY))
    val sEnd = Offset(sElbow.x + if (sElbow.x >= sTarget.x) 50f else -50f, sElbow.y)

    // Arrow on target
    drawLine(color, sTarget, sElbow, strokeWidth = 1.5f)
    drawLine(color, sElbow, sEnd, strokeWidth = 1.5f)
    drawArrowHead(sTarget, sElbow, color)

    val nativePaint = Paint().apply {
        this.color = color.hashCode()
        textSize = 12f
        isAntiAlias = true
        isFakeBoldText = true
        textAlign = if (sElbow.x >= sTarget.x) Paint.Align.LEFT else Paint.Align.RIGHT
    }
    val textX = if (sElbow.x >= sTarget.x) sEnd.x + 6f else sEnd.x - 6f
    drawContext.canvas.nativeCanvas.drawText(leader.text, textX, sEnd.y + 4f, nativePaint)
}

private fun DrawScope.drawCadNozzle(
    nozzle: CadNozzleComponent,
    worldToScreen: (Offset) -> Offset,
    zoomScale: Float,
    color: Color
) {
    val sCenter = worldToScreen(Offset(nozzle.cx, nozzle.cy))
    val sFlangeR = (nozzle.flangeDiaMm * 0.5f) * zoomScale
    val sNeckR = sFlangeR * 0.6f

    // Flange Circle
    drawCircle(color, radius = sFlangeR, center = sCenter, style = Stroke(width = 2f))
    // Pipe Bore Circle
    drawCircle(color.copy(alpha = 0.7f), radius = sNeckR, center = sCenter, style = Stroke(width = 1.5f))
    // Center Mark cross
    drawLine(color.copy(alpha = 0.5f), Offset(sCenter.x - sFlangeR - 10f, sCenter.y), Offset(sCenter.x + sFlangeR + 10f, sCenter.y), strokeWidth = 1f)
    drawLine(color.copy(alpha = 0.5f), Offset(sCenter.x, sCenter.y - sFlangeR - 10f), Offset(sCenter.x, sCenter.y + sFlangeR + 10f), strokeWidth = 1f)
}

private fun distBetween(p1: Offset, p2: Offset): Float {
    return sqrt((p2.x - p1.x).pow(2) + (p2.y - p1.y).pow(2))
}

private fun findClosestEntity(point: Offset, entities: List<CadEntity>): CadEntity? {
    // Quick proximity hit test within 35mm
    val threshold = 35f
    for (entity in entities.reversed()) {
        when (entity) {
            is CadLine -> {
                val d = distToSegment(point, Offset(entity.x1, entity.y1), Offset(entity.x2, entity.y2))
                if (d <= threshold) return entity
            }
            is CadRect -> {
                if (point.x in entity.x..(entity.x + entity.width) &&
                    point.y in entity.y..(entity.y + entity.height)) {
                    return entity
                }
            }
            is CadCircle -> {
                val d = distBetween(point, Offset(entity.cx, entity.cy))
                if (abs(d - entity.radius) <= threshold || d <= entity.radius) return entity
            }
            is CadDimension -> {
                val d = distToSegment(point, Offset(entity.x1, entity.y1), Offset(entity.x2, entity.y2))
                if (d <= threshold) return entity
            }
            is CadLeader -> {
                if (distBetween(point, Offset(entity.targetX, entity.targetY)) <= threshold ||
                    distBetween(point, Offset(entity.elbowX, entity.elbowY)) <= threshold) return entity
            }
            is CadText -> {
                if (distBetween(point, Offset(entity.x, entity.y)) <= threshold) return entity
            }
            is CadNozzleComponent -> {
                if (distBetween(point, Offset(entity.cx, entity.cy)) <= entity.flangeDiaMm) return entity
            }
            is CadHopperCone -> {
                if (point.x in entity.topX..(entity.topX + entity.topWidth) &&
                    point.y in entity.topY..(entity.topY + entity.height)) return entity
            }
            else -> {}
        }
    }
    return null
}

private fun distToSegment(p: Offset, v: Offset, w: Offset): Float {
    val l2 = (w.x - v.x).pow(2) + (w.y - v.y).pow(2)
    if (l2 == 0f) return distBetween(p, v)
    val t = (((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2).coerceIn(0f, 1f)
    val proj = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
    return distBetween(p, proj)
}

private fun DrawScope.drawSelectedEntityGrips(
    entity: CadEntity,
    worldToScreen: (Offset) -> Offset,
    zoomScale: Float,
    isRotateMode: Boolean,
    currentAngleDeg: Float
) {
    val gripBlue = Color(0xFF0284C7)
    val gripGold = Color(0xFFF59E0B)
    val boxColor = Color(0xFFF59E0B)
    val gripSize = 12f

    fun drawSquareGrip(center: Offset, color: Color = gripBlue) {
        drawRect(
            color = color,
            topLeft = Offset(center.x - gripSize * 0.5f, center.y - gripSize * 0.5f),
            size = Size(gripSize, gripSize)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(center.x - gripSize * 0.5f, center.y - gripSize * 0.5f),
            size = Size(gripSize, gripSize),
            style = Stroke(width = 1.2f)
        )
    }

    fun drawCenterGrip(center: Offset) {
        drawCircle(color = gripGold, radius = 6f, center = center)
        drawCircle(color = Color.White, radius = 6f, center = center, style = Stroke(width = 1.2f))
    }

    when (entity) {
        is CadLine -> {
            val s1 = worldToScreen(Offset(entity.x1, entity.y1))
            val s2 = worldToScreen(Offset(entity.x2, entity.y2))
            val smid = Offset((s1.x + s2.x) * 0.5f, (s1.y + s2.y) * 0.5f)

            // Bounding dashed box
            val minX = min(s1.x, s2.x) - 10f
            val maxX = max(s1.x, s2.x) + 10f
            val minY = min(s1.y, s2.y) - 10f
            val maxY = max(s1.y, s2.y) + 10f
            drawRect(
                color = boxColor.copy(alpha = 0.6f),
                topLeft = Offset(minX, minY),
                size = Size(maxX - minX, maxY - minY),
                style = Stroke(width = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
            )

            drawSquareGrip(s1)
            drawSquareGrip(s2)
            drawSquareGrip(smid, Color(0xFF38BDF8))
            drawCenterGrip(smid)

            if (isRotateMode) {
                drawRotateCompass(smid, currentAngleDeg)
            }
        }
        is CadCircle -> {
            val sc = worldToScreen(Offset(entity.cx, entity.cy))
            val sr = entity.radius * zoomScale
            drawSquareGrip(Offset(sc.x, sc.y - sr))
            drawSquareGrip(Offset(sc.x, sc.y + sr))
            drawSquareGrip(Offset(sc.x - sr, sc.y))
            drawSquareGrip(Offset(sc.x + sr, sc.y))
            drawCenterGrip(sc)

            drawRect(
                color = boxColor.copy(alpha = 0.6f),
                topLeft = Offset(sc.x - sr - 8f, sc.y - sr - 8f),
                size = Size((sr + 8f) * 2f, (sr + 8f) * 2f),
                style = Stroke(width = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
            )

            if (isRotateMode) {
                drawRotateCompass(sc, currentAngleDeg)
            }
        }
        is CadRect -> {
            val sTopLeft = worldToScreen(Offset(entity.x, entity.y))
            val sw = entity.width * zoomScale
            val sh = entity.height * zoomScale
            val sc = Offset(sTopLeft.x + sw * 0.5f, sTopLeft.y + sh * 0.5f)

            drawRect(
                color = boxColor.copy(alpha = 0.6f),
                topLeft = Offset(sTopLeft.x - 6f, sTopLeft.y - 6f),
                size = Size(sw + 12f, sh + 12f),
                style = Stroke(width = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
            )

            drawSquareGrip(sTopLeft)
            drawSquareGrip(Offset(sTopLeft.x + sw, sTopLeft.y))
            drawSquareGrip(Offset(sTopLeft.x, sTopLeft.y + sh))
            drawSquareGrip(Offset(sTopLeft.x + sw, sTopLeft.y + sh))
            drawCenterGrip(sc)

            if (isRotateMode) {
                drawRotateCompass(sc, currentAngleDeg)
            }
        }
        is CadDimension -> {
            val s1 = worldToScreen(Offset(entity.x1, entity.y1))
            val s2 = worldToScreen(Offset(entity.x2, entity.y2))
            val sc = Offset((s1.x + s2.x) * 0.5f, (s1.y + s2.y) * 0.5f)
            drawSquareGrip(s1)
            drawSquareGrip(s2)
            drawCenterGrip(sc)

            if (isRotateMode) {
                drawRotateCompass(sc, currentAngleDeg)
            }
        }
        else -> {
            val center = CadTransformEngine.getEntityCenter(entity)
            val sc = worldToScreen(center)
            drawCenterGrip(sc)
            if (isRotateMode) {
                drawRotateCompass(sc, currentAngleDeg)
            }
        }
    }
}

private fun DrawScope.drawRotateCompass(center: Offset, angleDeg: Float) {
    val compassRadius = 55f
    drawCircle(
        color = Color(0xFFF59E0B),
        radius = compassRadius,
        center = center,
        style = Stroke(width = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
    )
    val rad = Math.toRadians(angleDeg.toDouble())
    val pointerEnd = Offset(
        (center.x + compassRadius * cos(rad)).toFloat(),
        (center.y + compassRadius * sin(rad)).toFloat()
    )
    drawLine(Color(0xFFF59E0B), center, pointerEnd, strokeWidth = 2.5f)
    drawCircle(Color.White, radius = 5f, center = pointerEnd)

    val nativePaint = Paint().apply {
        color = 0xFFF59E0B.toInt()
        textSize = 20f
        isFakeBoldText = true
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    drawContext.canvas.nativeCanvas.drawText("%.1f°".format(angleDeg), center.x, center.y - compassRadius - 8f, nativePaint)
}
