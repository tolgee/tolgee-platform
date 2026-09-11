package io.tolgee.ee.service.accountSecurity

import io.tolgee.ee.service.connectedApps.ConnectedAppService
import io.tolgee.service.security.UserSessionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * The one place that knows a sweep was a person pressing "revoke all other sessions" rather than a
 * password changing - which is what decides that it should produce a revocation event per grant.
 */
@Service
class AccountRevocationService(
  private val userSessionService: UserSessionService,
  private val connectedAppService: ConnectedAppService,
) {
  @Transactional
  fun revokeAllOthers(
    userAccountId: Long,
    currentDeviceId: String?,
    revokedById: Long,
  ) {
    userSessionService.revokeAllOthers(
      userAccountId = userAccountId,
      currentDeviceId = currentDeviceId,
      revokedById = revokedById,
    )
    connectedAppService.revokeAllForUser(userAccountId)
  }
}
