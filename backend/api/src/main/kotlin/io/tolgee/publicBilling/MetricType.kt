package io.tolgee.publicBilling

enum class MetricType(
  val useSeats: Boolean,
  val useKeys: Boolean,
  val useStrings: Boolean,
  val useWords: Boolean,
) {
  /**
   * Non-free plans, where user cay be charged by their usage
   */
  KEYS_SEATS(useSeats = true, useKeys = true, useStrings = false, useWords = false),
  STRINGS(useSeats = false, useKeys = false, useStrings = true, useWords = false),
  HOSTED_WORDS(useSeats = false, useKeys = false, useStrings = false, useWords = true),
}
