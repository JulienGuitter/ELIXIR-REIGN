package com.mjm.elixir_reign.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.mjm.elixir_reign.server.type.ServerConfig
import java.io.File

object ConfigManager {
    private val configFile = File("config.yml")

    private lateinit var config: ServerConfig

    private fun loadConfig(): ServerConfig{
        if(!configFile.exists()){
            println("Generate config file")

            println(ConfigManager::class.java.getResource("/config.yml"))

            val inputStream = object {}.javaClass
                .getResourceAsStream("/config.yml")
                ?: throw IllegalStateException("config.yml not found in ressources")

            configFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
        }

        val mapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()

        config = mapper.readValue(File("config.yml"))

        return config
    }

    fun getConfig(): ServerConfig {
        if(!::config.isInitialized){
            return loadConfig()
        }
        return config
    }
}
