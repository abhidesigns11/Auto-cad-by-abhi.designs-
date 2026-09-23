package com.example.model

object SampleEquipmentDrawings {

    fun getMbbrStpTankProject(): CadProject {
        val entities = mutableListOf<CadEntity>()

        // 1. Text annotations
        entities.add(CadText(x = 550f, y = 140f, text = "PLATE THICKNESS - 5 MM", textHeightMm = 28f, isBold = true))
        entities.add(CadText(x = 260f, y = 840f, text = "FRONT VIEW", textHeightMm = 22f, isBold = true))
        entities.add(CadText(x = 1380f, y = 840f, text = "TOP VIEW", textHeightMm = 22f, isBold = true))
        entities.add(CadText(x = 180f, y = 920f, text = "TANK SIZE- 1000(L) X 1000(W) X 1000(H) MM", textHeightMm = 18f))
        entities.add(CadText(x = 180f, y = 960f, text = "SETTLING TANK -800(L) X 800(W) X 800(H) MM", textHeightMm = 18f))

        // FRONT VIEW (Left Side):
        // Main Tank: Left=150, Top=250, Width=400 (represents 1000mm, scale 0.4 for sheet layout), Height=400 (1000mm)
        // Let's use world mm units directly:
        // Main Tank: X: 150..1150, Y: 250..1250 (1000 x 1000)
        // Settling Tank: X: 1150..1950, Y: 250..750 (Straight: 500h), then Hopper cone Y: 750..1050 (300h)
        val tX = 150f
        val tY = 250f
        val tW = 500f // 1000 mm scaled or 1:1 mm
        // We'll use 1:1 true mm for CAD precision!
        // To fit nicely on drawing sheet (2200 x 1400 mm):
        // Front View:
        val fLeft = 180f
        val fTop = 260f
        val fTankW = 450f
        val fTankH = 450f
        val fSetW = 360f
        val fSetStraightH = 225f
        val fSetHopperH = 135f

        // Front Main Tank Rectangle
        entities.add(CadRect(x = fLeft, y = fTop, width = fTankW, height = fTankH, plateThicknessMm = 5f))

        // Settling Tank straight top
        entities.add(CadLine(x1 = fLeft + fTankW, y1 = fTop, x2 = fLeft + fTankW + fSetW, y2 = fTop))
        entities.add(CadLine(x1 = fLeft + fTankW + fSetW, y1 = fTop, x2 = fLeft + fTankW + fSetW, y2 = fTop + fSetStraightH))
        entities.add(CadLine(x1 = fLeft + fTankW, y1 = fTop + fSetStraightH, x2 = fLeft + fTankW + fSetW, y2 = fTop + fSetStraightH, strokeType = StrokeType.CONTINUOUS))

        // Settling Tank Hopper cone lines (funnel down to drain)
        val hopperApexX = fLeft + fTankW + (fSetW * 0.5f)
        val hopperApexY = fTop + fSetStraightH + fSetHopperH
        entities.add(CadLine(x1 = fLeft + fTankW, y1 = fTop + fSetStraightH, x2 = hopperApexX - 15f, y2 = hopperApexY))
        entities.add(CadLine(x1 = fLeft + fTankW + fSetW, y1 = fTop + fSetStraightH, x2 = hopperApexX + 15f, y2 = hopperApexY))
        entities.add(CadLine(x1 = hopperApexX - 15f, y1 = hopperApexY, x2 = hopperApexX + 15f, y2 = hopperApexY))

        // Drain Nozzle on hopper bottom
        entities.add(CadLine(x1 = hopperApexX - 10f, y1 = hopperApexY, x2 = hopperApexX - 10f, y2 = hopperApexY + 25f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = hopperApexX + 10f, y1 = hopperApexY, x2 = hopperApexX + 10f, y2 = hopperApexY + 25f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = hopperApexX - 20f, y1 = hopperApexY + 25f, x2 = hopperApexX + 20f, y2 = hopperApexY + 25f, layerId = "NOZZLE"))

        // Drain Nozzle on main tank
        entities.add(CadCircle(cx = fLeft + 200f, cy = fTop + fTankH - 30f, radius = 8f, layerId = "NOZZLE"))

        // Inlet Nozzle with flange on top-left
        entities.add(CadLine(x1 = fLeft - 25f, y1 = fTop + 20f, x2 = fLeft, y2 = fTop + 20f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = fLeft - 25f, y1 = fTop + 40f, x2 = fLeft, y2 = fTop + 40f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = fLeft - 25f, y1 = fTop + 10f, x2 = fLeft - 25f, y2 = fTop + 50f, layerId = "NOZZLE"))

        // Overflow Nozzle between tanks
        entities.add(CadLine(x1 = fLeft + fTankW - 15f, y1 = fTop + 30f, x2 = fLeft + fTankW + 15f, y2 = fTop + 30f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = fLeft + fTankW - 15f, y1 = fTop + 20f, x2 = fLeft + fTankW - 15f, y2 = fTop + 40f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = fLeft + fTankW + 15f, y1 = fTop + 20f, x2 = fLeft + fTankW + 15f, y2 = fTop + 40f, layerId = "NOZZLE"))

        // Outlet Nozzle with flange on settling tank right
        val outX = fLeft + fTankW + fSetW
        entities.add(CadLine(x1 = outX, y1 = fTop + 30f, x2 = outX + 25f, y2 = fTop + 30f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = outX, y1 = fTop + 50f, x2 = outX + 25f, y2 = fTop + 50f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = outX + 25f, y1 = fTop + 20f, x2 = outX + 25f, y2 = fTop + 60f, layerId = "NOZZLE"))

        // Front View Dimensions (Red)
        entities.add(CadDimension(x1 = fLeft, y1 = fTop, x2 = fLeft + fTankW, y2 = fTop, offsetDistance = -40f, textOverride = "1000"))
        entities.add(CadDimension(x1 = fLeft + fTankW, y1 = fTop, x2 = outX, y2 = fTop, offsetDistance = -40f, textOverride = "800"))
        entities.add(CadDimension(x1 = fLeft, y1 = fTop, x2 = outX, y2 = fTop, offsetDistance = -80f, textOverride = "1800"))
        entities.add(CadDimension(x1 = fLeft, y1 = fTop, x2 = fLeft, y2 = fTop + fTankH, offsetDistance = -45f, textOverride = "1000"))
        entities.add(CadDimension(x1 = outX, y1 = fTop, x2 = outX, y2 = fTop + fSetStraightH, offsetDistance = 45f, textOverride = "500"))
        entities.add(CadDimension(x1 = outX, y1 = fTop + fSetStraightH, x2 = outX, y2 = hopperApexY, offsetDistance = 45f, textOverride = "300"))
        entities.add(CadDimension(x1 = outX, y1 = fTop, x2 = outX, y2 = hopperApexY, offsetDistance = 85f, textOverride = "800"))

        // Front View Leaders & Labels
        entities.add(CadLeader(targetX = fLeft - 25f, targetY = fTop + 30f, elbowX = fLeft - 60f, elbowY = fTop - 10f, text = "INLET NOZZLE - 1\""))
        entities.add(CadLeader(targetX = fLeft + fTankW, targetY = fTop + 30f, elbowX = fLeft + fTankW - 80f, elbowY = fTop - 30f, text = "OVER FLOW NOZZLE - 1\""))
        entities.add(CadLeader(targetX = outX + 25f, targetY = fTop + 40f, elbowX = outX + 50f, elbowY = fTop - 10f, text = "OUTLET NOZZLE - 1\""))
        entities.add(CadLeader(targetX = hopperApexX, targetY = hopperApexY + 25f, elbowX = hopperApexX + 60f, elbowY = hopperApexY + 60f, text = "DRAIN NOZZLE - 1\""))
        entities.add(CadLeader(targetX = fLeft + 200f, targetY = fTop + fTankH - 30f, elbowX = fLeft - 50f, elbowY = fTop + fTankH - 20f, text = "DRAIN NOZZLE - 1\""))

        // TOP VIEW (Right Side):
        val topL = 1200f
        val topT = 260f
        val topTankW = 450f
        val topTankH = 450f
        val topSetW = 360f
        val topSetH = 360f

        // Main Tank Top Outline
        entities.add(CadRect(x = topL, y = topT, width = topTankW, height = topTankH, plateThicknessMm = 5f))
        // Settling Tank Top Outline
        entities.add(CadRect(x = topL + topTankW, y = topT, width = topSetW, height = topSetH, plateThicknessMm = 5f))

        // Settling Tank diagonal hopper lines to center circle
        val setCenterCX = topL + topTankW + (topSetW * 0.5f)
        val setCenterCY = topT + (topSetH * 0.5f)
        entities.add(CadCircle(cx = setCenterCX, cy = setCenterCY, radius = 22f, layerId = "0"))
        entities.add(CadLine(x1 = topL + topTankW, y1 = topT, x2 = setCenterCX - 15f, y2 = setCenterCY - 15f))
        entities.add(CadLine(x1 = topL + topTankW + topSetW, y1 = topT, x2 = setCenterCX + 15f, y2 = setCenterCY - 15f))
        entities.add(CadLine(x1 = topL + topTankW, y1 = topT + topSetH, x2 = setCenterCX - 15f, y2 = setCenterCY + 15f))
        entities.add(CadLine(x1 = topL + topTankW + topSetW, y1 = topT + topSetH, x2 = setCenterCX + 15f, y2 = setCenterCY + 15f))

        // Top View Nozzles
        entities.add(CadLine(x1 = topL - 25f, y1 = topT + 225f - 10f, x2 = topL, y2 = topT + 225f - 10f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = topL - 25f, y1 = topT + 225f + 10f, x2 = topL, y2 = topT + 225f + 10f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = topL - 25f, y1 = topT + 225f - 20f, x2 = topL - 25f, y2 = topT + 225f + 20f, layerId = "NOZZLE"))

        val topOutX = topL + topTankW + topSetW
        entities.add(CadLine(x1 = topOutX, y1 = topT + 180f - 10f, x2 = topOutX + 25f, y2 = topT + 180f - 10f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = topOutX, y1 = topT + 180f + 10f, x2 = topOutX + 25f, y2 = topT + 180f + 10f, layerId = "NOZZLE"))
        entities.add(CadLine(x1 = topOutX + 25f, y1 = topT + 180f - 20f, x2 = topOutX + 25f, y2 = topT + 180f + 20f, layerId = "NOZZLE"))

        // Top View Dimensions
        entities.add(CadDimension(x1 = topL, y1 = topT, x2 = topL + topTankW, y2 = topT, offsetDistance = -40f, textOverride = "1000"))
        entities.add(CadDimension(x1 = topL + topTankW, y1 = topT, x2 = topOutX, y2 = topT, offsetDistance = -40f, textOverride = "800"))
        entities.add(CadDimension(x1 = topL, y1 = topT, x2 = topL, y2 = topT + topTankH, offsetDistance = -40f, textOverride = "1000"))
        entities.add(CadDimension(x1 = topOutX, y1 = topT, x2 = topOutX, y2 = topT + topSetH, offsetDistance = 40f, textOverride = "800"))

        return CadProject(
            name = "MBBR STP Tank & Settling Unit",
            subtitle = "2D Blueprint with Hopper Bottom & Isometric Model",
            entities = entities,
            isometricModelType = IsometricModelType.MBBR_STP_TANK,
            titleBlock = TitleBlockInfo(
                drawingTitle = "MBBR STP TANK & SETTLING TANK",
                materialOfConstruction = "SS 304 X 5 MM THK MATT FINISH",
                drawingNo = "SSEC/MBBR-02KLD",
                generalNotes = "PLATE THICKNESS - 5 MM\nAll Welds Ground Flush and Passivated.\nHydro-tested at 1.5 bar."
            )
        )
    }

    fun getSsCycloneHopperProject(): CadProject {
        val entities = mutableListOf<CadEntity>()

        // Image 2: SS Pressure Cyclone & Hopper Vessel
        // Top View:
        val topCx = 1000f
        val topCy = 300f
        val rOuter = 216.5f // Ø433
        val rFlange = 215f  // Ø430
        val rShell = 200f   // Ø400

        // Flange Circles & PCD
        entities.add(CadCircle(cx = topCx, cy = topCy, radius = rOuter, layerId = "0"))
        entities.add(CadCircle(cx = topCx, cy = topCy, radius = rFlange, layerId = "0"))
        entities.add(CadCircle(cx = topCx, cy = topCy, radius = rShell, layerId = "0"))
        entities.add(CadCircle(cx = topCx, cy = topCy, radius = 210f, isPcd = true, layerId = "CENTER")) // PCD

        // 8 Flange Bolt Holes Ø16
        for (i in 0 until 8) {
            val angle = i * 45.0 * Math.PI / 180.0
            val hx = (topCx + 210f * Math.cos(angle)).toFloat()
            val hy = (topCy + 210f * Math.sin(angle)).toFloat()
            entities.add(CadCircle(cx = hx, cy = hy, radius = 8f, layerId = "0"))
        }

        // Center 50 NB Nozzle
        entities.add(CadCircle(cx = topCx, cy = topCy, radius = 30f, layerId = "NOZZLE"))
        entities.add(CadCircle(cx = topCx, cy = topCy, radius = 60f, layerId = "NOZZLE"))
        entities.add(CadCircle(cx = topCx, cy = topCy, radius = 80f, isPcd = true, layerId = "CENTER"))

        // Top View Centerlines
        entities.add(CadLine(x1 = topCx - 260f, y1 = topCy, x2 = topCx + 260f, y2 = topCy, strokeType = StrokeType.CENTER_LINE, layerId = "CENTER"))
        entities.add(CadLine(x1 = topCx, y1 = topCy - 260f, x2 = topCx, y2 = topCy + 260f, strokeType = StrokeType.CENTER_LINE, layerId = "CENTER"))

        // Inspection Door (hinged flap 123.5 x 15 mm)
        entities.add(CadRect(x = topCx + 50f, y = topCy + 70f, width = 123.5f, height = 15f, layerId = "0"))
        // Hinge pads
        entities.add(CadRect(x = topCx + 60f, y = topCy + 60f, width = 25f, height = 10f, layerId = "0"))
        entities.add(CadRect(x = topCx + 130f, y = topCy + 60f, width = 25f, height = 10f, layerId = "0"))

        // 4 Support brackets on top view
        entities.add(CadRect(x = topCx - rShell - 65f, y = topCy - 20f, width = 65f, height = 40f, layerId = "0"))
        entities.add(CadCircle(cx = topCx - rShell - 32.5f, cy = topCy, radius = 8f, layerId = "0"))

        entities.add(CadRect(x = topCx + rShell, y = topCy - 20f, width = 65f, height = 40f, layerId = "0"))
        entities.add(CadCircle(cx = topCx + rShell + 32.5f, cy = topCy, radius = 8f, layerId = "0"))

        entities.add(CadRect(x = topCx - 20f, y = topCy - rShell - 65f, width = 40f, height = 65f, layerId = "0"))
        entities.add(CadCircle(cx = topCx, cy = topCy - rShell - 32.5f, radius = 8f, layerId = "0"))

        entities.add(CadRect(x = topCx - 20f, y = topCy + rShell, width = 40f, height = 65f, layerId = "0"))
        entities.add(CadCircle(cx = topCx, cy = topCy + rShell + 32.5f, radius = 8f, layerId = "0"))

        // Dimensions on top view
        entities.add(CadLeader(targetX = topCx, targetY = topCy - 30f, elbowX = topCx - 140f, elbowY = topCy - 100f, text = "50 NB NOZZEL"))
        entities.add(CadLeader(targetX = topCx + 110f, targetY = topCy + 75f, elbowX = topCx + 220f, elbowY = topCy + 120f, text = "DOOR - 123.5 x 15"))
        entities.add(CadDimension(type = DimensionType.LINEAR_HORIZONTAL, x1 = topCx - rOuter, y1 = topCy, x2 = topCx + rOuter, y2 = topCy, offsetDistance = -240f, textOverride = "Ø433"))
        entities.add(CadDimension(type = DimensionType.LINEAR_HORIZONTAL, x1 = topCx - rFlange, y1 = topCy, x2 = topCx + rFlange, y2 = topCy, offsetDistance = -215f, textOverride = "Ø430"))
        entities.add(CadDimension(type = DimensionType.LINEAR_HORIZONTAL, x1 = topCx - rShell, y1 = topCy, x2 = topCx + rShell, y2 = topCy, offsetDistance = -190f, textOverride = "Ø400"))

        // Front Elevation View:
        val fCx = 1000f
        val fTopY = 650f
        val fShellW = 400f
        val fFlangeW = 430f

        // Top 50NB Nozzle with flange (60 mm height)
        entities.add(CadRect(x = fCx - 40f, y = fTopY - 60f, width = 80f, height = 15f, layerId = "NOZZLE")) // Nozzle Flange
        entities.add(CadRect(x = fCx - 25f, y = fTopY - 45f, width = 50f, height = 45f, layerId = "NOZZLE")) // Nozzle Neck

        // Top Cover Flange
        entities.add(CadRect(x = fCx - fFlangeW * 0.5f, y = fTopY, width = fFlangeW, height = 18f, layerId = "0"))

        // Straight Shell: 250 mm height
        val shellH = 250f
        entities.add(CadLine(x1 = fCx - fShellW * 0.5f, y1 = fTopY + 18f, x2 = fCx - fShellW * 0.5f, y2 = fTopY + 18f + shellH))
        entities.add(CadLine(x1 = fCx + fShellW * 0.5f, y1 = fTopY + 18f, x2 = fCx + fShellW * 0.5f, y2 = fTopY + 18f + shellH))

        // Centerline
        entities.add(CadLine(x1 = fCx, y1 = fTopY - 80f, x2 = fCx, y2 = fTopY + 18f + shellH + 200f + 80f, strokeType = StrokeType.CENTER_LINE, layerId = "CENTER"))

        // Conical Bottom Hopper: 200 mm height, tapering from 400 down to 80 mm
        val coneH = 200f
        val bottomNozzleW = 70f
        val coneEndY = fTopY + 18f + shellH + coneH
        entities.add(CadLine(x1 = fCx - fShellW * 0.5f, y1 = fTopY + 18f + shellH, x2 = fCx - bottomNozzleW * 0.5f, y2 = coneEndY))
        entities.add(CadLine(x1 = fCx + fShellW * 0.5f, y1 = fTopY + 18f + shellH, x2 = fCx + bottomNozzleW * 0.5f, y2 = coneEndY))

        // Bottom Nozzle & Flange (60 mm height)
        entities.add(CadRect(x = fCx - bottomNozzleW * 0.5f, y = coneEndY, width = bottomNozzleW, height = 50f, layerId = "NOZZLE"))
        entities.add(CadRect(x = fCx - 55f, y = coneEndY + 50f, width = 110f, height = 15f, layerId = "NOZZLE"))

        // Support Brackets on elevation
        val brkY = fTopY + 18f + 150f
        entities.add(CadRect(x = fCx - fShellW * 0.5f - 65f, y = brkY, width = 65f, height = 16f, layerId = "0"))
        entities.add(CadLine(x1 = fCx - fShellW * 0.5f - 65f, y1 = brkY + 16f, x2 = fCx - fShellW * 0.5f, y2 = brkY + 60f)) // Gusset

        entities.add(CadRect(x = fCx + fShellW * 0.5f, y = brkY, width = 65f, height = 16f, layerId = "0"))
        entities.add(CadLine(x1 = fCx + fShellW * 0.5f + 65f, y1 = brkY + 16f, x2 = fCx + fShellW * 0.5f, y2 = brkY + 60f)) // Gusset

        // Elevation Dimensions (Left chain as indicated in reference blueprint)
        entities.add(CadDimension(type = DimensionType.LINEAR_VERTICAL, x1 = fCx - fShellW * 0.5f, y1 = fTopY - 60f, x2 = fCx - fShellW * 0.5f, y2 = fTopY, offsetDistance = -100f, textOverride = "60"))
        entities.add(CadDimension(type = DimensionType.LINEAR_VERTICAL, x1 = fCx - fShellW * 0.5f, y1 = fTopY + 18f, x2 = fCx - fShellW * 0.5f, y2 = fTopY + 18f + shellH, offsetDistance = -100f, textOverride = "250"))
        entities.add(CadDimension(type = DimensionType.LINEAR_VERTICAL, x1 = fCx - fShellW * 0.5f, y1 = fTopY + 18f + shellH, x2 = fCx - fShellW * 0.5f, y2 = coneEndY, offsetDistance = -100f, textOverride = "200"))
        entities.add(CadDimension(type = DimensionType.LINEAR_VERTICAL, x1 = fCx - fShellW * 0.5f, y1 = coneEndY, x2 = fCx - fShellW * 0.5f, y2 = coneEndY + 65f, offsetDistance = -100f, textOverride = "60"))
        entities.add(CadDimension(type = DimensionType.LINEAR_VERTICAL, x1 = fCx - fShellW * 0.5f, y1 = fTopY - 60f, x2 = fCx - fShellW * 0.5f, y2 = coneEndY + 65f, offsetDistance = -160f, textOverride = "570"))

        // Thickness & Material Callouts
        entities.add(CadLeader(targetX = fCx + fShellW * 0.5f, targetY = fTopY + 100f, elbowX = fCx + fShellW * 0.5f + 60f, elbowY = fTopY + 80f, text = "S.S PLATE 1.5mm THICK."))
        entities.add(CadLeader(targetX = fCx + 100f, targetY = coneEndY - 60f, elbowX = fCx + 220f, elbowY = coneEndY - 40f, text = "S.S PLATE 1.5mm THICK."))

        return CadProject(
            name = "SS Cyclone Hopper & Reactor Vessel",
            subtitle = "Ø430 Flange, Conical Bottom, 50 NB Nozzle & Hinged Door",
            entities = entities,
            isometricModelType = IsometricModelType.SS_CYCLONE_HOPPER,
            titleBlock = TitleBlockInfo(
                drawingTitle = "SS CYCLONE HOPPER VESSEL",
                materialOfConstruction = "SS 304 X 1.5 MM THK MATT FINISH",
                drawingNo = "SSEC/CH-430",
                generalNotes = "M.O.C: SS 304 1.5mm Thk.\nAll inner welds ground flush Ra < 0.4 µm mirror finish.\nEquipped with 50 NB top nozzle and conical bottom drain."
            )
        )
    }

    fun getSsMobileTrayRackProject(): CadProject {
        val entities = mutableListOf<CadEntity>()

        // Image 3: SS Mobile Tray Rack / Trolley with Castors & Multi-tier Runners
        val rX = 400f
        val rY = 220f
        val rW = 550f
        val rH = 850f

        // Frame Uprights (Square SS Tubing 38 x 38 mm)
        entities.add(CadRect(x = rX, y = rY, width = 30f, height = rH, layerId = "0"))
        entities.add(CadRect(x = rX + rW * 0.5f - 15f, y = rY, width = 30f, height = rH, layerId = "0")) // Center divider upright
        entities.add(CadRect(x = rX + rW - 30f, y = rY, width = 30f, height = rH, layerId = "0"))

        // Top canopy & Bottom base sheet
        entities.add(CadRect(x = rX - 10f, y = rY - 20f, width = rW + 20f, height = 20f, layerId = "0"))
        entities.add(CadRect(x = rX - 10f, y = rY + rH, width = rW + 20f, height = 25f, layerId = "0"))

        // 16 tiers of L-angle runners on both columns
        val numTiers = 16
        val spacing = (rH - 60f) / numTiers
        for (i in 0 until numTiers) {
            val tierY = rY + 40f + i * spacing
            // Left column runners
            entities.add(CadLine(x1 = rX + 30f, y1 = tierY, x2 = rX + rW * 0.5f - 15f, y2 = tierY, layerId = "0"))
            entities.add(CadLine(x1 = rX + 30f, y1 = tierY, x2 = rX + 45f, y2 = tierY - 12f, layerId = "0")) // angle lip

            // Right column runners
            entities.add(CadLine(x1 = rX + rW * 0.5f + 15f, y1 = tierY, x2 = rX + rW - 30f, y2 = tierY, layerId = "0"))
            entities.add(CadLine(x1 = rX + rW * 0.5f + 15f, y1 = tierY, x2 = rX + rW * 0.5f + 30f, y2 = tierY - 12f, layerId = "0")) // angle lip
        }

        // Side Utility Tray / Bin (16" shelf as noted in Image 3)
        entities.add(CadRect(x = rX - 140f, y = rY + 200f, width = 130f, height = 120f, layerId = "0"))
        entities.add(CadRect(x = rX - 140f, y = rY + 450f, width = 130f, height = 120f, layerId = "0"))
        entities.add(CadLeader(targetX = rX - 140f, targetY = rY + 260f, elbowX = rX - 220f, elbowY = rY + 220f, text = "16\" SIDE STORAGE RACK"))

        // Heavy-duty Castor Wheels at bottom (4 castors)
        val wheelY = rY + rH + 25f
        entities.add(CadCircle(cx = rX + 25f, cy = wheelY + 40f, radius = 35f, layerId = "0"))
        entities.add(CadCircle(cx = rX + 25f, cy = wheelY + 40f, radius = 10f, layerId = "0"))
        entities.add(CadRect(x = rX + 10f, y = wheelY, width = 30f, height = 15f, layerId = "0")) // Bracket

        entities.add(CadCircle(cx = rX + rW - 45f, cy = wheelY + 40f, radius = 35f, layerId = "0"))
        entities.add(CadCircle(cx = rX + rW - 45f, cy = wheelY + 40f, radius = 10f, layerId = "0"))
        entities.add(CadRect(x = rX + rW - 60f, y = wheelY, width = 30f, height = 15f, layerId = "0")) // Bracket

        // Dimensions
        entities.add(CadDimension(x1 = rX, y1 = rY - 20f, x2 = rX + rW, y2 = rY - 20f, offsetDistance = -50f, textOverride = "750 mm (W)"))
        entities.add(CadDimension(x1 = rX, y1 = rY, x2 = rX, y2 = rY + rH, offsetDistance = -60f, textOverride = "1600 mm (H)"))
        entities.add(CadDimension(x1 = rX, y1 = rY + rH, x2 = rX, y2 = wheelY + 75f, offsetDistance = -60f, textOverride = "150 mm Castor"))

        entities.add(CadText(x = rX + 80f, y = rY + rH + 140f, text = "SS 304 16-TIER MOBILE TRAY TROLLEY", textHeightMm = 24f, isBold = true))
        entities.add(CadText(x = rX + 80f, y = rY + rH + 180f, text = "Capacity: 32 Trays (16 Tiers x 2 Bays) | Swivel Castor Wheels with Brakes", textHeightMm = 16f))

        return CadProject(
            name = "SS Mobile Tray Rack & Trolley",
            subtitle = "16-Tier Trolley with Side Shelf & Heavy Duty Castors",
            entities = entities,
            isometricModelType = IsometricModelType.SS_MOBILE_TRAY_RACK,
            titleBlock = TitleBlockInfo(
                drawingTitle = "SS 304 MOBILE TRAY TROLLEY",
                materialOfConstruction = "SS 304 X 1.2 MM & 38x38 PIPE",
                drawingNo = "SSEC/TR-16T",
                generalNotes = "All corners argon welded & passivated.\nCastor wheels 100mm PU with SS 304 swivel brackets & total lock brakes."
            )
        )
    }

    fun getSsDoubleSinkProject(): CadProject {
        val entities = mutableListOf<CadEntity>()

        // Image 4: SS Commercial Double Sink Table with Splashback & Cross Bracing
        val sX = 350f
        val sY = 250f
        val sW = 800f
        val sH = 450f

        // Table Top Sheet with 100mm Rear Splashback
        entities.add(CadRect(x = sX - 20f, y = sY - 80f, width = sW + 40f, height = 80f, layerId = "0")) // Splashback
        entities.add(CadLine(x1 = sX - 20f, y1 = sY, x2 = sX + sW + 20f, y2 = sY))
        entities.add(CadRect(x = sX - 25f, y = sY, width = sW + 50f, height = 40f, layerId = "0")) // Rim / Marine edge

        // 2 Deep Pressed Sink Bowls (each 320 x 300 mm)
        val bowl1X = sX + 40f
        val bowl2X = sX + 420f
        val bowlY = sY + 60f
        val bowlW = 320f
        val bowlH = 260f

        // Bowl 1
        entities.add(CadRect(x = bowl1X, y = bowlY, width = bowlW, height = bowlH, plateThicknessMm = 1.5f, layerId = "0"))
        entities.add(CadCircle(cx = bowl1X + bowlW * 0.5f, cy = bowlY + bowlH * 0.5f, radius = 22f, layerId = "NOZZLE")) // Drain hole Ø90mm
        entities.add(CadCircle(cx = bowl1X + bowlW * 0.5f, cy = bowlY + bowlH * 0.5f, radius = 10f, layerId = "NOZZLE"))

        // Bowl 2
        entities.add(CadRect(x = bowl2X, y = bowlY, width = bowlW, height = bowlH, plateThicknessMm = 1.5f, layerId = "0"))
        entities.add(CadCircle(cx = bowl2X + bowlW * 0.5f, cy = bowlY + bowlH * 0.5f, radius = 22f, layerId = "NOZZLE"))
        entities.add(CadCircle(cx = bowl2X + bowlW * 0.5f, cy = bowlY + bowlH * 0.5f, radius = 10f, layerId = "NOZZLE"))

        // Tubular Legs (4 uprights 38mm dia)
        val legL = sX + 10f
        val legR = sX + sW - 30f
        val legB = sY + 520f
        entities.add(CadRect(x = legL, y = sY + 40f, width = 35f, height = 480f, layerId = "0"))
        entities.add(CadRect(x = legR, y = sY + 40f, width = 35f, height = 480f, layerId = "0"))

        // Cross Bracing / Under-pipe
        val braceY = sY + 380f
        entities.add(CadRect(x = legL + 35f, y = braceY, width = legR - legL - 35f, height = 25f, layerId = "0"))

        // Adjustable Bullet Feet
        entities.add(CadRect(x = legL + 5f, y = legB, width = 25f, height = 25f, layerId = "0"))
        entities.add(CadRect(x = legR + 5f, y = legB, width = 25f, height = 25f, layerId = "0"))

        // Dimensions
        entities.add(CadDimension(x1 = sX - 25f, y1 = sY - 80f, x2 = sX + sW + 25f, y2 = sY - 80f, offsetDistance = -50f, textOverride = "1500 mm (Overall Length)"))
        entities.add(CadDimension(x1 = sX - 25f, y1 = sY - 80f, x2 = sX - 25f, y2 = sY, offsetDistance = -50f, textOverride = "100 mm Splashback"))
        entities.add(CadDimension(x1 = sX - 25f, y1 = sY, x2 = sX - 25f, y2 = legB + 25f, offsetDistance = -80f, textOverride = "850 mm Working Height"))
        entities.add(CadDimension(x1 = bowl1X, y1 = bowlY, x2 = bowl1X + bowlW, y2 = bowlY, offsetDistance = -30f, textOverride = "500 mm Bowl"))

        // Callouts
        entities.add(CadLeader(targetX = sX + 150f, targetY = sY - 40f, elbowX = sX + 120f, elbowY = sY - 120f, text = "100 mm REAR SPLASHBACK"))
        entities.add(CadLeader(targetX = bowl1X + bowlW * 0.5f, targetY = bowlY + bowlH * 0.5f, elbowX = bowl1X - 60f, elbowY = bowlY + 160f, text = "DEEP DRAWN SS SINK BOWL Ø90 DRAIN"))

        entities.add(CadText(x = sX + 120f, y = legB + 100f, text = "SS 304 COMMERCIAL DOUBLE SINK UNIT", textHeightMm = 24f, isBold = true))
        entities.add(CadText(x = sX + 120f, y = legB + 140f, text = "Top: SS 304 1.5mm Thk | Frame: 38mm OD SS 304 Pipe with Adjustable Bullet Feet", textHeightMm = 16f))

        return CadProject(
            name = "SS Commercial Double Sink Unit",
            subtitle = "Two Deep Pressed Bowls, Under-pipe Bracing & Rear Splashback",
            entities = entities,
            isometricModelType = IsometricModelType.SS_DOUBLE_SINK_TABLE,
            titleBlock = TitleBlockInfo(
                drawingTitle = "SS 304 DOUBLE SINK TABLE",
                materialOfConstruction = "SS 304 X 1.5 MM THK MATT FINISH",
                drawingNo = "SSEC/DS-1500",
                generalNotes = "Pressed seamless bowls with anti-drip marine edges.\nAll pipe joints argon TIG welded and passivated."
            )
        )
    }

    fun getSsLockerCabinetProject(): CadProject {
        val entities = mutableListOf<CadEntity>()

        // Image 5: SS 304 Shoes & Apron Locker
        val lX = 400f
        val lY = 240f
        val lW = 750f
        val lH = 800f

        // Outer Locker Cabinet Body
        entities.add(CadRect(x = lX, y = lY, width = lW, height = lH, plateThicknessMm = 1.2f, layerId = "0"))

        // Sanitary Sloped Top (avoids dust accumulation per pharma standards)
        entities.add(CadLine(x1 = lX, y1 = lY, x2 = lX, y2 = lY - 80f, layerId = "0"))
        entities.add(CadLine(x1 = lX, y1 = lY - 80f, x2 = lX + lW, y2 = lY, layerId = "0"))

        // Vertical Compartment Dividers (3 bays)
        val bayW = lW / 3f
        entities.add(CadLine(x1 = lX + bayW, y1 = lY, x2 = lX + bayW, y2 = lY + lH, layerId = "0"))
        entities.add(CadLine(x1 = lX + bayW * 2f, y1 = lY, x2 = lX + bayW * 2f, y2 = lY + lH, layerId = "0"))

        // Horizontal Shelves (Top Apron Hanging Section + Bottom Shoes Compartments)
        val apronH = lH * 0.65f
        entities.add(CadLine(x1 = lX, y1 = lY + apronH, x2 = lX + lW, y2 = lY + apronH, layerId = "0"))

        // Bottom Shoes Sub-shelves (2 tiers per bay)
        val shoeTierH = (lH - apronH) * 0.5f
        entities.add(CadLine(x1 = lX, y1 = lY + apronH + shoeTierH, x2 = lX + lW, y2 = lY + apronH + shoeTierH, layerId = "0"))

        // Louvers & Laser Etched Numbering Badges
        for (bay in 0 until 3) {
            val cx = lX + bay * bayW + bayW * 0.5f
            // Door Handles & Numbering plate
            entities.add(CadRect(x = cx - 25f, y = lY + 40f, width = 50f, height = 25f, layerId = "TEXT"))
            entities.add(CadText(x = cx - 18f, y = lY + 58f, text = "0${bay + 1}", textHeightMm = 14f, isBold = true, customColorArgb = 0xFFF59E0BL))

            // Ventilation Louvers
            for (v in 0 until 4) {
                entities.add(CadLine(x1 = cx - 35f, y1 = lY + 120f + v * 16f, x2 = cx + 35f, y2 = lY + 120f + v * 16f, layerId = "0"))
            }

            // Shoes section labels
            entities.add(CadText(x = cx - 30f, y = lY + apronH + 30f, text = "SHOE A", textHeightMm = 10f))
            entities.add(CadText(x = cx - 30f, y = lY + apronH + shoeTierH + 30f, text = "SHOE B", textHeightMm = 10f))
        }

        // Base skirting legs (100mm height)
        entities.add(CadRect(x = lX + 20f, y = lY + lH, width = 45f, height = 70f, layerId = "0"))
        entities.add(CadRect(x = lX + lW - 65f, y = lY + lH, width = 45f, height = 70f, layerId = "0"))

        // Dimensions
        entities.add(CadDimension(x1 = lX, y1 = lY - 80f, x2 = lX + lW, y2 = lY - 80f, offsetDistance = -40f, textOverride = "1200 mm Width"))
        entities.add(CadDimension(x1 = lX, y1 = lY - 80f, x2 = lX, y2 = lY + lH + 70f, offsetDistance = -60f, textOverride = "1950 mm Height"))
        entities.add(CadDimension(x1 = lX, y1 = lY + apronH, x2 = lX, y2 = lY + lH, offsetDistance = -30f, textOverride = "600 mm Shoe Base"))

        entities.add(CadLeader(targetX = lX + bayW * 0.5f, targetY = lY + 50f, elbowX = lX - 120f, elbowY = lY + 30f, text = "LASER PRINTED ID NUMBERS"))
        entities.add(CadLeader(targetX = lX + lW * 0.5f, targetY = lY - 40f, elbowX = lX + lW + 40f, elbowY = lY - 50f, text = "SLOPED HYGIENIC TOP"))

        return CadProject(
            name = "SS 304 Shoes & Apron Locker",
            subtitle = "Cleanroom Pharma Furniture with Laser Etching & Sloped Top",
            entities = entities,
            isometricModelType = IsometricModelType.SS_LOCKER_CABINET,
            titleBlock = TitleBlockInfo(
                clientName = "ARISTO PHARMA PVT LTD.",
                clientAddress = "DAMAN",
                consultant = "-",
                drawnBy = "RIPEN KOLI",
                checkedBy = "DUD",
                approvedBy = "DUD",
                scale = "N.T.S.",
                jobNo = "JOB-SSEC-4491",
                companyName = "SHREE SAI ENGINEERING COMPANY",
                companyAddress = "A/16, GLOBAL INDUSTRIAL PARK, SURVEY NO. 26/2, NEAR NAHULI RAILWAY CROSSING, OFF N.H. No.8, VALVADA - VAPI, DIST. VALSAD - 396 105",
                companyEmail = "shreesaiengineeringworks@gmail.com",
                drawingTitle = "SS 304 SHOES AND APRON LOCKER",
                materialOfConstruction = "SS 304 X 1.2 X 1MM THK MATT FINISH",
                drawingNo = "SSEC/116",
                drawingDate = "22/09/2026",
                poNo = "PO/4491",
                poDate = "15/09/2026",
                generalNotes = "Note:- All Numbers is Laser Printing As Per Your Requirement.\nMaterial: SS 304 sheet 1.2mm outer, 1.0mm inner partition. Matt finish."
            )
        )
    }

    fun getAllProjects(): List<CadProject> = listOf(
        getMbbrStpTankProject(),
        getSsCycloneHopperProject(),
        getSsMobileTrayRackProject(),
        getSsDoubleSinkProject(),
        getSsLockerCabinetProject()
    )
}
