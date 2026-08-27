package io.tolgee.repository.branching

import io.tolgee.model.branching.snapshot.KeySnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface KeySnapshotRepositoryOss : JpaRepository<KeySnapshot, Long> {
  @Modifying
  @Query("delete from KeySnapshot ks where ks.project.id = :projectId")
  fun deleteAllByProjectId(projectId: Long)
}
