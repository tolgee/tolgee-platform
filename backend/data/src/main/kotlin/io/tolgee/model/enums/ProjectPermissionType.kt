package io.tolgee.model.enums

enum class ProjectPermissionType(val power: Int, val availableScopes: Array<Scope>) {
  VIEW(1, arrayOf(Scope.TRANSLATIONS_VIEW, Scope.SCREENSHOTS_VIEW, Scope.ACTIVITY_VIEW)),
  TRANSLATE(
    2,
    arrayOf(
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATION_COMMENTS_ADD,
      Scope.TRANSLATION_STATE_EDIT
    )
  ),
  EDIT(
    3,
    arrayOf(
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.KEYS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.SCREENSHOTS_UPLOAD,
      Scope.SCREENSHOTS_DELETE,
      Scope.ACTIVITY_VIEW,
      Scope.IMPORT,
      Scope.TRANSLATION_COMMENTS_ADD,
      Scope.TRANSLATION_STATE_EDIT
    )
  ),
  MANAGE(
    4,
    arrayOf(Scope.ADMIN)
  );
}
