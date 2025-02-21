package io.tolgee.hateoas.organization

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class PublicUsageModel(
  val organizationId: Long,
  @Schema(description = "Current balance of standard credits. Standard credits are refilled every month")
  val creditBalance: Long,
  @Schema(
    description = """How many credits are included in your current plan""",
  )
  val includedMtCredits: Long,
  @Schema(description = "Date when credits were refilled. (In epoch format)")
  val creditBalanceRefilledAt: Long,
  @Schema(description = "Date when credits will be refilled. (In epoch format)")
  val creditBalanceNextRefillAt: Long,
  @Schema(description = "Currently used credits over credits included in plan and extra credits")
  val currentPayAsYouGoMtCredits: Long,
  @Schema(
    description =
      "The maximum amount organization can spend" +
        " on MT credit usage before they reach the spending limit",
  )
  val availablePayAsYouGoMtCredits: Long,
  @Schema(
    description = """How many translations can be stored within your organization""",
  )
  val translationSlotsLimit: Long,
  @Schema(
    description =
      "How many translation slots are included in current subscription plan. " +
        "How many translation slots can organization use without additional costs",
  )
  val includedTranslationSlots: Long,
  @Schema(
    description =
      "How many translations are included in current subscription plan. " +
        "How many translations can organization use without additional costs",
  )
  val includedTranslations: Long,
  @Schema(
    description = """How many translations slots are currently used by organization""",
  )
  val currentTranslationSlots: Long,
  @Schema(
    description = """How many non-empty translations are currently stored by organization""",
  )
  val currentTranslations: Long,
  @Schema(
    description =
      "How many translations can be stored until reaching the limit. " +
        "(For pay us you go, the top limit is the spending limit)",
  )
  val translationsLimit: Long,
) : RepresentationModel<PublicUsageModel>(), Serializable {
  @Schema(
    deprecated = true,
    description =
      "Customers were able to buy extra credits separately in the past.\n\n" +
        "This option is not available anymore and this field is kept only for " +
        "backward compatibility purposes and is always 0.",
  )
  val extraCreditBalance: Long = 0
}
