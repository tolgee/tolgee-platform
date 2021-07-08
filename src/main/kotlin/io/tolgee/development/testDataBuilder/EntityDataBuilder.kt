package io.tolgee.development.testDataBuilder

interface EntityDataBuilder<T> {
    val self: T

    fun self(ft: T.() -> Unit): T {
        ft(self)
        return self
    }
}
