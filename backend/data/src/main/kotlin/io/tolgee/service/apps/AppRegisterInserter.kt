package io.tolgee.service.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.model.Organization
import io.tolgee.model.apps.App
import io.tolgee.repository.apps.AppRepository
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service

/**
 * Inserts the app row and its initial secret. It runs inside the caller's register+install
 * transaction, so a failed install rolls the app back with it. The unique-constraint violation a
 * concurrent registration of the same manifest id raises is flushed here and left to propagate: the
 * non-transactional caller catches it after the rollback and reports [Message.APP_ALREADY_REGISTERED].
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
    val saved = appRepository.saveAndFlush(app)
    val issued = appSecretService.mintSecret(saved)
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
