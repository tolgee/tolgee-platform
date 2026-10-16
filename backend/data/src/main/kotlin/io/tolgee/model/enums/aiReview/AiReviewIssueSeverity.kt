package io.tolgee.model.enums.aiReview

enum class AiReviewIssueSeverity(
  val minPoints: Int,
  val maxPoints: Int,
) {
  CRITICAL(75, 100),
  MAJOR(25, 50),
  MINOR(5, 15),
  ;

  fun clampPoints(points: Int): Int = points.coerceIn(minPoints, maxPoints)
}
