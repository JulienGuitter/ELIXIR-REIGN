package com.mjm.elixir_reign.server.logging

import com.esotericsoftware.kryonet.Connection
import com.mjm.elixir_reign.server.type.ServerConfig

object ServerLog {
    @Volatile
    private var level: LogLevel = LogLevel.NORMAL

    fun configure(config: ServerConfig) {
        level = LogLevel.from(config.logLevel)
    }

    fun info(message: String) {
        if (level == LogLevel.NONE) return
        println(message)
    }

    fun inbound(connectionId: Int?, message: Any) {
        if (level != LogLevel.VERBOSE) return
        println("IN  [${connectionId ?: -1}] ${message::class.simpleName}")
    }

    fun outbound(connectionId: Int?, message: Any) {
        if (level != LogLevel.VERBOSE) return
        println("OUT [${connectionId ?: -1}] ${message::class.simpleName}")
    }

    fun sendTcp(connection: Connection?, message: Any) {
        if (connection == null) return
        outbound(connection.id, message)
        connection.sendTCP(message)
    }

    enum class LogLevel {
        NONE,
        NORMAL,
        VERBOSE;

        companion object {
            fun from(raw: String?): LogLevel {
                return entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: NORMAL
            }
        }
    }
}
