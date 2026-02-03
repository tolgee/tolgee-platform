package io.tolgee.core.domain.translation.service

import io.tolgee.model.translation.Translation
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface INameTranslationQueries : JpaRepository<Translation, Long> {
  @Query(
    """
    from Translation t
    where t.id = :translationId
      and t.key.project.id = :projectId
    """,
  )
  fun find(
    projectId: Long,
    translationId: Long,
  ): Translation?
}
