package io.tolgee.model.enums

enum class ProjectPermissionType(val availableScopes: Array<Scope>) {
  NONE(arrayOf()),
  VIEW(arrayOf(Scope.TRANSLATIONS_VIEW, Scope.SCREENSHOTS_VIEW, Scope.ACTIVITY_VIEW)),
  TRANSLATE(
    arrayOf(
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATION_COMMENTS_ADD,
      Scope.TRANSLATION_COMMENTS_SET_STATE,
      Scope.TRANSLATION_STATE_EDIT
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
      Scope.IMPORT,
      Scope.TRANSLATION_COMMENTS_ADD,
      Scope.TRANSLATION_STATE_EDIT
    )
  ),
  MANAGE(
    arrayOf(Scope.ADMIN)
  );
}
