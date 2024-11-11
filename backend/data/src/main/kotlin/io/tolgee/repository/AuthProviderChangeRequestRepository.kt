package io.tolgee.repository

import io.tolgee.model.AuthProviderChangeRequest
import io.tolgee.model.UserAccount
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AuthProviderChangeRequestRepository : JpaRepository<AuthProviderChangeRequest, Long> {
  fun findByUserAccountId(id: Long): Optional<AuthProviderChangeRequest>

  @Query(
    "SELECT a FROM AuthProviderChangeRequest a WHERE a.userAccount = :userAccount AND a.isConfirmed = :isConfirmed",
  )
  fun findByUserAccountAndIsConfirmed(
    userAccount: UserAccount,
    isConfirmed: Boolean,
  ): Optional<AuthProviderChangeRequest>
}
