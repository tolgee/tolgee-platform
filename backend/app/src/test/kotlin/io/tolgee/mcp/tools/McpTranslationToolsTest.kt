package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpTranslationToolsTest : AbstractMcpTest() {
  lateinit var data: McpTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = createTestDataWithPat()
    client = createMcpClient(data.pat.token!!)

    // Create a key with a translation for testing
    callTool(
      client,
      "create_keys",
      mapOf(
        "projectId" to data.projectId,
        "keys" to listOf(mapOf("name" to "greeting", "translations" to mapOf("en" to "Hello"))),
      ),
    )
  }

  @Test
  fun `get_translations returns translations`() {
    val json =
      callToolAndGetJson(
        client,
        "get_translations",
        mapOf("projectId" to data.projectId, "keyName" to "greeting"),
      )
    assertThat(json).isNotNull()
    assertThat(json.has("keyName")).isTrue()
    assertThat(json["keyName"].asText()).isEqualTo("greeting")
    assertThat(json.has("translations")).isTrue()
    val translations = json["translations"]
    assertThat(translations.isArray).isTrue()
    val enTranslation =
      (0 until translations.size())
        .map { translations[it] }
        .find { it["languageTag"].asText() == "en" }
    assertThat(enTranslation).isNotNull()
    assertThat(enTranslation!!["text"].asText()).isEqualTo("Hello")
  }

  @Test
  fun `set_translation updates translation text`() {
    callTool(
      client,
      "set_translation",
      mapOf(
        "projectId" to data.projectId,
        "keyName" to "greeting",
        "translations" to mapOf("en" to "Hi there"),
      ),
    )

    val key = keyService.find(data.projectId, "greeting", null)!!
    val en = languageService.findByTag("en", data.projectId)!!
    val translation = translationService.find(key, en).orElse(null)
    assertThat(translation).isNotNull()
    assertThat(translation.text).isEqualTo("Hi there")
  }

  @Test
  fun `get_translations with language filter`() {
    // Create a second language and translation
    callTool(
      client,
      "create_language",
      mapOf("projectId" to data.projectId, "name" to "German", "tag" to "de"),
    )
    callTool(
      client,
      "set_translation",
      mapOf(
        "projectId" to data.projectId,
        "keyName" to "greeting",
        "translations" to mapOf("de" to "Hallo"),
      ),
    )

    // Request only German translations
    val json =
      callToolAndGetJson(
        client,
        "get_translations",
        mapOf("projectId" to data.projectId, "keyName" to "greeting", "languages" to listOf("de")),
      )
    assertThat(json).isNotNull()
    assertThat(json.has("translations")).isTrue()
    val translations = json["translations"]
    val languageTags = (0 until translations.size()).map { translations[it]["languageTag"].asText() }
    assertThat(languageTags).contains("de")
    assertThat(languageTags).doesNotContain("en")
  }
}
