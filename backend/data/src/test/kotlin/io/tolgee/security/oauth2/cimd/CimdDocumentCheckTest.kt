package io.tolgee.security.oauth2.cimd

import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.dao.DataIntegrityViolationException
import java.util.Date

class CimdDocumentCheckTest {
  private val claimedAt = Date(1_000_000)

  @Test
  fun `a client the CIMD path does not serve is never claimed or read`() {
    val lifecycle = mock<CimdClientLifecycleService>()
    val registry = mock<OAuth2ClientRegistry> { on { servesCimdClient(any()) } doReturn false }

    check(lifecycle, registry).checkIfDue(CLIENT).assert.isEqualTo(CimdDocumentCheck.Outcome.NOT_DUE)

    verify(lifecycle, never()).claimCheck(any())
    verify(registry, never()).resolveForCheck(any())
  }

  @Test
  fun `a client read within the interval is not read again`() {
    val lifecycle = mock<CimdClientLifecycleService> { on { claimCheck(any()) } doReturn null }
    val registry = servingRegistry()

    check(lifecycle, registry).checkIfDue(CLIENT).assert.isEqualTo(CimdDocumentCheck.Outcome.NOT_DUE)

    verify(registry, never()).resolveForCheck(any())
  }

  @Test
  fun `a claimed check records what the publisher answered`() {
    val lifecycle = claimingLifecycle()
    val registry = servingRegistry(CimdResolution.Withdrawn)

    check(lifecycle, registry).checkIfDue(CLIENT).assert.isEqualTo(CimdDocumentCheck.Outcome.CHECKED)

    verify(lifecycle).recordCheckResult(CLIENT, CimdResolution.Withdrawn)
  }

  /** The claim is what stops a second caller from fetching too, so a fetch that never happened must give it back. */
  @Test
  fun `a fetch this server had no room for is no attempt`() {
    val lifecycle = claimingLifecycle()
    val registry = servingRegistry(resolution = null)

    check(lifecycle, registry).checkIfDue(CLIENT).assert.isEqualTo(CimdDocumentCheck.Outcome.NOT_ATTEMPTED)

    verify(lifecycle).releaseCheck(CLIENT, claimedAt)
    verify(lifecycle, never()).recordCheckResult(any(), any())
  }

  @Test
  fun `losing the race for a client's first attempt is not an error`() {
    val lifecycle =
      mock<CimdClientLifecycleService> {
        on { claimCheck(any()) } doAnswer { throw DataIntegrityViolationException("duplicate client_id") }
      }
    val registry = servingRegistry()

    check(lifecycle, registry).checkIfDue(CLIENT).assert.isEqualTo(CimdDocumentCheck.Outcome.NOT_DUE)

    verify(registry, never()).resolveForCheck(any())
  }

  @Test
  fun `a fetch that throws gives the claim back before the error leaves`() {
    val lifecycle = claimingLifecycle()
    val registry =
      mock<OAuth2ClientRegistry> {
        on { servesCimdClient(any()) } doReturn true
        on { resolveForCheck(any()) } doAnswer { throw IllegalStateException("boom") }
      }

    assertThrows<IllegalStateException> { check(lifecycle, registry).checkIfDue(CLIENT) }

    verify(lifecycle).releaseCheck(CLIENT, claimedAt)
  }

  private fun claimingLifecycle() =
    mock<CimdClientLifecycleService> { on { claimCheck(eq(CLIENT)) } doReturn claimedAt }

  private fun servingRegistry(resolution: CimdResolution? = CimdResolution.Unavailable) =
    mock<OAuth2ClientRegistry> {
      on { servesCimdClient(any()) } doReturn true
      on { resolveForCheck(any()) } doReturn resolution
    }

  private fun check(
    lifecycle: CimdClientLifecycleService,
    registry: OAuth2ClientRegistry,
  ) = CimdDocumentCheck(lifecycle, registry)

  companion object {
    private const val CLIENT = "https://publisher.example/client"
  }
}
