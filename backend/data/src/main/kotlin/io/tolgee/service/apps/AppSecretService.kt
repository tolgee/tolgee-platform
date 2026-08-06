package io.tolgee.service.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppSecret
import io.tolgee.repository.apps.AppSecretRepository
import io.tolgee.util.constantTimeEquals
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

/**
 * Owns the app-level client secrets. They identify and administer the app across every organization
 * that installed it; nothing they authenticate reaches a tenant's data.
 *
 * Rotation is the same two steps as at the install layer — issue while the old one still works, then
 * revoke it separately. See [AppInstallSecretService].
 */
@Service
class AppSecretService(
  private val appSecretRepository: AppSecretRepository,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
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

  /**
   * Issues an additional secret. Every secret already issued keeps authenticating until it is
   * revoked — that is the point of splitting rotation in two.
   */
  @Transactional
  fun issue(app: App): IssueResult {
    if (countLive(app.id) >= MAX_LIVE_SECRETS) {
      throw BadRequestException(Message.APP_TOO_MANY_LIVE_SECRETS)
    }
    return issueInitial(app)
  }

  /**
   * @param allowRevokingLast false refuses to revoke the app's only live secret. False for the
   *   app-initiated path, matching [AppInstallSecretService.revoke]: an app authenticates with a
   *   secret, so revoking its last one would lock it out of the endpoint that issues a replacement.
   *   The owning organization may do it — that is the kill switch for a leaked credential.
   */
  @Transactional
  fun revoke(
    appId: Long,
    secretId: Long,
    allowRevokingLast: Boolean,
  ): AppSecret {
    val secret =
      appSecretRepository.findByIdAndAppId(secretId, appId)
        ?: throw NotFoundException(Message.APP_SECRET_NOT_FOUND)

    if (secret.revokedAt != null) return secret

    if (!allowRevokingLast && countLive(appId) <= 1) {
      throw BadRequestException(Message.APP_CANNOT_REVOKE_LAST_SECRET)
    }

    secret.revokedAt = currentDateProvider.date
    return appSecretRepository.save(secret)
  }

  @Transactional(readOnly = true)
  fun list(appId: Long): List<AppSecret> {
    return appSecretRepository.findAllByAppIdOrderByCreatedAtDesc(appId)
  }

  /**
   * The app's live secret matching [plaintextSecret], or null when none does. Compared in constant
   * time rather than looked up by hash, exactly as [AppInstallSecretService.findLiveMatching] does.
   */
  @Transactional(readOnly = true)
  fun findLiveMatching(
    appId: Long,
    plaintextSecret: String,
  ): AppSecret? {
    val providedHash = keyGenerator.hash(plaintextSecret)
    return appSecretRepository
      .findAllByAppIdAndRevokedAtIsNull(appId)
      .firstOrNull { constantTimeEquals(providedHash, it.secretHash) }
  }

  @Async
  @Transactional
  fun updateLastUsedAsync(
    secretId: Long,
    previousLastUsedAt: Date?,
  ) {
    runSentryCatching {
      val now = currentDateProvider.date
      val throttle = AppInstallSecretService.LAST_USED_THROTTLE_MS
      if (previousLastUsedAt != null && now.time - previousLastUsedAt.time < throttle) {
        return@runSentryCatching
      }
      appSecretRepository.updateLastUsedById(secretId, now)
    }
  }

  private fun countLive(appId: Long): Long {
    return appSecretRepository.countByAppIdAndRevokedAtIsNull(appId)
  }

  companion object {
    const val MAX_LIVE_SECRETS = AppInstallSecretService.MAX_LIVE_SECRETS
  }
}
