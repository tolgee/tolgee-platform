package io.tolgee.unit.configuration

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.publicBillingConfProvider.PublicBillingConfProvider
import io.tolgee.configuration.SelfHostedReviewAsk
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.response.PublicBillingConfigurationDTO
import io.tolgee.service.InstanceIdService
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.springframework.boot.ansi.AnsiOutput
import org.springframework.mock.env.MockEnvironment
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import java.util.Date

class SelfHostedReviewAskTest {
  private val properties = TolgeeProperties()
  private val environment = MockEnvironment()
  private var billingEnabled = false
  private val now = Instant.parse("2026-10-09T10:00:00Z")
  private var instanceCreatedAt: Instant? = now.minus(Duration.ofDays(30))
  private var instanceLookupFails = false

  private val instanceIdService =
    mock<InstanceIdService> {
      on { getInstanceCreatedAt() } doAnswer {
        if (instanceLookupFails) throw IllegalStateException("Database unavailable")
        instanceCreatedAt?.let { Date.from(it) }
      }
    }

  private val currentDateProvider =
    mock<CurrentDateProvider> {
      on { date } doAnswer { Date.from(now) }
    }

  private val billingConfProvider =
    object : PublicBillingConfProvider {
      override fun invoke() = PublicBillingConfigurationDTO(enabled = billingEnabled)
    }

  @BeforeEach
  fun disableAnsi() {
    AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER)
  }

  @AfterEach
  fun restoreAnsi() {
    AnsiOutput.setEnabled(AnsiOutput.Enabled.DETECT)
  }

  @Test
  fun `prints the review ask on self-hosted instances`() {
    val output = capturedOutput()
    output.assert.contains(SelfHostedReviewAsk.G2_REVIEW_URL)
    output.assert.contains("Hi, I'm Jan, the founder of Tolgee. 👋")
    output.assert.contains("(   \\_/   )  │")
    output.assert.contains("TOLGEE_REVIEW_ASK_ENABLED=false")
  }

  @Test
  fun `prints nothing on Tolgee Cloud`() {
    billingEnabled = true
    capturedOutput().assert.isEmpty()
  }

  @Test
  fun `prints nothing when disabled`() {
    properties.reviewAsk.enabled = false
    capturedOutput().assert.isEmpty()
  }

  @ParameterizedTest
  @ValueSource(strings = ["off", "OFF", "false"])
  fun `prints nothing when the Spring banner is turned off`(bannerMode: String) {
    environment.setProperty("spring.main.banner-mode", bannerMode)
    capturedOutput().assert.isEmpty()
  }

  @Test
  fun `prints nothing on an instance younger than 5 days`() {
    instanceCreatedAt = now.minus(Duration.ofDays(5)).plusSeconds(1)
    capturedOutput().assert.isEmpty()
  }

  @Test
  fun `prints once the instance is 5 days old`() {
    instanceCreatedAt = now.minus(Duration.ofDays(5))
    capturedOutput().assert.contains(SelfHostedReviewAsk.G2_REVIEW_URL)
  }

  @Test
  fun `prints nothing when the instance creation date is unknown`() {
    instanceCreatedAt = null
    capturedOutput().assert.isEmpty()
  }

  @Test
  fun `a failure while deciding does not break startup`() {
    instanceLookupFails = true
    assertDoesNotThrow { reviewAsk().onApplicationReady() }
  }

  @Test
  fun `falls back to ASCII when the output cannot encode unicode`() {
    val output = capturedOutput(Charsets.US_ASCII)
    output.assert.contains("Thanks a lot <3")
    output.assert.contains("(   \\_/   )  |")
    output
      .replace("Could you do one thing for me?", "")
      .assert
      .doesNotContain("?")
  }

  private fun capturedOutput(charset: Charset = Charsets.UTF_8): String {
    val bytes = ByteArrayOutputStream()
    reviewAsk().printIfEnabled(PrintStream(bytes, true, charset))
    return bytes.toString(charset)
  }

  private fun reviewAsk() =
    SelfHostedReviewAsk(properties, billingConfProvider, environment, instanceIdService, currentDateProvider)
}
