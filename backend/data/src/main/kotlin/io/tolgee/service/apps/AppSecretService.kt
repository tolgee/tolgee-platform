package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppSecret
import io.tolgee.repository.apps.AppSecretRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Owns the app-level client secrets. They identify and administer the app across every organization
 * that installed it; nothing they authenticate reaches a tenant's data.
 */
@Service
class AppSecretService(
  private val appSecretRepository: AppSecretRepository,
  private val keyGenerator: KeyGenerator,
) {
  data class IssueResult(
    val secret: AppSecret,
    val plaintextSecret: String,
  )

  /** Issues the first secret of a freshly registered app, inside the caller's transaction. */
  fun issueInitial(app: App): IssueResult {
    val plaintext = AppService.APP_CLIENT_SECRET_PREFIX + keyGenerator.generate(256)
    val secret =
      AppSecret().apply {
        this.app = app
        this.secretHash = keyGenerator.hash(plaintext)
        this.secretPrefix = plaintext.take(AppService.APP_CLIENT_SECRET_PREFIX_DISPLAY_LENGTH)
      }
    return IssueResult(appSecretRepository.save(secret), plaintext)
  }

  @Transactional(readOnly = true)
  fun list(appId: Long): List<AppSecret> {
    return appSecretRepository.findAllByAppIdOrderByCreatedAtDesc(appId)
  }
}
