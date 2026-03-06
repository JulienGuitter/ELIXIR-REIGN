package com.mjm.elixir_reign.shared.network

import type.GameType

// Login packets
class PacketLogin(var pseudo: String = "", var version: String = "", var gameType: GameType = GameType.G1V4)
class PacketLoginAccepted(var myId: Int=0)
class PacketLoginRefused(var reason: String = "")
