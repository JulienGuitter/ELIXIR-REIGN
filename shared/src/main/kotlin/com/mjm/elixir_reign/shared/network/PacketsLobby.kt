package com.mjm.elixir_reign.shared.network

import com.mjm.elixir_reign.shared.logic.UnitType
import com.mjm.elixir_reign.shared.type.GameType

// Lobby/Login packets
class PacketLogin(
    var pseudo: String = "",
    var version: String = "",
    var gameType: GameType = GameType.G1V3,
    var instanceUuid: String = ""
)
class PacketLoginAccepted(var myId: Int=0)
class PacketLoginRefused(var reason: String = "")
class PacketServerInfo(var disponibilityCount: Int = 0)
class PacketCreateInstance(var gameType: GameType = GameType.G1V3, var uuid: String = "")
class PacketRedirectToInstance(var ip: String = "", var port: Int = 0, var uuid: String = "")
class PacketConnectToInstance(var uuid: String = "")

// Lightweight in-game sync packet for multiplayer mode.
class PacketGameplayTick(var deltaMs: Int = 0)

class PacketPlayerSummary(
    var id: Int = 0,
    var name: String = "",
    var gold: Int = 0,
    var elixir: Int = 0,
    var darkElixir: Int = 0
)

class PacketGameInit(
    var myPlayerId: Int = 0,
    var mapWidth: Int = 0,
    var mapHeight: Int = 0,
    var chunkSize: Int = 0,
    var players: ArrayList<PacketPlayerSummary> = arrayListOf()
)

class PacketMapChunk(
    var chunkX: Int = 0,
    var chunkY: Int = 0,
    var terrainOrdinals: IntArray = intArrayOf()
)

class PacketUnitSnapshot(
    var unitId: Int = 0,
    var ownerPlayerId: Int = 0,
    var unitType: UnitType = UnitType.BARBARIAN,
    var row: Float = 0f,
    var col: Float = 0f,
    var targetRow: Float = 0f,
    var targetCol: Float = 0f,
    var moving: Boolean = false
)

class PacketVisibilityUpdate(
    var fullSync: Boolean = true,
    var visibleChunkIndices: IntArray = intArrayOf(),
    var visibleTileIndices: IntArray = intArrayOf(),
    var hiddenTileIndices: IntArray = intArrayOf()
)

class PacketUnitRemove(var unitId: Int = 0)

class PacketMoveUnitsRequest(
    var unitIds: IntArray = intArrayOf(),
    var targetRow: Int = 0,
    var targetCol: Int = 0
)

class PacketGameReady
