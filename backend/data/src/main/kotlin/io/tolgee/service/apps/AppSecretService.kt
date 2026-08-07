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
 * Owns the app-level client secrets — the app's only long-lived credentials. Everything the app
 * does starts here: the token endpoint exchanges them for short-lived install-scoped tokens.
 *
 * Rotation is two separate steps — issue while the old one still works, then revoke it separately —
 * so an app is never without a working secret mid-rotation.
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
   * Revoking also stamps [App.tokensInvalidBefore], so every access token already minted from any of
   * the app's secrets stops validating at once. Without it a leaked secret would keep buying access
   * for as long as the tokens it minted live, which is the whole window revocation exists to close.
   *
   * @param allowRevokingLast false refuses to revoke the app's only live secret. False for the
   *   app-initiated path: an app authenticates with a secret, so revoking its last one would lock
   *   it out of the endpoint that issues a replacement. The owning organization may do it — that
   *   is the kill switch for a leaked credential.
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

    val now = currentDateProvider.date
    secret.revokedAt = now
    // Truncated to the second because a JWT's `iat` is expressed in whole seconds: against an
    // untruncated cutoff, the token the app mints to recover from this very revocation would be
    // rejected whenever it lands in the same second. The cost is that a token issued earlier in
    // that same second survives.
    secret.app.tokensInvalidBefore = Date(now.time / 1000L * 1000L)
    return appSecretRepository.save(secret)
  }

  @Transactional(readOnly = true)
  fun list(appId: Long): List<AppSecret> {
    return appSecretRepository.findAllByAppIdOrderByCreatedAtDesc(appId)
  }

  /**
   * The app's live secret matching [plaintextSecret], or null when none does. Compared in constant
   * time rather than looked up by hash, so a timing side channel cannot confirm a guessed secret.
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
      val throttle = LAST_USED_THROTTLE_MS
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
    /**
     * A rotation needs two, and a staged rollout across environments a few more. Beyond that the
     * list stops being something an operator can reason about before revoking, and every extra live
     * secret is another copy that can leak — so issuing is refused rather than silently unbounded.
     */
    const val MAX_LIVE_SECRETS = 5

    const val LAST_USED_THROTTLE_MS = 60_000L
  }
}
