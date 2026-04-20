package io.tolgee.service.dataImport

import com.fasterxml.jackson.databind.ObjectMapper
import io.tolgee.activity.data.RevisionType
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Manually records activity for imported keys and translations via JDBC,
 * bypassing the Hibernate activity interceptor.
 *
 * Why: the interceptor accumulates [io.tolgee.model.activity.ActivityModifiedEntity]
 * objects in [io.tolgee.activity.ActivityHolder.modifiedEntities], which lives
 * outside the Hibernate session — so [io.tolgee.util.flushAndClear] does NOT
 * clear them. At bulk-import scale (100k+ keys) these pile up across all
 * batches, causing OOM. Then at commit time, `BeforeTransactionCompletionProcess`
 * tries to persist them all at once, which is also slow.
 *
 * By setting `disableActivityLogging = true` on each entity before flush and
 * writing the activity rows here via JDBC after each batch, we keep the
 * interceptor's state empty and persist activity incrementally.
 *
 * The format matches what the interceptor produces for new entities (ADD).
 */
class ImportActivityRecorder(
  private val jdbcTemplate: JdbcTemplate,
  private val objectMapper: ObjectMapper,
  private val activityRevisionId: Long,
  private val branchId: Long?,
) {
  fun recordKeys(keys: Collection<Key>) {
    if (keys.isEmpty()) return
    jdbcTemplate.batchUpdate(
      """INSERT INTO activity_modified_entity
         (entity_class, entity_id, describing_data, describing_relations,
          modifications, revision_type, activity_revision_id, branch_id)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
      keys.toList(),
      1000,
    ) { ps, key ->
      ps.setString(1, "Key")
      ps.setLong(2, key.id)
      ps.setObject(3, jsonb(emptyMap<String, Any>()))
      ps.setObject(4, jsonb(emptyMap<String, Any>()))
      ps.setObject(
        5,
        jsonb(
          mapOf(
            "name" to mapOf("old" to null, "new" to key.name),
            "isPlural" to mapOf("old" to null, "new" to key.isPlural),
          ),
        ),
      )
      ps.setInt(6, RevisionType.ADD.ordinal)
      ps.setLong(7, activityRevisionId)
      ps.setObject(8, branchId)
    }
  }

  fun recordKeyMetas(keyMetas: Collection<KeyMeta>) {
    if (keyMetas.isEmpty()) return
    jdbcTemplate.batchUpdate(
      """INSERT INTO activity_modified_entity
         (entity_class, entity_id, describing_data, describing_relations,
          modifications, revision_type, activity_revision_id, branch_id)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)
         ON CONFLICT (activity_revision_id, entity_class, entity_id) DO NOTHING""",
      keyMetas.toList(),
      1000,
    ) { ps, meta ->
      ps.setString(1, "KeyMeta")
      ps.setLong(2, meta.id)
      ps.setObject(3, jsonb(emptyMap<String, Any>()))
      ps.setObject(4, jsonb(emptyMap<String, Any>()))
      ps.setObject(
        5,
        jsonb(
          mapOf(
            "description" to mapOf("old" to null, "new" to meta.description),
            "custom" to mapOf("old" to null, "new" to meta.custom),
          ),
        ),
      )
      ps.setInt(6, RevisionType.ADD.ordinal)
      ps.setLong(7, activityRevisionId)
      ps.setObject(8, branchId)
    }
  }

  fun recordTranslations(translations: Collection<Translation>) {
    if (translations.isEmpty()) return
    jdbcTemplate.batchUpdate(
      """INSERT INTO activity_modified_entity
         (entity_class, entity_id, describing_data, describing_relations,
          modifications, revision_type, activity_revision_id, branch_id)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
      translations.toList(),
      1000,
    ) { ps, translation ->
      ps.setString(1, "Translation")
      ps.setLong(2, translation.id)
      ps.setObject(3, jsonb(emptyMap<String, Any>()))
      ps.setObject(
        4,
        jsonb(
          mapOf(
            "key" to mapOf("entityClass" to "Key", "entityId" to translation.key.id),
            "language" to mapOf("entityClass" to "Language", "entityId" to translation.language.id),
          ),
        ),
      )
      ps.setObject(
        5,
        jsonb(
          mapOf(
            "text" to mapOf("old" to null, "new" to translation.text),
            "state" to mapOf("old" to null, "new" to translation.state.name),
            "outdated" to mapOf("old" to null, "new" to translation.outdated),
            "auto" to mapOf("old" to null, "new" to translation.auto),
          ),
        ),
      )
      ps.setInt(6, RevisionType.ADD.ordinal)
      ps.setLong(7, activityRevisionId)
      ps.setObject(8, branchId)
    }
  }

  fun recordDescribingEntities(
    keys: Collection<Key>,
    translations: Collection<Translation>,
  ) {
    val languages = translations.map { it.language }.distinctBy { it.id }

    val entities = mutableListOf<Triple<String, Long, Map<String, Any?>>>()

    keys.forEach { key ->
      entities.add(Triple("Key", key.id, mapOf("name" to key.name)))
    }
    languages.forEach { lang ->
      // Mirror the @ActivityDescribingProp fields on Language: tag, name, flagEmoji.
      // Order matters for byte-for-byte parity with the standard interceptor output.
      entities.add(
        Triple(
          "Language",
          lang.id,
          linkedMapOf<String, Any?>(
            "tag" to lang.tag,
            "name" to lang.name,
            "flagEmoji" to lang.flagEmoji,
          ),
        ),
      )
    }

    if (entities.isEmpty()) return
    jdbcTemplate.batchUpdate(
      """INSERT INTO activity_describing_entity
         (entity_class, entity_id, data, describing_relations, activity_revision_id)
         VALUES (?, ?, ?, ?, ?)
         ON CONFLICT (activity_revision_id, entity_class, entity_id) DO NOTHING""",
      entities,
      1000,
    ) { ps, (entityClass, entityId, data) ->
      ps.setString(1, entityClass)
      ps.setLong(2, entityId)
      ps.setObject(3, jsonb(data))
      ps.setObject(4, jsonb(emptyMap<String, Any>()))
      ps.setLong(5, activityRevisionId)
    }
  }

  private fun jsonb(data: Any?): PGobject {
    return PGobject().apply {
      type = "jsonb"
      value = objectMapper.writeValueAsString(data)
    }
  }
}
