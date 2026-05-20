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
            writeDefaultConfig()
        }

        val mapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()

        config = mapper.readValue(File("config.yml"))

        return config
    }

    private fun writeDefaultConfig() {
        val inputStream = object {}.javaClass.getResourceAsStream("/config.yml")
        if (inputStream != null) {
            inputStream.use { input ->
                configFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return
        }

        configFile.writeText(DEFAULT_CONFIG_YAML)
    }

    fun getConfig(): ServerConfig {
        if(!::config.isInitialized){
            return loadConfig()
        }
        return config
    }

    private const val DEFAULT_CONFIG_YAML = """port: 54555

lobby: true
instance: true
maxInstances: 4

serversIP:
  - this

logLevel: VERBOSE
"""
}
