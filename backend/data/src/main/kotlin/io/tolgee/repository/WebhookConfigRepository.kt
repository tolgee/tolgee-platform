package io.tolgee.repository

import io.tolgee.model.Project
import io.tolgee.model.webhook.WebhookConfig
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface WebhookConfigRepository : JpaRepository<WebhookConfig, Long> {
  @Query(
    """
    from WebhookConfig wc
        left join fetch wc.automationActions aa
      where wc.id = :id and wc.project.id = :projectId
  """,
  )
  fun findByIdAndProjectId(
    id: Long,
    projectId: Long,
  ): WebhookConfig?

  @Query(
    """
    from WebhookConfig wc
    where wc.project.id = :projectId
  """,
  )
  fun findByProjectId(
    projectId: Long,
    pageable: Pageable,
  ): Page<WebhookConfig>

  fun countByProject(project: Project): Int
}
