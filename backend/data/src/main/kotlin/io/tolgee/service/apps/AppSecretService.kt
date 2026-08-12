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
   * Two guard rails protect an ordinary rotation from cutting the app off, and both are lifted by
   * [force] — the kill switch for a credential known to have leaked, where breaking the app now is
   * exactly the point:
   *  - the app's only live secret cannot be revoked (issue a replacement first);
   *  - a secret cannot be revoked while no other live secret has ever been used, i.e. while the app
   *    has not demonstrably moved to a replacement. `lastUsedAt` is written on a secret's first use,
   *    so this reads whether some other live secret has authenticated at least once.
   *
   * @param force bypasses both guards. The owner's kill switch passes it; an app rotating itself
   *   never does — it revokes its old secret only once it is authenticating with the new one.
   */
  @Transactional
  fun revoke(
    appId: Long,
    secretId: Long,
    force: Boolean,
  ): AppSecret {
    val secret =
      appSecretRepository.findByIdAndAppId(secretId, appId)
        ?: throw NotFoundException(Message.APP_SECRET_NOT_FOUND)

    if (secret.revokedAt != null) return secret

    if (!force) {
      if (countLive(appId) <= 1) {
        throw BadRequestException(Message.APP_CANNOT_REVOKE_LAST_SECRET)
      }
      if (!hasOtherUsedLiveSecret(appId, secretId)) {
        throw BadRequestException(Message.APP_SECRET_REPLACEMENT_UNUSED)
      }
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

  /**
   * Stamps a secret's first use **synchronously**, in the caller's transaction. The revoke guard
   * reads `lastUsedAt` to decide whether the app has moved to a replacement, so this one write must
   * not race the operator's revoke; every later use goes through [updateLastUsedAsync], off the
   * token hot path. Called only when [AppSecret.lastUsedAt] is still null, so it runs once per
   * secret.
   */
  @Transactional
  fun recordFirstUse(secretId: Long) {
    appSecretRepository.updateLastUsedById(secretId, currentDateProvider.date)
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

  /** Whether a live secret other than [excludingSecretId] has been used at least once. */
  private fun hasOtherUsedLiveSecret(
    appId: Long,
    excludingSecretId: Long,
  ): Boolean {
    return appSecretRepository
      .findAllByAppIdAndRevokedAtIsNull(appId)
      .any { it.id != excludingSecretId && it.lastUsedAt != null }
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
