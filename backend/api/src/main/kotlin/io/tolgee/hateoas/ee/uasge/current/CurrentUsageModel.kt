package io.tolgee.hateoas.ee.uasge.current

import io.swagger.v3.oas.annotations.media.Schema
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
  @Schema(description = "For MT credits, the values are in full credits. Not Cents.")
  val credits: CurrentUsageItemModel,
  val keys: CurrentUsageItemModel,
  val isPayAsYouGo: Boolean,
) : RepresentationModel<CurrentUsageModel>(),
  Serializable
