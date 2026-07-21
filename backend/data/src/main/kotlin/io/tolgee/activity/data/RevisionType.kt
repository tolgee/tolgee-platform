package io.tolgee.activity.data

/** Ordinal-persisted by [io.tolgee.model.activity.ActivityModifiedEntity.revisionType] — do not reorder. */
enum class RevisionType {
  ADD,
  MOD,
  DEL,
  ;

  fun isAdd(): Boolean {
    return this == ADD
  }

  fun isMod(): Boolean {
    return this == MOD
  }

  fun isDel(): Boolean {
    return this == DEL
  }
}
