package com.example.model

import androidx.compose.ui.geometry.Offset
import java.util.UUID
import kotlin.math.*

object CadTransformEngine {

    fun rotatePoint(point: Offset, pivot: Offset, angleDeg: Float): Offset {
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()
        val dx = point.x - pivot.x
        val dy = point.y - pivot.y
        return Offset(
            x = pivot.x + (dx * cosA - dy * sinA),
            y = pivot.y + (dx * sinA + dy * cosA)
        )
    }

    fun getEntityCenter(entity: CadEntity): Offset {
        return when (entity) {
            is CadLine -> Offset((entity.x1 + entity.x2) * 0.5f, (entity.y1 + entity.y2) * 0.5f)
            is CadRect -> Offset(entity.x + entity.width * 0.5f, entity.y + entity.height * 0.5f)
            is CadCircle -> Offset(entity.cx, entity.cy)
            is CadArc -> Offset(entity.cx, entity.cy)
            is CadDimension -> Offset((entity.x1 + entity.x2) * 0.5f, (entity.y1 + entity.y2) * 0.5f)
            is CadLeader -> Offset((entity.targetX + entity.elbowX) * 0.5f, (entity.targetY + entity.elbowY) * 0.5f)
            is CadText -> Offset(entity.x + 30f, entity.y - entity.textHeightMm * 0.5f)
            is CadNozzleComponent -> Offset(entity.cx, entity.cy)
            is CadHopperCone -> Offset(entity.topX + entity.topWidth * 0.5f, entity.topY + entity.height * 0.5f)
            else -> Offset.Zero
        }
    }

    fun rotateEntity(entity: CadEntity, angleDeg: Float, customPivot: Offset? = null): CadEntity {
        val pivot = customPivot ?: getEntityCenter(entity)

        return when (entity) {
            is CadLine -> {
                val p1 = rotatePoint(Offset(entity.x1, entity.y1), pivot, angleDeg)
                val p2 = rotatePoint(Offset(entity.x2, entity.y2), pivot, angleDeg)
                entity.copy(x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y)
            }
            is CadRect -> {
                val center = Offset(entity.x + entity.width * 0.5f, entity.y + entity.height * 0.5f)
                val newCenter = rotatePoint(center, pivot, angleDeg)
                val normAngle = ((angleDeg % 360f) + 360f) % 360f
                val swapDims = (normAngle in 45f..135f) || (normAngle in 225f..315f)
                val newW = if (swapDims) entity.height else entity.width
                val newH = if (swapDims) entity.width else entity.height
                entity.copy(
                    x = newCenter.x - newW * 0.5f,
                    y = newCenter.y - newH * 0.5f,
                    width = newW,
                    height = newH
                )
            }
            is CadCircle -> {
                val newCenter = rotatePoint(Offset(entity.cx, entity.cy), pivot, angleDeg)
                entity.copy(cx = newCenter.x, cy = newCenter.y)
            }
            is CadArc -> {
                val newCenter = rotatePoint(Offset(entity.cx, entity.cy), pivot, angleDeg)
                val newStart = (entity.startAngleDeg + angleDeg) % 360f
                entity.copy(cx = newCenter.x, cy = newCenter.y, startAngleDeg = newStart)
            }
            is CadDimension -> {
                val p1 = rotatePoint(Offset(entity.x1, entity.y1), pivot, angleDeg)
                val p2 = rotatePoint(Offset(entity.x2, entity.y2), pivot, angleDeg)
                val normAngle = ((angleDeg % 360f) + 360f) % 360f
                val newType = if (normAngle in 45f..135f || normAngle in 225f..315f) {
                    when (entity.type) {
                        DimensionType.LINEAR_HORIZONTAL -> DimensionType.LINEAR_VERTICAL
                        DimensionType.LINEAR_VERTICAL -> DimensionType.LINEAR_HORIZONTAL
                        else -> entity.type
                    }
                } else {
                    entity.type
                }
                entity.copy(x1 = p1.x, y1 = p1.y, x2 = p2.x, y2 = p2.y, type = newType)
            }
            is CadLeader -> {
                val t = rotatePoint(Offset(entity.targetX, entity.targetY), pivot, angleDeg)
                val e = rotatePoint(Offset(entity.elbowX, entity.elbowY), pivot, angleDeg)
                entity.copy(targetX = t.x, targetY = t.y, elbowX = e.x, elbowY = e.y)
            }
            is CadText -> {
                val p = rotatePoint(Offset(entity.x, entity.y), pivot, angleDeg)
                entity.copy(x = p.x, y = p.y)
            }
            is CadNozzleComponent -> {
                val c = rotatePoint(Offset(entity.cx, entity.cy), pivot, angleDeg)
                val newAngle = (entity.angleDeg + angleDeg) % 360f
                entity.copy(cx = c.x, cy = c.y, angleDeg = newAngle)
            }
            is CadHopperCone -> {
                val c = rotatePoint(Offset(entity.topX + entity.topWidth * 0.5f, entity.topY + entity.height * 0.5f), pivot, angleDeg)
                entity.copy(topX = c.x - entity.topWidth * 0.5f, topY = c.y - entity.height * 0.5f)
            }
        }
    }

    fun translateEntity(entity: CadEntity, dx: Float, dy: Float): CadEntity {
        return when (entity) {
            is CadLine -> entity.copy(x1 = entity.x1 + dx, y1 = entity.y1 + dy, x2 = entity.x2 + dx, y2 = entity.y2 + dy)
            is CadRect -> entity.copy(x = entity.x + dx, y = entity.y + dy)
            is CadCircle -> entity.copy(cx = entity.cx + dx, cy = entity.cy + dy)
            is CadArc -> entity.copy(cx = entity.cx + dx, cy = entity.cy + dy)
            is CadDimension -> entity.copy(x1 = entity.x1 + dx, y1 = entity.y1 + dy, x2 = entity.x2 + dx, y2 = entity.y2 + dy)
            is CadLeader -> entity.copy(targetX = entity.targetX + dx, targetY = entity.targetY + dy, elbowX = entity.elbowX + dx, elbowY = entity.elbowY + dy)
            is CadText -> entity.copy(x = entity.x + dx, y = entity.y + dy)
            is CadNozzleComponent -> entity.copy(cx = entity.cx + dx, cy = entity.cy + dy)
            is CadHopperCone -> entity.copy(topX = entity.topX + dx, topY = entity.topY + dy)
        }
    }

    fun duplicateEntity(entity: CadEntity, offset: Offset = Offset(30f, 30f)): CadEntity {
        val moved = translateEntity(entity, offset.x, offset.y)
        val newId = UUID.randomUUID().toString()
        return when (moved) {
            is CadLine -> moved.copy(id = newId)
            is CadRect -> moved.copy(id = newId)
            is CadCircle -> moved.copy(id = newId)
            is CadArc -> moved.copy(id = newId)
            is CadDimension -> moved.copy(id = newId)
            is CadLeader -> moved.copy(id = newId)
            is CadText -> moved.copy(id = newId)
            is CadNozzleComponent -> moved.copy(id = newId)
            is CadHopperCone -> moved.copy(id = newId)
        }
    }
}
