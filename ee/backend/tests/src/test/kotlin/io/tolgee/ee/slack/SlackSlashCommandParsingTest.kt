package io.tolgee.ee.slack

import io.tolgee.ee.api.v2.controllers.slack.SlackSlashCommandController.Companion.parseCommand
import io.tolgee.ee.api.v2.controllers.slack.SlackSlashCommandController.Companion.parseOptions
import io.tolgee.testing.assert
import org.junit.jupiter.api.Test

class SlackSlashCommandParsingTest {
  @Test
  fun `parses unsubscribe arguments`() {
    parseCommandParts("unsubscribe 1").assert.isEqualTo(listOf("unsubscribe", "1", "", ""))
    parseCommandParts("unsubscribe 1 fr").assert.isEqualTo(listOf("unsubscribe", "1", "fr", ""))
    parseCommandParts("unsubscribe 1 zh_Hans").assert.isEqualTo(listOf("unsubscribe", "1", "zh_Hans", ""))
  }

  @Test
  fun `rejects unsubscribe with language tag glued to project id`() {
    parseCommandParts("unsubscribe 1fr").assert.isNull()
  }

  @Test
  fun `rejects unsubscribe with text after its arguments`() {
    parseCommandParts("unsubscribe 1 fr --on new_key").assert.isNull()
    parseCommandParts("unsubscribe 1 fr de").assert.isNull()
    parseCommandParts("unsubscribe 1 --on new_key").assert.isNull()
    parseCommandParts("unsubscribe 1 fr,de").assert.isNull()
  }

  @Test
  fun `parses language tag with underscore`() {
    parseCommandParts("subscribe 1 zh_Hans").assert.isEqualTo(listOf("subscribe", "1", "zh_Hans", ""))
  }

  @Test
  fun `parses language tag with hyphen`() {
    parseCommandParts(
      "subscribe 1 pt-BR --on new_key",
    ).assert.isEqualTo(listOf("subscribe", "1", "pt-BR", "--on new_key"))
  }

  @Test
  fun `parses options without language tag`() {
    parseCommandParts("subscribe 1 --on new_key").assert.isEqualTo(listOf("subscribe", "1", "", "--on new_key"))
  }

  @Test
  fun `parses two options`() {
    parseOptions("--on translation_changed --global false").assert.isEqualTo(
      mapOf("--on" to "translation_changed", "--global" to "false"),
    )
  }

  @Test
  fun `parses two options in reverse order`() {
    parseOptions("--global false --on new_key, base_changed").assert.isEqualTo(
      mapOf("--global" to "false", "--on" to "new_key, base_changed"),
    )
  }

  @Test
  fun `trims trailing whitespace`() {
    parseOptions("--global false  ").assert.isEqualTo(mapOf("--global" to "false"))
  }

  @Test
  fun `parses no options`() {
    parseOptions("").assert.isEqualTo(emptyMap<String, String>())
  }

  @Test
  fun `rejects option without value`() {
    parseOptions("--on new_key --global").assert.isNull()
  }

  @Test
  fun `rejects repeated option`() {
    parseOptions("--on new_key --on base_changed").assert.isNull()
  }

  @Test
  fun `rejects option followed by another option`() {
    parseOptions("--on --global false").assert.isNull()
  }

  private fun parseCommandParts(text: String): List<String>? = parseCommand(text)?.toList()
}
