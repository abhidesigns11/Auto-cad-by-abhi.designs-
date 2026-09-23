package com.example.model

enum class Cad3DTool(
    val title: String,
    val subtitle: String,
    val iconName: String
) {
    ORBIT_PAN("Orbit & Pan", "360° Free Rotate & Camera Move", "360"),
    SELECT_PART("Select & Inspect", "Touch to select, highlight & measure", "TouchApp"),
    ADD_SOLID("Add Primitive", "Add Box, Tank, Shell, Cone or Ring", "AddBox"),
    BOOLEAN_CSG("CSG Boolean", "Union (+ Add) or Subtract (- Cutout)", "Difference"),
    TRANSFORM_MOVE("Precision Move", "Translate along X, Y, Z millimeter axes", "OpenWith"),
    TRANSFORM_SCALE("Extrude & Scale", "Resize length, width, diameter, thickness", "Straighten"),
    ROTATE_AXIS("Rotate Axis", "Roll, pitch, or yaw part by exact degrees", "RotateRight"),
    MATERIAL_FINISH("SS Finish", "Polished 2B, Mirror #8, Brushed, Sandblasted", "Palette"),
    SECTION_PLANE("Section Cut", "Cross-section interior slicing plane", "ContentCut"),
    EXPORT_3D("Export 3D", "Export to STEP, IGES, STL 3D Mesh", "Download")
}

enum class StainlessSteelGrade(
    val code: String,
    val finishName: String,
    val colorHex: Long,
    val roughness: String
) {
    SS_304_2B("SS 304 2B", "Cold Rolled Mill Finish", 0xFF94A3B8, "Ra 0.4 µm"),
    SS_316L_MIRROR("SS 316L #8", "Ultra High Mirror Polish", 0xFFE2E8F0, "Ra 0.1 µm"),
    SS_304_BRUSHED("SS 304 #4", "Directional Satin Brushed", 0xFFCBD5E1, "Ra 0.8 µm"),
    SS_316_ELECTROPOLISH("SS 316 EP", "Pharma Grade Electropolished", 0xFFF1F5F9, "Ra 0.2 µm"),
    SS_DISH_PVD_GOLD("Titanium Gold", "PVD Vacuum Coated Architectural", 0xFFF59E0B, "Ra 0.3 µm")
}
