package io.tolgee.batch

enum class JobCharacter(
  /**
   * How many threads can be used for jobs with this character
   * When other jobs witch other characters are in queue
   */
  val maxConcurrencyRatio: Double,
) {
  SLOW(0.2),
  FAST(0.8),
}
