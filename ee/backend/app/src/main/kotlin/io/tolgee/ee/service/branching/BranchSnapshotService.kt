package io.tolgee.ee.service.branching

import io.opentelemetry.instrumentation.annotations.WithSpan
import io.tolgee.ee.repository.branching.KeySnapshotRepository
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.snapshot.KeySnapshot
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
class BranchSnapshotService(
  private val keySnapshotRepository: KeySnapshotRepository,
  private val entityManager: EntityManager,
) {
  @WithSpan
  @Transactional
  fun createInitialSnapshot(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    deleteSnapshots(targetBranch.id)
    insertKeySnapshots(projectId, sourceBranch, targetBranch)
    insertTranslationSnapshots(targetBranch.id)
    insertKeyMetaSnapshots(targetBranch.id)
  }

  private fun getBranchFilter(
    keyAlias: String,
    isDefault: Boolean,
    paramName: String,
  ): String {
    return if (isDefault) {
      "($keyAlias.branch_id = :$paramName OR $keyAlias.branch_id IS NULL)"
    } else {
      "$keyAlias.branch_id = :$paramName"
    }
  }

  private fun insertKeySnapshots(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    // Read data from targetBranch (just copied from sourceBranch within the same transaction).
    // Read original_key_id from sourceBranch for merge-analyzer correlation.
    val sql =
      """
      INSERT INTO branch_key_snapshot (
        id, name, namespace, is_plural, plural_arg_name, max_char_limit,
        original_key_id, branch_key_id, branch_id, project_id,
        screenshot_references, created_at, updated_at
      )
      SELECT
        nextval('hibernate_sequence'),
        tk.name,
        ns.name,
        tk.is_plural,
        tk.plural_arg_name,
        tk.max_char_limit,
        sk.id,
        tk.id,
        :targetBranchId,
        :projectId,
        (SELECT coalesce(jsonb_agg(jsonb_build_object(
            'screenshotId', ksr.screenshot_id,
            'positions', ksr.positions,
            'originalText', ksr.original_text
        )), '[]'::jsonb)
        FROM key_screenshot_reference ksr WHERE ksr.key_id = tk.id),
        now(), now()
      FROM key tk
      LEFT JOIN namespace ns ON ns.id = tk.namespace_id
      JOIN key sk ON sk.project_id = :projectId
                 AND ${getBranchFilter("sk", sourceBranch.isDefault, "sourceBranchId")}
                 AND sk.name = tk.name
                 AND coalesce(sk.namespace_id, -1) = coalesce(tk.namespace_id, -1)
                 AND tk.deleted_at IS NULL
      WHERE tk.project_id = :projectId
        AND tk.branch_id = :targetBranchId
        AND tk.deleted_at IS NULL
      """.trimIndent()

    entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  /**
   * Inserts translation snapshots by joining directly to the already-committed
   * branch_key_snapshot rows for the given branch, avoiding a temporary table.
   */
  private fun insertTranslationSnapshots(branchId: Long) {
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
        ks.id,
        now(), now()
      FROM translation t
      JOIN language lang ON lang.id = t.language_id
      JOIN branch_key_snapshot ks ON ks.branch_key_id = t.key_id AND ks.branch_id = :branchId
      """.trimIndent()

    entityManager.createNativeQuery(sql).setParameter("branchId", branchId).executeUpdate()
  }

  /**
   * Inserts key meta snapshots by joining directly to the already-committed
   * branch_key_snapshot rows for the given branch, avoiding a temporary table.
   */
  private fun insertKeyMetaSnapshots(branchId: Long) {
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
        ks.id,
        now(), now()
      FROM key_meta km
      JOIN branch_key_snapshot ks ON ks.branch_key_id = km.key_id AND ks.branch_id = :branchId
      """.trimIndent()

    entityManager.createNativeQuery(sql).setParameter("branchId", branchId).executeUpdate()
  }

  @WithSpan
  @Transactional
  fun rebuildSnapshotFromSource(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    deleteSnapshots(sourceBranch.id)
    insertKeySnapshotsForRebuild(projectId, sourceBranch, targetBranch)
    insertTranslationSnapshots(sourceBranch.id)
    insertKeyMetaSnapshots(sourceBranch.id)
  }

  private fun insertKeySnapshotsForRebuild(
    projectId: Long,
    sourceBranch: Branch,
    targetBranch: Branch,
  ) {
    val sql =
      """
      INSERT INTO branch_key_snapshot (
        id, name, namespace, is_plural, plural_arg_name, max_char_limit,
        original_key_id, branch_key_id, branch_id, project_id,
        screenshot_references, created_at, updated_at
      )
      SELECT
        nextval('hibernate_sequence'),
        sk.name,
        ns.name,
        sk.is_plural,
        sk.plural_arg_name,
        sk.max_char_limit,
        tk.id,
        sk.id,
        :sourceBranchId,
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
                 AND ${getBranchFilter("tk", targetBranch.isDefault, "targetBranchId")}
                 AND tk.name = sk.name
                 AND coalesce(tk.namespace_id, -1) = coalesce(sk.namespace_id, -1)
                 AND tk.deleted_at IS NULL
      WHERE sk.project_id = :projectId
        AND ${getBranchFilter("sk", sourceBranch.isDefault, "sourceBranchId")}
        AND sk.deleted_at IS NULL
      """.trimIndent()

    entityManager
      .createNativeQuery(sql)
      .setParameter("projectId", projectId)
      .setParameter("sourceBranchId", sourceBranch.id)
      .setParameter("targetBranchId", targetBranch.id)
      .executeUpdate()
  }

  @WithSpan
  fun getSnapshotKeys(branchId: Long): List<KeySnapshot> = keySnapshotRepository.findAllByBranchId(branchId)

  @WithSpan
  fun getSnapshotKeysByOriginalKeyIdIn(
    branchId: Long,
    originalKeyIds: Collection<Long>,
  ): List<KeySnapshot> {
    if (originalKeyIds.isEmpty()) {
      return emptyList()
    }
    return keySnapshotRepository.findAllByBranchIdAndOriginalKeyIdIn(branchId, originalKeyIds)
  }

  @WithSpan
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
}
