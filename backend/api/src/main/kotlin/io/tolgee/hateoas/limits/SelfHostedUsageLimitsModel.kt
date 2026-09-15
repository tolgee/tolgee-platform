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
  // A licence server older than this instance omits the field; unlimited is the safe absence
  // value, since the cloud still enforces at renewal.
  val words: LimitModel = LimitModel(-1, -1),
  /** What the instance enforces on: the customer's setting AND eligible AND a higher tier exists. */
  val autoUpgradeEffective: Boolean? = null,
) : RepresentationModel<SelfHostedUsageLimitsModel>()
