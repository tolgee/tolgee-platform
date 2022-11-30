package io.tolgee.model.enums

fun Scope.unpack() = Scope.getSelfAndRequirements(this)

fun Array<Scope>.unpack() = Scope.getUnpackedScopes(this)
