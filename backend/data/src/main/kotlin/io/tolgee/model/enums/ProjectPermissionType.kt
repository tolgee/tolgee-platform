package io.tolgee.model.enums

enum class ProjectPermissionType(val availableScopes: Array<Scope>) {
  NONE(arrayOf()),
  VIEW(
    arrayOf(
      Scope.TRANSLATIONS_VIEW,
      Scope.SCREENSHOTS_VIEW,
      Scope.ACTIVITY_VIEW,
      Scope.KEYS_VIEW,
      Scope.TASKS_VIEW,
    ),
  ),
  TRANSLATE(
    arrayOf(
      Scope.KEYS_VIEW,
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATIONS_COMMENTS_ADD,
      Scope.TRANSLATIONS_COMMENTS_SET_STATE,
      Scope.TASKS_VIEW,
    ),
  ),
  REVIEW(
    arrayOf(
      Scope.KEYS_VIEW,
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.SCREENSHOTS_VIEW,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATIONS_COMMENTS_ADD,
      Scope.TRANSLATIONS_COMMENTS_SET_STATE,
      Scope.TRANSLATIONS_STATE_EDIT,
      Scope.TASKS_VIEW,
    ),
  ),
  EDIT(
    arrayOf(
      Scope.KEYS_VIEW,
      Scope.TRANSLATIONS_VIEW,
      Scope.TRANSLATIONS_EDIT,
      Scope.KEYS_EDIT,
      Scope.KEYS_DELETE,
      Scope.KEYS_CREATE,
      Scope.SCREENSHOTS_VIEW,
      Scope.SCREENSHOTS_UPLOAD,
      Scope.SCREENSHOTS_DELETE,
      Scope.ACTIVITY_VIEW,
      Scope.TRANSLATIONS_COMMENTS_ADD,
      Scope.TRANSLATIONS_COMMENTS_SET_STATE,
      Scope.TRANSLATIONS_COMMENTS_EDIT,
      Scope.TRANSLATIONS_STATE_EDIT,
      Scope.BATCH_PRE_TRANSLATE_BY_TM,
      Scope.BATCH_MACHINE_TRANSLATE,
      Scope.BATCH_JOBS_VIEW,
      Scope.TASKS_VIEW,
      Scope.PROMPTS_VIEW,
      Scope.PROMPTS_EDIT,
    ),
  ),
  MANAGE(
    arrayOf(Scope.ADMIN),
  ),
  ;

  companion object {
    private fun expandAvailableScopes(permission: ProjectPermissionType): Array<Scope> {
      val result = mutableSetOf<Scope>()
      permission.availableScopes.forEach {
        result.add(it)
        it.expand().forEach { scope ->
          result.add(scope)
        }
      }
      return result.toTypedArray()
    }

    fun getRoles(): Map<String, Array<Scope>> {
      val result = mutableMapOf<String, Array<Scope>>()
      values().forEach { value -> result[value.name] = expandAvailableScopes(value) }
      return result.toMap()
    }

    fun findByScope(scope: Scope): List<ProjectPermissionType> {
      return values().filter { expandAvailableScopes(it).contains(scope) }
    }
  }
}
