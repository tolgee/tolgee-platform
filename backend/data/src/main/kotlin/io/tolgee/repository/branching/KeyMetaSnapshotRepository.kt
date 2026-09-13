package io.tolgee.repository.branching

import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface KeyMetaSnapshotRepository : JpaRepository<KeyMetaSnapshot, Long> {
  @Modifying
  @Query(
    """
    delete from KeyMetaSnapshot kms
    where kms.keySnapshot.id in (select ks.id from KeySnapshot ks where ks.project.id = :projectId)
    """,
  )
  fun deleteAllByProjectId(projectId: Long)
}
