package io.tolgee.ee.service.branching.merging

import io.tolgee.model.Language
import io.tolgee.model.Screenshot
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMerge
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.util.forEachRow
import io.tolgee.util.getJson
import io.tolgee.util.getJsonSet
import io.tolgee.util.getLongOrNull
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.ResultSet

/**
 * The returned keys and snapshots are detached and carry only the columns selected here. Reload a key by
 * id before modifying it or reading anything else from it.
 */
@Component
class BranchMergeKeyLoader(
  private val entityManager: EntityManager,
) {
  class Loaded(
    val snapshots: List<KeySnapshot>,
    val sourceKeys: List<Key>,
    val targetKeys: List<Key>,
  )

  fun load(merge: BranchMerge): Loaded {
    // Plain JDBC skips Hibernate's auto-flush, so without this the queries miss changes still pending
    // in the persistence context.
    entityManager.flush()
    return entityManager.unwrap(Session::class.java).doReturningWork { connection ->
      Loaded(
        snapshots = loadSnapshots(connection, merge.sourceBranch.id),
        sourceKeys = loadKeys(connection, merge.sourceBranch),
        targetKeys = loadKeys(connection, merge.targetBranch),
      )
    }
  }

  private fun loadKeys(
    connection: Connection,
    branch: Branch,
  ): List<Key> {
    val project = branch.project
    val where = keysOf(branch)
    val params = longArrayOf(project.id, branch.id)
    val keys = LinkedHashMap<Long, Key>()
    val namespaces = HashMap<Long, Namespace>()
    val languages = HashMap<Long, Language>()

    connection.forEachRow(
      """
      select k.id, k.name, k.branch_id, k.is_plural, k.plural_arg_name, k.max_char_limit,
        ns.id as namespace_id, ns.name as namespace_name,
        km.id as meta_id, km.description, km.custom::text as custom
      from key k
      left join namespace ns on ns.id = k.namespace_id
      left join key_meta km on km.key_id = k.id
      where $where
      """,
      params,
    ) { row ->
      val key = Key(row.getString("name"))
      key.id = row.getLong("id")
      key.project = project
      key.branch = branch.takeIf { row.getObject("branch_id") != null }
      key.namespace =
        row.getLongOrNull("namespace_id")?.let { namespaceId ->
          namespaces.getOrPut(namespaceId) {
            Namespace(row.getString("namespace_name"), project).apply { id = namespaceId }
          }
        }
      key.isPlural = row.getBoolean("is_plural")
      key.pluralArgName = row.getString("plural_arg_name")
      key.maxCharLimit = row.getObject("max_char_limit") as Int?
      key.keyMeta =
        row.getLongOrNull("meta_id")?.let { metaId ->
          KeyMeta(key).apply {
            id = metaId
            description = row.getString("description")
            custom = row.getJson("custom")
          }
        }
      keys[key.id] = key
    }

    connection.forEachRow(
      """
      select km.key_id, tg.id, tg.name
      from key k
      join key_meta km on km.key_id = k.id
      join key_meta_tags kmt on kmt.key_metas_id = km.id
      join tag tg on tg.id = kmt.tags_id
      where $where
      """,
      params,
    ) { row ->
      val meta = keys[row.getLong("key_id")]?.keyMeta ?: return@forEachRow
      meta.tags.add(
        Tag().apply {
          id = row.getLong("id")
          name = row.getString("name")
          this.project = project
        },
      )
    }

    connection.forEachRow(
      """
      select t.key_id, t.id, t.text, t.state, l.id as language_id, l.tag
      from key k
      join translation t on t.key_id = k.id
      join language l on l.id = t.language_id
      where $where
      """,
      params,
    ) { row ->
      val key = keys[row.getLong("key_id")] ?: return@forEachRow
      val languageId = row.getLong("language_id")
      val translation = Translation(row.getString("text"))
      translation.id = row.getLong("id")
      translation.key = key
      translation.language =
        languages.getOrPut(languageId) {
          Language().apply {
            id = languageId
            tag = row.getString("tag")
          }
        }
      translation.state = row.getTranslationState()
      key.translations.add(translation)
    }

    connection.forEachRow(
      """
      select t.key_id, t.id as translation_id, lb.id, lb.name
      from key k
      join translation t on t.key_id = k.id
      join translation_label tl on tl.translation_id = t.id
      join label lb on lb.id = tl.label_id
      where $where
      """,
      params,
    ) { row ->
      val translationId = row.getLong("translation_id")
      val translation =
        keys[row.getLong("key_id")]?.translations?.find { it.id == translationId } ?: return@forEachRow
      translation.labels.add(
        Label().apply {
          id = row.getLong("id")
          name = row.getString("name")
        },
      )
    }

    connection.forEachRow(
      """
      select r.key_id, r.screenshot_id, r.positions::text as positions, r.original_text
      from key k
      join key_screenshot_reference r on r.key_id = k.id
      where $where
      """,
      params,
    ) { row ->
      val key = keys[row.getLong("key_id")] ?: return@forEachRow
      key.keyScreenshotReferences.add(
        KeyScreenshotReference().apply {
          this.key = key
          screenshot = Screenshot().apply { id = row.getLong("screenshot_id") }
          positions = row.getJson("positions")
          originalText = row.getString("original_text")
        },
      )
    }

    return keys.values.toList()
  }

  private fun loadSnapshots(
    connection: Connection,
    branchId: Long,
  ): List<KeySnapshot> {
    val params = longArrayOf(branchId)
    val snapshots = LinkedHashMap<Long, KeySnapshot>()

    connection.forEachRow(
      """
      select s.id, s.name, s.namespace, s.is_plural, s.plural_arg_name, s.max_char_limit,
        s.original_key_id, s.branch_key_id, s.screenshot_references::text as screenshot_references,
        m.id as meta_id, m.description, m.custom::text as custom, m.tags::text as tags
      from branch_key_snapshot s
      left join branch_key_meta_snapshot m on m.key_snapshot_id = s.id
      where s.branch_id = ?
      """,
      params,
    ) { row ->
      val snapshot =
        KeySnapshot(
          name = row.getString("name"),
          namespace = row.getString("namespace"),
          isPlural = row.getBoolean("is_plural"),
          pluralArgName = row.getString("plural_arg_name"),
          maxCharLimit = row.getObject("max_char_limit") as Int?,
          originalKeyId = row.getLong("original_key_id"),
          branchKeyId = row.getLong("branch_key_id"),
        )
      snapshot.id = row.getLong("id")
      snapshot.screenshotReferences = row.getJsonSet("screenshot_references")
      snapshot.keyMetaSnapshot =
        row.getLongOrNull("meta_id")?.let { metaId ->
          KeyMetaSnapshot(
            description = row.getString("description"),
            custom = row.getJson("custom"),
            tags = row.getJsonSet("tags"),
          ).apply { id = metaId }
        }
      snapshots[snapshot.id] = snapshot
    }

    connection.forEachRow(
      """
      select ts.key_snapshot_id, ts.id, ts.language, ts.value, ts.state, ts.labels::text as labels
      from branch_key_snapshot s
      join branch_translation_snapshot ts on ts.key_snapshot_id = s.id
      where s.branch_id = ?
      """,
      params,
    ) { row ->
      val snapshot = snapshots[row.getLong("key_snapshot_id")] ?: return@forEachRow
      snapshot.translations.add(
        TranslationSnapshot(
          language = row.getString("language"),
          value = row.getString("value").orEmpty(),
          state = row.getTranslationState(),
        ).apply {
          id = row.getLong("id")
          labels = row.getJsonSet("labels")
        },
      )
    }

    return snapshots.values.toList()
  }

  private fun keysOf(branch: Branch): String {
    val activeInProject = "k.project_id = ? and k.deleted_at is null"
    if (branch.isDefault) return "$activeInProject and (k.branch_id = ? or k.branch_id is null)"
    return "$activeInProject and k.branch_id = ?"
  }

  private fun ResultSet.getTranslationState(): TranslationState {
    val ordinal = getObject("state") as Number? ?: return TranslationState.TRANSLATED
    return TRANSLATION_STATES[ordinal.toInt()]
  }

  companion object {
    private val TRANSLATION_STATES = TranslationState.entries
  }
}
