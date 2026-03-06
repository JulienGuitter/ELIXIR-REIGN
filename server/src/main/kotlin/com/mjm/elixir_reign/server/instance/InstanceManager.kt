package com.mjm.elixir_reign.server.instance

import com.mjm.elixir_reign.server.ConfigManager

object InstanceManager {
    private var config = ConfigManager.getConfig()

    private var instances = mutableListOf<Instance>()

    fun init(){

    }
}
