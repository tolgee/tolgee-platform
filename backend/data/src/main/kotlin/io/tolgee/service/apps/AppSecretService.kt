package io.tolgee.service.apps

import io.tolgee.component.KeyGenerator
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppSecret
import io.tolgee.repository.apps.AppSecretRepository
import org.springframework.stereotype.Service

/**
 * Owns the app-level client secrets — the app's only long-lived credentials. Everything the app
 * does starts here: the token endpoint exchanges them for short-lived install-scoped tokens.
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
        this.name = displayName(plaintext)
      }
    return IssueResult(appSecretRepository.save(secret), plaintext)
  }

  /** How a secret is identified wherever it is shown: its start and end, e.g. `tgpubs_ab…yz`. */
  fun list(appId: Long): List<AppSecret> {
    return appSecretRepository.findAllByAppIdOrderByCreatedAtDesc(appId)
  }

  companion object {
    fun displayName(plaintext: String): String {
      val start = plaintext.take(AppService.APP_CLIENT_SECRET_PREFIX_DISPLAY_LENGTH)
      val end = plaintext.takeLast(AppService.APP_CLIENT_SECRET_SUFFIX_DISPLAY_LENGTH)
      return "$start…$end"
    }
  }
}
