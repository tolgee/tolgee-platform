package io.tolgee.webhook.contract

import com.fasterxml.jackson.databind.ObjectMapper
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
 * Drives a real key rename and verifies the captured webhook payload conforms
 * to the OpenAPI spec's `webhooks.KEY_NAME_EDIT` contract.
 */
@SpringBootTest
class KeyNameEditWebhookContractTest : ProjectAuthControllerTest("/v2/projects/") {
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
  fun `payload conforms to KEY_NAME_EDIT schema`() {
    triggerKeyNameEdit()
    val payload = fixture.waitForWebhookWithType("KEY_NAME_EDIT")
    fixture.assertConformsTo(payload, "KEY_NAME_EDIT")
  }

  @Test
  @ProjectJWTAuthTestMethod
  fun `modifications include the name diff`() {
    triggerKeyNameEdit()
    val payload = fixture.waitForWebhookWithType("KEY_NAME_EDIT")
    val keyEntry =
      payload
        .path("activityData")
        .path("modifiedEntities")
        .path("Key")
        .get(0)
    val nameMod = keyEntry.path("modifications").path("name")
    assertThat(nameMod.path("old").asText()).isEqualTo("k1")
    assertThat(nameMod.path("new").asText()).isEqualTo("renamed")
  }

  private fun triggerKeyNameEdit() {
    val keyId = createKey("k1")
    performProjectAuthPut("keys/$keyId", mapOf("name" to "renamed")).andIsOk
  }

  private fun createKey(name: String): Long {
    val response = performProjectAuthPost("keys", mapOf("name" to name)).andIsCreated.andReturn()
    return ObjectMapper().readTree(response.response.contentAsString).path("id").asLong()
  }
}
