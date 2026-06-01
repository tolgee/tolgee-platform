package io.tolgee.webhook.contract

import io.tolgee.ProjectAuthControllerTest
import io.tolgee.fixtures.andIsCreated
import io.tolgee.fixtures.andIsOk
import io.tolgee.testing.annotations.ProjectJWTAuthTestMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.client.RestTemplate

/**
 * Drives a real translation update and verifies the captured webhook payload
 * conforms to the OpenAPI spec's `webhooks.SET_TRANSLATIONS` contract.
 */
@SpringBootTest
class SetTranslationsWebhookContractTest : ProjectAuthControllerTest("/v2/projects/") {
  @MockitoBean
  @Qualifier("webhookRestTemplate")
  lateinit var webhookRestTemplate: RestTemplate

  @Autowired
  lateinit var fixture: WebhookContractFixture

  @BeforeEach
  fun setup() {
    val data = fixture.install()
    userAccount = data.user
    projectSupplier = { data.projectBuilder.self }
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `payload conforms to SET_TRANSLATIONS schema`() {
    triggerSetTranslations()
    val payload = fixture.waitForWebhookWithType("SET_TRANSLATIONS")
    fixture.assertConformsTo(payload, "SET_TRANSLATIONS")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `modifiedEntities contains only Translation entries`() {
    triggerSetTranslations()
    val payload = fixture.waitForWebhookWithType("SET_TRANSLATIONS")
    val keys =
      payload
        .path("activityData")
        .path("modifiedEntities")
        .fieldNames()
        .asSequence()
        .toSet()
    assertThat(keys).containsExactly("Translation")
  }

  private fun triggerSetTranslations() {
    // Create the key first so the subsequent /translations call only triggers
    // SET_TRANSLATIONS (touching just Translation), not a key+translation create.
    performProjectAuthPost("keys", mapOf("name" to "k1")).andIsCreated
    performProjectAuthPost(
      "translations",
      mapOf(
        "key" to "k1",
        "translations" to mapOf("en" to "hello-world-${System.nanoTime()}"),
      ),
    ).andIsOk
  }
}
