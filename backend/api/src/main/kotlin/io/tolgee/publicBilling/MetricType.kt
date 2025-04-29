package io.tolgee.publicBilling

enum class MetricType(
  val useSeats: Boolean,
  val useKeys: Boolean,
  val useStrings: Boolean,
) {
  /**
   * Non-free plans, where user cay be charged by their usage
   */
  KEYS_SEATS(useSeats = true, useKeys = true, useStrings = false),
  STRINGS(useSeats = false, useKeys = false, useStrings = true),
}
