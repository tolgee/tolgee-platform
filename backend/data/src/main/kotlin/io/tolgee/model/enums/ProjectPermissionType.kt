package io.tolgee.model.enums

enum class ProjectPermissionType(val availableScopes: Array<Scope>) {
  NONE(arrayOf()),
  VIEW(arrayOf(Scope.TRANSLATIONS_VIEW, Scope.SCREENSHOTS_VIEW, Scope.ACTIVITY_VIEW, Scope.KEYS_VIEW)),
  TRANSLATE(
    arrayOf(
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATIONS_COMMENTS_ADD,
      Scope.TRANSLATIONS_COMMENTS_SET_STATE,
    )
  ),
  REVIEW(
    arrayOf(
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATIONS_COMMENTS_ADD,
      Scope.TRANSLATIONS_COMMENTS_SET_STATE,
      Scope.TRANSLATIONS_STATE_EDIT
    )
  ),
  EDIT(
    arrayOf(
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.KEYS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.SCREENSHOTS_UPLOAD,
      Scope.SCREENSHOTS_DELETE,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATIONS_COMMENTS_ADD,
      Scope.TRANSLATIONS_STATE_EDIT
    )
  ),
  MANAGE(
    arrayOf(Scope.ADMIN)
  );

  companion object {
    fun getRoles(): Map<String, Array<Scope>> {
      val result = mutableMapOf<String, Array<Scope>>()
      ProjectPermissionType.values().forEach { value -> result[value.name] = value.availableScopes }
      return result.toMap()
    }
  }
}
