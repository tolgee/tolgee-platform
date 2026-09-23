package io.tolgee.security.oauth2

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.tolgee.Metrics
import io.tolgee.component.LockingProvider
import io.tolgee.configuration.tolgee.OAuth2ServerProperties
import io.tolgee.security.oauth2.cimd.CimdResolution
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OAuth2CimdDocumentCheckTest {
  @Test
  fun `an attempt is recorded even when the document could not be read`() {
    val service = serviceWith(UNREADABLE, READABLE)
    val registry =
      mock<OAuth2ClientRegistry> {
        on { resolveForCheck(eq(UNREADABLE)) } doReturn CimdResolution.Unavailable
        on { resolveForCheck(eq(READABLE)) } doReturn CimdResolution.Withdrawn
      }

    check(service, registry).checkBatch()

    verify(service).recordCheckAttempt(UNREADABLE)
    verify(service).recordCheckAttempt(READABLE)
  }

  @Test
  fun `one client that throws does not take the rest of the round down with it`() {
    val service = serviceWith(UNREADABLE, READABLE)
    val registry =
      mock<OAuth2ClientRegistry> {
        on { resolveForCheck(eq(UNREADABLE)) } doAnswer { throw IllegalStateException("boom") }
        on { resolveForCheck(eq(READABLE)) } doReturn CimdResolution.Withdrawn
      }

    check(service, registry).checkBatch()

    verify(service).recordClientWithdrawn(READABLE)
    verify(service).recordCheckAttempt(UNREADABLE)
  }

  /**
   * The check reads the document itself, so the only answer that means "we did not read it" is an unavailable
   * one. Acting on that would turn our own blip, or our own full fetch budget, into a publisher's retirement.
   */
  @Test
  fun `an answer the check could not read is not acted on, but still counts as an attempt`() {
    val service = serviceWith(READABLE)
    val registry =
      mock<OAuth2ClientRegistry> {
        on { resolveForCheck(any()) } doReturn CimdResolution.Unavailable
      }

    check(service, registry).checkBatch()

    verify(service, never()).recordClientWithdrawn(any())
    verify(service, never()).recordDocumentRead(any())
    verify(service).recordCheckAttempt(READABLE)
  }

  /**
   * Building the work list is one query, but a round that dies on it is not one client skipped - it is every
   * round from then on, at the same line, and seven days later every third-party grant on the instance.
   */
  @Test
  fun `a round that cannot build its work list ends quietly instead of killing the schedule`() {
    val service =
      mock<OAuth2AuthorizationService> {
        on { clientIdsDueForCheck(any()) } doAnswer { throw IllegalStateException("too many parameters") }
      }

    check(service, mock<OAuth2ClientRegistry>()).checkBatch().assert.isEqualTo(0)
  }

  @Test
  fun `a backlog measurement that fails does not cost the round its work`() {
    val service = serviceWith(READABLE)
    whenever(service.clientsDueForCheckCount()).thenAnswer { throw IllegalStateException("statement timeout") }
    val registry =
      mock<OAuth2ClientRegistry> {
        on { resolveForCheck(any()) } doReturn CimdResolution.Withdrawn
      }

    check(service, registry).checkBatch()

    verify(service).recordClientWithdrawn(READABLE)
  }

  /**
   * The kill switch has to stop the round itself, not just the lock: this is the only way an operator can take
   * the outbound fetching off an instance.
   */
  @Test
  fun `the flag stops the scheduled round even where the lock would let it run`() {
    val service = serviceWith(READABLE)
    val registry = mock<OAuth2ClientRegistry>()
    val disabled = OAuth2ServerProperties().apply { cimdEnabled = false }

    check(service, registry, disabled, AlwaysFreeLockingProvider()).checkClientDocuments()

    verify(service, never()).clientIdsDueForCheck(any())
    verify(registry, never()).resolveForCheck(any())
  }

  /**
   * Every replica ticks on the same cron, and the marks and stamps a round writes are shared. Without the lock
   * each replica repeats every publisher's outbound fetch for nothing.
   */
  @Test
  fun `the scheduled round is held behind the lock`() {
    val service = serviceWith(READABLE)

    // A LockingProvider that never runs the body stands for a replica that did not win the lock.
    check(service, mock<OAuth2ClientRegistry>(), locking = mock<LockingProvider>()).checkClientDocuments()

    verify(service, never()).clientIdsDueForCheck(any())
  }

  @Test
  fun `the replica that holds the lock does the round's work`() {
    val service = serviceWith(READABLE)
    val registry =
      mock<OAuth2ClientRegistry> {
        on { resolveForCheck(any()) } doReturn CimdResolution.Withdrawn
      }

    check(service, registry, locking = AlwaysFreeLockingProvider()).checkClientDocuments()

    verify(service).recordClientWithdrawn(READABLE)
    verify(service).recordCheckAttempt(READABLE)
  }

  private fun serviceWith(vararg clientIds: String) =
    mock<OAuth2AuthorizationService> { on { clientIdsDueForCheck(any()) } doReturn clientIds.toList() }

  private fun check(
    service: OAuth2AuthorizationService,
    registry: OAuth2ClientRegistry,
    properties: OAuth2ServerProperties = OAuth2ServerProperties(),
    locking: LockingProvider = mock<LockingProvider>(),
  ) = OAuth2CimdDocumentCheck(
    service,
    registry,
    properties,
    locking,
    Metrics(SimpleMeterRegistry()),
  )

  companion object {
    private const val UNREADABLE = "https://unreadable.example/client"
    private const val READABLE = "https://readable.example/client"
  }
}
