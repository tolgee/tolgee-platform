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
 * Drives a real language deletion and verifies the captured webhook payload
 * conforms to the OpenAPI spec's `webhooks.DELETE_LANGUAGE` contract. This event
 * has `restrictEntitiesInList = [Language::class]`, so even though deleting a
 * language cascades to translations in the DB, the payload's modifiedEntities
 * must only carry Language entries.
 */
@SpringBootTest
class DeleteLanguageWebhookContractTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `payload conforms to DELETE_LANGUAGE schema`() {
    val deletedId = triggerDeleteLanguage()
    val payload = fixture.waitForWebhookWithType("DELETE_LANGUAGE")
    fixture.assertConformsTo(payload, "DELETE_LANGUAGE")
    assertThat(deletedId).isPositive
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `modifiedEntities contains only Language despite cascade`() {
    triggerDeleteLanguage()
    val payload = fixture.waitForWebhookWithType("DELETE_LANGUAGE")
    val keys =
      payload
        .path("activityData")
        .path("modifiedEntities")
        .fieldNames()
        .asSequence()
        .toSet()
    assertThat(keys).containsExactly("Language")
  }

  private fun triggerDeleteLanguage(): Long {
    // Create a fresh language so deletion doesn't break the default English base.
    val response =
      performProjectAuthPost(
        "languages",
        mapOf("tag" to "de", "name" to "German", "originalName" to "Deutsch"),
      ).andIsOk.andReturn()
    val langId =
      com.fasterxml.jackson.databind
        .ObjectMapper()
        .readTree(response.response.contentAsString)
        .path("id")
        .asLong()
    performProjectAuthDelete("languages/$langId", null).andIsOk
    return langId
  }
}
