package com.mjm.elixir_reign.server.type

data class ServerConfig(
    val port: Int,
    val lobby: Boolean,
    val instance: Boolean,
    var maxInstances: Int,
    var serversIP: List<String>
)
