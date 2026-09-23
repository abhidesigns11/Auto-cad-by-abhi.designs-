package com.example.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class StrokeType {
    CONTINUOUS,
    CENTER_LINE, // Long dash - dot - long dash
    DASHED_HIDDEN // Short dashed line
}

enum class DimensionType {
    LINEAR_HORIZONTAL,
    LINEAR_VERTICAL,
    ALIGNED,
    DIAMETER,
    RADIUS
}

enum class CadTool {
    SELECT,
    ROTATE,
    PAN_ZOOM,
    LINE,
    POLYLINE,
    RECTANGLE,
    CIRCLE,
    ARC,
    CENTERLINE,
    NOZZLE_STAMP,
    CONE_HOPPER,
    DIMENSION,
    LEADER_NOTE,
    TEXT_NOTE,
    OFFSET_WALL,
    MEASURE_TAPE,
    ERASE
}

enum class ViewDisplayMode {
    BLUEPRINT_2D,
    ISOMETRIC_3D,
    SHEET_PRINT_LAYOUT
}

enum class CanvasThemeMode {
    AUTOCAD_DARK,      // Classic Dark CAD #1E1E1E / #0F172A
    ENGINEERING_WHITE, // Clean print paper
    BLUEPRINT_CYAN     // Traditional Cyan #0A2540
}

data class CadLayer(
    val id: String,
    val name: String,
    val colorArgb: Long,
    val strokeWidthMm: Float = 0.5f,
    val isVisible: Boolean = true,
    val isLocked: Boolean = false
) {
    val composeColor: Color get() = Color(colorArgb)
}

sealed interface CadEntity {
    val id: String
    val layerId: String
}

data class CadLine(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "0",
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val strokeType: StrokeType = StrokeType.CONTINUOUS,
    val strokeWidthMm: Float = 0.5f,
    val customColorArgb: Long? = null
) : CadEntity

data class CadRect(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "0",
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val plateThicknessMm: Float = 0f,
    val isFilled: Boolean = false,
    val customColorArgb: Long? = null
) : CadEntity

data class CadCircle(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "0",
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val isPcd: Boolean = false, // Pitch Circle Diameter (dashed)
    val customColorArgb: Long? = null
) : CadEntity

data class CadArc(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "0",
    val cx: Float,
    val cy: Float,
    val radius: Float,
    val startAngleDeg: Float,
    val sweepAngleDeg: Float,
    val strokeType: StrokeType = StrokeType.CONTINUOUS,
    val customColorArgb: Long? = null
) : CadEntity

data class CadDimension(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "DIM",
    val type: DimensionType = DimensionType.ALIGNED,
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val offsetDistance: Float = 25f,
    val textOverride: String? = null,
    val prefix: String = "",
    val suffix: String = ""
) : CadEntity

data class CadLeader(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "NOZZLE",
    val targetX: Float,
    val targetY: Float,
    val elbowX: Float,
    val elbowY: Float,
    val text: String,
    val nozzleSizeNb: String = "50 NB"
) : CadEntity

data class CadText(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "TEXT",
    val x: Float,
    val y: Float,
    val text: String,
    val textHeightMm: Float = 16f,
    val isBold: Boolean = false,
    val customColorArgb: Long? = null
) : CadEntity

data class CadNozzleComponent(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "NOZZLE",
    val cx: Float,
    val cy: Float,
    val sizeNb: Int, // e.g. 25 NB (1"), 50 NB (2"), 80 NB (3")
    val flangeDiaMm: Float,
    val neckLengthMm: Float,
    val angleDeg: Float = 0f,
    val label: String = "NOZZLE"
) : CadEntity

data class CadHopperCone(
    override val id: String = UUID.randomUUID().toString(),
    override val layerId: String = "0",
    val topX: Float,
    val topY: Float,
    val topWidth: Float,
    val bottomWidth: Float,
    val height: Float,
    val wallThicknessMm: Float = 5f
) : CadEntity

data class TitleBlockInfo(
    val clientName: String = "ARISTO PHARMA PVT LTD.",
    val clientAddress: String = "DAMAN",
    val consultant: String = "-",
    val drawnBy: String = "RIPEN KOLI",
    val checkedBy: String = "DUD",
    val approvedBy: String = "DUD",
    val scale: String = "N.T.S.",
    val jobNo: String = "JOB-2026-SS-09",
    val companyName: String = "SHREE SAI ENGINEERING COMPANY",
    val companyAddress: String = "A/16, GLOBAL INDUSTRIAL PARK, SURVEY NO. 26/2, NEAR NAHULI RAILWAY CROSSING, OFF N.H. No.8, VALVADA - VAPI, DIST. VALSAD - 396 105",
    val companyEmail: String = "shreesaiengineeringworks@gmail.com",
    val drawingTitle: String = "SS 304 EQUIPMENT FABRICATION BLUEPRINT",
    val materialOfConstruction: String = "SS 304 X 1.2 X 1MM THK MATT FINISH",
    val drawingNo: String = "SSEC/116",
    val drawingDate: String = "22/09/2026",
    val poNo: String = "PO/4491",
    val poDate: String = "15/09/2026",
    val generalNotes: String = "Note:- All Numbers is Laser Printing As Per Your Requirement.\nAll SS welds to be ground smooth, pickled and passivated. Ra < 0.6 µm."
)

data class CadProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val subtitle: String = "Stainless Steel Fabrication Drawing",
    val entities: List<CadEntity> = emptyList(),
    val layers: List<CadLayer> = defaultCadLayers(),
    val titleBlock: TitleBlockInfo = TitleBlockInfo(),
    val sheetWidthMm: Float = 2100f,
    val sheetHeightMm: Float = 1400f,
    val isometricModelType: IsometricModelType = IsometricModelType.MBBR_STP_TANK
)

enum class IsometricModelType {
    MBBR_STP_TANK,           // Image 1: 1000x1000 Tank + 800x800 Settling Hopper Tank + Nozzles
    SS_CYCLONE_HOPPER,       // Image 2: Ø430/Ø400 Vessel with Conical Bottom, Door & 50NB Nozzle
    SS_MOBILE_TRAY_RACK,     // Image 3: SS Mobile Trolley with Castors & Multi-tier Runners
    SS_DOUBLE_SINK_TABLE,    // Image 4: SS Table with Two Pressed Sinks, Legs & Splashback
    SS_LOCKER_CABINET,       // Image 5: SS 304 Apron & Shoes Locker
    CUSTOM_3D_GENERATED      // Parametric from 2D geometry
}

fun defaultCadLayers(): List<CadLayer> = listOf(
    CadLayer("0", "0 (Outline/Plates)", 0xFFE2E8F0L, 0.6f),
    CadLayer("DIM", "Dimensions (Red)", 0xFFEF4444L, 0.3f),
    CadLayer("CENTER", "Centerlines (Green)", 0xFF10B981L, 0.35f),
    CadLayer("NOZZLE", "Nozzles & Fittings (Cyan)", 0xFF38BDF8L, 0.5f),
    CadLayer("TEXT", "Annotations / Text (Yellow)", 0xFFF59E0BL, 0.4f),
    CadLayer("HIDDEN", "Hidden Lines (Gray)", 0xFF64748BL, 0.3f)
)
