package io.tolgee.repository

import io.tolgee.model.LLMProvider
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface LLMProviderRepository : JpaRepository<LLMProvider, Long> {
  @Query(
    """
    from LLMProvider p
    where
      p.organization.id = :organizationId
    """,
  )
  fun getAll(organizationId: Long): List<LLMProvider>
}
