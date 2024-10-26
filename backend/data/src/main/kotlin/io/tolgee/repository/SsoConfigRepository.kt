package io.tolgee.repository

import io.tolgee.model.SsoConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface SsoConfigRepository : JpaRepository<SsoConfig, Long> {
  @Query("SELECT s FROM SsoConfig s JOIN s.userAccounts u WHERE s.domainName = :domain AND u.thirdPartyAuthId = :sub")
  fun findByDomainAndSub(
    domain: String,
    sub: String,
  ): SsoConfig?

  fun findByDomainName(domain: String): SsoConfig?
}
