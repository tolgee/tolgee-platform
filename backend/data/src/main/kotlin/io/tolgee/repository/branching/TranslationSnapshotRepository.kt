package io.tolgee.repository.branching

import io.tolgee.model.branching.snapshot.TranslationSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TranslationSnapshotRepository : JpaRepository<TranslationSnapshot, Long> {
  @Modifying
  @Query(
    """
    delete from TranslationSnapshot ts
    where ts.keySnapshot.id in (select ks.id from KeySnapshot ks where ks.project.id = :projectId)
    """,
  )
  fun deleteAllByProjectId(projectId: Long)
}
