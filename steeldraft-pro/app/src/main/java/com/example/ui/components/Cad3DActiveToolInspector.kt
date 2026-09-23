package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlin.math.roundToInt

@Composable
fun Cad3DActiveToolInspector(
    activeTool: Cad3DTool,
    selectedPart: Solid3DPart?,
    allParts: List<Solid3DPart>,
    onUpdatePart: (Solid3DPart) -> Unit,
    onAddPrimitive: (Solid3DShapeType) -> Unit,
    onDeletePart: (String) -> Unit,
    onDuplicatePart: (Solid3DPart) -> Unit,
    onDismissInspector: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xF2101626),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        shadowElevation = 18.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // iOS drag indicator
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x4DFFFFFF))
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0x26007AFF),
                        shape = CircleShape,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = when (activeTool) {
                                Cad3DTool.ORBIT_PAN -> Icons.Default.ThreeDRotation
                                Cad3DTool.SELECT_PART -> Icons.Default.TouchApp
                                Cad3DTool.ADD_SOLID -> Icons.Default.AddBox
                                Cad3DTool.BOOLEAN_CSG -> Icons.Default.Difference
                                Cad3DTool.TRANSFORM_MOVE -> Icons.Default.OpenWith
                                Cad3DTool.TRANSFORM_SCALE -> Icons.Default.Straighten
                                Cad3DTool.ROTATE_AXIS -> Icons.Default.RotateRight
                                Cad3DTool.MATERIAL_FINISH -> Icons.Default.Palette
                                Cad3DTool.SECTION_PLANE -> Icons.Default.ContentCut
                                Cad3DTool.EXPORT_3D -> Icons.Default.FileDownload
                            },
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.padding(5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = activeTool.title.uppercase(),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = if (selectedPart != null) "Active: ${selectedPart.name} (${selectedPart.shapeType.label})" else activeTool.subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismissInspector,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Tool Panel",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (activeTool) {
                Cad3DTool.ADD_SOLID -> {
                    Text(
                        "Tap primitive shape to inject into 3D Assembly:",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Solid3DShapeType.values().forEach { shape ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x1F2563EB),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4038BDF8)),
                                modifier = Modifier
                                    .clickable { onAddPrimitive(shape) }
                                    .testTag("add_shape_${shape.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = shape.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Cad3DTool.BOOLEAN_CSG -> {
                    if (selectedPart == null) {
                        Text(
                            "Select a 3D component above to set its CSG Boolean status.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Solid3DBooleanOp.values().forEach { op ->
                                val isCur = selectedPart.booleanOp == op
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isCur) {
                                        if (op == Solid3DBooleanOp.SUBTRACT) Color(0xFFDC2626) else Color(0xFF007AFF)
                                    } else Color(0x1AFFFFFF),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCur) Color(0x80FFFFFF) else Color(0x22FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clickable { onUpdatePart(selectedPart.copy(booleanOp = op)) }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = op.label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Cad3DTool.TRANSFORM_MOVE -> {
                    if (selectedPart == null) {
                        Text("Select a 3D part to adjust millimeter positions.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // X
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pos X: ${selectedPart.posX.roundToInt()} mm", fontSize = 11.sp, color = Color(0xFFEF4444), modifier = Modifier.width(90.dp))
                                Slider(
                                    value = selectedPart.posX,
                                    onValueChange = { onUpdatePart(selectedPart.copy(posX = it)) },
                                    valueRange = -500f..500f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                            // Y
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pos Y: ${selectedPart.posY.roundToInt()} mm", fontSize = 11.sp, color = Color(0xFF10B981), modifier = Modifier.width(90.dp))
                                Slider(
                                    value = selectedPart.posY,
                                    onValueChange = { onUpdatePart(selectedPart.copy(posY = it)) },
                                    valueRange = -500f..500f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                            // Z
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Pos Z: ${selectedPart.posZ.roundToInt()} mm", fontSize = 11.sp, color = Color(0xFF38BDF8), modifier = Modifier.width(90.dp))
                                Slider(
                                    value = selectedPart.posZ,
                                    onValueChange = { onUpdatePart(selectedPart.copy(posZ = it)) },
                                    valueRange = -500f..500f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                        }
                    }
                }

                Cad3DTool.TRANSFORM_SCALE -> {
                    if (selectedPart == null) {
                        Text("Select a 3D part to edit its 3D dimensions.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dim X/Dia: ${selectedPart.dimX.roundToInt()} mm", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(110.dp))
                                Slider(
                                    value = selectedPart.dimX,
                                    onValueChange = { onUpdatePart(selectedPart.copy(dimX = it.coerceAtLeast(10f))) },
                                    valueRange = 20f..1200f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dim Y/H: ${selectedPart.dimY.roundToInt()} mm", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(110.dp))
                                Slider(
                                    value = selectedPart.dimY,
                                    onValueChange = { onUpdatePart(selectedPart.copy(dimY = it.coerceAtLeast(10f))) },
                                    valueRange = 20f..1500f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Dim Z/Thk: ${selectedPart.dimZ.roundToInt()} mm", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(110.dp))
                                Slider(
                                    value = selectedPart.dimZ,
                                    onValueChange = { onUpdatePart(selectedPart.copy(dimZ = it.coerceAtLeast(10f))) },
                                    valueRange = 10f..1000f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                        }
                    }
                }

                Cad3DTool.ROTATE_AXIS -> {
                    if (selectedPart == null) {
                        Text("Select a part to rotate its angle.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    } else {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Rotation: ${selectedPart.rotationDeg.roundToInt()}°", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = selectedPart.rotationDeg,
                                    onValueChange = { onUpdatePart(selectedPart.copy(rotationDeg = it)) },
                                    valueRange = 0f..360f,
                                    modifier = Modifier.weight(1f).height(24.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(0f, 45f, 90f, 180f, 270f).forEach { ang ->
                                    FilterChip(
                                        selected = selectedPart.rotationDeg == ang,
                                        onClick = { onUpdatePart(selectedPart.copy(rotationDeg = ang)) },
                                        label = { Text("${ang.toInt()}°", fontSize = 9.sp) },
                                        modifier = Modifier.height(26.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Cad3DTool.MATERIAL_FINISH -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Select Stainless Steel finish & surface texture:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StainlessSteelGrade.values().forEach { grade ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(grade.colorHex),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                    modifier = Modifier
                                        .clickable {
                                            selectedPart?.let {
                                                onUpdatePart(it.copy(color = Color(grade.colorHex)))
                                            }
                                        }
                                        .height(34.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            grade.code,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            grade.roughness,
                                            fontSize = 8.sp,
                                            color = Color(0xFF334155)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Cad3DTool.SECTION_PLANE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Dynamic Internal Slicing Plane", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                            Text("Reveals baffle walls, pipe penetrations & inner shell", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                selectedPart?.let {
                                    onUpdatePart(it.copy(isWireframeOnly = !it.isWireframeOnly))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(if (selectedPart?.isWireframeOnly == true) "Solid View" else "Wireframe Cut", fontSize = 10.sp)
                        }
                    }
                }

                Cad3DTool.EXPORT_3D -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Export Solid Assembly (Mesh & Parametric B-Rep):", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onDismissInspector,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("STEP / IGES (CNC)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onDismissInspector,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.weight(1f).height(36.dp)
                            ) {
                                Icon(Icons.Default.ViewInAr, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("STL 3D Print", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Cad3DTool.ORBIT_PAN, Cad3DTool.SELECT_PART -> {
                    // Quick Action Row for Selected Part
                    if (selectedPart != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Part: ${selectedPart.name}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilledTonalButton(
                                    onClick = { onDuplicatePart(selectedPart) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Duplicate", fontSize = 9.sp)
                                }
                                if (allParts.size > 1) {
                                    FilledTonalButton(
                                        onClick = { onDeletePart(selectedPart.id) },
                                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0x33EF4444), contentColor = Color(0xFFEF4444)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Delete", fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            "Drag 1 finger to Orbit 360°. Pinch 2 fingers to Zoom. Use the toolbar to add shapes or apply cuts.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
