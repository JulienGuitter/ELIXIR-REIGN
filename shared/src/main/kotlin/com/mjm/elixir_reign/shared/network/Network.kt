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

        // Common types
        kryo?.register(ArrayList::class.java)
        kryo?.register(IntArray::class.java)
        kryo?.register(GameType::class.java)
        kryo?.register(EntityType::class.java)
        kryo?.register(TerrainType::class.java)
        kryo?.register(PlayerState::class.java)
        kryo?.register(UnitState::class.java)
        kryo?.register(BuildingInstanceState::class.java)

        // Lobby/Login packets register
        kryo?.register(PacketLogin::class.java)
        kryo?.register(PacketLoginAccepted::class.java)
        kryo?.register(PacketLoginRefused::class.java)
        kryo?.register(PacketServerInfo::class.java)
        kryo?.register(PacketCreateInstance::class.java)
        kryo?.register(PacketRedirectToInstance::class.java)
        kryo?.register(PacketConnectToInstance::class.java)
        kryo?.register(PacketGameplayTick::class.java)
        kryo?.register(PlayerConnectionState::class.java)
        kryo?.register(PacketPlayerSummary::class.java)
        kryo?.register(PacketPlayerStatus::class.java)
        kryo?.register(PacketPlayerPresenceUpdate::class.java)
        kryo?.register(PacketGameInit::class.java)
        kryo?.register(PacketMapChunk::class.java)
        kryo?.register(PacketUnitSnapshot::class.java)
        kryo?.register(PacketVisibilityUpdate::class.java)
        kryo?.register(PacketUnitRemove::class.java)
        kryo?.register(PacketMoveUnitsRequest::class.java)
        kryo?.register(PacketPlaceBuildingRequest::class.java)
        kryo?.register(PacketPlaceBuildingResult::class.java)
        kryo?.register(PacketBuildingSnapshot::class.java)
        kryo?.register(PacketBuildingRemove::class.java)
        kryo?.register(PacketPlayerResources::class.java)
        kryo?.register(PacketUpgradeBuildingRequest::class.java)
        kryo?.register(PacketUpgradeBuildingResult::class.java)
        kryo?.register(PacketTrainUnitRequest::class.java)
        kryo?.register(PacketTrainUnitResult::class.java)
        kryo?.register(PacketGameOver::class.java)
        kryo?.register(PacketGameReady::class.java)
    }
}
