package io.tolgee.mcp

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.development.testDataBuilder.data.LanguagePermissionsTestData
import io.tolgee.model.ApiKey
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.Scope
import io.tolgee.testing.assert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * MCP tools must enforce the same per-language permissions as their REST twins.
 * See GHSA-j998-54vh-rwpg.
 */
class McpLanguagePermissionsTest : AbstractMcpTest() {
  private lateinit var testData: LanguagePermissionsTestData

  private fun clientFor(
    user: LanguagePermissionsTestData.() -> UserAccount,
    vararg scopes: Scope,
  ): McpSyncClient {
    var apiKey: ApiKey? = null
    testData.projectBuilder.addApiKey {
      key = "language_permissions_test_pak"
      userAccount = testData.user()
      scopesEnum = scopes.toMutableSet()
      apiKey = this
    }
    testDataService.saveTestData(testData.root)
    return createMcpClientWithPak(apiKey!!.encodedKey!!)
  }

  @BeforeEach
  fun setup() {
    testData = LanguagePermissionsTestData()
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(testData.root)
  }

  @Test
  fun `set_translation denies languages outside the user's translate languages`() {
    val client = clientFor({ translateEnOnlyUser }, Scope.TRANSLATIONS_EDIT)

    assertToolFails(
      client,
      "set_translation",
      mapOf("keyName" to "key", "translations" to mapOf("de" to "overwritten")),
      expectedError = "language_not_permitted",
    )
  }

  @Test
  fun `get_translations returns only languages the user may view`() {
    val client = clientFor({ viewEnOnlyUser }, Scope.TRANSLATIONS_VIEW)

    val json = callToolAndGetJson(client, "get_translations", mapOf("keyName" to "key"))

    val translations = json["translations"]
    val returnedTags = (0 until translations.size()).map { translations[it]["languageTag"].asString() }
    returnedTags.assert.containsExactly("en")
  }

  @Test
  fun `get_translations silently drops an explicitly requested non-viewable language`() {
    val client = clientFor({ viewEnOnlyUser }, Scope.TRANSLATIONS_VIEW)

    val json =
      callToolAndGetJson(
        client,
        "get_translations",
        mapOf("keyName" to "key", "languages" to listOf("de")),
      )

    json["translations"].size().assert.isZero()
  }

  @Test
  fun `search_keys denies searching in a language the user may not view`() {
    val client = clientFor({ viewEnOnlyUser }, Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW)

    assertToolFails(
      client,
      "search_keys",
      mapOf("query" to "key", "languageTag" to "de"),
      expectedError = "language_not_permitted",
    )
  }

  @Test
  fun `search_keys allows searching in a viewable language`() {
    val client = clientFor({ viewEnOnlyUser }, Scope.KEYS_VIEW, Scope.TRANSLATIONS_VIEW)

    assertToolSucceeds(client, "search_keys", mapOf("query" to "key", "languageTag" to "en"))
  }

  @Test
  fun `set_translation allows a language the user may translate`() {
    val client = clientFor({ translateEnOnlyUser }, Scope.TRANSLATIONS_EDIT)

    assertToolSucceeds(
      client,
      "set_translation",
      mapOf("keyName" to "key", "translations" to mapOf("en" to "updated")),
    )
  }
}
