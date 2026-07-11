package io.tolgee.repository.branching

import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.BranchMergeChange
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface BranchMergeChangeRepositoryOss : JpaRepository<BranchMergeChange, Long> {
  @Modifying
  @Query("delete from BranchMergeChange bmc where bmc.sourceKey.id in :ids or bmc.targetKey.id in :ids")
  fun deleteBySourceOrTargetIds(ids: Collection<Long>)
}
