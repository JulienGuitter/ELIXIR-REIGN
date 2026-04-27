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
import com.mjm.elixir_reign.shared.network.PacketConnectToInstance
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketGameReady
import com.mjm.elixir_reign.shared.network.PacketGameplayTick
import com.mjm.elixir_reign.shared.network.PacketLogin
import com.mjm.elixir_reign.shared.network.PacketLoginAccepted
import com.mjm.elixir_reign.shared.network.PacketLoginRefused
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketMoveUnitsRequest
import com.mjm.elixir_reign.shared.network.PacketPlayerPresenceUpdate
import com.mjm.elixir_reign.shared.network.PacketRedirectToInstance
import com.mjm.elixir_reign.shared.network.PacketUnitRemove
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.network.PacketVisibilityUpdate
import com.mjm.elixir_reign.core.session.GameSession
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

    fun startMatchmaking(gameType: GameType) {
        synchronized(lock) {
            stopAllClientsLocked(clearState = true)
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
                        setStatus(Localization.get("network.status.redirecting"))
                        connectToInstance(message)
                    }
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

    fun canReconnectToLastInstance(): Boolean {
        return lastInstanceRedirect != null
    }

    fun reconnectToLastInstance(): Boolean {
        val redirect = lastInstanceRedirect ?: return false

        errorText = null
        gameReady = false
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

    private fun connectToInstance(redirect: PacketRedirectToInstance, isReconnectAttempt: Boolean = false) {
        val host = if (redirect.ip == "this" || redirect.ip.isBlank()) resolveHost() else redirect.ip
        val port = if (redirect.port > 0) redirect.port else resolvePort()
        val instanceUuid = redirect.uuid
        lastInstanceRedirect = PacketRedirectToInstance(ip = host, port = port, uuid = instanceUuid)
        if (!isReconnectAttempt) {
            reconnectAttemptAfterDisconnect = false
        }

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
                    }

                    is PacketMapChunk -> {
                        GameSession.applyMapChunk(message)
                    }

                    is PacketVisibilityUpdate -> {
                        GameSession.applyVisibilityUpdate(message)
                    }

                    is PacketUnitSnapshot -> {
                        GameSession.applyUnitSnapshot(message)
                    }

                    is PacketUnitRemove -> {
                        GameSession.applyUnitRemove(message)
                    }

                    is PacketGameReady -> {
                        gameReady = true
                        reconnectAttemptAfterDisconnect = false
                    }

                    is PacketPlayerPresenceUpdate -> {
                        GameSession.applyPlayerPresenceUpdate(message)
                    }
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
}
