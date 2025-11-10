package io.tolgee.openapi

import io.tolgee.fixtures.andAssertThatJson
import io.tolgee.fixtures.andIsOk
import io.tolgee.fixtures.andPrettyPrint
import io.tolgee.testing.AbstractControllerTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiTest : AbstractControllerTest() {
  @Test
  fun `accessible with api key is generated`() {
    performGet("/v3/api-docs/Accessible with Project API key (All)").andIsOk.andAssertThatJson {
      node("paths./v2/projects/keys/{keyId}/tags.put.summary")
        .isEqualTo("Tag key")
    }
  }

  @Test
  fun `all internal is generated`() {
    performGet("/v3/api-docs/All Internal - for Tolgee Web application")
      .andIsOk.andPrettyPrint
      .andAssertThatJson {
        node("paths./v2/projects/{projectId}/keys/{keyId}/tags.put.summary")
          .isEqualTo("Tag key")
      }
  }

  @Test
  fun `internal api doesn't contain API key endpoints`() {
    performGet("/v3/api-docs/V2 Internal - for Tolgee Web application")
      .andIsOk.andPrettyPrint
      .andAssertThatJson {
        node("paths").isObject.doesNotContainKey("/v2/projects/languages")
      }
  }

  @Test
  fun `accessible with api key has languages endpoint`() {
    performGet("/v3/api-docs/Accessible with Project API key (All)").andIsOk.andAssertThatJson {
      node("paths").isObject.containsKey("/v2/projects/languages")
    }
  }

  @Test
  fun `all public works`() {
    performGet("/v3/api-docs/Public API (All)").andIsOk.andAssertThatJson {
      node("paths")
        .isObject
        .doesNotContainKey("/v2/projects/languages")
        .containsKey("/v2/projects/{projectId}/languages")
    }
  }
}
