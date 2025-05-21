package io.tolgee.model.enums

import com.fasterxml.jackson.annotation.JsonValue
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException

enum class Scope(
  @get:JsonValue
  var value: String,
) {
  TRANSLATIONS_VIEW("translations.view"),
  TRANSLATIONS_EDIT("translations.edit"),
  KEYS_EDIT("keys.edit"),
  SCREENSHOTS_UPLOAD("screenshots.upload"),
  SCREENSHOTS_DELETE("screenshots.delete"),
  SCREENSHOTS_VIEW("screenshots.view"),
  ACTIVITY_VIEW("activity.view"),
  LANGUAGES_EDIT("languages.edit"),
  ADMIN("admin"),
  PROJECT_EDIT("project.edit"),
  MEMBERS_VIEW("members.view"),
  MEMBERS_EDIT("members.edit"),
  TRANSLATIONS_COMMENTS_ADD("translation-comments.add"),
  TRANSLATIONS_COMMENTS_EDIT("translation-comments.edit"),
  TRANSLATIONS_COMMENTS_SET_STATE("translation-comments.set-state"),
  TRANSLATIONS_STATE_EDIT("translations.state-edit"),
  KEYS_VIEW("keys.view"),
  KEYS_DELETE("keys.delete"),
  KEYS_CREATE("keys.create"),
  BATCH_JOBS_VIEW("batch-jobs.view"),
  BATCH_JOBS_CANCEL("batch-jobs.cancel"),
  BATCH_PRE_TRANSLATE_BY_TM("translations.batch-by-tm"),
  BATCH_MACHINE_TRANSLATE("translations.batch-machine"),
  CONTENT_DELIVERY_MANAGE("content-delivery.manage"),
  CONTENT_DELIVERY_PUBLISH("content-delivery.publish"),
  WEBHOOKS_MANAGE("webhooks.manage"),
  TASKS_VIEW("tasks.view"),
  TASKS_EDIT("tasks.edit"),
  PROMPTS_VIEW("prompts.view"),
  PROMPTS_EDIT("prompts.edit"),
  ;

  fun expand() = Scope.expand(this)

  companion object {
    private val keysView = HierarchyItem(KEYS_VIEW)
    private val translationsView = HierarchyItem(TRANSLATIONS_VIEW, listOf(keysView))
    private val screenshotsView = HierarchyItem(SCREENSHOTS_VIEW, listOf(keysView))
    private val translationsEdit = HierarchyItem(TRANSLATIONS_EDIT, listOf(translationsView))
    private val tasksView = HierarchyItem(TASKS_VIEW, listOf(translationsView))

    val hierarchy =
      HierarchyItem(
        ADMIN,
        listOf(
          translationsEdit,
          HierarchyItem(
            KEYS_EDIT,
            listOf(
              keysView,
            ),
          ),
          HierarchyItem(
            KEYS_DELETE,
            listOf(
              keysView,
            ),
          ),
          HierarchyItem(
            KEYS_CREATE,
            listOf(keysView),
          ),
          HierarchyItem(
            SCREENSHOTS_UPLOAD,
            listOf(
              screenshotsView,
            ),
          ),
          HierarchyItem(
            SCREENSHOTS_DELETE,
            listOf(
              screenshotsView,
            ),
          ),
          HierarchyItem(TASKS_EDIT, listOf(tasksView)),
          HierarchyItem(PROMPTS_EDIT, listOf(HierarchyItem(PROMPTS_VIEW))),
          HierarchyItem(ACTIVITY_VIEW),
          HierarchyItem(LANGUAGES_EDIT, listOf(HierarchyItem(PROMPTS_VIEW))),
          HierarchyItem(PROJECT_EDIT),
          HierarchyItem(
            MEMBERS_EDIT,
            listOf(HierarchyItem(MEMBERS_VIEW)),
          ),
          HierarchyItem(
            TRANSLATIONS_COMMENTS_SET_STATE,
            listOf(translationsView),
          ),
          HierarchyItem(
            TRANSLATIONS_COMMENTS_ADD,
            listOf(translationsView),
          ),
          HierarchyItem(
            TRANSLATIONS_COMMENTS_EDIT,
            listOf(translationsView),
          ),
          HierarchyItem(
            TRANSLATIONS_STATE_EDIT,
            listOf(HierarchyItem(TRANSLATIONS_VIEW)),
          ),
          HierarchyItem(BATCH_JOBS_VIEW),
          HierarchyItem(BATCH_JOBS_CANCEL),
          HierarchyItem(BATCH_PRE_TRANSLATE_BY_TM, listOf(translationsEdit)),
          HierarchyItem(BATCH_MACHINE_TRANSLATE, listOf(translationsEdit)),
          HierarchyItem(CONTENT_DELIVERY_MANAGE, listOf(HierarchyItem(CONTENT_DELIVERY_PUBLISH))),
          HierarchyItem(WEBHOOKS_MANAGE),
        ),
      )

    private fun expand(item: HierarchyItem): MutableSet<Scope> {
      val descendants =
        item.requires.flatMap {
          expand(it)
        }.toMutableSet()

      descendants.add(item.scope)
      return descendants
    }

    private fun getScopeHierarchyItems(
      root: HierarchyItem,
      scope: Scope,
    ): List<HierarchyItem> {
      val items = mutableListOf<HierarchyItem>()
      if (root.scope == scope) {
        items.add(root)
      }
      root.requires.forEach { items.addAll(getScopeHierarchyItems(it, scope)) }
      return items
    }

    private fun getScopeHierarchyItems(scope: Scope): Array<HierarchyItem> {
      return getScopeHierarchyItems(root = hierarchy, scope).toTypedArray()
    }

    fun expand(scope: Scope): Array<Scope> {
      val hierarchyItems = getScopeHierarchyItems(scope)
      return hierarchyItems.flatMap { expand(it) }.toSet().toTypedArray()
    }

    /**
     * Returns all scopes recursively
     *
     * Example: When permittedScopes == [ADMIN], it returns all the scopes which is included in
     * ADMIN scope, (TRANSLATION_VIEW, KEYS_EDIT, etc.)
     *
     */
    fun expand(permittedScopes: Array<Scope>?): Array<Scope> {
      permittedScopes ?: return arrayOf()
      return permittedScopes.flatMap { expand(it).toList() }.toSet().toTypedArray()
    }

    fun expand(permittedScopes: Collection<Scope>): Array<Scope> {
      return expand(permittedScopes.toTypedArray())
    }

    /**
     * Returns all possible scopes that contain required scope
     *
     * Example: When permittedScope === KEYS_VIEW, it returns [KEYS_VIEW, KEYS_EDIT, ADMIN]
     * when user has any of these scopes, he effectively has KEYS_VIEW
     */
    fun selfAndAncestors(
      permittedScope: Scope,
      root: HierarchyItem = hierarchy,
    ): List<Scope> {
      val result = mutableSetOf<Scope>()
      root.requires.forEach {
        result.addAll(selfAndAncestors(permittedScope, it))
      }
      if (result.isNotEmpty() || root.scope === permittedScope) {
        result.add(root.scope)
      }
      return result.toList()
    }

    fun fromValue(value: String): Scope {
      for (scope in values()) {
        if (scope.value == value) {
          return scope
        }
      }
      throw NotFoundException(Message.SCOPE_NOT_FOUND)
    }

    fun parse(scopes: Collection<String>?): Set<Scope> {
      scopes ?: return setOf()
      return scopes.map { stringScope ->
        Scope.values().find { it.value == stringScope } ?: throw BadRequestException(
          Message.SCOPE_NOT_FOUND,
          listOf(stringScope),
        )
      }.toSet()
    }
  }

  data class HierarchyItem(val scope: Scope, val requires: List<HierarchyItem> = listOf())
}
