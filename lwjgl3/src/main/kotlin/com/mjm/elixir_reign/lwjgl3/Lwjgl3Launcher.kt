package com.mjm.elixir_reign.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.mjm.elixir_reign.core.Main
import java.nio.file.Files
import java.nio.file.Path

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

            // IDE classpaths may omit resources, so resolve icons from classpath or local project paths.
            val iconPaths = resolveWindowIconPaths()
            if (iconPaths != null) {
                setWindowIcon(*iconPaths)
            } else {
                System.err.println("Window icons not found on classpath, continuing without custom icon.")
            }
        }
    }

    private fun resolveWindowIconPaths(): Array<String>? {
        val iconNames = arrayOf(
            "elixir_reign_x128.png",
            "elixir_reign_x64.png",
            "elixir_reign_x32.png",
            "elixir_reign_x16.png"
        )

        val classLoader = Lwjgl3Launcher::class.java.classLoader
        if (iconNames.all { classLoader.getResource(it) != null }) {
            return iconNames
        }

        val localCandidates = listOf(
            Path.of(""),
            Path.of("lwjgl3", "src", "main", "resources"),
            Path.of("assets")
        )

        for (base in localCandidates) {
            if (iconNames.all { Files.isRegularFile(base.resolve(it)) }) {
                return iconNames.map { base.resolve(it).toString().replace('\\', '/') }.toTypedArray()
            }
        }

        return null
    }
}
