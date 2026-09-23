package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*

@Composable
fun Manual3DSolidBuilderViewport(
    modifier: Modifier = Modifier
) {
    // List of 3D parts combined together (CSG / Solid Modeler)
    var solidParts by remember {
        mutableStateOf(
            listOf(
                Solid3DPart(
                    name = "Tank Body / Base",
                    shapeType = Solid3DShapeType.CYLINDER,
                    booleanOp = Solid3DBooleanOp.UNION,
                    posX = 0f,
                    posY = 0f,
                    posZ = 0f,
                    dimX = 300f,
                    dimY = 400f,
                    dimZ = 300f,
                    color = Color(0xFF94A3B8)
                ),
                Solid3DPart(
                    name = "Bottom Discharge Cone",
                    shapeType = Solid3DShapeType.CONE,
                    booleanOp = Solid3DBooleanOp.UNION,
                    posX = 0f,
                    posY = -300f,
                    posZ = 0f,
                    dimX = 300f,
                    dimY = 200f,
                    dimZ = 80f,
                    color = Color(0xFFCBD5E1)
                ),
                Solid3DPart(
                    name = "Manhole Cutout (-)",
                    shapeType = Solid3DShapeType.CYLINDER,
                    booleanOp = Solid3DBooleanOp.SUBTRACT,
                    posX = 120f,
                    posY = 50f,
                    posZ = 0f,
                    dimX = 100f,
                    dimY = 120f,
                    dimZ = 100f,
                    color = Color(0x99EF4444)
                )
            )
        )
    }

    var selectedPartId by remember { mutableStateOf<String?>(solidParts.firstOrNull()?.id) }
    val selectedPart = solidParts.find { it.id == selectedPartId }

    // 3D Viewport Navigation Orbit Controls
    var yaw by remember { mutableFloatStateOf(45f) }
    var pitch by remember { mutableFloatStateOf(30f) }
    var zoomScale by remember { mutableFloatStateOf(0.7f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Dialog state for adding new shape
    var showAddShapeDialog by remember { mutableStateOf(false) }

    // Compute 3D Mesh dynamically
    val mesh = remember(solidParts) {
        Manual3DSolidEngine.generateMeshFromParts(solidParts)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090D16))
    ) {
        // 1. Interactive 3D Canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.15f, 4.0f)
                        panOffset += pan
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        yaw += dragAmount.x * 0.45f
                        pitch = (pitch - dragAmount.y * 0.45f).coerceIn(-89f, 89f)
                    }
                }
        ) {
            val cx = size.width * 0.5f + panOffset.x
            val cy = size.height * 0.5f + panOffset.y

            // Render Faces with Depth Sorting (Painter's Algorithm)
            val rotatedFaces = mesh.faces.map { face ->
                val rotatedVertices = face.vertices.map { v ->
                    IsometricEngine.rotatePoint(v, yaw, pitch)
                }
                val depth = face.centerDepth(rotatedVertices)
                Triple(face, rotatedVertices, depth)
            }.sortedBy { it.third }

            // Draw Faces
            for ((face, rVerts, _) in rotatedFaces) {
                if (rVerts.size >= 3) {
                    val p = Path()
                    val p0 = IsometricEngine.projectToScreen(rVerts[0], cx, cy, zoomScale)
                    p.moveTo(p0.x, p0.y)
                    for (i in 1 until rVerts.size) {
                        val pi = IsometricEngine.projectToScreen(rVerts[i], cx, cy, zoomScale)
                        p.lineTo(pi.x, pi.y)
                    }
                    p.close()
                    drawPath(path = p, color = face.baseColor)
                }
            }

            // Draw 3D Lines & Outline Edges
            for (line in mesh.lines) {
                val r1 = IsometricEngine.rotatePoint(line.p1, yaw, pitch)
                val r2 = IsometricEngine.rotatePoint(line.p2, yaw, pitch)
                val p1 = IsometricEngine.projectToScreen(r1, cx, cy, zoomScale)
                val p2 = IsometricEngine.projectToScreen(r2, cx, cy, zoomScale)
                drawLine(
                    color = line.color,
                    start = p1,
                    end = p2,
                    strokeWidth = line.strokeWidth * zoomScale.coerceAtLeast(0.6f)
                )
            }
        }

        // 2. Top Bar: 3D CSG Builder Header & Quick Presets
        Surface(
            color = Color(0xDD0F172A),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ViewInAr, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("3D SOLID MODELER & CSG SHAPE MAKER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text("Build custom shapes by adding, combining, or carving out parts (like Logo Maker)", color = Color(0xFF94A3B8), fontSize = 9.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { showAddShapeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp).testTag("btn_add_3d_shape")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Shape", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            yaw = 45f
                            pitch = 30f
                            zoomScale = 0.7f
                            panOffset = Offset.Zero
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset View", tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // 3. Bottom Dock: Shapes / Layers Stack & Selected Part Transform Editor
        Surface(
            color = Color(0xF00F172A),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Horizontal list of 3D parts in model
                Text(
                    text = "3D SHAPES & BOOLEAN OPERATIONS (${solidParts.size} parts):",
                    color = Color(0xFFF59E0B),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(solidParts) { part ->
                        val isSel = part.id == selectedPartId
                        Card(
                            modifier = Modifier
                                .clickable { selectedPartId = part.id }
                                .border(
                                    width = if (isSel) 2.dp else 1.dp,
                                    color = if (isSel) Color(0xFF38BDF8) else Color(0xFF475569),
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSel) Color(0xFF1E293B) else Color(0xFF0F172A)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(part.booleanOp.symbol, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(part.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("${part.shapeType.label} • ${part.booleanOp.label}", color = if (part.booleanOp == Solid3DBooleanOp.SUBTRACT) Color(0xFFEF4444) else Color(0xFF38BDF8), fontSize = 8.sp)
                                }
                                if (isSel && solidParts.size > 1) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            solidParts = solidParts.filter { it.id != part.id }
                                            selectedPartId = solidParts.firstOrNull()?.id
                                        },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // If a part is selected, show its 3D Position & Size Sliders
                if (selectedPart != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color(0xFF1E293B))
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Transform '${selectedPart.name}':", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        // Boolean Op Selector Chips
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Solid3DBooleanOp.values().forEach { op ->
                                FilterChip(
                                    selected = selectedPart.booleanOp == op,
                                    onClick = {
                                        solidParts = solidParts.map { if (it.id == selectedPart.id) it.copy(booleanOp = op) else it }
                                    },
                                    label = { Text(op.label, fontSize = 8.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (op == Solid3DBooleanOp.SUBTRACT) Color(0xFFDC2626) else Color(0xFF0284C7),
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.height(26.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Position X & Y quick adjustments
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pos Y: ${selectedPart.posY.toInt()}mm", color = Color(0xFF94A3B8), fontSize = 10.sp, modifier = Modifier.width(80.dp))
                        Slider(
                            value = selectedPart.posY,
                            onValueChange = { newY ->
                                solidParts = solidParts.map { if (it.id == selectedPart.id) it.copy(posY = newY) else it }
                            },
                            valueRange = -500f..500f,
                            modifier = Modifier.weight(1f).height(24.dp)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    solidParts = solidParts.map { if (it.id == selectedPart.id) it.copy(dimX = (it.dimX * 1.15f).coerceAtMost(1000f)) else it }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Size +", fontSize = 9.sp)
                            }
                            FilledTonalButton(
                                onClick = {
                                    solidParts = solidParts.map { if (it.id == selectedPart.id) it.copy(dimX = (it.dimX * 0.85f).coerceAtLeast(30f)) else it }
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Size -", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }

        // Add Shape Dialog
        if (showAddShapeDialog) {
            AlertDialog(
                onDismissRequest = { showAddShapeDialog = false },
                title = { Text("Add 3D Shape to Assembly", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Select primitive shape to add or use as cutout:", fontSize = 11.sp, color = Color(0xFF64748B))

                        Solid3DShapeType.values().forEach { st ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newPart = Solid3DPart(
                                            name = "${st.label} ${solidParts.size + 1}",
                                            shapeType = st,
                                            booleanOp = Solid3DBooleanOp.UNION,
                                            posX = 0f,
                                            posY = 0f,
                                            posZ = 0f,
                                            dimX = 220f,
                                            dimY = 220f,
                                            dimZ = 220f,
                                            color = Color(0xFF94A3B8)
                                        )
                                        solidParts = solidParts + newPart
                                        selectedPartId = newPart.id
                                        showAddShapeDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF0284C7))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(st.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Parametric 3D solid primitive", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddShapeDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}
