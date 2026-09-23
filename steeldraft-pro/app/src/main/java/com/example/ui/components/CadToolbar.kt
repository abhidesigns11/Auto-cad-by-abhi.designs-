package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CadTool
import com.example.model.CanvasThemeMode

@Composable
fun CadRibbonToolbar(
    activeTool: CadTool,
    onToolSelected: (CadTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            CadToolButton(
                tool = CadTool.SELECT,
                label = "Select / Edit",
                icon = Icons.Default.NearMe,
                isSelected = activeTool == CadTool.SELECT,
                onClick = { onToolSelected(CadTool.SELECT) },
                accentColor = Color(0xFFF59E0B)
            )

            CadToolButton(
                tool = CadTool.ROTATE,
                label = "Rotate",
                icon = Icons.Default.RotateRight,
                isSelected = activeTool == CadTool.ROTATE,
                onClick = { onToolSelected(CadTool.ROTATE) },
                accentColor = Color(0xFFF59E0B)
            )

            CadToolButton(
                tool = CadTool.PAN_ZOOM,
                label = "Pan/Zoom",
                icon = Icons.Default.PanTool,
                isSelected = activeTool == CadTool.PAN_ZOOM,
                onClick = { onToolSelected(CadTool.PAN_ZOOM) }
            )

            CadToolButton(
                tool = CadTool.LINE,
                label = "Line (Exact mm)",
                icon = Icons.Default.Timeline,
                isSelected = activeTool == CadTool.LINE,
                onClick = { onToolSelected(CadTool.LINE) }
            )

            CadToolButton(
                tool = CadTool.RECTANGLE,
                label = "Rectangle",
                icon = Icons.Default.CropSquare,
                isSelected = activeTool == CadTool.RECTANGLE,
                onClick = { onToolSelected(CadTool.RECTANGLE) }
            )

            CadToolButton(
                tool = CadTool.CIRCLE,
                label = "Circle/Flange",
                icon = Icons.Default.RadioButtonUnchecked,
                isSelected = activeTool == CadTool.CIRCLE,
                onClick = { onToolSelected(CadTool.CIRCLE) }
            )

            CadToolButton(
                tool = CadTool.CENTERLINE,
                label = "Centerline",
                icon = Icons.Default.LinearScale,
                isSelected = activeTool == CadTool.CENTERLINE,
                onClick = { onToolSelected(CadTool.CENTERLINE) }
            )

            CadToolButton(
                tool = CadTool.DIMENSION,
                label = "Show Size (Dim)",
                icon = Icons.Default.Straighten,
                isSelected = activeTool == CadTool.DIMENSION,
                onClick = { onToolSelected(CadTool.DIMENSION) },
                accentColor = Color(0xFFEF4444)
            )

            CadToolButton(
                tool = CadTool.NOZZLE_STAMP,
                label = "Nozzle",
                icon = Icons.Default.Adjust,
                isSelected = activeTool == CadTool.NOZZLE_STAMP,
                onClick = { onToolSelected(CadTool.NOZZLE_STAMP) }
            )

            CadToolButton(
                tool = CadTool.CONE_HOPPER,
                label = "Hopper Cone",
                icon = Icons.Default.FilterAlt,
                isSelected = activeTool == CadTool.CONE_HOPPER,
                onClick = { onToolSelected(CadTool.CONE_HOPPER) }
            )

            CadToolButton(
                tool = CadTool.LEADER_NOTE,
                label = "Leader Note",
                icon = Icons.AutoMirrored.Filled.ArrowRightAlt,
                isSelected = activeTool == CadTool.LEADER_NOTE,
                onClick = { onToolSelected(CadTool.LEADER_NOTE) }
            )

            CadToolButton(
                tool = CadTool.TEXT_NOTE,
                label = "Text/Specs",
                icon = Icons.Default.TextFields,
                isSelected = activeTool == CadTool.TEXT_NOTE,
                onClick = { onToolSelected(CadTool.TEXT_NOTE) }
            )

            CadToolButton(
                tool = CadTool.MEASURE_TAPE,
                label = "Measure",
                icon = Icons.Default.SquareFoot,
                isSelected = activeTool == CadTool.MEASURE_TAPE,
                onClick = { onToolSelected(CadTool.MEASURE_TAPE) }
            )

            CadToolButton(
                tool = CadTool.ERASE,
                label = "Erase",
                icon = Icons.Default.Delete,
                isSelected = activeTool == CadTool.ERASE,
                onClick = { onToolSelected(CadTool.ERASE) }
            )
        }
    }
}

@Composable
private fun CadToolButton(
    tool: CadTool,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color? = null
) {
    val bg = if (isSelected) {
        accentColor ?: Color(0xFF0284C7)
    } else {
        if (accentColor != null) Color(0xFF261820) else Color(0xFF1E293B)
    }
    val contentCol = if (isSelected) Color.White else (accentColor ?: Color(0xFFE2E8F0))

    Surface(
        onClick = onClick,
        color = bg,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) (accentColor ?: Color(0xFF38BDF8)) else (accentColor?.copy(alpha = 0.5f) ?: Color(0xFF334155))
        ),
        modifier = Modifier.testTag("tool_btn_${tool.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentCol, modifier = Modifier.size(16.dp))
            Text(text = label, color = contentCol, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        }
    }
}

@Composable
fun CadStatusBar(
    currentX: Float,
    currentY: Float,
    measuredDist: Float,
    measuredAngle: Float,
    isOrthoEnabled: Boolean,
    onToggleOrtho: () -> Unit,
    isSnapEnabled: Boolean,
    onToggleSnap: () -> Unit,
    canvasTheme: CanvasThemeMode,
    onCycleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0B132B),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Coordinate readout
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "X: %.1f mm  Y: %.1f mm".format(currentX, currentY),
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )

                if (measuredDist > 0f) {
                    Text(
                        text = "DIST: %.0f mm (%.0f°)".format(measuredDist, measuredAngle),
                        color = Color(0xFFF59E0B),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Toggles: Ortho (F8) and Snap and Theme
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Ortho toggle
                FilterChip(
                    selected = isOrthoEnabled,
                    onClick = onToggleOrtho,
                    label = { Text("ORTHO", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.height(26.dp).testTag("ortho_toggle")
                )

                // Snap toggle
                FilterChip(
                    selected = isSnapEnabled,
                    onClick = onToggleSnap,
                    label = { Text("SNAP", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0284C7),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF1E293B),
                        labelColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.height(26.dp).testTag("snap_toggle")
                )

                // Canvas theme toggle
                IconButton(
                    onClick = onCycleTheme,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
