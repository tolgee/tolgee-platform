package io.tolgee.service.apps.lifecycle

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleDeliveryOutcome
import io.tolgee.dtos.apps.AppLifecycleOrganization
import io.tolgee.dtos.apps.AppLifecyclePayload
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import tools.jackson.databind.ObjectMapper

/**
 * Hands an app a secret-carrying event over a signed POST to the `baseUrl` in its manifest, and
 * tells the caller whether it landed. Only two events travel this way now — an app being registered
 * and an operator rotating its secret — and both happen with a human at a dialog, so the delivery
 * is **synchronous** and its outcome is shown there. An app that discovers the rest (its installs)
 * asks Tolgee itself.
 *
 * A failure never propagates: the credentials were already returned in the response, so a dead app
 * host must not roll back the registration or rotation. The blast radius of the synchronous call is
 * bounded by the shared apps HTTP timeout, and Tolgee just fetched the manifest from this same host
 * seconds earlier, so the host is already known reachable.
 */
@Service
class AppLifecycleDeliveryService(
  private val appRepository: AppRepository,
  private val organizationRepository: OrganizationRepository,
  private val appService: AppService,
  private val httpClient: AppLifecycleHttpClient,
  private val tolgeeProperties: TolgeeProperties,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  /**
   * Everything a delivery needs about the app, taken while the app still exists. Read before a
   * removal that will delete the app so the app can still be told about it.
   */
  data class AppTarget(
    val appEntityId: Long,
    val appIdentifier: String,
    val baseUrl: String,
    val signingSecret: String,
  )

  /** Reads the app's delivery target, minting its signing secret if it was backfilled without one. */
  fun resolveTarget(appEntityId: Long): AppTarget? {
    return executeInNewTransaction(transactionManager) {
      val app = appRepository.findById(appEntityId).orElse(null) ?: return@executeInNewTransaction null
      AppTarget(
        appEntityId = app.id,
        appIdentifier = app.appId,
        baseUrl = app.baseUrl,
        signingSecret = appService.resolveWebhookSecret(app.id),
      )
    }
  }

  /**
   * Delivers [eventType] now and reports whether the app took it. Returns
   * [AppLifecycleDeliveryOutcome.NOT_ATTEMPTED] when the app is already gone (nothing to deliver to);
   * a failure is a value, never thrown.
   */
  fun deliverNow(
    appEntityId: Long,
    eventType: AppLifecycleEventType,
    appCredentials: AppLifecycleAppCredentials? = null,
    organizationId: Long? = null,
  ): AppLifecycleDeliveryOutcome {
    val target = resolveTarget(appEntityId) ?: return AppLifecycleDeliveryOutcome.NOT_ATTEMPTED
    return deliverNow(target, eventType, appCredentials, organizationId)
  }

  fun deliverNow(
    target: AppTarget,
    eventType: AppLifecycleEventType,
    appCredentials: AppLifecycleAppCredentials? = null,
    organizationId: Long? = null,
  ): AppLifecycleDeliveryOutcome {
    val organization = organizationId?.let { organizationRepository.findById(it).orElse(null) }
    val payload =
      AppLifecyclePayload(
        eventType = eventType.wireName,
        appId = target.appIdentifier,
        tolgeeInstanceUrl = tolgeeProperties.backEndUrl,
        app = appCredentials,
        organization = organization?.let { AppLifecycleOrganization(id = it.id, name = it.name, slug = it.slug) },
      )

    return try {
      httpClient.post(target.baseUrl, objectMapper.writeValueAsString(payload), target.signingSecret)
      AppLifecycleDeliveryOutcome.DELIVERED
    } catch (e: AppLifecycleHttpClient.DeliveryFailedException) {
      logger.info("App lifecycle {} delivery to {} failed: {}", eventType.wireName, target.baseUrl, e.message)
      AppLifecycleDeliveryOutcome.failed(e.message ?: "delivery failed")
    }
  }
}
