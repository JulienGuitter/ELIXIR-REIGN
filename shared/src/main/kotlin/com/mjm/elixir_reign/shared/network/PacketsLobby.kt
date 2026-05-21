package com.mjm.elixir_reign.shared.network

import com.mjm.elixir_reign.shared.logic.EntityType
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

enum class PlayerConnectionState {
    CONNECTED,
    WAITING_RECONNECTION,
    DISCONNECTED
}

class PacketPlayerSummary(
    var id: Int = 0,
    var name: String = "",
    var gold: Int = 0,
    var elixir: Int = 0,
    var darkElixir: Int = 0,
    var connectionState: PlayerConnectionState = PlayerConnectionState.CONNECTED
)

class PacketPlayerStatus(
    var id: Int = 0,
    var connectionState: PlayerConnectionState = PlayerConnectionState.CONNECTED
)

class PacketPlayerPresenceUpdate(
    var players: ArrayList<PacketPlayerStatus> = arrayListOf()
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
    var entityType: EntityType = EntityType.BARBARIAN,
    var row: Float = 0f,
    var col: Float = 0f,
    var targetRow: Float = 0f,
    var targetCol: Float = 0f,
    var moving: Boolean = false,
    var currentHP: Float = 100f,
    var maxHP: Float = 100f,
    var barracksId: Int = 0
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

class PacketPlaceBuildingRequest(
    var requestId: Int = 0,
    var entityType: EntityType = EntityType.GOLD_MINE,
    var row: Int = 0,
    var col: Int = 0
)

class PacketPlaceBuildingResult(
    var requestId: Int = 0,
    var accepted: Boolean = false,
    var reason: String = "",
    var buildingId: Int = 0
)

class PacketBuildingSnapshot(
    var buildingId: Int = 0,
    var ownerPlayerId: Int = 0,
    var entityType: EntityType = EntityType.GOLD_MINE,
    var row: Int = 0,
    var col: Int = 0,
    var level: Int = 1,
    var currentHP: Float = 100f,
    var maxHP: Float = 100f,
    var destroyed: Boolean = false,
    var maxFormedUnits: Int = 0,
    var trainingQueue: ArrayList<EntityType> = arrayListOf(),
    var hasActiveTraining: Boolean = false,
    var activeTrainingUnitType: EntityType = EntityType.BARBARIAN,
    var activeTrainingElapsedSeconds: Float = 0f
)

class PacketBuildingRemove(var buildingId: Int = 0)

class PacketPlayerResources(
    var gold: Int = 0,
    var elixir: Int = 0,
    var darkElixir: Int = 0
)

class PacketUpgradeBuildingRequest(
    var requestId: Int = 0,
    var buildingId: Int = 0
)

class PacketUpgradeBuildingResult(
    var requestId: Int = 0,
    var accepted: Boolean = false,
    var reason: String = "",
    var buildingId: Int = 0,
    var level: Int = 1
)

class PacketTrainUnitRequest(
    var requestId: Int = 0,
    var buildingId: Int = 0,
    var entityType: EntityType = EntityType.BARBARIAN
)

class PacketTrainUnitResult(
    var requestId: Int = 0,
    var accepted: Boolean = false,
    var reason: String = "",
    var buildingId: Int = 0,
    var entityType: EntityType = EntityType.BARBARIAN
)

class PacketGameOver(
    var winnerPlayerId: Int = 0,
    var eliminatedPlayerIds: IntArray = intArrayOf()
)

class PacketGameReady
