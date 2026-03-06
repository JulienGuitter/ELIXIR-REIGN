package com.mjm.elixir_reign.core

import com.badlogic.gdx.ApplicationAdapter
import com.mjm.elixir_reign.core.network.temporary

class Main : ApplicationAdapter() {
    override fun create() {
        val temp = temporary()
        temp.testConnection()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun render() {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun dispose() {
    }
}
