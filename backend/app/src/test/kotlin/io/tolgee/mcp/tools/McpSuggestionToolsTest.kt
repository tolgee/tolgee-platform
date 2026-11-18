package io.tolgee.mcp.tools

import io.modelcontextprotocol.client.McpSyncClient
import io.tolgee.AbstractMcpTest
import io.tolgee.development.testDataBuilder.data.McpSuggestionTestData
import io.tolgee.testing.assertions.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class McpSuggestionToolsTest : AbstractMcpTest() {
  lateinit var data: McpSuggestionTestData
  lateinit var client: McpSyncClient

  @BeforeEach
  fun setup() {
    data = McpSuggestionTestData()
    testDataService.saveTestData(data.base.root)
    client = createMcpClientWithPat(data.ownerPat.token!!)
  }

  @AfterEach
  fun cleanup() {
    testDataService.cleanTestData(data.base.root)
  }

  @Test
  fun `list_keys_with_suggestions returns only keys with active suggestions in the language`() {
    val json = callToolAndGetJson(client, "list_keys_with_suggestions", projectArgs("languageTag" to "cs"))
    assertThat(keyNames(json)).isEqualTo(setOf("key 0", "key 2", "key 3", "pluralKey"))
    assertThat(json["totalItems"].asLong()).isEqualTo(4)
  }

  @Test
  fun `list_keys_with_suggestions rejects an unknown language instead of listing every key`() {
    val result = callTool(client, "list_keys_with_suggestions", projectArgs("languageTag" to "de-DE"))
    assertThat(result.isError).isTrue()
  }

  @Test
  fun `list_suggestions returns the suggestions of a key`() {
    val json = callToolAndGetJson(client, "list_suggestions", key0Args())
    assertThat(json["totalItems"].asLong()).isEqualTo(2)
    val first = json["items"][0]
    assertThat(first["state"].asString()).isEqualTo("ACTIVE")
    assertThat(first["translation"].asString()).isEqualTo(data.base.translatorsCzechSuggestion.self.translation)
    assertThat(first["author"]["name"].asString()).isNotEmpty()
  }

  @Test
  fun `list_suggestions filters by state`() {
    val declined = callToolAndGetJson(client, "list_suggestions", key0Args("states" to listOf("DECLINED")))
    assertThat(declined["totalItems"].asLong()).isEqualTo(0)
    val all = callToolAndGetJson(client, "list_suggestions", key0Args("states" to emptyList<String>()))
    assertThat(all["totalItems"].asLong()).isEqualTo(2)
  }

  @Test
  fun `list_suggestions rejects an unknown state name`() {
    val invalid = callTool(client, "list_suggestions", key0Args("states" to listOf("active")))
    assertThat(invalid.isError).isTrue()
  }

  @Test
  fun `list_suggestions reports an unknown key`() {
    val result = callTool(client, "list_suggestions", projectArgs("keyName" to "nope", "languageTag" to "cs"))
    assertThat(result.isError).isTrue()
  }

  @Test
  fun `accept_suggestion sets the translation and declines the others when asked`() {
    val accepted = data.base.translatorsCzechSuggestion.self
    val json =
      callToolAndGetJson(
        client,
        "accept_suggestion",
        key0Args("suggestionId" to accepted.id, "declineOther" to true),
      )
    assertThat(json["accepted"]["state"].asString()).isEqualTo("ACCEPTED")
    assertThat(json["declined"].size()).isEqualTo(1)
    assertThat(json["declined"][0].asLong()).isEqualTo(data.base.reviewersCzechSuggestion.self.id)

    val translations =
      callToolAndGetJson(client, "get_translations", projectArgs("keyName" to "key 0", "languages" to listOf("cs")))
    assertThat(translations["translations"][0]["text"].asString()).isEqualTo(accepted.translation)
  }

  @Test
  fun `accept_suggestion leaves the other suggestions active by default`() {
    val json =
      callToolAndGetJson(
        client,
        "accept_suggestion",
        key0Args("suggestionId" to data.base.reviewersCzechSuggestion.self.id),
      )
    assertThat(json["declined"].size()).isEqualTo(0)
    val active = callToolAndGetJson(client, "list_suggestions", key0Args("states" to listOf("ACTIVE")))
    assertThat(active["totalItems"].asLong()).isEqualTo(1)
    assertThat(active["items"][0]["id"].asLong()).isEqualTo(data.base.translatorsCzechSuggestion.self.id)
  }

  @Test
  fun `decline_suggestion stores the declined state and leaves the others alone`() {
    val declinedId = data.base.reviewersCzechSuggestion.self.id
    val json = callToolAndGetJson(client, "decline_suggestion", key0Args("suggestionId" to declinedId))
    assertThat(json["state"].asString()).isEqualTo("DECLINED")

    val declined = callToolAndGetJson(client, "list_suggestions", key0Args("states" to listOf("DECLINED")))
    assertThat(declined["totalItems"].asLong()).isEqualTo(1)
    assertThat(declined["items"][0]["id"].asLong()).isEqualTo(declinedId)
    val active = callToolAndGetJson(client, "list_suggestions", key0Args("states" to listOf("ACTIVE")))
    assertThat(active["totalItems"].asLong()).isEqualTo(1)
  }

  @Test
  fun `suggest-only PAK can list but not accept`() {
    val suggestOnly = createMcpClientWithPak(data.suggestOnlyApiKey.encodedKey!!)
    val json = callToolAndGetJson(suggestOnly, "list_suggestions", mapOf("keyName" to "key 0", "languageTag" to "cs"))
    assertThat(json["totalItems"].asLong()).isEqualTo(2)
    assertToolFails(
      suggestOnly,
      "accept_suggestion",
      mapOf(
        "keyName" to "key 0",
        "languageTag" to "cs",
        "suggestionId" to data.base.translatorsCzechSuggestion.self.id,
      ),
    )
  }

  @Test
  fun `reviewer limited to Czech cannot moderate English suggestions`() {
    val czechReviewer = createMcpClientWithPat(data.czechReviewerPat.token!!)
    val englishSuggestionId = data.base.czechReviewerEnglishSuggestion.self.id
    val englishArgs = projectArgs("keyName" to "key 3", "languageTag" to "en", "suggestionId" to englishSuggestionId)
    assertToolFails(czechReviewer, "accept_suggestion", englishArgs, "language_not_permitted")
    assertToolFails(czechReviewer, "decline_suggestion", englishArgs, "language_not_permitted")
    assertToolFails(
      czechReviewer,
      "list_suggestions",
      projectArgs("keyName" to "key 3", "languageTag" to "en"),
      "language_not_permitted",
    )
    val czech =
      callToolAndGetJson(
        czechReviewer,
        "decline_suggestion",
        key0Args("suggestionId" to data.base.translatorsCzechSuggestion.self.id),
      )
    assertThat(czech["state"].asString()).isEqualTo("DECLINED")
  }

  private fun projectArgs(vararg more: Pair<String, Any?>) = mapOf("projectId" to data.base.project.id, *more)

  private fun key0Args(vararg more: Pair<String, Any?>) =
    projectArgs("keyName" to "key 0", "languageTag" to "cs", *more)

  private fun keyNames(json: JsonNode) =
    (0 until json["items"].size())
      .map {
        json["items"][it]["keyName"].asString()
      }.toSet()
}
