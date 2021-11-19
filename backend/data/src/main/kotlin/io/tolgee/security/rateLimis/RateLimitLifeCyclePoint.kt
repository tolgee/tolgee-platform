package io.tolgee.security.rateLimis

enum class RateLimitLifeCyclePoint {
  /**
   * Execute the Limit as soon as possible, so no additional
   * DB queries will be executed
   */
  ENTRY,

  /**
   * When user is logged in, so we can use the user
   * information to apply user-specific rates
   */
  AFTER_AUTHORIZATION,
}
