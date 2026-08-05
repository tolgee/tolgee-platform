package io.tolgee.util

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.StringNode
import tools.jackson.module.kotlin.jacksonObjectMapper

fun updateStringsInJson(
  node: JsonNode?,
  fn: (t: String) -> String,
): JsonNode? {
  val mapper = jacksonObjectMapper()
  if (node == null) return null

  return when {
    node.isObject -> {
      val objectNode = mapper.createObjectNode()
      node.properties().forEach { (key, value) ->
        objectNode.set(key, updateStringsInJson(value, fn))
      }
      objectNode
    }

    node.isArray -> {
      val arrayNode = mapper.createArrayNode()
      node.values().forEach { element ->
        arrayNode.add(updateStringsInJson(element, fn))
      }
      arrayNode
    }

    node.isTextual -> {
      StringNode(fn(node.asText()))
    }

    else -> node // Return unchanged for non-string nodes (e.g., numbers, booleans, null)
  }
}
