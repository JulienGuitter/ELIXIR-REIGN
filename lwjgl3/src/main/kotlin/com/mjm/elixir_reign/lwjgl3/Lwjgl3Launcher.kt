package com.mjm.elixir_reign.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.mjm.elixir_reign.core.Main

object Lwjgl3Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        if (StartupHelper.startNewJvmIfRequired()) return // Gère le support macOS et aide sur Windows.
        createApplication()
    }

    private fun createApplication(): Lwjgl3Application {
        return Lwjgl3Application(Main(), getDefaultConfiguration())
    }

    private fun getDefaultConfiguration(): Lwjgl3ApplicationConfiguration {
        return Lwjgl3ApplicationConfiguration().apply {
            setTitle("ELIXIR-REIGN")

            // La Vsync limite les FPS à ce que ton écran peut afficher (évite les déchirures d'écran)
            useVsync(true)

            // Limite les FPS au taux de rafraîchissement de l'écran actif + 1
            setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1)

            setWindowedMode(640, 480)

            // Tes nouvelles icônes !
            setWindowIcon(
                "elixir_reign_x128.png",
                "elixir_reign_x64.png",
                "elixir_reign_x32.png",
                "elixir_reign_x16.png"
            )
        }
    }
}
