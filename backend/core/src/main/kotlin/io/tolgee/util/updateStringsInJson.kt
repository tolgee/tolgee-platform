package io.tolgee.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.TextNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun updateStringsInJson(
  node: JsonNode?,
  fn: (t: String) -> String,
): JsonNode? {
  val mapper = jacksonObjectMapper()
  if (node == null) return null

  return when {
    node.isObject -> {
      val objectNode = mapper.createObjectNode()
      node.fields().forEach { (key, value) ->
        objectNode.set<JsonNode>(key, updateStringsInJson(value, fn))
      }
      objectNode
    }

    node.isArray -> {
      val arrayNode = mapper.createArrayNode()
      node.elements().forEach { element ->
        arrayNode.add(updateStringsInJson(element, fn))
      }
      arrayNode
    }

    node.isTextual -> {
      TextNode(fn(node.asText()))
    }

    else -> node // Return unchanged for non-string nodes (e.g., numbers, booleans, null)
  }
}
