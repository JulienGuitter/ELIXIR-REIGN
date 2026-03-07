package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.mjm.elixir_reign.core.ecs.components.TerrainComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent

/**
 * RenderSystem côté client (ECS-pur)
 * Affiche les TextureRegion à l'écran
 *
 * Components requis:
 * - PositionComponent (où afficher)
 * - TextureRegionComponent (quoi afficher - déjà chargé!)
 * - SpriteComponent (métadonnées: dimensions, scale)
 */
class RenderSystem(private val batch: SpriteBatch) : IteratingSystem(
    Family.all(
        PositionComponent::class.java,
        TextureRegionComponent::class.java,
        SpriteComponent::class.java
    ).get()
) {
    private val terrainShader = createTerrainShader()

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val position = entity.getComponent(PositionComponent::class.java)
        val textureRegion = entity.getComponent(TextureRegionComponent::class.java)
        val sprite = entity.getComponent(SpriteComponent::class.java)
        val terrain = entity.getComponent(TerrainComponent::class.java)

        if (terrain != null) {
            drawTerrain(position, sprite, textureRegion.textureRegion, terrain)
            return
        }

        // Vérifier que la TextureRegion est valide avant de dessiner
        if (textureRegion.textureRegion.texture != null) {
            // Dessiner le sprite
            batch.draw(
                textureRegion.textureRegion,
                position.x,
                position.y,
                sprite.width * sprite.scaleX,
                sprite.height * sprite.scaleY
            )
        }
    }

    private fun drawTerrain(
        position: PositionComponent,
        sprite: SpriteComponent,
        region: TextureRegion,
        terrain: TerrainComponent
    ) {
        if (region.texture == null) {
            return
        }

        val drawWidth = sprite.width * sprite.scaleX
        val drawHeight = sprite.height * sprite.scaleY

        batch.flush()
        batch.setShader(terrainShader)
        terrainShader.bind()
        terrainShader.setUniformf("u_terrainOrigin", position.x, position.y)
        terrainShader.setUniformf("u_terrainSize", drawWidth, drawHeight)
        terrainShader.setUniformf("u_regionMin", region.u, region.v)
        terrainShader.setUniformf("u_regionSize", region.u2 - region.u, region.v2 - region.v)
        terrainShader.setUniformf(
            "u_gridSize",
            terrain.gridColumns.toFloat(),
            terrain.gridRows.toFloat()
        )
        terrainShader.setUniformf("u_margin", terrain.margin)
        terrainShader.setUniformf(
            "u_regionTexel",
            1f / region.regionWidth.toFloat(),
            1f / region.regionHeight.toFloat()
        )

        batch.draw(region, position.x, position.y, drawWidth, drawHeight)

        batch.flush()
        batch.setShader(null)
    }

    fun dispose() {
        terrainShader.dispose()
    }

    private fun createTerrainShader(): ShaderProgram {
        ShaderProgram.pedantic = false
        return ShaderProgram(TERRAIN_VERTEX_SHADER, TERRAIN_FRAGMENT_SHADER).also { shader ->
            require(shader.isCompiled) { "Terrain shader compilation failed: ${shader.log}" }
        }
    }

    companion object {
        private const val TERRAIN_VERTEX_SHADER = """
attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord0;
uniform mat4 u_projTrans;
varying vec4 v_color;
varying vec2 v_worldPos;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_color.a = v_color.a * (255.0 / 254.0);
    v_worldPos = a_position.xy;
    v_texCoords = a_texCoord0;
    gl_Position = u_projTrans * a_position;
}
"""

        private const val TERRAIN_FRAGMENT_SHADER = """
#ifdef GL_ES
precision mediump float;
precision mediump int;
#endif

varying vec4 v_color;
varying vec2 v_worldPos;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_terrainOrigin;
uniform vec2 u_terrainSize;
uniform vec2 u_regionMin;
uniform vec2 u_regionSize;
uniform vec2 u_gridSize;
uniform vec2 u_regionTexel;
uniform float u_margin;

float clampMargin(float margin, float cols, float rows) {
    float clamped = clamp(margin, 0.02, 0.499);
    clamped = min(clamped, (cols - 0.01) * 0.5);
    clamped = min(clamped, (rows - 0.01) * 0.5);
    return clamped;
}

float mapAxis(float value, float scale, float margin, float centerSpan) {
    if (value < margin) {
        return value;
    }
    if (value > scale - margin) {
        return 1.0 - (scale - value);
    }
    return margin + mod(value - margin, centerSpan);
}

void main() {
    vec2 local = (v_worldPos - u_terrainOrigin) / u_terrainSize;
    if (local.x < 0.0 || local.x > 1.0 || local.y < 0.0 || local.y > 1.0) {
        discard;
    }

    float cols = u_gridSize.x;
    float rows = u_gridSize.y;
    float sum = cols + rows;
    float u = ((local.x * sum) - rows + (local.y * sum)) * 0.5;
    float v = ((local.y * sum) - (local.x * sum) + rows) * 0.5;

    if (u < 0.0 || u > cols || v < 0.0 || v > rows) {
        discard;
    }

    float margin = clampMargin(u_margin, cols, rows);
    float centerSpan = 1.0 - (2.0 * margin);
    vec2 tileLocal = vec2(
        0.5 * (1.0 + mapAxis(u, cols, margin, centerSpan) - mapAxis(v, rows, margin, centerSpan)),
        0.5 * (mapAxis(u, cols, margin, centerSpan) + mapAxis(v, rows, margin, centerSpan))
    );

    tileLocal = clamp(tileLocal, u_regionTexel * 0.5, vec2(1.0) - (u_regionTexel * 0.5));
    vec2 atlasUv = u_regionMin + (tileLocal * u_regionSize);

    gl_FragColor = v_color * texture2D(u_texture, atlasUv);
}
"""
    }
}
