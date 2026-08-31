package io.tolgee.service.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.apps.App
import io.tolgee.model.apps.AppSecret
import io.tolgee.repository.apps.AppSecretRepository
import io.tolgee.util.constantTimeEquals
import io.tolgee.util.runSentryCatching
import org.springframework.cache.annotation.CacheEvict
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

@Service
class AppSecretService(
  private val appSecretRepository: AppSecretRepository,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
  private val appActivityRecorder: AppActivityRecorder,
) {
  data class IssueResult(
    val secret: AppSecret,
    val plaintextSecret: String,
  )

  fun mintSecret(app: App): IssueResult {
    val plaintext = AppService.APP_CLIENT_SECRET_PREFIX + keyGenerator.generate(256)
    val secret =
      AppSecret().apply {
        this.app = app
        this.secretHash = keyGenerator.hash(plaintext)
        this.name = displayName(plaintext)
      }
    return IssueResult(appSecretRepository.save(secret), plaintext)
  }

  @Transactional
  fun issue(app: App): IssueResult {
    if (activeSecrets(app.id).size >= MAX_LIVE_SECRETS) {
      throw BadRequestException(Message.APP_TOO_MANY_LIVE_SECRETS)
    }
    return mintSecret(app)
  }

  /**
   * Puts every active secret except [keepSecretId] on a [graceSeconds] deadline, never extending one
   * already expiring sooner, and returns the latest of those deadlines. Call inside the same
   * transaction as [issue] so a failure cannot leave a deadline-less extra secret.
   */
  fun expireOthers(
    appId: Long,
    keepSecretId: Long,
    graceSeconds: Long,
  ): Date? {
    val now = currentDateProvider.date
    val others = activeSecrets(appId).filter { it.id != keepSecretId }
    if (others.isEmpty()) return null

    val expiry = Date(now.time + graceSeconds * 1000L)
    others.forEach { secret ->
      val current = secret.expiresAt
      if (current == null || current.after(expiry)) {
        secret.expiresAt = expiry
      }
    }
    appSecretRepository.saveAll(others)
    return others.mapNotNull { it.expiresAt }.maxOrNull()
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.APPS], key = "#appId", condition = "#force")
  fun revoke(
    appId: Long,
    secretId: Long,
    force: Boolean,
  ): AppSecret {
    val secret =
      appSecretRepository.findByIdAndAppId(secretId, appId)
        ?: throw NotFoundException(Message.APP_SECRET_NOT_FOUND)
    appActivityRecorder.record(secret.app)

    if (force) {
      secret.app.tokensInvalidBefore = currentDateProvider.date
    }

    if (secret.revokedAt != null) return appSecretRepository.save(secret)

    if (!force && isActive(secret = secret) && activeSecrets(appId).size <= 1) {
      throw BadRequestException(Message.APP_CANNOT_REVOKE_LAST_SECRET)
    }

    secret.revokedAt = currentDateProvider.date
    return appSecretRepository.save(secret)
  }

  @Transactional(readOnly = true)
  fun list(appId: Long): List<AppSecret> {
    return appSecretRepository.findAllByAppIdOrderByCreatedAtDesc(appId)
  }

  /** Constant-time compared rather than hash-looked-up, so timing cannot confirm a guessed secret. */
  @Transactional(readOnly = true)
  fun findLiveMatching(
    appId: Long,
    plaintextSecret: String,
  ): AppSecret? {
    val providedHash = keyGenerator.hash(plaintextSecret)
    return activeSecrets(appId).firstOrNull { constantTimeEquals(providedHash, it.secretHash) }
  }

  @Async
  @Transactional
  fun updateLastUsedAsync(
    secretId: Long,
    previousLastUsedAt: Date?,
  ) {
    runSentryCatching {
      val now = currentDateProvider.date
      if (previousLastUsedAt != null && now.time - previousLastUsedAt.time < LAST_USED_THROTTLE_MS) {
        return@runSentryCatching
      }
      appSecretRepository.updateLastUsedById(secretId, now)
    }
  }

  private fun isActive(secret: AppSecret): Boolean {
    if (secret.revokedAt != null) return false
    val expiresAt = secret.expiresAt ?: return true
    return expiresAt.after(currentDateProvider.date)
  }

  private fun activeSecrets(appId: Long): List<AppSecret> {
    return appSecretRepository.findActiveByAppId(appId, currentDateProvider.date)
  }

  companion object {
    const val MAX_LIVE_SECRETS = 3

    const val LAST_USED_THROTTLE_MS = 60_000L

    fun displayName(plaintext: String): String {
      val start = plaintext.take(AppService.APP_CLIENT_SECRET_PREFIX_DISPLAY_LENGTH)
      val end = plaintext.takeLast(AppService.APP_CLIENT_SECRET_SUFFIX_DISPLAY_LENGTH)
      return "$start…$end"
    }
  }
}
