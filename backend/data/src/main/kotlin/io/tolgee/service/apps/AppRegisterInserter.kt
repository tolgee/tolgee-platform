package io.tolgee.service.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.Organization
import io.tolgee.model.apps.App
import io.tolgee.repository.apps.AppRepository
import jakarta.persistence.EntityManager
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Inserts the app row and its initial secret in their own transaction so that the unique-constraint
 * violation a concurrent registration of the same manifest id raises - which Hibernate answers by
 * dooming the *current* transaction - is confined here and translated to
 * [Message.APP_ALREADY_REGISTERED], leaving the caller's transaction committable.
 */
@Service
class AppRegisterInserter(
  private val appRepository: AppRepository,
  private val appSecretService: AppSecretService,
  private val keyGenerator: KeyGenerator,
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
) {
  data class Inserted(
    val app: App,
    val credentials: AppService.AppCredentials,
  )

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun insert(
    organizationId: Long,
    manifestUrl: String,
    fetched: AppManifestFetcher.FetchResult,
  ): Inserted {
    val clientId = AppService.APP_CLIENT_ID_PREFIX + keyGenerator.generate(128)
    val webhookSecret = keyGenerator.generate(256)
    val app =
      App().apply {
        this.organization = entityManager.getReference(Organization::class.java, organizationId)
        this.appId = fetched.manifest.id
        this.manifestUrl = manifestUrl
        this.manifestScopes = AppService.joinScopes(fetched.scopes)
        this.name = fetched.manifest.name
        this.version = fetched.manifest.version
        this.baseUrl = fetched.manifest.baseUrl
        this.icon = fetched.icon
        this.manifestJson = fetched.rawJson
        this.clientId = clientId
        this.webhookSecret = webhookSecret
      }
    AppService.markManifestHealthy(app, currentDateProvider.date)
    val saved =
      try {
        appRepository.saveAndFlush(app)
      } catch (_: DataIntegrityViolationException) {
        throw BadRequestException(Message.APP_ALREADY_REGISTERED)
      }
    val issued = appSecretService.issueInitial(saved)
    return Inserted(
      app = saved,
      credentials =
        AppService.AppCredentials(
          clientId = clientId,
          clientSecret = issued.plaintextSecret,
          webhookSecret = webhookSecret,
        ),
    )
  }
}
