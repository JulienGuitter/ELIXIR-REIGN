package com.mjm.elixir_reign.core.tools.sprites.sprite_sheet

import com.badlogic.gdx.Gdx
import org.json.JSONObject

class SpriteSheetParser {
    fun parseJson(jsonPath: String): SpriteSheet {
        val jsonString = Gdx.files.internal(jsonPath).readString()
        val json = JSONObject(jsonString)

        val name = json.getString("name")
        val cellWidth = json.getInt("cell_width")
        val cellHeight = json.getInt("cell_height")
        val columns = json.getInt("columns")

        val clipsArray = json.getJSONArray("clips")
        val clips = mutableListOf<AnimationClip>()

        for (i in 0 until clipsArray.length()) {
            val clipJson = clipsArray.getJSONObject(i)
            val clip = parseClip(clipJson, cellWidth, cellHeight, columns)
            clips.add(clip)
        }

        return SpriteSheet(name, cellWidth, cellHeight, columns, clips)
    }

    private fun parseClip(
        clipJson: JSONObject,
        cellWidth: Int,
        cellHeight: Int,
        columns: Int
    ): AnimationClip {
        val name = clipJson.getString("name")
        val startFrame = clipJson.getInt("start_frame")
        val frameCount = clipJson.getInt("frame_count")
        val fps = clipJson.getInt("fps")

        val frames = mutableListOf<Frame>()
        for (i in 0 until frameCount) {
            val frameIndex = startFrame + i
            val (x, y) = calculateFramePosition(frameIndex, columns, cellWidth, cellHeight)
            frames.add(Frame(frameIndex, x, y))
        }

        return AnimationClip(name, startFrame, frameCount, fps, frames)
    }

    private fun calculateFramePosition(
        frameIndex: Int,
        columns: Int,
        cellWidth: Int,
        cellHeight: Int
    ): Pair<Int, Int> {
        val row = frameIndex / columns
        val col = frameIndex % columns
        return Pair(col * cellWidth, row * cellHeight)
    }
}
