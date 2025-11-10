package io.tolgee.ee.service.branching

import io.tolgee.ee.repository.branching.KeySnapshotRepository
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot
import io.tolgee.model.key.Key
import io.tolgee.repository.KeyRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.LinkedHashMap

@Service
class BranchSnapshotService(
  private val keyRepository: KeyRepository,
  private val keySnapshotRepository: KeySnapshotRepository,
) {

  @Transactional
  fun createInitialSnapshot(projectId: Long, sourceBranch: Branch, targetBranch: Branch) {
    keySnapshotRepository.deleteAllByBranchId(targetBranch.id)

    val sourceKeys = keyRepository.findAllDetailedByBranch(
      projectId = projectId,
      branchId = sourceBranch.id,
      includeOrphanDefault = sourceBranch.isDefault,
    )
    val targetKeys = keyRepository.findAllDetailedByBranch(
      projectId = projectId,
      branchId = targetBranch.id,
      includeOrphanDefault = false,
    )

    val sourceBySignature = sourceKeys.associateBy { it.snapshotSignature() }

    val snapshots = targetKeys.mapNotNull { branchKey ->
      val sourceKey = sourceBySignature[branchKey.snapshotSignature()] ?: return@mapNotNull null
      buildSnapshot(sourceKey, branchKey, targetBranch)
    }

    keySnapshotRepository.saveAll(snapshots)
  }

  private fun buildSnapshot(
    sourceKey: Key,
    branchKey: Key,
    branch: Branch,
  ): KeySnapshot {
    val snapshot = KeySnapshot(
      name = sourceKey.name,
      namespace = sourceKey.namespace?.name,
      isPlural = sourceKey.isPlural,
      pluralArgName = sourceKey.pluralArgName,
      originalKeyId = sourceKey.id,
      branchKeyId = branchKey.id,
    ).apply {
      this.project = branch.project
      this.branch = branch
      this.disableActivityLogging = true
    }

    sourceKey.keyMeta?.let { meta ->
      val snapshotMeta = KeyMetaSnapshot(
        description = meta.description,
        custom = meta.custom?.let { LinkedHashMap(it) },
      ).apply { disableActivityLogging = true }
      snapshotMeta.keySnapshot = snapshot
      snapshot.keyMetaSnapshot = snapshotMeta
    }

    sourceKey.translations.forEach { translation ->
      val translationSnapshot = TranslationSnapshot(
        language = translation.language.tag,
        value = translation.text.orEmpty(),
        state = translation.state,
      ).apply { disableActivityLogging = true }
      translationSnapshot.keySnapshot = snapshot
      snapshot.translations.add(translationSnapshot)
    }

    return snapshot
  }

  private fun Key.snapshotSignature(): Pair<Long?, String> {
    return this.namespace?.id to this.name
  }
}
