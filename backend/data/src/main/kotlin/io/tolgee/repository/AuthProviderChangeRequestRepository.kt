package io.tolgee.repository

import io.tolgee.model.AuthProviderChangeRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AuthProviderChangeRequestRepository : JpaRepository<AuthProviderChangeRequest, Long> {
  fun deleteByUserAccountId(id: Long): Int

  fun findByUserAccountId(id: Long): AuthProviderChangeRequest?
}
