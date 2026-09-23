package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import kotlin.math.*

@Composable
fun CadEntityEditorDock(
    selectedEntity: CadEntity,
    onEntityUpdated: (CadEntity) -> Unit,
    onEntityDeleted: (String) -> Unit,
    onEntityDuplicated: (CadEntity) -> Unit,
    onDeselect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Rotate & Transform, 1: Measurements & Specs
    var customRotateAngleText by remember { mutableStateOf("45") }

    // Line edit fields
    var lineLengthText by remember(selectedEntity) {
        mutableStateOf(
            if (selectedEntity is CadLine) {
                val dx = selectedEntity.x2 - selectedEntity.x1
                val dy = selectedEntity.y2 - selectedEntity.y1
                "%.1f".format(sqrt(dx * dx + dy * dy))
            } else ""
        )
    }

    // Circle edit fields
    var circleDiaText by remember(selectedEntity) {
        mutableStateOf(
            if (selectedEntity is CadCircle) "%.1f".format(selectedEntity.radius * 2f) else ""
        )
    }

    // Rect edit fields
    var rectWidthText by remember(selectedEntity) {
        mutableStateOf(if (selectedEntity is CadRect) "%.1f".format(selectedEntity.width) else "")
    }
    var rectHeightText by remember(selectedEntity) {
        mutableStateOf(if (selectedEntity is CadRect) "%.1f".format(selectedEntity.height) else "")
    }

    // Dimension edit fields
    var dimTextOverride by remember(selectedEntity) {
        mutableStateOf(if (selectedEntity is CadDimension) selectedEntity.textOverride ?: "" else "")
    }
    var dimOffsetDistText by remember(selectedEntity) {
        mutableStateOf(if (selectedEntity is CadDimension) "%.0f".format(selectedEntity.offsetDistance) else "")
    }

    // Leader / Text notes
    var textNoteContent by remember(selectedEntity) {
        mutableStateOf(
            when (selectedEntity) {
                is CadLeader -> selectedEntity.text
                is CadText -> selectedEntity.text
                else -> ""
            }
        )
    }

    Surface(
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        shadowElevation = 12.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Entity Details and Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (selectedEntity) {
                        is CadLine -> Icons.Default.Timeline
                        is CadRect -> Icons.Default.CropSquare
                        is CadCircle -> Icons.Default.RadioButtonUnchecked
                        is CadDimension -> Icons.Default.Straighten
                        is CadNozzleComponent -> Icons.Default.Adjust
                        is CadLeader -> Icons.Default.ChatBubbleOutline
                        is CadText -> Icons.Default.TextFields
                        else -> Icons.Default.Category
                    }
                    val entityName = when (selectedEntity) {
                        is CadLine -> "LINE [Layer: ${selectedEntity.layerId}]"
                        is CadRect -> "RECTANGLE / PLATE [${selectedEntity.plateThicknessMm}mm]"
                        is CadCircle -> if (selectedEntity.isPcd) "PCD CIRCLE [Dashed]" else "CIRCLE / FLANGE"
                        is CadDimension -> "BLUEPRINT DIMENSION"
                        is CadNozzleComponent -> "FLANGED NOZZLE [${selectedEntity.sizeNb} NB]"
                        is CadLeader -> "LEADER CALLOUT"
                        is CadText -> "TEXT ANNOTATION"
                        else -> "CAD ENTITY"
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entityName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Action Buttons: Duplicate, Delete, Close
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            val duplicated = CadTransformEngine.duplicateEntity(selectedEntity)
                            onEntityDuplicated(duplicated)
                        },
                        modifier = Modifier.size(30.dp).testTag("duplicate_entity_button")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = { onEntityDeleted(selectedEntity.id) },
                        modifier = Modifier.size(30.dp).testTag("delete_entity_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }

                    IconButton(
                        onClick = onDeselect,
                        modifier = Modifier.size(30.dp).testTag("deselect_entity_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-tabs: 0: ROTATE & NUDGE, 1: EDIT MEASUREMENTS
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White,
                modifier = Modifier.height(34.dp)
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ROTATE & MOVE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EDIT MEASUREMENTS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (activeTab == 0) {
                // ROTATE & MOVE CONTROLS
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Quick Rotate Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ROTATE:",
                            fontSize = 11.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            RotateQuickButton("⟲ -90°") {
                                onEntityUpdated(CadTransformEngine.rotateEntity(selectedEntity, -90f))
                            }
                            RotateQuickButton("⟳ +90°") {
                                onEntityUpdated(CadTransformEngine.rotateEntity(selectedEntity, 90f))
                            }
                            RotateQuickButton("⟲ -45°") {
                                onEntityUpdated(CadTransformEngine.rotateEntity(selectedEntity, -45f))
                            }
                            RotateQuickButton("⟳ +45°") {
                                onEntityUpdated(CadTransformEngine.rotateEntity(selectedEntity, 45f))
                            }
                            RotateQuickButton("180°") {
                                onEntityUpdated(CadTransformEngine.rotateEntity(selectedEntity, 180f))
                            }
                        }
                    }

                    // Custom Angle Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = customRotateAngleText,
                            onValueChange = { customRotateAngleText = it },
                            label = { Text("Custom Angle (°)", fontSize = 9.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFF59E0B),
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .width(130.dp)
                                .height(46.dp)
                                .testTag("custom_rotate_input")
                        )

                        Button(
                            onClick = {
                                val deg = customRotateAngleText.toFloatOrNull() ?: 0f
                                if (deg != 0f) {
                                    onEntityUpdated(CadTransformEngine.rotateEntity(selectedEntity, deg))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("apply_custom_rotate_button")
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Nudge arrow controls
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nudge (10mm): ", fontSize = 9.sp, color = Color(0xFF94A3B8))
                            NudgeButton(Icons.Default.ArrowBack) { onEntityUpdated(CadTransformEngine.translateEntity(selectedEntity, -10f, 0f)) }
                            NudgeButton(Icons.Default.ArrowForward) { onEntityUpdated(CadTransformEngine.translateEntity(selectedEntity, 10f, 0f)) }
                            NudgeButton(Icons.Default.ArrowUpward) { onEntityUpdated(CadTransformEngine.translateEntity(selectedEntity, 0f, -10f)) }
                            NudgeButton(Icons.Default.ArrowDownward) { onEntityUpdated(CadTransformEngine.translateEntity(selectedEntity, 0f, 10f)) }
                        }
                    }
                }
            } else {
                // MEASUREMENT / PROPERTY EDITING
                when (selectedEntity) {
                    is CadLine -> {
                        val dx = selectedEntity.x2 - selectedEntity.x1
                        val dy = selectedEntity.y2 - selectedEntity.y1
                        val curLen = sqrt(dx * dx + dy * dy)
                        val curAngle = (atan2(dy, dx) * 180.0 / PI).let { if (it < 0) it + 360f else it }.toFloat()
                        var lineAngleInput by remember(selectedEntity) { mutableStateOf("%.1f".format(curAngle)) }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = lineLengthText,
                                    onValueChange = { lineLengthText = it },
                                    label = { Text("Exact Length (mm)", fontSize = 9.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF475569)
                                    ),
                                    modifier = Modifier.width(130.dp).height(46.dp).testTag("edit_line_length_input")
                                )

                                OutlinedTextField(
                                    value = lineAngleInput,
                                    onValueChange = { lineAngleInput = it },
                                    label = { Text("Line Angle (°)", fontSize = 9.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFFF59E0B),
                                        unfocusedBorderColor = Color(0xFF475569)
                                    ),
                                    modifier = Modifier.width(100.dp).height(46.dp).testTag("edit_line_angle_input")
                                )

                                Button(
                                    onClick = {
                                        val newLen = lineLengthText.toFloatOrNull() ?: curLen
                                        val newAng = lineAngleInput.toFloatOrNull() ?: curAngle
                                        if (newLen > 0.1f) {
                                            val rad = Math.toRadians(newAng.toDouble())
                                            val newX2 = (selectedEntity.x1 + newLen * cos(rad)).toFloat()
                                            val newY2 = (selectedEntity.y1 + newLen * sin(rad)).toFloat()
                                            onEntityUpdated(selectedEntity.copy(x2 = newX2, y2 = newY2))
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    modifier = Modifier.height(38.dp).testTag("apply_line_length_button")
                                ) {
                                    Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick length preset chips & angle preset chips
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Presets:", fontSize = 9.sp, color = Color(0xFF94A3B8))
                                listOf(60f, 100f, 200f, 250f, 400f, 570f, 1000f).forEach { pre ->
                                    FilterChip(
                                        selected = lineLengthText == pre.toInt().toString(),
                                        onClick = {
                                            lineLengthText = pre.toInt().toString()
                                            val rad = Math.toRadians((lineAngleInput.toFloatOrNull() ?: curAngle).toDouble())
                                            val newX2 = (selectedEntity.x1 + pre * cos(rad)).toFloat()
                                            val newY2 = (selectedEntity.y1 + pre * sin(rad)).toFloat()
                                            onEntityUpdated(selectedEntity.copy(x2 = newX2, y2 = newY2))
                                        },
                                        label = { Text("%.0fmm".format(pre), fontSize = 9.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                listOf(0f, 45f, 90f, 180f, 270f).forEach { ang ->
                                    FilterChip(
                                        selected = lineAngleInput == ang.toInt().toString(),
                                        onClick = {
                                            lineAngleInput = ang.toInt().toString()
                                            val len = lineLengthText.toFloatOrNull() ?: curLen
                                            val rad = Math.toRadians(ang.toDouble())
                                            val newX2 = (selectedEntity.x1 + len * cos(rad)).toFloat()
                                            val newY2 = (selectedEntity.y1 + len * sin(rad)).toFloat()
                                            onEntityUpdated(selectedEntity.copy(x2 = newX2, y2 = newY2))
                                        },
                                        label = { Text("${ang.toInt()}°", fontSize = 9.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    is CadCircle -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = circleDiaText,
                                onValueChange = { circleDiaText = it },
                                label = { Text("Diameter Ø (mm)", fontSize = 9.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.width(130.dp).height(46.dp).testTag("edit_circle_dia_input")
                            )

                            Button(
                                onClick = {
                                    val dia = circleDiaText.toFloatOrNull()
                                    if (dia != null && dia > 1f) {
                                        onEntityUpdated(selectedEntity.copy(radius = dia * 0.5f))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.height(34.dp).testTag("apply_circle_dia_button")
                            ) {
                                Text("Set Dia", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // PCD toggle chip
                            FilterChip(
                                selected = selectedEntity.isPcd,
                                onClick = { onEntityUpdated(selectedEntity.copy(isPcd = !selectedEntity.isPcd)) },
                                label = { Text("PCD Centerline", fontSize = 10.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    is CadRect -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = rectWidthText,
                                onValueChange = { rectWidthText = it },
                                label = { Text("Width (mm)", fontSize = 9.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.width(100.dp).height(46.dp).testTag("edit_rect_width_input")
                            )

                            OutlinedTextField(
                                value = rectHeightText,
                                onValueChange = { rectHeightText = it },
                                label = { Text("Height (mm)", fontSize = 9.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.width(100.dp).height(46.dp).testTag("edit_rect_height_input")
                            )

                            Button(
                                onClick = {
                                    val w = rectWidthText.toFloatOrNull() ?: selectedEntity.width
                                    val h = rectHeightText.toFloatOrNull() ?: selectedEntity.height
                                    if (w > 0.5f && h > 0.5f) {
                                        onEntityUpdated(selectedEntity.copy(width = w, height = h))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.height(34.dp).testTag("apply_rect_size_button")
                            ) {
                                Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is CadDimension -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedTextField(
                                value = dimTextOverride,
                                onValueChange = { dimTextOverride = it },
                                label = { Text("Dimension Text", fontSize = 9.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFEF4444),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.width(120.dp).height(46.dp).testTag("edit_dim_text_input")
                            )

                            OutlinedTextField(
                                value = dimOffsetDistText,
                                onValueChange = { dimOffsetDistText = it },
                                label = { Text("Offset (mm)", fontSize = 9.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFEF4444),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.width(90.dp).height(46.dp).testTag("edit_dim_offset_input")
                            )

                            Button(
                                onClick = {
                                    val off = dimOffsetDistText.toFloatOrNull() ?: selectedEntity.offsetDistance
                                    onEntityUpdated(
                                        selectedEntity.copy(
                                            textOverride = dimTextOverride.ifBlank { null },
                                            offsetDistance = off
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.height(34.dp).testTag("apply_dim_edit_button")
                            ) {
                                Text("Update", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Orientation toggle
                            FilterChip(
                                selected = selectedEntity.type == DimensionType.LINEAR_VERTICAL,
                                onClick = {
                                    val nextType = if (selectedEntity.type == DimensionType.LINEAR_VERTICAL) {
                                        DimensionType.LINEAR_HORIZONTAL
                                    } else {
                                        DimensionType.LINEAR_VERTICAL
                                    }
                                    onEntityUpdated(selectedEntity.copy(type = nextType))
                                },
                                label = {
                                    Text(if (selectedEntity.type == DimensionType.LINEAR_VERTICAL) "↕ Vert" else "↔ Horiz", fontSize = 9.sp)
                                },
                                modifier = Modifier.height(30.dp)
                            )
                        }
                    }

                    is CadLeader, is CadText -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = textNoteContent,
                                onValueChange = { textNoteContent = it },
                                label = { Text("Annotation Text / Note", fontSize = 9.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF475569)
                                ),
                                modifier = Modifier.weight(1f).height(46.dp).testTag("edit_note_text_input")
                            )

                            Button(
                                onClick = {
                                    when (selectedEntity) {
                                        is CadLeader -> onEntityUpdated(selectedEntity.copy(text = textNoteContent))
                                        is CadText -> onEntityUpdated(selectedEntity.copy(text = textNoteContent))
                                        else -> {}
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                modifier = Modifier.height(34.dp).testTag("apply_note_text_button")
                            ) {
                                Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Selected entity can be moved, duplicated, or rotated via the Rotate & Move tab.",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RotateQuickButton(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFFFCD34D),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun NudgeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(26.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
    }
}
