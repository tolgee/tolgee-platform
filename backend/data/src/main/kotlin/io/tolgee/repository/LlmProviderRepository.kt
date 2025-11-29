package io.tolgee.repository

import io.tolgee.model.LlmProvider
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface LlmProviderRepository : JpaRepository<LlmProvider, Long> {
  @Query(
    """
    from LlmProvider p
    where
      p.organization.id = :organizationId
    """,
  )
  fun getAll(organizationId: Long): List<LlmProvider>

  fun deleteByIdAndOrganizationId(
    id: Long,
    organizationId: Long,
  )
}
