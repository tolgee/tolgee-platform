package io.tolgee.core.concepts.conversions

interface Into<T>

class ShortCircuitException(val value: Any?) : Exception(null, null, false, false)

fun shortCircuit(value: Any?): Nothing = throw ShortCircuitException(value)

@Suppress("UNCHECKED_CAST")
inline fun <T, C : Into<T>> converting(conversions: C, block: C.() -> T): T = try {
    conversions.block()
} catch (e: ShortCircuitException) {
    e.value as T
}
