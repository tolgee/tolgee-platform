package io.tolgee.repository

import io.tolgee.model.Prompt
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface PromptRepository : JpaRepository<Prompt, Long> {
  @Query(
    """
    from Prompt p
    where
      p.project.id = :projectId
      and (
        :search is null or (lower(p.name) like lower(concat('%', cast(:search as string), '%'))
        or lower(p.name) like lower(concat('%', cast(:search as string),'%')))
      )
    """,
  )
  fun getAllPaged(
    projectId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<Prompt>

  @Query(
    """
    from Prompt p
    where
      p.project.id = :projectId
      and p.id = :promptId
    """,
  )
  fun findPrompt(
    projectId: Long,
    promptId: Long,
  ): Prompt?
}
