package io.tolgee.unit.apps

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.apps.AppManifestDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.service.apps.AppIconResolver
import io.tolgee.service.apps.AppManifestValidator
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

class AppManifestValidatorTest {
  @AfterEach
  fun cleanup() {
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun `accepts a valid manifest`() {
    assertDoesNotThrow { validator().validate(manifest()) }
  }

  @Test
  fun `collects every content error into one rejection`() {
    val exception =
      assertThrows<BadRequestException> {
        validator().validate(
          manifest(
            name = "",
            scopes = """["not-a-scope"]""",
            entry = "https://other.example.com/page",
          ),
        )
      }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_INVALID.code)
    exception.params!!.assert.contains(
      "name must not be blank",
      "unknown scope: not-a-scope",
      "project-dashboard-page 'home' entry must stay on the app's own origin",
    )
  }

  @Test
  fun `rejects an unsupported top-level feature and module together`() {
    val json =
      """
      {
        "id": "a", "name": "A", "version": "1", "baseUrl": "https://app.example.com",
        "decoratorsUrl": "https://app.example.com/d",
        "modules": {
          "project-dashboard-page": [{"key": "home", "title": "Home", "icon": "x", "entry": "/"}],
          "key-action": []
        }
      }
      """.trimIndent()
    val exception = assertThrows<BadRequestException> { validator().validate(parse(json)) }
    exception.params!!.assert.contains("unsupported manifest features: decoratorsUrl, key-action")
  }

  @Test
  fun `rejects a duplicate dashboard page key`() {
    val json =
      """
      {
        "id": "a", "name": "A", "version": "1", "baseUrl": "https://app.example.com",
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "x", "entry": "/"},
            {"key": "home", "title": "Again", "icon": "x", "entry": "/again"}
          ]
        }
      }
      """.trimIndent()
    val exception = assertThrows<BadRequestException> { validator().validate(parse(json)) }
    exception.params!!.assert.contains("duplicate project-dashboard-page key 'home'")
  }

  @Test
  fun `an absolute entry on the app's own origin is accepted`() {
    assertDoesNotThrow {
      validator().validate(manifest(entry = "https://app.example.com/deep/page"))
    }
  }

  @Test
  fun `rejects a manifest served from Tolgee's configured origin with its own code`() {
    val properties = TolgeeProperties().apply { frontEndUrl = "https://app.example.com" }
    val exception =
      assertThrows<BadRequestException> { validator(properties).validate(manifest()) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_SAME_ORIGIN_AS_TOLGEE.code)
  }

  @Test
  fun `rejects Tolgee's origin matched only after normalization`() {
    val properties = TolgeeProperties().apply { frontEndUrl = "https://APP.example.com:443/tolgee" }
    val exception =
      assertThrows<BadRequestException> { validator(properties).validate(manifest()) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_SAME_ORIGIN_AS_TOLGEE.code)
  }

  @Test
  fun `with no Tolgee URL configured, the current request origin stands in`() {
    val request = MockHttpServletRequest()
    request.scheme = "https"
    request.serverName = "app.example.com"
    request.serverPort = 443
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

    val exception =
      assertThrows<BadRequestException> { validator().validate(manifest()) }
    exception.code.assert.isEqualTo(Message.APP_MANIFEST_SAME_ORIGIN_AS_TOLGEE.code)
  }

  @Test
  fun `an app on another origin passes with the request origin present`() {
    val request = MockHttpServletRequest()
    request.scheme = "https"
    request.serverName = "tolgee.example.com"
    request.serverPort = 443
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))

    assertDoesNotThrow { validator().validate(manifest()) }
  }

  private fun validator(properties: TolgeeProperties = TolgeeProperties()): AppManifestValidator =
    AppManifestValidator(properties, AppIconResolver())

  private fun manifest(
    name: String = "Test App",
    scopes: String = """["translations.view"]""",
    entry: String = "/",
  ): AppManifestDto =
    parse(
      """
      {
        "id": "test-app",
        "name": "$name",
        "version": "0.1.0",
        "baseUrl": "https://app.example.com",
        "scopes": $scopes,
        "modules": {
          "project-dashboard-page": [
            {"key": "home", "title": "Home", "icon": "🏠", "entry": "$entry"}
          ]
        }
      }
      """.trimIndent(),
    )

  private fun parse(json: String): AppManifestDto = jacksonObjectMapper().readValue(json)
}
