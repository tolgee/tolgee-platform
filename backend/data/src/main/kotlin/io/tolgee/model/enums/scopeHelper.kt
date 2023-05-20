package io.tolgee.model.enums

fun Scope.unpack() = Scope.expand(this)

fun Array<Scope>.unpack() = Scope.expand(this)
