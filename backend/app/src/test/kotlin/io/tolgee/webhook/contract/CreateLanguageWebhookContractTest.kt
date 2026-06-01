package io.tolgee.webhook.contract

import io.tolgee.ProjectAuthControllerTest
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
 * Drives a real language creation and verifies the captured webhook payload
 * conforms to the OpenAPI spec's `webhooks.CREATE_LANGUAGE` contract.
 */
@SpringBootTest
class CreateLanguageWebhookContractTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `payload conforms to CREATE_LANGUAGE schema`() {
    triggerCreateLanguage()
    val payload = fixture.waitForWebhookWithType("CREATE_LANGUAGE")
    fixture.assertConformsTo(payload, "CREATE_LANGUAGE")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `modifiedEntities contains only Language entries`() {
    triggerCreateLanguage()
    val payload = fixture.waitForWebhookWithType("CREATE_LANGUAGE")
    val keys =
      payload
        .path("activityData")
        .path("modifiedEntities")
        .fieldNames()
        .asSequence()
        .toSet()
    assertThat(keys).containsExactly("Language")
  }

  private fun triggerCreateLanguage() {
    performProjectAuthPost(
      "languages",
      mapOf("tag" to "fr", "name" to "French", "originalName" to "Français"),
    ).andIsOk
  }
}
