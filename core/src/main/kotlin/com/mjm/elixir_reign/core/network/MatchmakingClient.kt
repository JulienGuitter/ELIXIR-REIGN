package com.mjm.elixir_reign.core.network

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.mjm.elixir_reign.core.i18n.Localization
import com.mjm.elixir_reign.core.utils.SettingsManager
import com.mjm.elixir_reign.shared.GameConfiguration
import com.mjm.elixir_reign.shared.network.Network
import com.mjm.elixir_reign.shared.network.PacketBuildingRemove
import com.mjm.elixir_reign.shared.network.PacketBuildingSnapshot
import com.mjm.elixir_reign.shared.network.PacketConnectToInstance
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketGameOver
import com.mjm.elixir_reign.shared.network.PacketGameReady
import com.mjm.elixir_reign.shared.network.PacketGameplayTick
import com.mjm.elixir_reign.shared.network.PacketKeepAlive
import com.mjm.elixir_reign.shared.network.PacketKeepAliveAck
import com.mjm.elixir_reign.shared.network.PacketLogin
import com.mjm.elixir_reign.shared.network.PacketLoginAccepted
import com.mjm.elixir_reign.shared.network.PacketLoginRefused
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketMoveUnitsRequest
import com.mjm.elixir_reign.shared.network.PacketPlaceBuildingRequest
import com.mjm.elixir_reign.shared.network.PacketPlaceBuildingResult
import com.mjm.elixir_reign.shared.network.PacketRedirectAck
import com.mjm.elixir_reign.shared.network.PacketPlayerPresenceUpdate
import com.mjm.elixir_reign.shared.network.PacketPlayerResources
import com.mjm.elixir_reign.shared.network.PacketRedirectToInstance
import com.mjm.elixir_reign.shared.network.PacketTrainUnitRequest
import com.mjm.elixir_reign.shared.network.PacketTrainUnitResult
import com.mjm.elixir_reign.shared.network.PacketUnitRemove
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.network.PacketUpgradeBuildingRequest
import com.mjm.elixir_reign.shared.network.PacketUpgradeBuildingResult
import com.mjm.elixir_reign.shared.network.PacketVisibilityUpdate
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.type.GameType
import kotlin.concurrent.thread

object MatchmakingClient {
    private val lock = Any()

    @Volatile
    private var lobbyClient: Client? = null

    @Volatile
    private var instanceClient: Client? = null

    @Volatile
    private var statusText: String = ""

    @Volatile
    private var errorText: String? = null

    @Volatile
    private var gameReady: Boolean = false

    @Volatile
    private var selectedGameType: GameType = GameType.G1V1

    @Volatile
    private var username: String = ""

    @Volatile
    private var lastGameplayTickSentAtMs: Long = 0L

    @Volatile
    private var lastInstanceRedirect: PacketRedirectToInstance? = null

    @Volatile
    private var reconnectAttemptAfterDisconnect: Boolean = false

    @Volatile
    private var instanceJoinConfirmed: Boolean = false

    @Volatile
    private var instanceReadyPacketReceived: Boolean = false

    @Volatile
    private var nextRequestId: Int = 1

    @Volatile
    private var lastPlacementResult: PacketPlaceBuildingResult? = null

    @Volatile
    private var lastUpgradeResult: PacketUpgradeBuildingResult? = null

    @Volatile
    private var lastTrainUnitResult: PacketTrainUnitResult? = null

    fun startMatchmaking(gameType: GameType) {
        synchronized(lock) {
            stopAllClientsLocked(clearState = true)
            lastInstanceRedirect = null
            SettingsManager.clearReconnectInfo()
            selectedGameType = gameType
            username = SettingsManager.username.trim()
            statusText = Localization.get("network.status.connecting")
        }

        if (username.isBlank()) {
            setError(Localization.get("network.error.usernameMissing"))
            return
        }

        val host = resolveHost()
        val port = resolvePort()

        val newLobbyClient = Client(Network.WRITE_BUFFER_SIZE, Network.OBJECT_BUFFER_SIZE)
        Network.register(newLobbyClient.kryo)
        newLobbyClient.addListener(object : Listener {
            override fun received(connection: Connection, message: Any) {
                when (message) {
                    is PacketLoginAccepted -> {
                        setStatus(Localization.get("network.status.waiting"))
                    }

                    is PacketLoginRefused -> {
                        val reason = message.reason.ifBlank {
                            Localization.get("network.error.connectionFailed", host, port)
                        }
                        setError(reason)
                    }

                    is PacketRedirectToInstance -> {
                        connection.sendTCP(PacketRedirectAck(uuid = message.uuid))
                        setStatus(Localization.get("network.status.redirecting"))
                        connectToInstance(message)
                    }

                    is PacketKeepAliveAck -> Unit
                }
            }

            override fun disconnected(connection: Connection?) {
                if (lobbyClient === newLobbyClient && !gameReady) {
                    setError(Localization.get("network.error.disconnected"))
                }
            }
        })

        synchronized(lock) {
            lobbyClient = newLobbyClient
        }

        thread(name = "kryonet-lobby-connect", isDaemon = true) {
            try {
                newLobbyClient.start()
                newLobbyClient.connect(5000, host, port, port)
                startKeepAlive(newLobbyClient, ConnectionScope.LOBBY)
                newLobbyClient.sendTCP(
                    PacketLogin(
                        pseudo = username,
                        version = GameConfiguration.VERSION,
                        gameType = gameType
                    )
                )
            } catch (_: Exception) {
                setError(Localization.get("network.error.connectionFailed", host, port))
            }
        }
    }

    fun startOfflineMode() {
        synchronized(lock) {
            stopAllClientsLocked(clearState = true)
            lastInstanceRedirect = null
            SettingsManager.clearReconnectInfo()
            instanceJoinConfirmed = false
            instanceReadyPacketReceived = false
            lastPlacementResult = null
            lastUpgradeResult = null
            lastTrainUnitResult = null
        }
    }

    fun cancelMatchmaking() {
        synchronized(lock) {
            stopAllClientsLocked(clearState = true)
        }
    }

    fun getStatusText(): String {
        return statusText.ifBlank { Localization.get("lobbyWaiting.title") }
    }

    fun getErrorText(): String? {
        return errorText
    }

    fun consumeGameReady(): Boolean {
        if (!gameReady) return false
        gameReady = false
        return true
    }

    fun consumeGameReadyWhenWorldReady(): Boolean {
        if (!gameReady) return false
        if (!GameSession.hasInitialMultiplayerVisibility()) return false
        gameReady = false
        return true
    }

    fun canReconnectToLastInstance(): Boolean {
        return reconnectRedirectOrNull() != null
    }

    fun reconnectToLastInstance(): Boolean {
        val redirect = reconnectRedirectOrNull() ?: return false

        errorText = null
        gameReady = false
        username = SettingsManager.username.trim()
        if (username.isBlank()) {
            setError(Localization.get("network.error.usernameMissing"))
            return false
        }
        setStatus(Localization.get("network.status.connecting"))
        connectToInstance(redirect, isReconnectAttempt = true)
        return true
    }

    fun sendGameplayTick(deltaSeconds: Float) {
        val client = instanceClient ?: return
        if (deltaSeconds <= 0f) return

        val now = System.currentTimeMillis()
        if (now - lastGameplayTickSentAtMs < GAMEPLAY_TICK_SEND_INTERVAL_MS) {
            return
        }

        val deltaMs = (deltaSeconds * 1000f).toInt().coerceAtLeast(1)
        try {
            client.sendUDP(PacketGameplayTick(deltaMs = deltaMs))
            lastGameplayTickSentAtMs = now
        } catch (_: Exception) {
            setError(Localization.get("network.error.disconnected"))
        }
    }

    fun sendMoveUnitsRequest(unitIds: IntArray, targetRow: Int, targetCol: Int) {
        if (unitIds.isEmpty()) return
        val client = instanceClient ?: return

        try {
            client.sendTCP(
                PacketMoveUnitsRequest(
                    unitIds = unitIds,
                    targetRow = targetRow,
                    targetCol = targetCol
                )
            )
        } catch (_: Exception) {
            setError(Localization.get("network.error.disconnected"))
        }
    }

    fun sendPlaceBuildingRequest(entityType: EntityType, row: Int, col: Int): Int {
        val client = instanceClient ?: return 0
        val requestId = nextRequestId++

        try {
            client.sendTCP(
                PacketPlaceBuildingRequest(
                    requestId = requestId,
                    entityType = entityType,
                    row = row,
                    col = col
                )
            )
        } catch (_: Exception) {
            setError(Localization.get("network.error.disconnected"))
            return 0
        }
        return requestId
    }

    fun consumePlacementResult(): PacketPlaceBuildingResult? {
        val result = lastPlacementResult
        lastPlacementResult = null
        return result
    }

    fun sendUpgradeBuildingRequest(buildingId: Int): Int {
        val client = instanceClient ?: return 0
        val requestId = nextRequestId++

        try {
            client.sendTCP(PacketUpgradeBuildingRequest(requestId = requestId, buildingId = buildingId))
        } catch (_: Exception) {
            setError(Localization.get("network.error.disconnected"))
            return 0
        }
        return requestId
    }

    fun consumeUpgradeResult(): PacketUpgradeBuildingResult? {
        val result = lastUpgradeResult
        lastUpgradeResult = null
        return result
    }

    fun sendTrainUnitRequest(buildingId: Int, entityType: EntityType): Int {
        val client = instanceClient ?: return 0
        val requestId = nextRequestId++

        try {
            client.sendTCP(PacketTrainUnitRequest(requestId = requestId, buildingId = buildingId, entityType = entityType))
        } catch (_: Exception) {
            setError(Localization.get("network.error.disconnected"))
            return 0
        }
        return requestId
    }

    fun consumeTrainUnitResult(): PacketTrainUnitResult? {
        val result = lastTrainUnitResult
        lastTrainUnitResult = null
        return result
    }

    private fun connectToInstance(redirect: PacketRedirectToInstance, isReconnectAttempt: Boolean = false) {
        val host = if (redirect.ip == "this" || redirect.ip.isBlank()) resolveHost() else redirect.ip
        val port = if (redirect.port > 0) redirect.port else resolvePort()
        val instanceUuid = redirect.uuid
        lastInstanceRedirect = PacketRedirectToInstance(ip = host, port = port, uuid = instanceUuid)
        persistReconnectInfo(host, port, instanceUuid)
        if (!isReconnectAttempt) {
            reconnectAttemptAfterDisconnect = false
        }
        instanceJoinConfirmed = false
        instanceReadyPacketReceived = false

        val newInstanceClient = Client(Network.WRITE_BUFFER_SIZE, Network.OBJECT_BUFFER_SIZE)
        Network.register(newInstanceClient.kryo)
        newInstanceClient.addListener(object : Listener {
            override fun received(connection: Connection, message: Any) {
                if (instanceClient !== newInstanceClient) {
                    return
                }

                when (message) {
                    is PacketLoginAccepted -> {
                        connection.sendTCP(PacketConnectToInstance(uuid = instanceUuid))
                        startJoinRetry(newInstanceClient, instanceUuid)
                        setStatus(Localization.get("network.status.connected"))
                    }

                    is PacketLoginRefused -> {
                        val reason = message.reason.ifBlank {
                            Localization.get("network.error.instanceConnectFailed", host, port)
                        }
                        setError(reason)
                    }

                    is PacketGameInit -> {
                        GameSession.applyGameInit(message)
                        updateInstanceJoinConfirmed()
                    }

                    is PacketMapChunk -> {
                        GameSession.applyMapChunk(message)
                        updateInstanceJoinConfirmed()
                    }

                    is PacketVisibilityUpdate -> {
                        GameSession.applyVisibilityUpdate(message)
                        updateInstanceJoinConfirmed()
                    }

                    is PacketUnitSnapshot -> {
                        GameSession.applyUnitSnapshot(message)
                    }

                    is PacketUnitRemove -> {
                        GameSession.applyUnitRemove(message)
                    }

                    is PacketBuildingSnapshot -> {
                        GameSession.applyBuildingSnapshot(message)
                    }

                    is PacketBuildingRemove -> {
                        GameSession.applyBuildingRemove(message)
                    }

                    is PacketPlayerResources -> {
                        GameSession.applyPlayerResources(message)
                    }

                    is PacketPlaceBuildingResult -> {
                        lastPlacementResult = message
                    }

                    is PacketUpgradeBuildingResult -> {
                        lastUpgradeResult = message
                    }

                    is PacketTrainUnitResult -> {
                        lastTrainUnitResult = message
                    }

                    is PacketGameReady -> {
                        instanceReadyPacketReceived = true
                        updateInstanceJoinConfirmed()
                        gameReady = true
                        reconnectAttemptAfterDisconnect = false
                    }

                    is PacketPlayerPresenceUpdate -> {
                        GameSession.applyPlayerPresenceUpdate(message)
                    }

                    is PacketGameOver -> {
                        GameSession.applyGameOver(message)
                    }

                    is PacketKeepAliveAck -> Unit
                }
            }

            override fun disconnected(connection: Connection?) {
                if (instanceClient !== newInstanceClient) return
                if (tryReconnectAfterDisconnect()) {
                    return
                }
                setError(Localization.get("network.error.disconnected"))
            }
        })

        synchronized(lock) {
            val stoppedLobbyClient = lobbyClient
            val stoppedInstanceClient = instanceClient

            lobbyClient = null
            instanceClient = newInstanceClient

            stoppedLobbyClient?.stop()
            stoppedInstanceClient?.stop()
        }

        thread(name = "kryonet-instance-connect", isDaemon = true) {
            try {
                newInstanceClient.start()
                newInstanceClient.connect(5000, host, port, port)
                startKeepAlive(newInstanceClient, ConnectionScope.INSTANCE)
                newInstanceClient.sendTCP(
                    PacketLogin(
                        pseudo = username,
                        version = GameConfiguration.VERSION,
                        gameType = selectedGameType,
                        instanceUuid = instanceUuid
                    )
                )
            } catch (_: Exception) {
                if (isReconnectAttempt) return@thread
                setError(Localization.get("network.error.instanceConnectFailed", host, port))
            }
        }
    }

    private fun startKeepAlive(client: Client, scope: ConnectionScope) {
        thread(name = "kryonet-${scope.threadName}-keepalive", isDaemon = true) {
            while (isCurrentClient(client, scope)) {
                try {
                    Thread.sleep(KEEPALIVE_INTERVAL_MS)
                    if (!isCurrentClient(client, scope)) break
                    if (scope == ConnectionScope.INSTANCE) {
                        refreshReconnectExpiry()
                    }
                    client.sendTCP(PacketKeepAlive(timestampMs = System.currentTimeMillis()))
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun startJoinRetry(client: Client, instanceUuid: String) {
        thread(name = "kryonet-instance-join-retry", isDaemon = true) {
            var attempts = 0
            while (attempts < INSTANCE_JOIN_RETRY_COUNT && instanceClient === client && !hasConfirmedInstanceJoin()) {
                try {
                    Thread.sleep(INSTANCE_JOIN_RETRY_INTERVAL_MS)
                    if (instanceClient !== client || hasConfirmedInstanceJoin()) break
                    client.sendTCP(PacketConnectToInstance(uuid = instanceUuid))
                    attempts++
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun updateInstanceJoinConfirmed() {
        instanceJoinConfirmed = hasConfirmedInstanceJoin()
    }

    private fun hasConfirmedInstanceJoin(): Boolean {
        return instanceReadyPacketReceived && GameSession.hasInitialMultiplayerVisibility()
    }

    private fun reconnectRedirectOrNull(): PacketRedirectToInstance? {
        lastInstanceRedirect?.let { redirect ->
            if (SettingsManager.reconnectExpiresAtMs >= System.currentTimeMillis()) {
                return redirect
            }
        }

        val expiresAtMs = SettingsManager.reconnectExpiresAtMs
        if (expiresAtMs <= System.currentTimeMillis()) {
            lastInstanceRedirect = null
            SettingsManager.clearReconnectInfo()
            return null
        }

        val host = SettingsManager.reconnectInstanceHost
        val port = SettingsManager.reconnectInstancePort
        val uuid = SettingsManager.reconnectInstanceUuid
        val gameType = GameType.entries.firstOrNull { it.name == SettingsManager.reconnectGameType }
        if (host.isBlank() || port <= 0 || uuid.isBlank() || gameType == null) {
            lastInstanceRedirect = null
            SettingsManager.clearReconnectInfo()
            return null
        }

        selectedGameType = gameType
        return PacketRedirectToInstance(ip = host, port = port, uuid = uuid).also {
            lastInstanceRedirect = it
        }
    }

    private fun persistReconnectInfo(host: String, port: Int, instanceUuid: String) {
        if (instanceUuid.isBlank()) return
        SettingsManager.reconnectInstanceHost = host
        SettingsManager.reconnectInstancePort = port
        SettingsManager.reconnectInstanceUuid = instanceUuid
        SettingsManager.reconnectGameType = selectedGameType.name
        refreshReconnectExpiry()
    }

    private fun refreshReconnectExpiry() {
        if (lastInstanceRedirect == null) return
        SettingsManager.reconnectExpiresAtMs = System.currentTimeMillis() + RECONNECT_WINDOW_MS
    }

    private fun isCurrentClient(client: Client, scope: ConnectionScope): Boolean {
        return when (scope) {
            ConnectionScope.LOBBY -> lobbyClient === client
            ConnectionScope.INSTANCE -> instanceClient === client
        }
    }

    private fun tryReconnectAfterDisconnect(): Boolean {
        if (reconnectAttemptAfterDisconnect) return false
        if (GameSession.myPlayerId <= 0) return false
        val redirect = lastInstanceRedirect ?: return false

        reconnectAttemptAfterDisconnect = true
        setStatus(Localization.get("network.status.connecting"))

        thread(name = "kryonet-instance-reconnect", isDaemon = true) {
            Thread.sleep(RECONNECT_RETRY_DELAY_MS)
            connectToInstance(redirect, isReconnectAttempt = true)
        }
        return true
    }

    private fun setStatus(text: String) {
        statusText = text
    }

    private fun setError(text: String) {
        errorText = text
        gameReady = false
        synchronized(lock) {
            stopAllClientsLocked(clearState = false)
        }
    }

    private fun stopAllClientsLocked(clearState: Boolean) {
        val stoppedLobbyClient = lobbyClient
        val stoppedInstanceClient = instanceClient

        lobbyClient = null
        instanceClient = null

        stoppedLobbyClient?.stop()
        stoppedInstanceClient?.stop()
        gameReady = false
        lastGameplayTickSentAtMs = 0L
        instanceJoinConfirmed = false
        instanceReadyPacketReceived = false

        if (clearState) {
            statusText = ""
            errorText = null
            reconnectAttemptAfterDisconnect = false
        }
    }

    private fun resolveHost(): String {
        val propertyHost = System.getProperty("elixir.server.host")?.trim().orEmpty()
        if (propertyHost.isNotBlank()) return propertyHost

        val envHost = System.getenv("ELIXIR_SERVER_HOST")?.trim().orEmpty()
        if (envHost.isNotBlank()) return envHost

        if (NetworkDefaults.BUILD_HOST.isNotBlank()) return NetworkDefaults.BUILD_HOST

        return if (Gdx.app.type == Application.ApplicationType.Android) "10.0.2.2" else "127.0.0.1"

    }

    private fun resolvePort(): Int {
        val propertyPort = System.getProperty("elixir.server.port")?.toIntOrNull()
        if (propertyPort != null && propertyPort > 0) return propertyPort

        val envPort = System.getenv("ELIXIR_SERVER_PORT")?.toIntOrNull()
        if (envPort != null && envPort > 0) return envPort

        if (NetworkDefaults.BUILD_PORT > 0) return NetworkDefaults.BUILD_PORT

        return Network.PORT
    }

    private const val GAMEPLAY_TICK_SEND_INTERVAL_MS = 500L
    private const val RECONNECT_RETRY_DELAY_MS = 1000L
    private const val KEEPALIVE_INTERVAL_MS = 5000L
    private const val INSTANCE_JOIN_RETRY_INTERVAL_MS = 1000L
    private const val INSTANCE_JOIN_RETRY_COUNT = 5
    private const val RECONNECT_WINDOW_MS = 3 * 60 * 1000L

    private enum class ConnectionScope(val threadName: String) {
        LOBBY("lobby"),
        INSTANCE("instance")
    }
}
