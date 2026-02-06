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
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.LinkedHashMap

@Service
class BranchSnapshotService(
  private val keyRepository: KeyRepository,
  private val keySnapshotRepository: KeySnapshotRepository,
  private val branchRepository: BranchRepository,
  private val entityManager: EntityManager,
) {
  @Transactional
  fun createInitialSnapshot(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    deleteSnapshots(targetBranch.id)
    createSnapshotMappingTable()
    insertKeySnapshots(projectId, sourceBranch, targetBranch)
    insertTranslationSnapshots()
    insertKeyMetaSnapshots()
    targetBranch.pending = false
    branchRepository.save(targetBranch)
  }

  private fun getSourceBranchFilter(
    keyAlias: String,
    sourceIsDefault: Boolean,
  ): String {
    return if (sourceIsDefault) {
      "($keyAlias.branch_id = :sourceBranchId OR $keyAlias.branch_id IS NULL)"
    } else {
      "$keyAlias.branch_id = :sourceBranchId"
    }
  }

  private fun createSnapshotMappingTable() {
    entityManager
      .createNativeQuery(
        """
        CREATE TEMPORARY TABLE temp_snapshot_mapping (
          source_key_id BIGINT NOT NULL PRIMARY KEY,
          snapshot_id BIGINT NOT NULL
        ) ON COMMIT DROP
        """.trimIndent(),
      ).executeUpdate()
  }

  private fun insertKeySnapshots(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    val sql =
      """
      WITH inserted AS (
        INSERT INTO branch_key_snapshot (
          id, name, namespace, is_plural, plural_arg_name,
          original_key_id, branch_key_id, branch_id, project_id,
          screenshot_references, created_at, updated_at
        )
        SELECT
          nextval('hibernate_sequence'),
          sk.name,
          ns.name,
          sk.is_plural,
          sk.plural_arg_name,
          sk.id,
          tk.id,
          :targetBranchId,
          :projectId,
          (SELECT coalesce(jsonb_agg(jsonb_build_object(
              'screenshotId', ksr.screenshot_id,
              'positions', ksr.positions,
              'originalText', ksr.original_text
          )), '[]'::jsonb)
          FROM key_screenshot_reference ksr WHERE ksr.key_id = sk.id),
          now(), now()
        FROM key sk
        LEFT JOIN namespace ns ON ns.id = sk.namespace_id
        JOIN key tk ON tk.project_id = :projectId
                   AND tk.branch_id = :targetBranchId
                   AND tk.name = sk.name
                   AND coalesce(tk.namespace_id, -1) = coalesce(sk.namespace_id, -1)
        WHERE sk.project_id = :projectId
          AND ${getSourceBranchFilter("sk", sourceBranch.isDefault)}
        RETURNING id, original_key_id
      )
      INSERT INTO temp_snapshot_mapping (source_key_id, snapshot_id)
      SELECT original_key_id, id FROM inserted
      """.trimIndent()

    entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  private fun insertTranslationSnapshots() {
    val sql =
      """
      INSERT INTO branch_translation_snapshot (
        id, language, value, state, labels, key_snapshot_id, created_at, updated_at
      )
      SELECT
        nextval('hibernate_sequence'),
        lang.tag,
        coalesce(t.text, ''),
        t.state,
        (SELECT coalesce(jsonb_agg(l.name), '[]'::jsonb)
         FROM translation_label tl
         JOIN label l ON l.id = tl.label_id
         WHERE tl.translation_id = t.id),
        m.snapshot_id,
        now(), now()
      FROM translation t
      JOIN language lang ON lang.id = t.language_id
      JOIN temp_snapshot_mapping m ON m.source_key_id = t.key_id
      """.trimIndent()

    entityManager.createNativeQuery(sql).executeUpdate()
  }

  private fun insertKeyMetaSnapshots() {
    val sql =
      """
      INSERT INTO branch_key_meta_snapshot (
        id, description, custom, tags, key_snapshot_id, created_at, updated_at
      )
      SELECT
        nextval('hibernate_sequence'),
        km.description,
        km.custom,
        (SELECT coalesce(jsonb_agg(t.name), '[]'::jsonb)
         FROM key_meta_tags kmt
         JOIN tag t ON t.id = kmt.tags_id
         WHERE kmt.key_metas_id = km.id),
        m.snapshot_id,
        now(), now()
      FROM key_meta km
      JOIN temp_snapshot_mapping m ON m.source_key_id = km.key_id
      """.trimIndent()

    entityManager.createNativeQuery(sql).executeUpdate()
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

  fun deleteSnapshots(branchId: Long) {
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
