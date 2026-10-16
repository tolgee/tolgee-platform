package io.tolgee.constants

enum class FeatureFlag {
  AI_REVIEW,
  ;

  companion object {
    fun findByName(name: String): FeatureFlag? {
      return entries.find { it.name == name }
    }
  }
}
