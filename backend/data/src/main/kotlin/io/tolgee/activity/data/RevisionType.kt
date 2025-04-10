package io.tolgee.activity.data

enum class RevisionType {
  ADD,
  MOD,
  DEL,
  ;

  fun isAdd(): Boolean = this == ADD

  fun isMod(): Boolean = this == MOD

  fun isDel(): Boolean = this == DEL
}
