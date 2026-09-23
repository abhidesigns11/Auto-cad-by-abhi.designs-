package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.model.DimensionType

/**
 * Direct Distance Entry bar for adding lines based on exact millimeter measurements.
 */
@Composable
fun LineMeasurementDock(
    currentStartX: Float,
    currentStartY: Float,
    onAddLine: (lengthMm: Float, angleDeg: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var lengthText by remember { mutableStateOf("250") }
    var selectedDirectionAngle by remember { mutableFloatStateOf(0f) }
    var customAngleText by remember { mutableStateOf("0") }

    val presetLengths = listOf(60f, 100f, 200f, 250f, 400f, 430f, 570f, 1000f)
    val presetAngles = listOf(0f, 30f, 45f, 60f, 90f, 135f, 180f, 270f)

    Surface(
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header Row with Coordinates
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DIRECT DISTANCE & ANGLE ENTRY",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Origin: (%.0f, %.0f) mm".format(currentStartX, currentStartY),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 1: Length Input & Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = lengthText,
                    onValueChange = { lengthText = it },
                    label = { Text("Length (mm)", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedLabelColor = Color(0xFF38BDF8),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .width(130.dp)
                        .height(50.dp)
                        .testTag("line_length_input")
                )

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetLengths.forEach { p ->
                        FilterChip(
                            selected = lengthText == p.toInt().toString(),
                            onClick = { lengthText = p.toInt().toString() },
                            label = { Text("%.0f".format(p), fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0284C7),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Angle Input, Quick Direction Presets & Draw Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customAngleText,
                    onValueChange = {
                        customAngleText = it
                        it.toFloatOrNull()?.let { ang -> selectedDirectionAngle = ang }
                    },
                    label = { Text("Angle (°)", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFF59E0B),
                        unfocusedBorderColor = Color(0xFF475569),
                        focusedLabelColor = Color(0xFFF59E0B),
                        unfocusedLabelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier
                        .width(100.dp)
                        .height(50.dp)
                        .testTag("line_angle_input")
                )

                // Quick Angle Chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presetAngles.forEach { a ->
                        FilterChip(
                            selected = selectedDirectionAngle == a,
                            onClick = {
                                selectedDirectionAngle = a
                                customAngleText = a.toInt().toString()
                            },
                            label = { Text("${a.toInt()}°", fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD97706),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.height(30.dp)
                        )
                    }
                }

                // Add Line Action Button
                Button(
                    onClick = {
                        val len = lengthText.toFloatOrNull() ?: 100f
                        val ang = customAngleText.toFloatOrNull() ?: selectedDirectionAngle
                        onAddLine(len, ang)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(38.dp).testTag("confirm_add_line_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Draw Line", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DirectionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF0369A1) else Color(0xFF1E293B),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFF94A3B8),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = label.substringBefore(" "),
                fontSize = 9.sp,
                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Blueprint Dimension tool dock for showing sizes between clicked points
 * (with witness lines, arrowheads, diameter prefixes, and stack offsets).
 */
@Composable
fun DimensionMeasurementDock(
    startPoint: androidx.compose.ui.geometry.Offset?,
    endPoint: androidx.compose.ui.geometry.Offset?,
    onPlaceDimension: (type: DimensionType, offsetDistance: Float, prefix: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var dimType by remember { mutableStateOf(DimensionType.LINEAR_VERTICAL) }
    var prefixText by remember { mutableStateOf("") }
    var offsetDist by remember { mutableFloatStateOf(-60f) }

    val dist = if (startPoint != null && endPoint != null) {
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y
        when (dimType) {
            DimensionType.LINEAR_VERTICAL -> kotlin.math.abs(dy)
            DimensionType.LINEAR_HORIZONTAL -> kotlin.math.abs(dx)
            else -> kotlin.math.sqrt(dx * dx + dy * dy)
        }
    } else 0f

    // Automatically guess orientation on first selection
    LaunchedEffect(startPoint, endPoint) {
        if (startPoint != null && endPoint != null) {
            val dx = kotlin.math.abs(endPoint.x - startPoint.x)
            val dy = kotlin.math.abs(endPoint.y - startPoint.y)
            dimType = if (dy >= dx) DimensionType.LINEAR_VERTICAL else DimensionType.LINEAR_HORIZONTAL
        }
    }

    Surface(
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
        shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header Row with Live Size Measurement
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BLUEPRINT DIMENSION / SHOW SIZE",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (dist > 0f) {
                    Surface(
                        color = Color(0xFFB91C1C),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "$prefixText%.0f mm".format(dist),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Click/drag from point A to B",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Dimension Mode & Diameter Prefix Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dimension Type Chips (Vertical, Horizontal, Aligned)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = dimType == DimensionType.LINEAR_VERTICAL,
                        onClick = { dimType = DimensionType.LINEAR_VERTICAL },
                        label = { Text("↕ Vertical", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                    FilterChip(
                        selected = dimType == DimensionType.LINEAR_HORIZONTAL,
                        onClick = { dimType = DimensionType.LINEAR_HORIZONTAL },
                        label = { Text("↔ Horizontal", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                    FilterChip(
                        selected = dimType == DimensionType.ALIGNED,
                        onClick = { dimType = DimensionType.ALIGNED },
                        label = { Text("⤢ Aligned", fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }

                // Diameter "Ø" Prefix Toggle (like Ø400, Ø430 in blueprint)
                FilterChip(
                    selected = prefixText == "Ø",
                    onClick = { prefixText = if (prefixText == "Ø") "" else "Ø" },
                    label = { Text("Ø Dia", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF38BDF8)
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Offset Distance & Place Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stack Offset Chips (allows stacking dimensions: 60/250/200 vs 570 outer chain)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Offset:", fontSize = 10.sp, color = Color(0xFF94A3B8))
                    listOf(-40f, -80f, -120f, -170f, 40f).forEach { off ->
                        FilterChip(
                            selected = offsetDist == off,
                            onClick = { offsetDist = off },
                            label = { Text("%.0f".format(off), fontSize = 9.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFB91C1C),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }

                // Place Dimension Button
                Button(
                    onClick = {
                        onPlaceDimension(dimType, offsetDist, prefixText)
                    },
                    enabled = dist > 2f,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444),
                        disabledContainerColor = Color(0xFF475569)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp).testTag("confirm_place_dimension_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Show Size", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
