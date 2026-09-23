package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Cad3DTool

@Composable
fun Cad3DToolbar(
    activeTool: Cad3DTool,
    onToolSelected: (Cad3DTool) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xEB0A0E17),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Cad3DToolItem(
                tool = Cad3DTool.ORBIT_PAN,
                icon = Icons.Default.ThreeDRotation,
                isSelected = activeTool == Cad3DTool.ORBIT_PAN,
                onClick = { onToolSelected(Cad3DTool.ORBIT_PAN) }
            )

            Cad3DToolItem(
                tool = Cad3DTool.SELECT_PART,
                icon = Icons.Default.TouchApp,
                isSelected = activeTool == Cad3DTool.SELECT_PART,
                onClick = { onToolSelected(Cad3DTool.SELECT_PART) }
            )

            Cad3DToolItem(
                tool = Cad3DTool.ADD_SOLID,
                icon = Icons.Default.AddBox,
                isSelected = activeTool == Cad3DTool.ADD_SOLID,
                onClick = { onToolSelected(Cad3DTool.ADD_SOLID) },
                badgeColor = Color(0xFF0284C7)
            )

            Cad3DToolItem(
                tool = Cad3DTool.BOOLEAN_CSG,
                icon = Icons.Default.Difference,
                isSelected = activeTool == Cad3DTool.BOOLEAN_CSG,
                onClick = { onToolSelected(Cad3DTool.BOOLEAN_CSG) },
                badgeColor = Color(0xFFF59E0B)
            )

            Cad3DToolItem(
                tool = Cad3DTool.TRANSFORM_MOVE,
                icon = Icons.Default.OpenWith,
                isSelected = activeTool == Cad3DTool.TRANSFORM_MOVE,
                onClick = { onToolSelected(Cad3DTool.TRANSFORM_MOVE) }
            )

            Cad3DToolItem(
                tool = Cad3DTool.TRANSFORM_SCALE,
                icon = Icons.Default.Straighten,
                isSelected = activeTool == Cad3DTool.TRANSFORM_SCALE,
                onClick = { onToolSelected(Cad3DTool.TRANSFORM_SCALE) }
            )

            Cad3DToolItem(
                tool = Cad3DTool.ROTATE_AXIS,
                icon = Icons.Default.RotateRight,
                isSelected = activeTool == Cad3DTool.ROTATE_AXIS,
                onClick = { onToolSelected(Cad3DTool.ROTATE_AXIS) }
            )

            Cad3DToolItem(
                tool = Cad3DTool.MATERIAL_FINISH,
                icon = Icons.Default.Palette,
                isSelected = activeTool == Cad3DTool.MATERIAL_FINISH,
                onClick = { onToolSelected(Cad3DTool.MATERIAL_FINISH) }
            )

            Cad3DToolItem(
                tool = Cad3DTool.SECTION_PLANE,
                icon = Icons.Default.ContentCut,
                isSelected = activeTool == Cad3DTool.SECTION_PLANE,
                onClick = { onToolSelected(Cad3DTool.SECTION_PLANE) }
            )

            Cad3DToolItem(
                tool = Cad3DTool.EXPORT_3D,
                icon = Icons.Default.FileDownload,
                isSelected = activeTool == Cad3DTool.EXPORT_3D,
                onClick = { onToolSelected(Cad3DTool.EXPORT_3D) },
                badgeColor = Color(0xFF10B981)
            )
        }
    }
}

@Composable
private fun Cad3DToolItem(
    tool: Cad3DTool,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    badgeColor: Color? = null
) {
    val bgAnim by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF007AFF) else Color(0x1AFFFFFF),
        animationSpec = spring(),
        label = "bg"
    )

    val contentAnim by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFFE2E8F0),
        animationSpec = spring(),
        label = "content"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgAnim,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0x66FFFFFF) else Color(0x1FFFFFFF)
        ),
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag("tool_3d_${tool.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tool.title,
                tint = contentAnim,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = tool.title,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentAnim
            )
            if (badgeColor != null && !isSelected) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
            }
        }
    }
}
