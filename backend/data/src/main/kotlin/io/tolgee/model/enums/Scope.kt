package io.tolgee.model.enums

import com.fasterxml.jackson.annotation.JsonValue
import io.tolgee.constants.Message
import io.tolgee.exceptions.NotFoundException

enum class Scope(
  @get:JsonValue
  var value: String
) {
  TRANSLATIONS_VIEW("translations.view"),
  TRANSLATIONS_EDIT("translations.edit"),
  KEYS_EDIT("keys.edit"),
  SCREENSHOTS_UPLOAD("screenshots.upload"),
  SCREENSHOTS_DELETE("screenshots.delete"),
  SCREENSHOTS_VIEW("screenshots.view"),
  ACTIVITY_VIEW("activity.view"),
  IMPORT("import"),
  LANGUAGES_EDIT("languages.edit"),
  ADMIN("admin"),
  PROJECT_EDIT("users.view"),
  MEMBERS_EDIT("permissions.edit"),
  MEMBERS_VIEW("users.view"),
  TRANSLATION_COMMENTS_ADD("translation-comments.add"),
  TRANSLATION_COMMENTS_EDIT("translation-comments.edit"),
  TRANSLATION_COMMENTS_SET_STATE("translation-comments.set-state"),
  TRANSLATION_STATE_EDIT("translation-state.edit")
  ;

  fun expand() = Scope.expand(this)

  companion object {
    val hierarchy = HierarchyItem(
      ADMIN,
      listOf(
        HierarchyItem(
          TRANSLATIONS_EDIT,
          listOf(
            HierarchyItem(TRANSLATIONS_VIEW)
          )
        ),
        HierarchyItem(
          KEYS_EDIT, listOf()
        ),
        HierarchyItem(
          SCREENSHOTS_UPLOAD,
          listOf(
            HierarchyItem(SCREENSHOTS_VIEW)
          )
        ),
        HierarchyItem(
          SCREENSHOTS_DELETE,
          listOf(
            HierarchyItem(SCREENSHOTS_VIEW)
          )
        ),
        HierarchyItem(ACTIVITY_VIEW),
        HierarchyItem(IMPORT),
        HierarchyItem(LANGUAGES_EDIT),
        HierarchyItem(PROJECT_EDIT),
        HierarchyItem(
          MEMBERS_EDIT,
          listOf(HierarchyItem(MEMBERS_VIEW))
        ),
        HierarchyItem(
          TRANSLATION_COMMENTS_SET_STATE,
          listOf(HierarchyItem(TRANSLATIONS_VIEW))
        ),
        HierarchyItem(
          TRANSLATION_COMMENTS_ADD,
          listOf(HierarchyItem(TRANSLATIONS_VIEW))
        ),
        HierarchyItem(
          TRANSLATION_COMMENTS_EDIT,
          listOf(HierarchyItem(TRANSLATIONS_VIEW))
        ),
        HierarchyItem(
          TRANSLATION_STATE_EDIT, listOf(HierarchyItem(TRANSLATIONS_VIEW))
        )
      )
    )

    private fun expand(item: HierarchyItem): MutableSet<Scope> {
      val descendants = item.requires.flatMap {
        expand(it)
      }.toMutableSet()

      descendants.add(item.scope)
      return descendants
    }

    private fun getScopeHierarchyItems(root: HierarchyItem, scope: Scope): List<HierarchyItem> {
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
      return hierarchyItems.flatMap { expand(it) }.toTypedArray()
    }

    /**
     * Returns all scopes recursively
     *
     * Example: When permittedScopes == [ADMIN], it returns all the scopes which is included in
     * ADMIN scope, (TRANSLATION_VIEW, KEYS_EDIT, etc.)
     *
     */
    fun expand(permittedScopes: Array<Scope>): Array<Scope> {
      return permittedScopes.flatMap { expand(it).toList() }.toSet().toTypedArray()
    }

    fun fromValue(value: String): Scope {
      for (scope in values()) {
        if (scope.value == value) {
          return scope
        }
      }
      throw NotFoundException(Message.SCOPE_NOT_FOUND)
    }
  }

  data class HierarchyItem(val scope: Scope, val requires: List<HierarchyItem> = listOf())
}
