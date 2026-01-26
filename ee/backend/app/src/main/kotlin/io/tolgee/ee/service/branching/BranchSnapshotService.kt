package io.tolgee.ee.service.branching

import io.tolgee.ee.repository.branching.BranchRepository
import io.tolgee.ee.repository.branching.KeySnapshotRepository
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeyScreenshotReferenceView
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.LinkedHashMap

@Service
class BranchSnapshotService(
  private val keyRepository: KeyRepository,
  private val keySnapshotRepository: KeySnapshotRepository,
  private val branchRepository: BranchRepository,
  private val entityManager: EntityManager,
) {
  /**
   * Creates initial snapshots for all keys in the target branch using raw SQL for performance.
   * Creates its own temp_key_mapping table if not already present (e.g., when called from tests).
   */
  @Transactional
  fun createInitialSnapshot(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    deleteSnapshots(targetBranch.id)

    // Ensure temp_key_mapping exists (may already exist if called after BranchCopyServiceSql)
    ensureKeyMappingTableExists(projectId, sourceBranch, targetBranch)

    // Insert key snapshots using batch sequence allocation
    insertKeySnapshots(projectId, targetBranch)

    // Insert key meta snapshots
    insertKeyMetaSnapshots(targetBranch)

    // Insert translation snapshots
    insertTranslationSnapshots(targetBranch)

    targetBranch.pending = false
    branchRepository.save(targetBranch)
  }

  /**
   * Creates temp_key_mapping table if it doesn't already exist.
   * This allows the service to work both standalone (tests) and after BranchCopyServiceSql.
   */
  private fun ensureKeyMappingTableExists(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    // Check if table already exists
    val checkSql = """
      SELECT EXISTS (
        SELECT 1 FROM pg_tables WHERE tablename = 'temp_key_mapping'
      )
    """
    val exists = entityManager.createNativeQuery(checkSql).singleResult as Boolean
    if (exists) return

    // Create the table
    val sourceBranchFilter =
      if (sourceBranch.isDefault) {
        "(sk.branch_id = :sourceBranchId OR sk.branch_id IS NULL)"
      } else {
        "sk.branch_id = :sourceBranchId"
      }

    val createTableSql = """
      CREATE TEMPORARY TABLE temp_key_mapping
      ON COMMIT DROP
      AS
      SELECT sk.id AS source_key_id, tk.id AS target_key_id
      FROM key sk
      JOIN key tk ON tk.project_id = sk.project_id
                 AND tk.branch_id = :targetBranchId
                 AND tk.name = sk.name
                 AND coalesce(tk.namespace_id,-1) = coalesce(sk.namespace_id,-1)
      WHERE sk.project_id = :projectId
        AND $sourceBranchFilter
    """
    entityManager
      .createNativeQuery(createTableSql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()

    // Add primary key
    entityManager
      .createNativeQuery("ALTER TABLE temp_key_mapping ADD PRIMARY KEY (source_key_id)")
      .executeUpdate()

    // Add index on target_key_id
    entityManager
      .createNativeQuery("CREATE INDEX idx_temp_key_mapping_target ON temp_key_mapping (target_key_id)")
      .executeUpdate()
  }

  private fun insertKeySnapshots(
    projectId: Long,
    targetBranch: Branch,
  ) {
    val sql = """
      WITH to_insert AS (
        SELECT sk.id AS source_key_id, tk.id AS target_key_id,
               sk.name, ns.name AS namespace, sk.is_plural, sk.plural_arg_name,
               COALESCE(
                 (SELECT jsonb_agg(jsonb_build_object(
                   'screenshotId', ksr.screenshot_id,
                   'positions', ksr.positions,
                   'originalText', ksr.original_text
                 ))
                 FROM key_screenshot_reference ksr
                 WHERE ksr.key_id = sk.id),
                 '[]'::jsonb
               ) AS screenshot_refs,
               row_number() OVER (ORDER BY sk.id) AS rn,
               count(*) OVER () AS total_count
        FROM temp_key_mapping m
        JOIN key sk ON sk.id = m.source_key_id
        JOIN key tk ON tk.id = m.target_key_id
        LEFT JOIN namespace ns ON ns.id = sk.namespace_id
      ), id_base AS (
        SELECT CASE WHEN max(total_count) > 0
               THEN setval('hibernate_sequence', nextval('hibernate_sequence') + max(total_count) - 1) - max(total_count) + 1
               ELSE 0 END AS start_id
        FROM to_insert
      )
      INSERT INTO branch_key_snapshot (
        id, name, namespace, is_plural, plural_arg_name, original_key_id, branch_key_id,
        project_id, branch_id, screenshot_references, created_at, updated_at
      )
      SELECT (SELECT start_id FROM id_base) + ti.rn - 1,
             ti.name, ti.namespace, ti.is_plural, ti.plural_arg_name,
             ti.source_key_id, ti.target_key_id,
             :projectId, :branchId, ti.screenshot_refs,
             :createdAt, :createdAt
      FROM to_insert ti
      WHERE ti.total_count > 0
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("branchId", targetBranch.id)
      .setParameter("createdAt", targetBranch.createdAt)
      .executeUpdate()
  }

  private fun insertKeyMetaSnapshots(targetBranch: Branch) {
    val sql = """
      WITH to_insert AS (
        SELECT bks.id AS key_snapshot_id,
               km.description,
               km.custom,
               COALESCE(
                 (SELECT jsonb_agg(t.name)
                  FROM key_meta_tags kmt
                  JOIN tag t ON t.id = kmt.tags_id
                  WHERE kmt.key_metas_id = km.id),
                 '[]'::jsonb
               ) AS tags,
               row_number() OVER (ORDER BY bks.id) AS rn,
               count(*) OVER () AS total_count
        FROM branch_key_snapshot bks
        JOIN temp_key_mapping m ON m.target_key_id = bks.branch_key_id
        JOIN key_meta km ON km.key_id = m.source_key_id
        WHERE bks.branch_id = :branchId
      ), id_base AS (
        SELECT CASE WHEN max(total_count) > 0
               THEN setval('hibernate_sequence', nextval('hibernate_sequence') + max(total_count) - 1) - max(total_count) + 1
               ELSE 0 END AS start_id
        FROM to_insert
      )
      INSERT INTO branch_key_meta_snapshot (
        id, key_snapshot_id, description, custom, tags, created_at, updated_at
      )
      SELECT (SELECT start_id FROM id_base) + ti.rn - 1,
             ti.key_snapshot_id, ti.description, ti.custom, ti.tags,
             :createdAt, :createdAt
      FROM to_insert ti
      WHERE ti.total_count > 0
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("branchId", targetBranch.id)
      .setParameter("createdAt", targetBranch.createdAt)
      .executeUpdate()
  }

  private fun insertTranslationSnapshots(targetBranch: Branch) {
    val sql = """
      WITH to_insert AS (
        SELECT bks.id AS key_snapshot_id,
               lang.tag AS language,
               COALESCE(t.text, '') AS value,
               COALESCE(t.state, 1) AS state,
               COALESCE(
                 (SELECT jsonb_agg(l.name)
                  FROM translation_label tl
                  JOIN label l ON l.id = tl.label_id
                  WHERE tl.translation_id = t.id),
                 '[]'::jsonb
               ) AS labels,
               row_number() OVER (ORDER BY t.id) AS rn,
               count(*) OVER () AS total_count
        FROM branch_key_snapshot bks
        JOIN temp_key_mapping m ON m.target_key_id = bks.branch_key_id
        JOIN translation t ON t.key_id = m.source_key_id
        JOIN language lang ON lang.id = t.language_id
        WHERE bks.branch_id = :branchId
      ), id_base AS (
        SELECT CASE WHEN max(total_count) > 0
               THEN setval('hibernate_sequence', nextval('hibernate_sequence') + max(total_count) - 1) - max(total_count) + 1
               ELSE 0 END AS start_id
        FROM to_insert
      )
      INSERT INTO branch_translation_snapshot (
        id, key_snapshot_id, language, value, state, labels, created_at, updated_at
      )
      SELECT (SELECT start_id FROM id_base) + ti.rn - 1,
             ti.key_snapshot_id, ti.language, ti.value, ti.state, ti.labels,
             :createdAt, :createdAt
      FROM to_insert ti
      WHERE ti.total_count > 0
    """
    entityManager
      .createNativeQuery(sql)
      .setParameter("branchId", targetBranch.id)
      .setParameter("createdAt", targetBranch.createdAt)
      .executeUpdate()
  }

  @Transactional
  fun rebuildSnapshotFromSource(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    deleteSnapshots(sourceBranch.id)

    val sourceKeys =
      keyRepository.findAllDetailedByBranch(
        projectId = projectId,
        branchId = sourceBranch.id,
        includeOrphanDefault = sourceBranch.isDefault,
      )
    val targetKeys =
      keyRepository.findAllFetchBranchAndNamespace(
        projectId = projectId,
        branchId = targetBranch.id,
        includeOrphanDefault = targetBranch.isDefault,
      )

    val targetBySignature = targetKeys.associateBy { it.snapshotSignature() }

    val snapshots =
      sourceKeys.mapNotNull { sourceKey ->
        val targetKey = targetBySignature[sourceKey.snapshotSignature()] ?: return@mapNotNull null
        buildSnapshot(
          snapshotKey = sourceKey,
          originalKeyId = targetKey.id,
          branchKeyId = sourceKey.id,
          branch = sourceBranch,
        )
      }

    keySnapshotRepository.saveAll(snapshots)
  }

  private fun buildSnapshot(
    snapshotKey: Key,
    originalKeyId: Long,
    branchKeyId: Long,
    branch: Branch,
  ): KeySnapshot {
    val snapshot =
      KeySnapshot(
        name = snapshotKey.name,
        namespace = snapshotKey.namespace?.name,
        isPlural = snapshotKey.isPlural,
        pluralArgName = snapshotKey.pluralArgName,
        originalKeyId = originalKeyId,
        branchKeyId = branchKeyId,
      ).apply {
        this.project = branch.project
        this.branch = branch
        this.disableActivityLogging = true
      }

    snapshotKey.keyMeta?.let { meta ->
      val snapshotMeta =
        KeyMetaSnapshot(
          description = meta.description,
          custom = meta.custom?.let { LinkedHashMap(it) },
          tags = meta.tags.mapTo(mutableSetOf()) { it.name },
        ).apply { disableActivityLogging = true }
      snapshotMeta.keySnapshot = snapshot
      snapshot.keyMetaSnapshot = snapshotMeta
    }

    snapshotKey.translations.forEach { translation ->
      val translationSnapshot =
        TranslationSnapshot(
          language = translation.language.tag,
          value = translation.text.orEmpty(),
          state = translation.state,
        ).apply { disableActivityLogging = true }
      translationSnapshot.labels = translation.labels.mapTo(mutableSetOf()) { it.name }
      translationSnapshot.keySnapshot = snapshot
      snapshot.translations.add(translationSnapshot)
    }

    snapshot.screenshotReferences.addAll(
      snapshotKey.keyScreenshotReferences.map {
        KeyScreenshotReferenceView(
          screenshotId = it.screenshot.id,
          positions = it.positions?.toList(),
          originalText = it.originalText,
        )
      },
    )

    return snapshot
  }

  fun getSnapshotKeys(branchId: Long): List<KeySnapshot> = keySnapshotRepository.findAllByBranchId(branchId)

  fun getSnapshotKeysByOriginalKeyIdIn(
    branchId: Long,
    originalKeyIds: Collection<Long>,
  ): List<KeySnapshot> {
    if (originalKeyIds.isEmpty()) {
      return emptyList()
    }
    return keySnapshotRepository.findAllByBranchIdAndOriginalKeyIdIn(branchId, originalKeyIds)
  }

  private fun deleteSnapshots(branchId: Long) {
    entityManager
      .createNativeQuery(
        """
        delete from branch_key_meta_snapshot
        where key_snapshot_id in (
          select id from branch_key_snapshot where branch_id = :branchId
        )
        """.trimIndent(),
      ).setParameter("branchId", branchId)
      .executeUpdate()
    entityManager
      .createNativeQuery(
        """
        delete from branch_translation_snapshot
        where key_snapshot_id in (
          select id from branch_key_snapshot where branch_id = :branchId
        )
        """.trimIndent(),
      ).setParameter("branchId", branchId)
      .executeUpdate()
    entityManager
      .createNativeQuery("delete from branch_key_snapshot where branch_id = :branchId")
      .setParameter("branchId", branchId)
      .executeUpdate()
  }

  private fun Key.snapshotSignature(): Pair<Long?, String> {
    return this.namespace?.id to this.name
  }
}
