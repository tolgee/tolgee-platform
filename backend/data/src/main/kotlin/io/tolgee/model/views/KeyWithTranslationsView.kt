package io.tolgee.model.views

import io.tolgee.model.Screenshot
import io.tolgee.model.key.Tag
import io.tolgee.service.queryBuilders.Cursorable
import java.sql.Timestamp

data class KeyWithTranslationsView(
  val keyId: Long,
  val createdAt: Timestamp,
  val keyName: String,
  val keyIsPlural: Boolean,
  val keyPluralArgName: String?,
  val keyMaxCharLimit: Int?,
  val branch: String?,
  val keyNamespaceId: Long?,
  val keyNamespace: String?,
  val keyDescription: String?,
  val screenshotCount: Long,
  val contextPresent: Boolean,
  val translations: MutableMap<String, TranslationView> = mutableMapOf(),
  val deletedAt: Timestamp? = null,
  val deletedByUserId: Long? = null,
  val deletedByUserName: String? = null,
  val deletedByUserUsername: String? = null,
  val deletedByUserAvatarHash: String? = null,
  val deletedByUserDeletedAt: Timestamp? = null,
) : Cursorable {
  lateinit var keyTags: List<Tag>
  var screenshots: Collection<Screenshot>? = null
  var tasks: List<KeyTaskView>? = null

  companion object {
    /**
     * Public sort/cursor property naming for translation columns. The wire format is
     * `translations.{languageTag}.{field}` and is shared by:
     *
     *  - Spring Data `Sort` parsing on the controller (`?sort=translations.de.text`)
     *  - cursor (de)serialization in `CursorUtil`
     *  - the column-resolution paths in `TranslationsViewQueryBuilder` and
     *    `CursorPredicateProvider`
     *
     * Building and parsing the format goes through these helpers so the contract has a
     * single source of truth.
     */
    fun translationProperty(
      languageTag: String,
      field: String,
    ): String = "${KeyWithTranslationsView::translations.name}.$languageTag.$field"

    /**
     * Returns `(languageTag, field)` for a [property] of shape `translations.{tag}.{field}`,
     * or `null` if [property] is not in that shape.
     */
    fun parseTranslationProperty(property: String): Pair<String, String>? {
      val parts = property.split(".")
      if (parts.size != 3) return null
      if (parts[0] != KeyWithTranslationsView::translations.name) return null
      return parts[1] to parts[2]
    }

    /**
     * Builds a [KeyWithTranslationsView] from a Query 1 (key-only) result row. The translation
     * data for the returned view is empty at this point; it is populated later by Query 2 in
     * `TranslationViewDataProvider`.
     *
     * The row layout is fixed to 12 key-level columns + optional 6 trashed-only columns. See
     * `QueryBase.kt` for the full list and order.
     */
    fun of(
      queryData: Array<Any?>,
      trashed: Boolean = false,
    ): KeyWithTranslationsView {
      val data = mutableListOf(*queryData)
      return KeyWithTranslationsView(
        keyId = data.removeFirst() as Long,
        createdAt = data.removeFirst() as Timestamp,
        keyName = data.removeFirst() as String,
        keyIsPlural = data.removeFirst() as Boolean,
        keyPluralArgName = data.removeFirst() as String?,
        keyMaxCharLimit = data.removeFirst() as Int?,
        branch = data.removeFirst() as String?,
        keyNamespaceId = data.removeFirst() as Long?,
        keyNamespace = data.removeFirst() as String?,
        keyDescription = data.removeFirst() as String?,
        screenshotCount = data.removeFirst() as Long,
        contextPresent = data.removeFirst() as Boolean,
        deletedAt = if (trashed) data.removeFirst() as Timestamp? else null,
        deletedByUserId = if (trashed) data.removeFirst() as Long? else null,
        deletedByUserName = if (trashed) data.removeFirst() as String? else null,
        deletedByUserUsername = if (trashed) data.removeFirst() as String? else null,
        deletedByUserAvatarHash = if (trashed) data.removeFirst() as String? else null,
        deletedByUserDeletedAt = if (trashed) data.removeFirst() as Timestamp? else null,
      )
    }
  }

  override fun toCursorValue(property: String): String? {
    parseTranslationProperty(property)?.let { (tag, field) ->
      val translation = translations[tag]
      return when (field) {
        TranslationView::text.name -> translation?.text
        TranslationView::id.name -> translation?.id?.toString()
        TranslationView::state.name -> translation?.state?.name
        else -> null
      }
    }
    return when (property) {
      KeyWithTranslationsView::keyId.name -> keyId.toString()
      KeyWithTranslationsView::createdAt.name -> createdAt.time.toString()
      KeyWithTranslationsView::keyNamespace.name -> keyNamespace
      KeyWithTranslationsView::keyName.name -> keyName
      KeyWithTranslationsView::screenshotCount.name -> screenshotCount.toString()
      KeyWithTranslationsView::branch.name -> branch
      else -> null
    }
  }
}
