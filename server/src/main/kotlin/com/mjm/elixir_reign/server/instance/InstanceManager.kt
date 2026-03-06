package com.mjm.elixir_reign.server.instance

import com.mjm.elixir_reign.server.ConfigManager
import type.GameType
import java.util.concurrent.ConcurrentHashMap

object InstanceManager {
    private var config = ConfigManager.getConfig()

    private var instances = mutableListOf<Instance>()
    var isInit = false
        private set

    fun init(){
        for(i in 0 until config.maxInstances){
            instances.add(Instance())
        }

        isInit = true
    }

    fun getAvailableInstances(): Int{
        var count = config.maxInstances - instances.size
        if(count < 0) count = 0
        return count
    }
}
