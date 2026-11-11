package io.tolgee.ee.service.aiReview.scoring

object DefaultSeverityRanges {
  val CRITICAL = 75..100
  val MAJOR = 25..50
  val MINOR = 5..15

  private const val UNKNOWN_SEVERITY_POINTS = 100

  fun fallbackPoints(severity: String): Int =
    when (severity.lowercase()) {
      "critical" -> CRITICAL.last
      "major" -> MAJOR.last
      "minor" -> MINOR.last
      else -> UNKNOWN_SEVERITY_POINTS
    }
}
