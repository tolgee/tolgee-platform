package io.tolgee.model.enums

enum class ConflictType {
  /**
   * When project.translationProtection = PROTECT_REVIEWED
   * user has permission to change the translation, but we don't recommend it by default
   */
  SHOULD_NOT_EDIT_REVIEWED,

  /**
   * When project.translationProtection = PROTECT_REVIEWED
   * and user has no state-edit permission
   */
  CANNOT_EDIT_REVIEWED,
  CANNOT_EDIT_DISABLED,
  ;

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
