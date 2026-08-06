package io.tolgee.service.apps.lifecycle

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.apps.AppLifecycleAppCredentials
import io.tolgee.dtos.apps.AppLifecycleInstall
import io.tolgee.dtos.apps.AppLifecycleOrganization
import io.tolgee.dtos.apps.AppLifecyclePayload
import io.tolgee.model.apps.AppDelivery
import io.tolgee.model.apps.AppLifecycleEventType
import io.tolgee.repository.OrganizationRepository
import io.tolgee.repository.apps.AppDeliveryRepository
import io.tolgee.repository.apps.AppRepository
import io.tolgee.service.apps.AppService
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.runSentryCatching
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

/**
 * Tells an app what happened to it, over a signed POST to the `baseUrl` in its manifest. That
 * delivery is the only channel per-install credentials travel over, and receiving it is what proves
 * the recipient controls the app's domain.
 *
 * Every entry point is fire-and-forget: the record is written in its own transaction and the HTTP
 * happens on another thread, so a dead app host can neither block nor roll back the registration,
 * install or rotation that triggered the delivery.
 */
@Service
class AppLifecycleDeliveryService(
  private val appRepository: AppRepository,
  private val organizationRepository: OrganizationRepository,
  private val appDeliveryRepository: AppDeliveryRepository,
  private val appService: AppService,
  private val dispatcher: AppLifecycleDeliveryDispatcher,
  private val tolgeeProperties: TolgeeProperties,
  private val objectMapper: ObjectMapper,
  private val transactionManager: PlatformTransactionManager,
) : Logging {
  /**
   * Everything a delivery needs about the app, taken while the app still exists. An uninstalled
   * delivery is sent after the install — and sometimes the app — is already gone.
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

  fun deliver(
    appEntityId: Long,
    eventType: AppLifecycleEventType,
    organizationId: Long? = null,
    appCredentials: AppLifecycleAppCredentials? = null,
    install: AppLifecycleInstall? = null,
  ) {
    runSentryCatching {
      val target = resolveTarget(appEntityId) ?: return@runSentryCatching
      deliver(target, eventType, organizationId, appCredentials, install)
    }
  }

  fun deliver(
    target: AppTarget,
    eventType: AppLifecycleEventType,
    organizationId: Long? = null,
    appCredentials: AppLifecycleAppCredentials? = null,
    install: AppLifecycleInstall? = null,
  ) {
    runSentryCatching {
      val prepared =
        executeInNewTransaction(transactionManager) {
          prepare(target, eventType, organizationId, appCredentials, install)
        }
      dispatcher.submit(prepared)
    }
  }

  @Transactional(readOnly = true)
  fun listForApp(appIdentifier: String): List<AppDelivery> {
    return appDeliveryRepository.findAllByAppIdentifierOrderByCreatedAtDesc(appIdentifier)
  }

  private fun prepare(
    target: AppTarget,
    eventType: AppLifecycleEventType,
    organizationId: Long?,
    appCredentials: AppLifecycleAppCredentials?,
    install: AppLifecycleInstall?,
  ): PendingAppDelivery {
    val organization = organizationId?.let { organizationRepository.findById(it).orElse(null) }

    val record =
      appDeliveryRepository.save(
        AppDelivery().apply {
          this.app = appRepository.findById(target.appEntityId).orElse(null)
          this.appIdentifier = target.appIdentifier
          this.organization = organization
          this.eventType = eventType
          this.targetUrl = target.baseUrl
        },
      )

    val payload =
      AppLifecyclePayload(
        eventType = eventType.wireName,
        deliveryId = record.id,
        appId = target.appIdentifier,
        tolgeeInstanceUrl = tolgeeProperties.backEndUrl,
        app = appCredentials,
        install = install,
        organization =
          organization?.let {
            AppLifecycleOrganization(id = it.id, name = it.name, slug = it.slug)
          },
      )

    return PendingAppDelivery(
      deliveryId = record.id,
      targetUrl = target.baseUrl,
      payload = objectMapper.writeValueAsString(payload),
      signingSecret = target.signingSecret,
    )
  }
}
