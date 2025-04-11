package io.tolgee.hateoas.ee.uasge.current

import org.springframework.hateoas.RepresentationModel
import java.io.Serializable

/**
 * Presents the current total usage for a subscription.
 *
 * It doesn't count proportional usage into consideration.
 */
@Suppress("unused")
open class CurrentUsageModel(
  val seats: CurrentUsageItemModel,
  val strings: CurrentUsageItemModel,
  val creditsInCents: CurrentUsageItemModel,
  val keys: CurrentUsageItemModel,
) : RepresentationModel<CurrentUsageModel>(), Serializable
