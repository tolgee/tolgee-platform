package io.tolgee.hateoas.initialData

import io.tolgee.api.SubscriptionStatus

/**
 * This model is used by Tolgee self-hosted instance to present data about
 * the Self-hosted EE subscription as part of initial data.
 *
 * Don't add any sensitive information here! This is publicly visible.
 */
data class InitialDataEeSubscriptionModel(
  val status: SubscriptionStatus,
)
