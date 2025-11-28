package io.tolgee.ee.repository.branching

import io.tolgee.model.branching.snapshot.KeySnapshot
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface KeySnapshotRepository : JpaRepository<KeySnapshot, Long> {
  @EntityGraph(attributePaths = ["translations", "keyMetaSnapshot"])
  fun findAllByBranchId(branchId: Long): List<KeySnapshot>

  fun deleteAllByBranchId(branchId: Long)
}
