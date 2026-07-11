package io.tolgee.formats

fun MatchGroupCollection.getGroupOrNull(name: String): MatchGroup? {
  try {
    return this[name]
  } catch (e: IllegalArgumentException) {
    if (e.message?.contains("No group with name") != true) {
      throw e
    }
    return null
  }
}
