 package com.mjm.elixir_reign.shared.worldgen

import com.mjm.elixir_reign.shared.terrain.TerrainType
import com.mjm.elixir_reign.shared.world.WorldMap
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

object ProceduralTerrainGenerator {
    fun generate(config: MapGenConfig): WorldMap {
        var failureReason = "Aucune tentative effectuee."

        repeat(config.maxGenerationAttempts) { attempt ->
            val attemptSeed = config.seed + ATTEMPT_SEED_STEP * attempt
            val context = GenerationContext(config = config, seed = attemptSeed)

            generateTerrain(context)

            val validation = MapValidator.validate(context)
            if (validation.isValid) {
                return buildWorldMap(context)
            }

            failureReason = validation.reason
        }

        error("Impossible de generer une map valide apres ${config.maxGenerationAttempts} tentatives: $failureReason")
    }

    private fun generateTerrain(context: GenerationContext) {
        context.grid.fill(MaterialType.GRASS)

        markCornerSpawnZones(context)
        carveCentralRiverCross(context)
        carveRiverArms(context)
        reapplyProtectedGrass(context)
        placeSandPatches(context)
        placeSymmetricResourcePatches(
            context = context,
            material = MaterialType.GOLD,
            rarity = context.config.goldRarity,
            maxCoverage = GOLD_MAX_COVERAGE,
            minCoverage = GOLD_MIN_COVERAGE
        )
        placeSymmetricResourcePatches(
            context = context,
            material = MaterialType.ELEXIR,
            rarity = context.config.elixirRarity,
            maxCoverage = ELEXIR_MAX_COVERAGE,
            minCoverage = ELEXIR_MIN_COVERAGE
        )
        reapplyProtectedGrass(context)
    }

    private fun buildWorldMap(context: GenerationContext): WorldMap {
        return WorldMap.build(
            width = context.config.width,
            height = context.config.height,
            chunkSize = context.config.chunkSize
        ) { row, col ->
            val material = context.grid[row, col] ?: return@build null
            material.toTerrainType(row = row, col = col, seed = context.seed)
        }
    }

    private fun markCornerSpawnZones(context: GenerationContext) {
        val size = context.config.spawnZoneSize
        val rowRanges = listOf(0 until size, context.config.height - size until context.config.height)
        val colRanges = listOf(0 until size, context.config.width - size until context.config.width)

        rowRanges.forEach { rows ->
            colRanges.forEach { cols ->
                for (row in rows) {
                    for (col in cols) {
                        context.setProtected(row, col)
                        context.grid[row, col] = MaterialType.GRASS
                    }
                }
            }
        }
    }

    private fun carveRiverArms(context: GenerationContext) {
        carveVerticalRiver(context, headingNorth = true)
        carveVerticalRiver(context, headingNorth = false)
        carveHorizontalRiver(context, headingWest = true)
        carveHorizontalRiver(context, headingWest = false)
    }

    private fun carveCentralRiverCross(context: GenerationContext) {
        val minDimension = min(context.config.width, context.config.height).toFloat()
        val baseWidth = context.config.riverWidthMax
        val crossReach = max(
            baseWidth + 2,
            (minDimension * context.config.lakeRadiusFraction * 0.45f).roundToInt()
        )
        val centerWidthBonus = max(1, baseWidth)
        val roughnessAmplitude = max(1, (context.config.lakeRoughness * 4f).roundToInt())
        val verticalPhase = context.random.nextFloat(0f, (PI * 2f).toFloat())
        val horizontalPhase = context.random.nextFloat(0f, (PI * 2f).toFloat())

        for (offset in -crossReach..crossReach) {
            val row = context.centerRow + offset
            if (!context.grid.inBounds(row, context.centerCol)) {
                continue
            }

            val width = computeCentralCrossWidth(
                baseWidth = baseWidth,
                centerWidthBonus = centerWidthBonus,
                normalizedCenterDistance = 1f - abs(offset).toFloat() / crossReach.toFloat(),
                wobble = computeChannelWobble(
                    offset = offset,
                    phase = verticalPhase,
                    amplitude = roughnessAmplitude
                )
            )
            val anchorCol = centeredAnchor(
                center = context.centerCol,
                width = width,
                limit = context.config.width
            )
            paintVerticalRiverStroke(context, row = row, anchorCol = anchorCol, width = width)
        }

        for (offset in -crossReach..crossReach) {
            val col = context.centerCol + offset
            if (!context.grid.inBounds(context.centerRow, col)) {
                continue
            }

            val width = computeCentralCrossWidth(
                baseWidth = baseWidth,
                centerWidthBonus = centerWidthBonus,
                normalizedCenterDistance = 1f - abs(offset).toFloat() / crossReach.toFloat(),
                wobble = computeChannelWobble(
                    offset = offset,
                    phase = horizontalPhase,
                    amplitude = roughnessAmplitude
                )
            )
            val anchorRow = centeredAnchor(
                center = context.centerRow,
                width = width,
                limit = context.config.height
            )
            paintHorizontalRiverStroke(context, anchorRow = anchorRow, col = col, width = width)
        }
    }

    private fun carveVerticalRiver(context: GenerationContext, headingNorth: Boolean) {
        val start = findLakeExit(context, if (headingNorth) Direction.NORTH else Direction.SOUTH) ?: return
        val riverWidth = pickRiverWidth(context)
        val minAnchorCol = context.config.spawnZoneSize + 1
        val maxAnchorCol = context.config.width - context.config.spawnZoneSize - riverWidth
        val startAnchorCol = (start.col - riverWidth / 2).coerceIn(minAnchorCol, maxAnchorCol)
        val endRow = if (headingNorth) 0 else context.config.height - 1
        val stepDirection = if (headingNorth) -1 else 1
        val totalSteps = kotlin.math.abs(endRow - start.row)

        if (totalSteps == 0) {
            paintVerticalRiverStroke(context, row = start.row, anchorCol = startAnchorCol, width = riverWidth)
            return
        }

        val idealAmplitude = max(4, (context.config.width * (0.06f + context.config.riverCurviness * 0.2f)).toInt())
        val maxEndDrift = max(2, idealAmplitude / 3)
        val endAnchorCol = (startAnchorCol + context.random.nextInt(-maxEndDrift, maxEndDrift + 1))
            .coerceIn(minAnchorCol, maxAnchorCol)
        var bendSign = chooseRiverBendSign(
            random = context.random,
            startAnchor = startAnchorCol,
            endAnchor = endAnchorCol,
            minAnchor = minAnchorCol,
            maxAnchor = maxAnchorCol
        )
        var amplitude = computeRiverBezierAmplitude(
            startAnchor = startAnchorCol,
            endAnchor = endAnchorCol,
            bendSign = bendSign,
            minAnchor = minAnchorCol,
            maxAnchor = maxAnchorCol,
            idealAmplitude = idealAmplitude
        )
        if (amplitude <= 1) {
            bendSign *= -1
            amplitude = computeRiverBezierAmplitude(
                startAnchor = startAnchorCol,
                endAnchor = endAnchorCol,
                bendSign = bendSign,
                minAnchor = minAnchorCol,
                maxAnchor = maxAnchorCol,
                idealAmplitude = idealAmplitude
            )
        }

        val control1Col = (startAnchorCol + bendSign * amplitude).coerceIn(minAnchorCol, maxAnchorCol)
        val control2Col = (endAnchorCol - bendSign * amplitude).coerceIn(minAnchorCol, maxAnchorCol)

        var previousAnchorCol: Int? = null

        for (step in 0..totalSteps) {
            val t = step.toFloat() / totalSteps.toFloat()
            val row = start.row + step * stepDirection
            val anchorCol = cubicBezier(
                p0 = startAnchorCol.toFloat(),
                p1 = control1Col.toFloat(),
                p2 = control2Col.toFloat(),
                p3 = endAnchorCol.toFloat(),
                t = t
            ).toInt().coerceIn(minAnchorCol, maxAnchorCol)

            paintVerticalRiverStroke(context, row = row, anchorCol = anchorCol, width = riverWidth)
            previousAnchorCol?.let { previous ->
                paintVerticalRiverBridge(
                    context = context,
                    row = row,
                    fromAnchorCol = previous,
                    toAnchorCol = anchorCol,
                    width = riverWidth
                )
            }
            previousAnchorCol = anchorCol
        }
    }

    private fun carveHorizontalRiver(context: GenerationContext, headingWest: Boolean) {
        val start = findLakeExit(context, if (headingWest) Direction.WEST else Direction.EAST) ?: return
        val riverWidth = pickRiverWidth(context)
        val minAnchorRow = context.config.spawnZoneSize + 1
        val maxAnchorRow = context.config.height - context.config.spawnZoneSize - riverWidth
        val startAnchorRow = (start.row - riverWidth / 2).coerceIn(minAnchorRow, maxAnchorRow)
        val endCol = if (headingWest) 0 else context.config.width - 1
        val stepDirection = if (headingWest) -1 else 1
        val totalSteps = kotlin.math.abs(endCol - start.col)

        if (totalSteps == 0) {
            paintHorizontalRiverStroke(context, anchorRow = startAnchorRow, col = start.col, width = riverWidth)
            return
        }

        val idealAmplitude = max(4, (context.config.height * (0.06f + context.config.riverCurviness * 0.2f)).toInt())
        val maxEndDrift = max(2, idealAmplitude / 3)
        val endAnchorRow = (startAnchorRow + context.random.nextInt(-maxEndDrift, maxEndDrift + 1))
            .coerceIn(minAnchorRow, maxAnchorRow)
        var bendSign = chooseRiverBendSign(
            random = context.random,
            startAnchor = startAnchorRow,
            endAnchor = endAnchorRow,
            minAnchor = minAnchorRow,
            maxAnchor = maxAnchorRow
        )
        var amplitude = computeRiverBezierAmplitude(
            startAnchor = startAnchorRow,
            endAnchor = endAnchorRow,
            bendSign = bendSign,
            minAnchor = minAnchorRow,
            maxAnchor = maxAnchorRow,
            idealAmplitude = idealAmplitude
        )
        if (amplitude <= 1) {
            bendSign *= -1
            amplitude = computeRiverBezierAmplitude(
                startAnchor = startAnchorRow,
                endAnchor = endAnchorRow,
                bendSign = bendSign,
                minAnchor = minAnchorRow,
                maxAnchor = maxAnchorRow,
                idealAmplitude = idealAmplitude
            )
        }

        val control1Row = (startAnchorRow + bendSign * amplitude).coerceIn(minAnchorRow, maxAnchorRow)
        val control2Row = (endAnchorRow - bendSign * amplitude).coerceIn(minAnchorRow, maxAnchorRow)

        var previousAnchorRow: Int? = null

        for (step in 0..totalSteps) {
            val t = step.toFloat() / totalSteps.toFloat()
            val col = start.col + step * stepDirection
            val anchorRow = cubicBezier(
                p0 = startAnchorRow.toFloat(),
                p1 = control1Row.toFloat(),
                p2 = control2Row.toFloat(),
                p3 = endAnchorRow.toFloat(),
                t = t
            ).toInt().coerceIn(minAnchorRow, maxAnchorRow)

            paintHorizontalRiverStroke(context, anchorRow = anchorRow, col = col, width = riverWidth)
            previousAnchorRow?.let { previous ->
                paintHorizontalRiverBridge(
                    context = context,
                    col = col,
                    fromAnchorRow = previous,
                    toAnchorRow = anchorRow,
                    width = riverWidth
                )
            }
            previousAnchorRow = anchorRow
        }
    }

    private fun placeSandPatches(context: GenerationContext) {
        val eligibleCells = countCells(context) { row, col, material ->
            material == MaterialType.GRASS && !context.isProtected(row, col)
        }
        val targetSandCells = (eligibleCells * context.config.sandCoverage).roundToInt()
        var sandPlaced = 0
        var attempts = 0

        while (sandPlaced < targetSandCells && attempts < targetSandCells * 8) {
            attempts++
            val seed = randomCell(context) { row, col ->
                context.grid[row, col] == MaterialType.GRASS &&
                    !context.isProtected(row, col)
            } ?: break

            val patchTarget = context.random.nextInt(6, max(10, min(context.config.width, context.config.height) / 2))
            val patch = growBlob(
                seed = seed,
                targetSize = patchTarget,
                random = context.random,
                allowDiagonal = true
            ) { row, col ->
                context.grid[row, col] == MaterialType.GRASS &&
                    !context.isProtected(row, col)
            }

            patch.forEach { cell ->
                if (sandPlaced >= targetSandCells) {
                    return@forEach
                }

                if (context.grid[cell.row, cell.col] == MaterialType.GRASS) {
                    context.grid[cell.row, cell.col] = MaterialType.SAND
                    sandPlaced++
                }
            }
        }
    }

    private fun placeSymmetricResourcePatches(
        context: GenerationContext,
        material: MaterialType,
        rarity: Float,
        maxCoverage: Float,
        minCoverage: Float
    ) {
        val halfHeight = context.config.height / 2
        val halfWidth = context.config.width / 2
        var availableNorthWest = 0

        for (row in 0 until halfHeight) {
            for (col in 0 until halfWidth) {
                if (isResourceBaseCandidate(context, row, col)) {
                    availableNorthWest++
                }
            }
        }

        val coverage = lerp(maxCoverage, minCoverage, rarity)
        val targetCellsPerQuadrant = max(3, (availableNorthWest * coverage).roundToInt())
        var placedCellsPerQuadrant = 0
        var attempts = 0

        while (placedCellsPerQuadrant < targetCellsPerQuadrant && attempts < RESOURCE_ATTEMPTS_LIMIT) {
            attempts++
            val seed = randomNorthWestCell(context) ?: break
            val remaining = targetCellsPerQuadrant - placedCellsPerQuadrant
            val patchTarget = min(remaining, context.random.nextInt(3, 8))
            val patch = growBlob(
                seed = seed,
                targetSize = patchTarget,
                random = context.random,
                allowDiagonal = true
            ) { row, col ->
                row in 0 until halfHeight &&
                    col in 0 until halfWidth &&
                    isResourceBaseCandidate(context, row, col)
            }

            if (patch.isEmpty()) {
                continue
            }

            val mirroredPatch = patch.map { baseCell ->
                mirroredCells(context, baseCell.row, baseCell.col)
            }

            val canPaint = mirroredPatch.all { group ->
                group.all { cell ->
                    val cellMaterial = context.grid[cell.row, cell.col]
                    cellMaterial == MaterialType.GRASS &&
                        !context.isProtected(cell.row, cell.col)
                }
            }

            if (!canPaint) {
                continue
            }

            mirroredPatch.forEach { group ->
                group.forEach { cell ->
                    context.grid[cell.row, cell.col] = material
                }
            }
            placedCellsPerQuadrant += patch.size
        }
    }

    private fun reapplyProtectedGrass(context: GenerationContext) {
        context.forEachProtected { row, col ->
            context.grid[row, col] = MaterialType.GRASS
        }
    }

    private fun findLakeExit(context: GenerationContext, direction: Direction): GridCell? {
        var row = context.centerRow
        var col = context.centerCol
        var lastWater: GridCell? = null
        var hasSeenWater = false

        repeat(max(context.config.width, context.config.height)) {
            if (!context.grid.inBounds(row, col)) {
                return lastWater
            }

            val material = context.grid[row, col]
            if (material == MaterialType.WATER) {
                lastWater = GridCell(row = row, col = col)
                hasSeenWater = true
            } else if (hasSeenWater) {
                return lastWater
            }

            row += direction.rowDelta
            col += direction.colDelta
        }

        return lastWater
    }

    private fun pickRiverWidth(context: GenerationContext): Int {
        if (context.config.riverWidthMin == context.config.riverWidthMax) {
            return context.config.riverWidthMin
        }

        return context.random.nextInt(context.config.riverWidthMin, context.config.riverWidthMax + 1)
    }

    private fun chooseRiverBendSign(
        random: Random,
        startAnchor: Int,
        endAnchor: Int,
        minAnchor: Int,
        maxAnchor: Int
    ): Int {
        val positiveRoom = min(maxAnchor - startAnchor, endAnchor - minAnchor)
        val negativeRoom = min(startAnchor - minAnchor, maxAnchor - endAnchor)

        return when {
            positiveRoom <= 1 && negativeRoom <= 1 -> if (random.nextBoolean()) 1 else -1
            positiveRoom <= 1 -> -1
            negativeRoom <= 1 -> 1
            random.nextBoolean() -> 1
            else -> -1
        }
    }

    private fun computeRiverBezierAmplitude(
        startAnchor: Int,
        endAnchor: Int,
        bendSign: Int,
        minAnchor: Int,
        maxAnchor: Int,
        idealAmplitude: Int
    ): Int {
        val maxAllowed = if (bendSign > 0) {
            min(maxAnchor - startAnchor, endAnchor - minAnchor)
        } else {
            min(startAnchor - minAnchor, maxAnchor - endAnchor)
        }

        return idealAmplitude.coerceAtMost(maxAllowed).coerceAtLeast(1)
    }

    private fun cubicBezier(p0: Float, p1: Float, p2: Float, p3: Float, t: Float): Float {
        val oneMinusT = 1f - t
        return oneMinusT * oneMinusT * oneMinusT * p0 +
            3f * oneMinusT * oneMinusT * t * p1 +
            3f * oneMinusT * t * t * p2 +
            t * t * t * p3
    }

    private fun paintVerticalRiverStroke(context: GenerationContext, row: Int, anchorCol: Int, width: Int) {
        repeat(width) { offset ->
            paintWaterCell(context, row, anchorCol + offset)
        }
    }

    private fun paintVerticalRiverBridge(
        context: GenerationContext,
        row: Int,
        fromAnchorCol: Int,
        toAnchorCol: Int,
        width: Int
    ) {
        val minCol = min(fromAnchorCol, toAnchorCol)
        val maxCol = max(fromAnchorCol, toAnchorCol) + width - 1
        for (col in minCol..maxCol) {
            paintWaterCell(context, row, col)
        }
    }

    private fun paintHorizontalRiverStroke(context: GenerationContext, anchorRow: Int, col: Int, width: Int) {
        repeat(width) { offset ->
            paintWaterCell(context, anchorRow + offset, col)
        }
    }

    private fun paintHorizontalRiverBridge(
        context: GenerationContext,
        col: Int,
        fromAnchorRow: Int,
        toAnchorRow: Int,
        width: Int
    ) {
        val minRow = min(fromAnchorRow, toAnchorRow)
        val maxRow = max(fromAnchorRow, toAnchorRow) + width - 1
        for (row in minRow..maxRow) {
            paintWaterCell(context, row, col)
        }
    }

    private fun paintWaterCell(context: GenerationContext, row: Int, col: Int) {
        if (!context.grid.inBounds(row, col) || context.isProtected(row, col)) {
            return
        }
        context.grid[row, col] = MaterialType.WATER
    }

    private fun paintGrassCell(context: GenerationContext, row: Int, col: Int) {
        if (!context.grid.inBounds(row, col) || context.isProtected(row, col)) {
            return
        }
        context.grid[row, col] = MaterialType.GRASS
    }

    private fun fillPolygon(
        context: GenerationContext,
        polygon: List<FloatPoint>,
        paint: (row: Int, col: Int) -> Unit
    ) {
        if (polygon.isEmpty()) {
            return
        }

        val minRow = max(0, polygon.minOf { it.row }.toInt() - 1)
        val maxRow = min(context.config.height - 1, polygon.maxOf { it.row }.toInt() + 1)
        val minCol = max(0, polygon.minOf { it.col }.toInt() - 1)
        val maxCol = min(context.config.width - 1, polygon.maxOf { it.col }.toInt() + 1)

        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                val inside = pointInPolygon(
                    row = row + 0.5f,
                    col = col + 0.5f,
                    polygon = polygon
                )
                if (inside) {
                    paint(row, col)
                }
            }
        }
    }

    private fun buildOrganicPolygon(
        centerRow: Float,
        centerCol: Float,
        baseRadius: Float,
        roughness: Float,
        pointCount: Int,
        random: Random,
        scaleX: Float,
        scaleY: Float,
        smoothPasses: Int = 3,
        harmonicStrength: Float = 0f,
        featureCount: Int = 0,
        featureStrength: Float = 0f
    ): List<FloatPoint> {
        val radii = FloatArray(pointCount) {
            baseRadius * (1f + random.nextFloat(-roughness, roughness))
        }

        if (harmonicStrength > 0f) {
            val phase2 = random.nextFloat(0f, (PI * 2f).toFloat())
            val phase3 = random.nextFloat(0f, (PI * 2f).toFloat())
            val phase5 = random.nextFloat(0f, (PI * 2f).toFloat())

            radii.indices.forEach { index ->
                val angle = index.toFloat() / pointCount.toFloat() * (PI * 2f).toFloat()
                val harmonicOffset =
                    sin(angle * 2f + phase2) * 0.55f +
                        sin(angle * 3f + phase3) * 0.3f +
                        sin(angle * 5f + phase5) * 0.15f
                radii[index] += baseRadius * harmonicStrength * harmonicOffset
            }
        }

        repeat(featureCount) {
            val centerIndex = random.nextInt(pointCount)
            val radiusInIndices = random.nextInt(max(2, pointCount / 14), max(3, pointCount / 6))
            val amplitude = baseRadius * featureStrength * random.nextFloat(-1f, 1f)

            radii.indices.forEach { index ->
                val directDistance = kotlin.math.abs(index - centerIndex)
                val circularDistance = min(directDistance, pointCount - directDistance)
                if (circularDistance > radiusInIndices) {
                    return@forEach
                }

                val influence = 1f - circularDistance.toFloat() / radiusInIndices.toFloat()
                radii[index] += amplitude * influence * influence
            }
        }

        radii.indices.forEach { index ->
            radii[index] = radii[index].coerceAtLeast(baseRadius * 0.55f)
        }

        repeat(smoothPasses) {
            val previous = radii.copyOf()
            for (index in radii.indices) {
                val left = previous[(index - 1 + pointCount) % pointCount]
                val current = previous[index]
                val right = previous[(index + 1) % pointCount]
                radii[index] = (left + current * 2f + right) / 4f
            }
        }

        return List(pointCount) { index ->
            val angle = index.toFloat() / pointCount.toFloat() * (PI * 2f).toFloat()
            val radius = radii[index]
            FloatPoint(
                row = centerRow + sin(angle) * radius * scaleY,
                col = centerCol + cos(angle) * radius * scaleX
            )
        }
    }

    private fun pointInPolygon(row: Float, col: Float, polygon: List<FloatPoint>): Boolean {
        var inside = false
        var previous = polygon.last()

        polygon.forEach { current ->
            val intersects = ((current.row > row) != (previous.row > row)) &&
                (col < (previous.col - current.col) * (row - current.row) / (previous.row - current.row + EPSILON) + current.col)
            if (intersects) {
                inside = !inside
            }
            previous = current
        }

        return inside
    }

    private fun computeCentralCrossWidth(
        baseWidth: Int,
        centerWidthBonus: Int,
        normalizedCenterDistance: Float,
        wobble: Int
    ): Int {
        val clampedDistance = normalizedCenterDistance.coerceIn(0f, 1f)
        val centerBoost = (clampedDistance * clampedDistance * centerWidthBonus.toFloat()).roundToInt()
        return (baseWidth + centerBoost + wobble).coerceAtLeast(baseWidth)
    }

    private fun computeChannelWobble(offset: Int, phase: Float, amplitude: Int): Int {
        if (amplitude <= 0) {
            return 0
        }

        val offsetValue = offset.toFloat()
        val primary = sin(offsetValue * 0.55f + phase) * amplitude.toFloat()
        val secondary = sin(offsetValue * 0.23f + phase * 0.61f) * amplitude.toFloat() * 0.45f
        return ((primary + secondary) * 0.35f).roundToInt()
    }

    private fun centeredAnchor(center: Int, width: Int, limit: Int): Int {
        return (center - width / 2).coerceIn(0, max(0, limit - width))
    }

    private fun growBlob(
        seed: GridCell,
        targetSize: Int,
        random: Random,
        allowDiagonal: Boolean,
        canUse: (row: Int, col: Int) -> Boolean
    ): List<GridCell> {
        if (!canUse(seed.row, seed.col) || targetSize <= 0) {
            return emptyList()
        }

        val blob = mutableListOf(seed)
        val seen = linkedSetOf(seed)
        var stagnation = 0
        val maxStagnation = targetSize * 12

        while (blob.size < targetSize && stagnation < maxStagnation) {
            val origin = blob[random.nextInt(blob.size)]
            val candidates = neighborCells(origin, allowDiagonal = allowDiagonal)
            val candidate = candidates[random.nextInt(candidates.size)]

            if (canUse(candidate.row, candidate.col) && seen.add(candidate)) {
                blob += candidate
                stagnation = 0
            } else {
                stagnation++
            }
        }

        return blob
    }

    private fun randomCell(
        context: GenerationContext,
        predicate: (row: Int, col: Int) -> Boolean
    ): GridCell? {
        repeat(RANDOM_CELL_ATTEMPTS) {
            val row = context.random.nextInt(context.config.height)
            val col = context.random.nextInt(context.config.width)
            if (predicate(row, col)) {
                return GridCell(row = row, col = col)
            }
        }
        return null
    }

    private fun randomNorthWestCell(context: GenerationContext): GridCell? {
        repeat(RANDOM_CELL_ATTEMPTS) {
            val row = context.random.nextInt(context.config.height / 2)
            val col = context.random.nextInt(context.config.width / 2)
            if (isResourceBaseCandidate(context, row, col)) {
                return GridCell(row = row, col = col)
            }
        }
        return null
    }

    private fun isResourceBaseCandidate(context: GenerationContext, row: Int, col: Int): Boolean {
        return context.grid[row, col] == MaterialType.GRASS &&
            !context.isProtected(row, col)
    }

    private fun mirroredCells(context: GenerationContext, row: Int, col: Int): List<GridCell> {
        val mirrored = linkedSetOf<GridCell>()
        mirrored += GridCell(row = row, col = col)
        mirrored += GridCell(row = row, col = context.config.width - 1 - col)
        mirrored += GridCell(row = context.config.height - 1 - row, col = col)
        mirrored += GridCell(
            row = context.config.height - 1 - row,
            col = context.config.width - 1 - col
        )
        return mirrored.toList()
    }

    private fun countCells(
        context: GenerationContext,
        predicate: (row: Int, col: Int, material: MaterialType) -> Boolean
    ): Int {
        var count = 0
        context.grid.forEachIndexed { row, col, material ->
            if (predicate(row, col, material)) {
                count++
            }
        }
        return count
    }

    private fun neighborCells(cell: GridCell, allowDiagonal: Boolean): List<GridCell> {
        val directions = if (allowDiagonal) {
            ALL_NEIGHBOR_STEPS
        } else {
            CARDINAL_STEPS
        }
        return directions.map { (rowDelta, colDelta) ->
            GridCell(row = cell.row + rowDelta, col = cell.col + colDelta)
        }
    }

    private fun MaterialType.toTerrainType(row: Int, col: Int, seed: Long): TerrainType {
        return when (this) {
            MaterialType.GRASS -> grassVariant(seed, row, col)
            MaterialType.SAND -> sandVariant(seed, row, col)
            MaterialType.WATER -> waterVariant(seed, row, col)
            MaterialType.GOLD -> TerrainType.GOLD
            MaterialType.ELEXIR -> TerrainType.ELEXIR
            MaterialType.DARK_ELEXIR -> TerrainType.DARK_ELEXIR
        }
    }

    private fun grassVariant(seed: Long, row: Int, col: Int): TerrainType {
        return when (variantIndex(seed = seed, row = row, col = col, salt = 11L)) {
            0 -> TerrainType.GRASS_1
            1 -> TerrainType.GRASS_2
            else -> TerrainType.GRASS_3
        }
    }

    private fun sandVariant(seed: Long, row: Int, col: Int): TerrainType {
        return when (variantIndex(seed = seed, row = row, col = col, salt = 23L)) {
            0 -> TerrainType.SAND_1
            1 -> TerrainType.SAND_2
            else -> TerrainType.SAND_3
        }
    }

    private fun waterVariant(seed: Long, row: Int, col: Int): TerrainType {
        return when (variantIndex(seed = seed, row = row, col = col, salt = 37L)) {
            0 -> TerrainType.WATER_1
            1 -> TerrainType.WATER_2
            else -> TerrainType.WATER_3
        }
    }

    private fun variantIndex(seed: Long, row: Int, col: Int, salt: Long): Int {
        var hash = seed xor salt
        hash = hash * 31L + row.toLong() * 92821L
        hash = hash * 31L + col.toLong() * 68917L
        hash = hash xor (hash ushr 33)
        return ((hash and Long.MAX_VALUE) % 3L).toInt()
    }

    private fun lerp(start: Float, end: Float, t: Float): Float {
        return start + (end - start) * t.coerceIn(0f, 1f)
    }

    private fun Random.nextFloat(min: Float, max: Float): Float {
        return min + nextFloat() * (max - min)
    }

    private class GenerationContext(
        val config: MapGenConfig,
        val seed: Long
    ) {
        val random = Random(seed)
        val grid = MutableMaterialGrid(config.width, config.height, MaterialType.GRASS)
        val centerRow = config.height / 2
        val centerCol = config.width / 2

        private val protectedMask = BooleanArray(config.width * config.height)

        fun isProtected(row: Int, col: Int): Boolean {
            return grid.inBounds(row, col) && protectedMask[index(row, col)]
        }

        fun setProtected(row: Int, col: Int) {
            if (grid.inBounds(row, col)) {
                protectedMask[index(row, col)] = true
            }
        }

        fun forEachProtected(action: (row: Int, col: Int) -> Unit) {
            for (row in 0 until config.height) {
                for (col in 0 until config.width) {
                    if (protectedMask[index(row, col)]) {
                        action(row, col)
                    }
                }
            }
        }

        private fun index(row: Int, col: Int): Int {
            return row * config.width + col
        }
    }

    private object MapValidator {
        fun validate(context: GenerationContext): ValidationResult {
            if (!protectedZonesStayOnGrass(context)) {
                return ValidationResult.invalid("Les coins proteges ne sont pas restes en herbe.")
            }

            if (!waterTouchesAllBorders(context)) {
                return ValidationResult.invalid("Une ou plusieurs rivieres n'atteignent pas le bord de la map.")
            }

            if (!playableAreasAreSeparated(context)) {
                return ValidationResult.invalid("Les rivieres ne separent pas correctement la map en 4 zones jouables.")
            }

            if (!resourcesStayBalanced(context)) {
                return ValidationResult.invalid("Les ressources ne restent pas symetriques entre les quadrants.")
            }

            return ValidationResult.valid()
        }

        private fun protectedZonesStayOnGrass(context: GenerationContext): Boolean {
            var valid = true
            context.forEachProtected { row, col ->
                if (context.grid[row, col] != MaterialType.GRASS) {
                    valid = false
                }
            }
            return valid
        }

        private fun waterTouchesAllBorders(context: GenerationContext): Boolean {
            val top = (context.config.spawnZoneSize until context.config.width - context.config.spawnZoneSize).any { col ->
                context.grid[0, col] == MaterialType.WATER
            }
            val bottom = (context.config.spawnZoneSize until context.config.width - context.config.spawnZoneSize).any { col ->
                context.grid[context.config.height - 1, col] == MaterialType.WATER
            }
            val left = (context.config.spawnZoneSize until context.config.height - context.config.spawnZoneSize).any { row ->
                context.grid[row, 0] == MaterialType.WATER
            }
            val right = (context.config.spawnZoneSize until context.config.height - context.config.spawnZoneSize).any { row ->
                context.grid[row, context.config.width - 1] == MaterialType.WATER
            }

            return top && bottom && left && right
        }

        private fun playableAreasAreSeparated(context: GenerationContext): Boolean {
            val components = buildLandComponents(context)
            val topLeft = componentAt(components, row = 0, col = 0, fallbackRow = 1, fallbackCol = 1)
            val topRight = componentAt(
                components,
                row = 0,
                col = context.config.width - 1,
                fallbackRow = 1,
                fallbackCol = context.config.width - 2
            )
            val bottomLeft = componentAt(
                components,
                row = context.config.height - 1,
                col = 0,
                fallbackRow = context.config.height - 2,
                fallbackCol = 1
            )
            val bottomRight = componentAt(
                components,
                row = context.config.height - 1,
                col = context.config.width - 1,
                fallbackRow = context.config.height - 2,
                fallbackCol = context.config.width - 2
            )
            val cornerComponents = listOf(topLeft, topRight, bottomLeft, bottomRight)
            if (cornerComponents.any { it < 0 }) {
                return false
            }
            if (cornerComponents.toSet().size != 4) {
                return false
            }

            return cornerComponents.all { componentId ->
                components.sizeOf(componentId) >= MIN_PLAYABLE_COMPONENT_SIZE
            }
        }

        private fun componentAt(
            components: LandComponents,
            row: Int,
            col: Int,
            fallbackRow: Int,
            fallbackCol: Int
        ): Int {
            val primary = components.idAt(row, col)
            if (primary >= 0) {
                return primary
            }

            return components.idAt(fallbackRow, fallbackCol)
        }

        private fun resourcesStayBalanced(context: GenerationContext): Boolean {
            val goldCounts = quadrantCounts(context, MaterialType.GOLD)
            val elixirCounts = quadrantCounts(context, MaterialType.ELEXIR)

            return goldCounts.toSet().size <= 1 && elixirCounts.toSet().size <= 1
        }

        private fun quadrantCounts(context: GenerationContext, material: MaterialType): List<Int> {
            val counts = IntArray(4)
            val halfHeight = context.config.height / 2
            val halfWidth = context.config.width / 2

            context.grid.forEachIndexed { row, col, currentMaterial ->
                if (currentMaterial != material) {
                    return@forEachIndexed
                }

                val quadrant = when {
                    row < halfHeight && col < halfWidth -> 0
                    row < halfHeight && col >= halfWidth -> 1
                    row >= halfHeight && col < halfWidth -> 2
                    else -> 3
                }
                counts[quadrant]++
            }

            return counts.toList()
        }

        private fun buildLandComponents(context: GenerationContext): LandComponents {
            val componentIds = IntArray(context.config.width * context.config.height) { -1 }
            val componentSizes = mutableMapOf<Int, Int>()
            var componentId = 0

            for (row in 0 until context.config.height) {
                for (col in 0 until context.config.width) {
                    if (!isLand(context, row, col)) {
                        continue
                    }
                    val startIndex = row * context.config.width + col
                    if (componentIds[startIndex] >= 0) {
                        continue
                    }

                    val queue = ArrayDeque<GridCell>()
                    queue.add(GridCell(row = row, col = col))
                    componentIds[startIndex] = componentId
                    var size = 0

                    while (queue.isNotEmpty()) {
                        val cell = queue.removeFirst()
                        size++

                        traversalNeighbors(context, cell).forEach { neighbor ->
                            val neighborIndex = neighbor.row * context.config.width + neighbor.col
                            if (componentIds[neighborIndex] >= 0 || !isLand(context, neighbor.row, neighbor.col)) {
                                return@forEach
                            }
                            componentIds[neighborIndex] = componentId
                            queue.add(neighbor)
                        }
                    }

                    componentSizes[componentId] = size
                    componentId++
                }
            }

            return LandComponents(
                width = context.config.width,
                ids = componentIds,
                sizes = componentSizes
            )
        }

        private fun traversalNeighbors(context: GenerationContext, cell: GridCell): List<GridCell> {
            val neighbors = mutableListOf<GridCell>()

            CARDINAL_STEPS.forEach { (rowDelta, colDelta) ->
                val nextRow = cell.row + rowDelta
                val nextCol = cell.col + colDelta
                if (isLand(context, nextRow, nextCol)) {
                    neighbors += GridCell(row = nextRow, col = nextCol)
                }
            }

            DIAGONAL_STEPS.forEach { (rowDelta, colDelta) ->
                val nextRow = cell.row + rowDelta
                val nextCol = cell.col + colDelta
                if (!isLand(context, nextRow, nextCol)) {
                    return@forEach
                }

                val sideA = GridCell(row = cell.row + rowDelta, col = cell.col)
                val sideB = GridCell(row = cell.row, col = cell.col + colDelta)
                if (isLand(context, sideA.row, sideA.col) && isLand(context, sideB.row, sideB.col)) {
                    neighbors += GridCell(row = nextRow, col = nextCol)
                }
            }

            return neighbors
        }

        private fun isLand(context: GenerationContext, row: Int, col: Int): Boolean {
            val material = context.grid[row, col] ?: return false
            return material != MaterialType.WATER
        }
    }

    private data class ValidationResult(
        val isValid: Boolean,
        val reason: String
    ) {
        companion object {
            fun valid(): ValidationResult = ValidationResult(isValid = true, reason = "")

            fun invalid(reason: String): ValidationResult {
                return ValidationResult(isValid = false, reason = reason)
            }
        }
    }

    private data class LandComponents(
        val width: Int,
        val ids: IntArray,
        val sizes: Map<Int, Int>
    ) {
        fun idAt(row: Int, col: Int): Int {
            if (row !in 0 until ids.size / width || col !in 0 until width) {
                return -1
            }
            return ids[row * width + col]
        }

        fun sizeOf(componentId: Int): Int {
            return sizes[componentId] ?: 0
        }
    }

    private data class GridCell(
        val row: Int,
        val col: Int
    )

    private data class FloatPoint(
        val row: Float,
        val col: Float
    )

    private enum class Direction(
        val rowDelta: Int,
        val colDelta: Int
    ) {
        NORTH(rowDelta = -1, colDelta = 0),
        SOUTH(rowDelta = 1, colDelta = 0),
        WEST(rowDelta = 0, colDelta = -1),
        EAST(rowDelta = 0, colDelta = 1)
    }

    private const val ATTEMPT_SEED_STEP = 7_919L
    private const val GOLD_MAX_COVERAGE = 0.075f
    private const val GOLD_MIN_COVERAGE = 0.025f
    private const val ELEXIR_MAX_COVERAGE = 0.055f
    private const val ELEXIR_MIN_COVERAGE = 0.018f
    private const val RANDOM_CELL_ATTEMPTS = 160
    private const val RESOURCE_ATTEMPTS_LIMIT = 220
    private const val MIN_PLAYABLE_COMPONENT_SIZE = 12
    private const val EPSILON = 0.0001f

    private val CARDINAL_STEPS = listOf(
        -1 to 0,
        1 to 0,
        0 to -1,
        0 to 1
    )

    private val DIAGONAL_STEPS = listOf(
        -1 to -1,
        -1 to 1,
        1 to -1,
        1 to 1
    )

    private val ALL_NEIGHBOR_STEPS = CARDINAL_STEPS + DIAGONAL_STEPS
}
