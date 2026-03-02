package com.mjm.elixir_reign.shared.network

// Login packets
class PacketLogin(var pseudo: String = "", var version: String = "")
class PacketLoginAccepted(var myId: Int=0)
class PacketLoginRefused(var reason: String = "")
