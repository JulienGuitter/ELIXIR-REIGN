package com.mjm.elixir_reign.shared.network

import com.mjm.elixir_reign.shared.type.GameType

// Lobby/Login packets
class PacketLogin(var pseudo: String = "", var version: String = "", var gameType: GameType = GameType.G1V3)
class PacketLoginAccepted(var myId: Int=0)
class PacketLoginRefused(var reason: String = "")
class PacketServerInfo(var disponibilityCount: Int = 0)
class PacketCreateInstance(var gameType: GameType = GameType.G1V3, var uuid: String = "")
class PacketRedirectToInstance(var ip: String = "", var port: Int = 0, var uuid: String = "")
class PacketConnectToInstance(var uuid: String = "")

// Lightweight in-game sync packet for multiplayer mode.
class PacketGameplayTick(var deltaMs: Int = 0)
