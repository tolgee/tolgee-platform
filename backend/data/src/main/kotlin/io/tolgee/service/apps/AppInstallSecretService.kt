package io.tolgee.service.apps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.KeyGenerator
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.apps.AppInstall
import io.tolgee.model.apps.AppInstallSecret
import io.tolgee.repository.apps.AppInstallSecretRepository
import io.tolgee.util.constantTimeEquals
import io.tolgee.util.runSentryCatching
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Date

/**
 * Owns the client secrets of an app install. Rotation is two deliberate steps — issue a second
 * secret, then revoke the first once [AppInstallSecret.lastUsedAt] shows the app has moved over —
 * so an install keeps its id, granted scopes, organization availability and per-project enablements
 * across a rotation, which deleting and re-registering it would all destroy.
 */
@Service
class AppInstallSecretService(
  private val appInstallSecretRepository: AppInstallSecretRepository,
  private val keyGenerator: KeyGenerator,
  private val currentDateProvider: CurrentDateProvider,
) {
  data class IssueResult(
    val secret: AppInstallSecret,
    val plaintextSecret: String,
  )

  /**
   * Issues an additional secret for the install. Every secret already issued keeps authenticating
   * until it is revoked — that is the point of splitting rotation in two.
   */
  @Transactional
  fun issue(install: AppInstall): IssueResult {
    if (countLive(install.id) >= MAX_LIVE_SECRETS) {
      throw BadRequestException(Message.APP_TOO_MANY_LIVE_SECRETS)
    }
    return issueInitial(install)
  }

  /**
   * Issues the first secret of a freshly created install, inside the caller's transaction and
   * without the cap check — the install has none yet.
   */
  fun issueInitial(install: AppInstall): IssueResult {
    val plaintext = AppInstallService.CLIENT_SECRET_PREFIX + keyGenerator.generate(256)
    val secret =
      AppInstallSecret().apply {
        this.appInstall = install
        this.secretHash = keyGenerator.hash(plaintext)
        this.secretPrefix = plaintext.take(AppInstallService.CLIENT_SECRET_PREFIX_DISPLAY_LENGTH)
      }
    return IssueResult(appInstallSecretRepository.save(secret), plaintext)
  }

  /**
   * @param allowRevokingLast false refuses to revoke the install's only live secret. False for the
   *   app-initiated path: an app authenticates with a secret, so revoking its last one would lock it
   *   out of the very endpoint that could issue a replacement. An operator may do it — that is the
   *   emergency kill switch for a leaked secret, and they can issue a new one afterwards.
   */
  @Transactional
  fun revoke(
    installId: Long,
    secretId: Long,
    allowRevokingLast: Boolean,
  ): AppInstallSecret {
    val secret =
      appInstallSecretRepository.findByIdAndAppInstallId(secretId, installId)
        ?: throw NotFoundException(Message.APP_INSTALL_SECRET_NOT_FOUND)

    if (secret.revokedAt != null) return secret

    if (!allowRevokingLast && countLive(installId) <= 1) {
      throw BadRequestException(Message.APP_CANNOT_REVOKE_LAST_SECRET)
    }

    secret.revokedAt = currentDateProvider.date
    return appInstallSecretRepository.save(secret)
  }

  @Transactional(readOnly = true)
  fun list(installId: Long): List<AppInstallSecret> {
    return appInstallSecretRepository.findAllByAppInstallIdOrderByCreatedAtDesc(installId)
  }

  /**
   * The install's live secret matching [plaintextSecret], or null when none does.
   *
   * Every live secret is compared in constant time rather than looked up by hash, so the endpoint
   * leaks no more about a wrong secret than it did when an install had exactly one. [MAX_LIVE_SECRETS]
   * bounds the work.
   */
  @Transactional(readOnly = true)
  fun findLiveMatching(
    installId: Long,
    plaintextSecret: String,
  ): AppInstallSecret? {
    val providedHash = keyGenerator.hash(plaintextSecret)
    return appInstallSecretRepository
      .findAllByAppInstallIdAndRevokedAtIsNull(installId)
      .firstOrNull { constantTimeEquals(providedHash, it.secretHash) }
  }

  /**
   * Records that a secret authenticated, unless [previousLastUsedAt] is recent enough that the write
   * would tell an operator nothing new. An app exchanges its credentials once per token lifetime, so
   * this is already off the request hot path; the throttle keeps a fleet of app replicas from
   * writing the same row on every boot.
   */
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
      appInstallSecretRepository.updateLastUsedById(secretId, now)
    }
  }

  private fun countLive(installId: Long): Long {
    return appInstallSecretRepository.countByAppInstallIdAndRevokedAtIsNull(installId)
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
