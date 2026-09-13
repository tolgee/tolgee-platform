package io.tolgee.ee.repository.branching

import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.repository.branching.KeySnapshotRepositoryOss
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.stereotype.Repository

@Repository
interface KeySnapshotRepository : KeySnapshotRepositoryOss {
  @EntityGraph(attributePaths = ["translations", "keyMetaSnapshot"])
  fun findAllByBranchId(branchId: Long): List<KeySnapshot>

  @EntityGraph(attributePaths = ["translations", "keyMetaSnapshot"])
  fun findAllByBranchIdAndOriginalKeyIdIn(
    branchId: Long,
    originalKeyIds: Collection<Long>,
  ): List<KeySnapshot>

  fun deleteAllByBranchId(branchId: Long)
}
