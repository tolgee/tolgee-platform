package io.tolgee.util

import tools.jackson.databind.JsonNode

/**
 * The text of [field], or null when it is absent or not a value node. `asString` throws on a container node in
 * Jackson 3, so a caller reading untrusted JSON through it directly turns a crafted body into a 500.
 */
fun JsonNode.textOrNull(field: String): String? {
  val value = get(field) ?: return null
  if (!value.isValueNode) return null
  return value.asString()
}
