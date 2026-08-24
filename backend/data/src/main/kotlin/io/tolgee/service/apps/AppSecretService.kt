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
 * At rest an app has a single active secret. A rotation mints a replacement and gives the outgoing
 * one an [AppSecret.expiresAt] grace window, so both authenticate until the old one lapses — that is
 * what lets an app that copies the secret in by hand switch over without being cut off. An expired
 * secret is treated as dead everywhere it is read, so nothing has to touch the row on a timer.
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
        this.name = displayName(plaintext)
      }
    return IssueResult(appSecretRepository.save(secret), plaintext)
  }

  /** Issues an additional active secret. Every existing active secret keeps authenticating. */
  @Transactional
  fun issue(app: App): IssueResult {
    if (activeSecrets(app.id).size >= MAX_LIVE_SECRETS) {
      throw BadRequestException(Message.APP_TOO_MANY_LIVE_SECRETS)
    }
    return issueInitial(app)
  }

  /**
   * Closes a rotation: every active secret other than the one just issued ([keepSecretId]) is put on
   * a [graceSeconds] deadline. None is revoked here — whether an app truly adopted a delivered
   * secret is unknowable from Tolgee's side, so outgoing secrets always live out their window (or
   * are revoked by hand from the list). A secret already expiring keeps its earlier deadline.
   *
   * @return when the outgoing secrets lapse, or null when there was nothing to retire.
   */
  @Transactional
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
      if (secret.expiresAt == null || secret.expiresAt!!.after(expiry)) {
        secret.expiresAt = expiry
      }
    }
    appSecretRepository.saveAll(others)
    return expiry
  }

  /**
   * Revokes a secret at once — the answer to a leaked credential, and how a rotation's grace window
   * is ended early. The app's only active secret cannot be revoked without [force], so an ordinary
   * revoke cannot leave the app with nothing to authenticate with.
   *
   * @param force also stamps [App.tokensInvalidBefore], invalidating every access token already
   *   minted from any of the app's secrets — the kill switch for a credential known to have leaked.
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

    val now = currentDateProvider.date
    if (!force && isActive(secret, now) && activeSecrets(appId).size <= 1) {
      throw BadRequestException(Message.APP_CANNOT_REVOKE_LAST_SECRET)
    }

    secret.revokedAt = now
    if (force) {
      // Truncated to the second because a JWT's `iat` is expressed in whole seconds: against an
      // untruncated cutoff, the token the app mints to recover from this very revocation would be
      // rejected whenever it lands in the same second. The cost is that a token issued earlier in
      // that same second survives.
      secret.app.tokensInvalidBefore = Date(now.time / 1000L * 1000L)
    }
    return appSecretRepository.save(secret)
  }

  @Transactional(readOnly = true)
  fun list(appId: Long): List<AppSecret> {
    return appSecretRepository.findAllByAppIdOrderByCreatedAtDesc(appId)
  }

  /**
   * The app's active secret matching [plaintextSecret], or null when none does. Compared in constant
   * time rather than looked up by hash, so a timing side channel cannot confirm a guessed secret. An
   * expired secret never matches.
   */
  @Transactional(readOnly = true)
  fun findLiveMatching(
    appId: Long,
    plaintextSecret: String,
  ): AppSecret? {
    val providedHash = keyGenerator.hash(plaintextSecret)
    return activeSecrets(appId).firstOrNull { constantTimeEquals(providedHash, it.secretHash) }
  }

  /**
   * Stamps a secret's first use **synchronously**, in the caller's transaction; every later use goes
   * through [updateLastUsedAsync], off the token hot path. Called only when [AppSecret.lastUsedAt] is
   * still null, so it runs once per secret.
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

  private fun isActive(
    secret: AppSecret,
    now: Date,
  ): Boolean {
    if (secret.revokedAt != null) return false
    val expiresAt = secret.expiresAt ?: return true
    return expiresAt.after(now)
  }

  private fun activeSecrets(appId: Long): List<AppSecret> {
    return appSecretRepository.findActiveByAppId(appId, currentDateProvider.date)
  }

  companion object {
    /**
     * The current secret plus two still living out rotation grace windows. Beyond that the list
     * stops being something an operator can reason about, and every extra live secret is another
     * copy that can leak — so a further rotation is refused until one expires or is revoked.
     */
    const val MAX_LIVE_SECRETS = 3

    const val LAST_USED_THROTTLE_MS = 60_000L

    /** How a secret is identified wherever it is shown: its start and end, e.g. `tgpubs_ab…yz`. */
    fun displayName(plaintext: String): String {
      val start = plaintext.take(AppService.APP_CLIENT_SECRET_PREFIX_DISPLAY_LENGTH)
      val end = plaintext.takeLast(AppService.APP_CLIENT_SECRET_SUFFIX_DISPLAY_LENGTH)
      return "$start…$end"
    }
  }
}
