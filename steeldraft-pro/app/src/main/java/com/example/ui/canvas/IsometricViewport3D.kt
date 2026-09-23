package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlin.math.max

enum class IsoRenderMode {
    SHADED_STAINLESS_STEEL,
    CAD_WIREFRAME,
    HIDDEN_LINE_REMOVED
}

enum class IsoViewSource {
    AUTO_FROM_2D_DRAWING,
    PRE_ENGINEERED_MODEL
}

@Composable
fun IsometricViewport3D(
    modelType: IsometricModelType,
    canvasTheme: CanvasThemeMode,
    project: CadProject? = null,
    modifier: Modifier = Modifier
) {
    var viewSource by remember {
        mutableStateOf(
            if (project != null && project.entities.isNotEmpty()) IsoViewSource.AUTO_FROM_2D_DRAWING
            else IsoViewSource.PRE_ENGINEERED_MODEL
        )
    }
    var selectedEquipmentType by remember(modelType) { mutableStateOf(modelType) }
    var extrudeDepthMm by remember { mutableFloatStateOf(300f) }

    var yaw by remember { mutableFloatStateOf(45f) }
    var pitch by remember { mutableFloatStateOf(30f) }
    var zoomScale by remember { mutableFloatStateOf(0.75f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    var renderMode by remember { mutableStateOf(IsoRenderMode.SHADED_STAINLESS_STEEL) }

    val mesh = remember(viewSource, selectedEquipmentType, project?.entities, extrudeDepthMm) {
        if (viewSource == IsoViewSource.AUTO_FROM_2D_DRAWING && project != null) {
            IsometricEngine.buildMeshFrom2DEntities(project.entities, extrudeDepthMm)
        } else {
            IsometricEngine.buildModelMesh(selectedEquipmentType)
        }
    }

    val bgColor = when (canvasTheme) {
        CanvasThemeMode.AUTOCAD_DARK -> Color(0xFF0F172A)
        CanvasThemeMode.ENGINEERING_WHITE -> Color(0xFFF8FAFC)
        CanvasThemeMode.BLUEPRINT_CYAN -> Color(0xFF0A2240)
    }

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("isometric_3d_canvas")
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.2f, 3.5f)
                        panOffset += pan
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yaw = (yaw + dragAmount.x * 0.5f) % 360f
                        pitch = (pitch - dragAmount.y * 0.5f).coerceIn(-85f, 85f)
                    }
                }
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val centerX = canvasW * 0.5f + panOffset.x
            val centerY = canvasH * 0.5f + panOffset.y

            // Draw 3D Ground Grid (Isometric Stainless Steel Grid)
            draw3dGroundGrid(
                centerX = centerX,
                centerY = centerY,
                yaw = yaw,
                pitch = pitch,
                scale = zoomScale,
                gridColor = if (canvasTheme == CanvasThemeMode.AUTOCAD_DARK) Color(0x2238BDF8) else Color(0x220F172A)
            )

            // Render Mesh Faces with Painter's Algorithm (Back-to-Front depth sorting)
            if (renderMode != IsoRenderMode.CAD_WIREFRAME) {
                // Rotate vertices for all faces and compute depth
                val rotatedFaces = mesh.faces.map { face ->
                    val rotVerts = face.vertices.map { v -> IsometricEngine.rotatePoint(v, yaw, pitch) }
                    val avgZ = rotVerts.map { it.z }.average().toFloat()
                    val normal = face.calculateNormal()
                    val rotNormal = IsometricEngine.rotatePoint(normal, yaw, pitch)
                    Triple(face, rotVerts, Pair(avgZ, rotNormal))
                }.sortedBy { it.third.first } // sort ascending Z (deepest drawn first)

                val lightDir = Point3D(0.5f, 0.7f, 0.4f)

                for ((face, rotVerts, depthAndNormal) in rotatedFaces) {
                    val rotNormal = depthAndNormal.second
                    // Simple dot product lighting for metallic stainless steel sheen
                    val dot = max(0.25f, (rotNormal.x * lightDir.x + rotNormal.y * lightDir.y + rotNormal.z * lightDir.z))

                    val r = ((face.baseColor.red * dot * 1.15f).coerceIn(0f, 1f))
                    val g = ((face.baseColor.green * dot * 1.18f).coerceIn(0f, 1f))
                    val b = ((face.baseColor.blue * dot * 1.25f).coerceIn(0f, 1f))
                    val shadedColor = Color(r, g, b, 0.95f)

                    val screenPoints = rotVerts.map { IsometricEngine.projectToScreen(it, centerX, centerY, zoomScale) }

                    if (screenPoints.size >= 3) {
                        val path = Path().apply {
                            moveTo(screenPoints[0].x, screenPoints[0].y)
                            for (i in 1 until screenPoints.size) {
                                lineTo(screenPoints[i].x, screenPoints[i].y)
                            }
                            close()
                        }
                        drawPath(path, shadedColor)
                        // Subtle edge highlight
                        drawPath(
                            path,
                            if (canvasTheme == CanvasThemeMode.ENGINEERING_WHITE) Color(0xFF64748B) else Color(0x66FFFFFF),
                            style = Stroke(width = 1.2f)
                        )
                    }
                }
            }

            // Render 3D Lines (Pipes, Flanges, Runners, Nozzles)
            for (line in mesh.lines) {
                val rot1 = IsometricEngine.rotatePoint(line.p1, yaw, pitch)
                val rot2 = IsometricEngine.rotatePoint(line.p2, yaw, pitch)

                val s1 = IsometricEngine.projectToScreen(rot1, centerX, centerY, zoomScale)
                val s2 = IsometricEngine.projectToScreen(rot2, centerX, centerY, zoomScale)

                val lineColor = if (canvasTheme == CanvasThemeMode.ENGINEERING_WHITE && line.color == Color(0xFFE2E8F0)) {
                    Color(0xFF0F172A)
                } else line.color

                drawLine(
                    color = lineColor,
                    start = s1,
                    end = s2,
                    strokeWidth = line.strokeWidth * zoomScale.coerceAtLeast(0.7f)
                )
            }
        }

        // Top Controls: Mode Switcher (Auto From 2D vs Pre-Engineered Equipment)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = viewSource == IsoViewSource.AUTO_FROM_2D_DRAWING,
                    onClick = { viewSource = IsoViewSource.AUTO_FROM_2D_DRAWING },
                    label = {
                        Text(
                            text = "⚡ Auto 3D (From 2D)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("iso_mode_from_2d_chip")
                )

                FilterChip(
                    selected = viewSource == IsoViewSource.PRE_ENGINEERED_MODEL,
                    onClick = { viewSource = IsoViewSource.PRE_ENGINEERED_MODEL },
                    label = {
                        Text(
                            text = "🏭 Equipment 3D Models",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.testTag("iso_mode_pre_engineered_chip")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Secondary row: Extrude depth OR Equipment selector
            if (viewSource == IsoViewSource.AUTO_FROM_2D_DRAWING) {
                Surface(
                    color = Color(0xDD0F172A),
                    shape = MaterialTheme.shapes.small,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("3D Depth:", fontSize = 10.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        listOf(100f, 250f, 400f, 600f, 1000f).forEach { depth ->
                            Surface(
                                onClick = { extrudeDepthMm = depth },
                                color = if (extrudeDepthMm == depth) Color(0xFF0284C7) else Color(0xFF1E293B),
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Text(
                                    text = "${depth.toInt()}mm",
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    IsometricModelType.values().filter { it != IsometricModelType.CUSTOM_3D_GENERATED }.forEach { equip ->
                        val label = when (equip) {
                            IsometricModelType.MBBR_STP_TANK -> "MBBR Tank"
                            IsometricModelType.SS_CYCLONE_HOPPER -> "Cyclone"
                            IsometricModelType.SS_MOBILE_TRAY_RACK -> "Tray Rack"
                            IsometricModelType.SS_DOUBLE_SINK_TABLE -> "Sink Table"
                            IsometricModelType.SS_LOCKER_CABINET -> "Locker"
                            else -> equip.name
                        }
                        Surface(
                            onClick = { selectedEquipmentType = equip },
                            color = if (selectedEquipmentType == equip) Color(0xFFF59E0B) else Color(0xCC1E293B),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = label,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Camera & Dimension Overlays HUD (AutoCAD 3D Viewport Controls)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Surface(
                color = Color(0xCC0F172A),
                shape = MaterialTheme.shapes.small,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = if (viewSource == IsoViewSource.AUTO_FROM_2D_DRAWING) "3D FROM 2D BLUEPRINT (${project?.entities?.size ?: 0} parts)" else "3D EQUIPMENT CAD MODEL",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Orbit: Yaw %.1f° | Pitch %.1f°".format(yaw, pitch),
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Scale: %.1f mm/px | 1-Finger Orbit, 2-Finger Zoom".format(zoomScale),
                        color = Color(0xFF94A3B8),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick View Presets (Isometric, Top, Front, Right)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { yaw = 45f; pitch = 30f },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD1E293B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("ISO", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { yaw = 0f; pitch = 0f },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD1E293B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("FRONT", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { yaw = 0f; pitch = 85f },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD1E293B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("TOP", fontSize = 10.sp, color = Color.White)
                }
                Button(
                    onClick = { yaw = 90f; pitch = 0f },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xDD1E293B)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("RIGHT", fontSize = 10.sp, color = Color.White)
                }
            }
        }

        // Render Mode Toggle (Shaded SS vs Wireframe)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = renderMode == IsoRenderMode.SHADED_STAINLESS_STEEL,
                onClick = { renderMode = IsoRenderMode.SHADED_STAINLESS_STEEL },
                label = { Text("SS Shaded", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = renderMode == IsoRenderMode.CAD_WIREFRAME,
                onClick = { renderMode = IsoRenderMode.CAD_WIREFRAME },
                label = { Text("Wireframe", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
            IconButton(
                onClick = { yaw = 45f; pitch = 30f; zoomScale = 0.75f; panOffset = Offset.Zero },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xDD1E293B), MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset View",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun DrawScope.draw3dGroundGrid(
    centerX: Float,
    centerY: Float,
    yaw: Float,
    pitch: Float,
    scale: Float,
    gridColor: Color
) {
    val gridSize = 600f
    val step = 100f

    var x = -gridSize
    while (x <= gridSize) {
        val p1 = IsometricEngine.rotatePoint(Point3D(x, 0f, -gridSize), yaw, pitch)
        val p2 = IsometricEngine.rotatePoint(Point3D(x, 0f, gridSize), yaw, pitch)
        val s1 = IsometricEngine.projectToScreen(p1, centerX, centerY, scale)
        val s2 = IsometricEngine.projectToScreen(p2, centerX, centerY, scale)
        drawLine(gridColor, s1, s2, strokeWidth = 0.8f)
        x += step
    }

    var z = -gridSize
    while (z <= gridSize) {
        val p1 = IsometricEngine.rotatePoint(Point3D(-gridSize, 0f, z), yaw, pitch)
        val p2 = IsometricEngine.rotatePoint(Point3D(gridSize, 0f, z), yaw, pitch)
        val s1 = IsometricEngine.projectToScreen(p1, centerX, centerY, scale)
        val s2 = IsometricEngine.projectToScreen(p2, centerX, centerY, scale)
        drawLine(gridColor, s1, s2, strokeWidth = 0.8f)
        z += step
    }
}
