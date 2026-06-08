package io.tolgee.formats.unity.`in`

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Parses Unity `.asset`/`.meta` text. Unity YAML carries `%TAG` directives and per-document
 * `!u!<classId>` tags that standard YAML parsers reject, so documents are split and those lines are
 * stripped before parsing.
 */
class UnityAssetParser(
  private val yamlMapper: ObjectMapper,
) {
  fun parseMonoBehaviour(content: ByteArray): JsonNode? {
    documents(content).forEach { doc ->
      val node = readTree(doc) ?: return@forEach
      node.get("MonoBehaviour")?.let { return it }
    }
    return null
  }

  fun parseMetaGuid(content: ByteArray): String? {
    documents(content).forEach { doc ->
      readTree(doc)?.get("guid")?.asText()?.let { return it }
    }
    return null
  }

  private fun documents(content: ByteArray): List<String> {
    return String(content, Charsets.UTF_8).split(DOCUMENT_SEPARATOR)
  }

  private fun readTree(document: String): JsonNode? {
    val cleaned =
      document
        .lineSequence()
        .filterNot { it.startsWith("%") }
        .joinToString("\n")
        .trim()
    if (cleaned.isEmpty()) {
      return null
    }
    return try {
      yamlMapper.readTree(cleaned)
    } catch (e: Exception) {
      null
    }
  }

  companion object {
    private val DOCUMENT_SEPARATOR = Regex("(?m)^---.*$")
  }
}
