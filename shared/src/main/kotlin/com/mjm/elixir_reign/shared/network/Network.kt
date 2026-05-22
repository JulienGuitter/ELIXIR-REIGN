package com.mjm.elixir_reign.shared.network

import com.esotericsoftware.kryo.Kryo
import com.mjm.elixir_reign.shared.game.PlayerState
import com.mjm.elixir_reign.shared.game.UnitState
import com.mjm.elixir_reign.shared.game.BuildingInstanceState
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.terrain.TerrainType
import com.mjm.elixir_reign.shared.type.GameType
import java.util.ArrayList

object Network {
    // Le port qu'on utilisera (TCP et UDP)
    const val PORT: Int = 54555
    const val WRITE_BUFFER_SIZE: Int = 1024 * 1024
    const val OBJECT_BUFFER_SIZE: Int = 1024 * 1024

    // La méthode pour tout enregistrer
    fun register(kryo: Kryo?) {
        if (kryo == null) return

        // Ces IDs font partie du protocole reseau. Garder les valeurs stables
        // et ajouter les nouveaux types sur un nouvel ID pour rester compatible.
        kryo.register(ArrayList::class.java, 100)
        kryo.register(IntArray::class.java, 101)
        kryo.register(GameType::class.java, 102)
        kryo.register(EntityType::class.java, 103)
        kryo.register(TerrainType::class.java, 104)
        kryo.register(PlayerState::class.java, 105)
        kryo.register(UnitState::class.java, 106)
        kryo.register(BuildingInstanceState::class.java, 107)

        // Lobby/Login packets register
        kryo.register(PacketLogin::class.java, 120)
        kryo.register(PacketLoginAccepted::class.java, 121)
        kryo.register(PacketLoginRefused::class.java, 122)
        kryo.register(PacketServerInfo::class.java, 123)
        kryo.register(PacketCreateInstance::class.java, 124)
        kryo.register(PacketRedirectToInstance::class.java, 125)
        kryo.register(PacketConnectToInstance::class.java, 126)
        kryo.register(PacketGameplayTick::class.java, 127)
        kryo.register(PlayerConnectionState::class.java, 128)
        kryo.register(PacketPlayerSummary::class.java, 129)
        kryo.register(PacketPlayerStatus::class.java, 130)
        kryo.register(PacketPlayerPresenceUpdate::class.java, 131)
        kryo.register(PacketGameInit::class.java, 132)
        kryo.register(PacketMapChunk::class.java, 133)
        kryo.register(PacketUnitSnapshot::class.java, 134)
        kryo.register(PacketVisibilityUpdate::class.java, 135)
        kryo.register(PacketUnitRemove::class.java, 136)
        kryo.register(PacketMoveUnitsRequest::class.java, 137)
        kryo.register(PacketPlaceBuildingRequest::class.java, 138)
        kryo.register(PacketPlaceBuildingResult::class.java, 139)
        kryo.register(PacketBuildingSnapshot::class.java, 140)
        kryo.register(PacketBuildingRemove::class.java, 141)
        kryo.register(PacketPlayerResources::class.java, 142)
        kryo.register(PacketUpgradeBuildingRequest::class.java, 143)
        kryo.register(PacketUpgradeBuildingResult::class.java, 144)
        kryo.register(PacketTrainUnitRequest::class.java, 145)
        kryo.register(PacketTrainUnitResult::class.java, 146)
        kryo.register(PacketGameOver::class.java, 147)
        kryo.register(PacketGameReady::class.java, 148)
        kryo.register(PacketRedirectAck::class.java, 149)
        kryo.register(PacketKeepAlive::class.java, 150)
        kryo.register(PacketKeepAliveAck::class.java, 151)
    }
}
