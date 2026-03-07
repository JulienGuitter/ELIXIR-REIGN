package com.mjm.elixir_reign.core.tools.terrain

import com.badlogic.gdx.graphics.Pixmap
import kotlin.math.ceil
import kotlin.math.floor

enum class IsoNineSliceRegion {
    NW,
    N,
    NE,
    W,
    C,
    E,
    SW,
    S,
    SE
}

object IsoNineSlice {
    fun uniformMap(frameIndex: Int): Map<IsoNineSliceRegion, Int> {
        return IsoNineSliceRegion.entries.associateWith { frameIndex }
    }

    fun nineMap(
        nw: Int,
        n: Int,
        ne: Int,
        w: Int,
        c: Int,
        e: Int,
        sw: Int,
        s: Int,
        se: Int
    ): Map<IsoNineSliceRegion, Int> {
        return mapOf(
            IsoNineSliceRegion.NW to nw,
            IsoNineSliceRegion.N to n,
            IsoNineSliceRegion.NE to ne,
            IsoNineSliceRegion.W to w,
            IsoNineSliceRegion.C to c,
            IsoNineSliceRegion.E to e,
            IsoNineSliceRegion.SW to sw,
            IsoNineSliceRegion.S to s,
            IsoNineSliceRegion.SE to se
        )
    }

    fun generate(
        frames: List<Pixmap>,
        frameIndex: Int,
        cols: Float = 3f,
        rows: Float = 3f,
        margin: Float = 0.25f
    ): Pixmap {
        return generate(frames, uniformMap(frameIndex), cols, rows, margin)
    }

    fun generate(
        frames: List<Pixmap>,
        frameMap: Map<IsoNineSliceRegion, Int>,
        cols: Float = 3f,
        rows: Float = 3f,
        margin: Float = 0.25f
    ): Pixmap {
        require(frames.isNotEmpty()) { "At least one frame is required." }
        require(cols >= 1f && rows >= 1f) { "cols and rows must be >= 1." }

        val sourceWidth = frames.first().width
        val sourceHeight = frames.first().height
        require(frames.all { it.width == sourceWidth && it.height == sourceHeight }) {
            "All frames must share the same size."
        }

        val halfWidth = sourceWidth / 2f
        val halfHeight = sourceHeight / 2f
        val clampedMargin = margin
            .coerceAtLeast(0.02f)
            .coerceAtMost(0.499f)
            .coerceAtMost((cols - 0.01f) / 2f)
            .coerceAtMost((rows - 0.01f) / 2f)
        val centerSpan = 1f - (2f * clampedMargin)

        val outputWidth = ceil(halfWidth * (cols + rows).toDouble()).toInt()
        val outputHeight = ceil(halfHeight * (cols + rows).toDouble()).toInt()
        val output = Pixmap(outputWidth, outputHeight, Pixmap.Format.RGBA8888)

        val defaultFrame = frameMap[IsoNineSliceRegion.C] ?: 0
        val regionFrames = IsoNineSliceRegion.entries.map { region ->
            (frameMap[region] ?: defaultFrame).coerceIn(0, frames.lastIndex)
        }

        for (y in 0 until outputHeight) {
            val py = y.toFloat()
            for (x in 0 until outputWidth) {
                val px = x.toFloat()

                val u = ((px / halfWidth) - rows + (py / halfHeight)) / 2f
                val v = ((py / halfHeight) - (px / halfWidth) + rows) / 2f
                if (u < 0f || u > cols || v < 0f || v > rows) {
                    continue
                }

                val regionIndex = classifyRegion(u, v, cols, rows, clampedMargin)
                val source = frames[regionFrames[regionIndex]]

                val sourceU = mapAxis(u, cols, clampedMargin, centerSpan)
                val sourceV = mapAxis(v, rows, clampedMargin, centerSpan)

                val sampleX = (halfWidth * (1f + sourceU - sourceV))
                    .coerceIn(0f, sourceWidth - 1.001f)
                val sampleY = (halfHeight * (sourceU + sourceV))
                    .coerceIn(0f, sourceHeight - 1.001f)

                output.drawPixel(x, y, sampleBilinear(source, sampleX, sampleY))
            }
        }

        return output
    }

    fun trimTransparent(source: Pixmap): Pixmap {
        var minX = source.width
        var minY = source.height
        var maxX = -1
        var maxY = -1

        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                val alpha = source.getPixel(x, y) and 0xFF
                if (alpha == 0) {
                    continue
                }

                if (x < minX) minX = x
                if (y < minY) minY = y
                if (x > maxX) maxX = x
                if (y > maxY) maxY = y
            }
        }

        if (maxX < minX || maxY < minY) {
            return Pixmap(1, 1, Pixmap.Format.RGBA8888)
        }

        val trimmedWidth = maxX - minX + 1
        val trimmedHeight = maxY - minY + 1
        val trimmed = Pixmap(trimmedWidth, trimmedHeight, Pixmap.Format.RGBA8888)
        trimmed.drawPixmap(
            source,
            0,
            0,
            minX,
            minY,
            trimmedWidth,
            trimmedHeight
        )
        return trimmed
    }

    private fun classifyRegion(
        u: Float,
        v: Float,
        cols: Float,
        rows: Float,
        margin: Float
    ): Int {
        val uClass = when {
            u < margin -> 0
            u > cols - margin -> 2
            else -> 1
        }
        val vClass = when {
            v < margin -> 0
            v > rows - margin -> 2
            else -> 1
        }
        return (vClass * 3) + uClass
    }

    private fun mapAxis(value: Float, scale: Float, margin: Float, centerSpan: Float): Float {
        return when {
            value < margin -> value
            value > scale - margin -> 1f - (scale - value)
            else -> margin + ((value - margin) % centerSpan)
        }
    }

    private fun sampleBilinear(source: Pixmap, x: Float, y: Float): Int {
        val x0 = floor(x.toDouble()).toInt()
        val y0 = floor(y.toDouble()).toInt()
        val x1 = (x0 + 1).coerceAtMost(source.width - 1)
        val y1 = (y0 + 1).coerceAtMost(source.height - 1)

        val fx = x - x0
        val fy = y - y0

        val c00 = unpack(source.getPixel(x0, y0))
        val c10 = unpack(source.getPixel(x1, y0))
        val c01 = unpack(source.getPixel(x0, y1))
        val c11 = unpack(source.getPixel(x1, y1))

        val r = bilinear(c00[0], c10[0], c01[0], c11[0], fx, fy)
        val g = bilinear(c00[1], c10[1], c01[1], c11[1], fx, fy)
        val b = bilinear(c00[2], c10[2], c01[2], c11[2], fx, fy)
        val a = bilinear(c00[3], c10[3], c01[3], c11[3], fx, fy)

        return pack(r, g, b, a)
    }

    private fun bilinear(
        c00: Int,
        c10: Int,
        c01: Int,
        c11: Int,
        fx: Float,
        fy: Float
    ): Int {
        val top = c00 + ((c10 - c00) * fx)
        val bottom = c01 + ((c11 - c01) * fx)
        return (top + ((bottom - top) * fy)).toInt().coerceIn(0, 255)
    }

    private fun unpack(pixel: Int): IntArray {
        return intArrayOf(
            (pixel ushr 24) and 0xFF,
            (pixel ushr 16) and 0xFF,
            (pixel ushr 8) and 0xFF,
            pixel and 0xFF
        )
    }

    private fun pack(r: Int, g: Int, b: Int, a: Int): Int {
        return (r shl 24) or (g shl 16) or (b shl 8) or a
    }
}
