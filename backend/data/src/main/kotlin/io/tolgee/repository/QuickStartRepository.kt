package io.tolgee.repository

import io.tolgee.model.QuickStart
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface QuickStartRepository : JpaRepository<QuickStart, Long> {
  fun findByUserAccountId(userAccountId: Long): QuickStart?
  fun findByUserAccountIdAndOrganizationId(userAccountId: Long, organizationId: Long): QuickStart?
}
