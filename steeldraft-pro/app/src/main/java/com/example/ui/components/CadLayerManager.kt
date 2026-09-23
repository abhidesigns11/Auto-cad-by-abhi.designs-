package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CadLayer

@Composable
fun CadLayerDialog(
    layers: List<CadLayer>,
    activeLayerId: String,
    onLayerSelected: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Layers, contentDescription = "Layers", tint = Color(0xFF0284C7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("AutoCAD Layer Properties", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Manage visibility and active drawing layer:",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                layers.forEach { layer ->
                    val isCurrent = layer.id == activeLayerId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLayerSelected(layer.id) }
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) Color(0xFF0284C7) else Color(0xFFE2E8F0)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) Color(0xFFF0F9FF) else Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Layer Color Swatch
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(layer.composeColor)
                                        .border(1.dp, Color(0xFF475569), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${layer.name} ${if (isCurrent) "(CURRENT)" else ""}",
                                        fontSize = 12.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isCurrent) Color(0xFF0284C7) else Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Lineweight: ${layer.strokeWidthMm} mm",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onToggleVisibility(layer.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    tint = if (layer.isVisible) Color(0xFF0284C7) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}
