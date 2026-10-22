package io.tolgee.ee.service.aiReview.matching

object BranchPattern {
  fun matches(
    pattern: String,
    name: String,
  ): Boolean = toRegex(pattern).matches(name)

  private fun toRegex(pattern: String): Regex =
    pattern
      .split("*")
      .joinToString(".*") { Regex.escape(it) }
      .toRegex()
}
