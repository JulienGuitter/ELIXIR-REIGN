package com.mjm.elixir_reign.android

import android.content.pm.ActivityInfo
import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.mjm.elixir_reign.core.Main

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        super.onCreate(savedInstanceState)

        val configuration = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }

        initialize(Main(AndroidPlatformBridge()), configuration)
    }
}

