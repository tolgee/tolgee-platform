package io.tolgee.model.enums

enum class ConflictType {
  CANNOT_EDIT_REVIEWED,
  CANNOT_EDIT_DISABLED,
  SHOULD_NOT_EDIT_REVIEWED;

  companion object {
    fun isOverridable(conflictType: ConflictType?): Boolean {
      return conflictType == null || conflictType == SHOULD_NOT_EDIT_REVIEWED
    }

    /**
     * Conflict type which is overridable and recommended to override
     *  e.g., for `SHOULD_NOT_EDIT_REVIEWED`, is overridable but not recommended to override
     */
    fun isOverridableAndRecommended(conflictType: ConflictType?): Boolean {
      return conflictType == null
    }
  }
}
