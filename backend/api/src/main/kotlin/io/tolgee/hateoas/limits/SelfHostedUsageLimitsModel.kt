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
  // Defaulted so an instance upgraded before the cloud side still deserializes the licence
  // response: without it a missing `words` fails the whole model, not just the word limits.
  // Unlimited is the safe absence value — the cloud enforces at renewal regardless.
  val words: LimitModel = LimitModel(-1, -1),
  val autoUpgradeEnabled: Boolean? = null,
) : RepresentationModel<SelfHostedUsageLimitsModel>()
