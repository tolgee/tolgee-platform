package io.tolgee.hateoas.limits

import org.springframework.hateoas.RepresentationModel

/**
 * Presents the limits for self-hosted instance.
 * For each limit presents
 *  - what's included in plan (included)
 *  - how much they can spend before reaching the spending limit (limit)
 */
class SelfHostedUsageLimitsModel(
  val keys: LimitModel,
  val seats: LimitModel,
  val mtCreditsInCents: LimitModel,
) : RepresentationModel<SelfHostedUsageLimitsModel>()
