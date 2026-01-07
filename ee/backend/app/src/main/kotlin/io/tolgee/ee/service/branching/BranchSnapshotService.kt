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

    val sourceKeys =
      keyRepository.findAllDetailedByBranch(
        projectId = projectId,
        branchId = sourceBranch.id,
        includeOrphanDefault = sourceBranch.isDefault,
      )
    val targetKeys =
      keyRepository.findAllDetailedByBranch(
        projectId = projectId,
        branchId = targetBranch.id,
        includeOrphanDefault = false,
      )

    val sourceBySignature = sourceKeys.associateBy { it.snapshotSignature() }

    val snapshots =
      targetKeys.mapNotNull { branchKey ->
        val sourceKey = sourceBySignature[branchKey.snapshotSignature()] ?: return@mapNotNull null
        buildSnapshot(
          snapshotKey = sourceKey,
          originalKeyId = sourceKey.id,
          branchKeyId = branchKey.id,
          branch = targetBranch,
        )
      }

    keySnapshotRepository.saveAll(snapshots)
    targetBranch.pending = false
    branchRepository.save(targetBranch)
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
      keyRepository.findAllDetailedByBranch(
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
          tags = meta.tags.mapTo(mutableSetOf()) { it },
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
        delete from branch_key_meta_snapshot_tags
        where key_meta_snapshot_id in (
          select id from branch_key_meta_snapshot
          where key_snapshot_id in (
            select id from branch_key_snapshot where branch_id = :branchId
          )
        )
        """.trimIndent(),
      ).setParameter("branchId", branchId)
      .executeUpdate()
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
