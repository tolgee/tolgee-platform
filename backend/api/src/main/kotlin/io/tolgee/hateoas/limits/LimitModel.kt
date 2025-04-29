package io.tolgee.hateoas.limits

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel

/**
 * Presents how much is of a metered unit (e.g. keys, seats, MT credits) customer
 *  - has included in the plan
 *  - can consume before they reach their spending limit
 */
class LimitModel(
  @Schema(
    description =
      "What's included in the plan." +
        "\n\n" +
        "-1 if unlimited",
  )
  val included: Long,
  @Schema(
    description =
      "What's the maximum value before using all the usage from spending limit." +
        "\n\n" +
        "-1 if unlimited",
  )
  val limit: Long,
) : RepresentationModel<LimitModel>()
