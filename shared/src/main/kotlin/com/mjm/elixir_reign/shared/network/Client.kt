package com.mjm.elixir_reign.shared.network

import com.esotericsoftware.kryonet.Connection
import com.mjm.elixir_reign.shared.type.GameType

class Client(
    var pseudo: String = "",
    var version: String = "",
    var gameType: GameType,
    var connection: Connection? = null
)
