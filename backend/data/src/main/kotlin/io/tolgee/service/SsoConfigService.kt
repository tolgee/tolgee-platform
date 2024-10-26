package io.tolgee.service

import io.tolgee.model.SsoConfig
import io.tolgee.model.UserAccount
import io.tolgee.repository.SsoConfigRepository
import org.springframework.stereotype.Service

@Service
class SsoConfigService(
  private val ssoConfigRepository: SsoConfigRepository,
) {
  fun findByDomainName(domain: String) = ssoConfigRepository.findByDomainName(domain)

  fun save(
    userAccount: UserAccount,
    domain: String,
  ): SsoConfig {
    val ssoConfig =
      ssoConfigRepository.findByDomainName(domain)
        ?: SsoConfig().apply { domainName = domain }

    ssoConfig.userAccounts.add(userAccount)
    userAccount.ssoConfig = ssoConfig
    return ssoConfigRepository.save(ssoConfig)
  }
}
