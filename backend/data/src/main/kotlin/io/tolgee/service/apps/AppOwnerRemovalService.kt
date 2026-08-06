package io.tolgee.service.apps

import io.tolgee.dtos.apps.AppLifecycleInstall
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.apps.AppInstallRepository
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.lifecycle.AppLifecycleDeliveryService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

/**
 * Withdraws an app from the whole server on its owner's behalf: every organization's install goes,
 * both layers of credentials go with it, and each of those organizations is announced to the app as
 * an uninstall.
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
  private val appLifecycleDeliveryService: AppLifecycleDeliveryService,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  private data class RemovedInstall(
    val installId: Long,
    val organizationId: Long?,
  )

  fun removeEverywhere(appEntityId: Long) {
    // Resolved first: the app row, and with it the signing secret and the base URL, is about to go.
    val target = appLifecycleDeliveryService.resolveTarget(appEntityId)

    val removed = executeInNewTransaction(transactionManager) { purge(appEntityId) }
    logger.info("Removed app {} from {} organizations", appEntityId, removed.size)

    target ?: return
    removed.forEach {
      appLifecycleDeliveryService.deliver(
        target = target,
        eventType = AppLifecycleEventType.APP_UNINSTALLED,
        organizationId = it.organizationId,
        install = AppLifecycleInstall(id = it.installId, clientId = null),
      )
    }
  }

  /**
   * Deleting the app takes `app_secret` and every install's `app_install_secret` with it through the
   * database's own cascades — which is what revoking both layers of credentials amounts to here, and
   * is stronger than marking them revoked.
   */
  private fun purge(appEntityId: Long): List<RemovedInstall> {
    val installs = appInstallRepository.findAllByRegisteredAppId(appEntityId)
    val removed = installs.map { RemovedInstall(installId = it.id, organizationId = it.organization?.id) }

    installs.forEach {
      appEnablementService.removeAllForAppInstall(it.id)
      appAvailabilityService.removeAllForAppInstall(it.id)
    }
    appInstallRepository.deleteAll(installs)
    appInstallRepository.flush()
    appRepository.deleteById(appEntityId)
    appRepository.flush()

    return removed
  }
}
