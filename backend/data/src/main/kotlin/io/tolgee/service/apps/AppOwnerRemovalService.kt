package io.tolgee.service.apps

import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.repository.apps.AppRepository
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

/**
 * Withdraws an app from the whole server on its owner's behalf: every organization's install goes,
 * and the app's credentials go with it.
 *
 * Distinct from [AppInstallService.remove], which is one organization deciding it no longer wants
 * the app. This is the publisher taking the app off the shelf, and it is what makes a compromised
 * app recoverable in one operation instead of one per tenant.
 */
@Service
class AppOwnerRemovalService(
  private val appRepository: AppRepository,
  private val appInstallRepository: AppInstallRepository,
  private val appEnablementService: AppEnablementService,
  private val appAvailabilityService: AppAvailabilityService,
  private val appInstallPrincipalService: AppInstallPrincipalService,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  fun removeEverywhere(appEntityId: Long) {
    val count = executeInNewTransaction(transactionManager) { purge(appEntityId) }
    logger.info("Removed app {} from {} organizations", appEntityId, count)
  }

  /**
   * Deleting the app takes `app_secret` with it through the database's own cascades — which is what
   * revoking the credentials amounts to here, and is stronger than marking them revoked. No uninstall
   * is delivered: the app is being taken down, so there is nothing to tell it, and every install
   * simply stops appearing in its discovery call.
   */
  private fun purge(appEntityId: Long): Int {
    val installs = appInstallRepository.findAllByRegisteredAppId(appEntityId)
    val principals = installs.map { it.principal }

    installs.forEach {
      appEnablementService.removeAllForAppInstall(it.id)
      appAvailabilityService.removeAllForAppInstall(it.id)
    }
    appInstallRepository.deleteAll(installs)
    appInstallRepository.flush()
    appRepository.deleteById(appEntityId)
    appRepository.flush()
    principals.forEach { appInstallPrincipalService.retire(it) }

    return installs.size
  }
}
