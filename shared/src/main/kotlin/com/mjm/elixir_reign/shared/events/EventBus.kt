package com.mjm.elixir_reign.shared.events

import kotlin.reflect.KClass

class EventBus {
    private val listeners = mutableMapOf<KClass<*>, MutableList<(Any) -> Unit>>()

    fun <T : Any> subscribe(eventType: KClass<T>, listener: (T) -> Unit): () -> Unit {
        val bucket = listeners.getOrPut(eventType) { mutableListOf() }
        val wrapper: (Any) -> Unit = { event ->
            @Suppress("UNCHECKED_CAST")
            listener(event as T)
        }
        bucket += wrapper
        return { bucket.remove(wrapper) }
    }

    inline fun <reified T : Any> subscribe(noinline listener: (T) -> Unit): () -> Unit {
        return subscribe(T::class, listener)
    }

    fun publish(event: Any) {
        listeners[event::class]?.toList()?.forEach { it(event) }
    }
}
