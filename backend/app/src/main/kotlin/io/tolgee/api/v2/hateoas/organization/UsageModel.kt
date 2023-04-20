package io.tolgee.api.v2.hateoas.organization

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class UsageModel(
  val organizationId: Long,

  @Schema(description = "Current balance of standard credits. Standard credits are refilled every month.")
  val creditBalance: Long,

  @Schema(
    description = """How many credits are included in your current plan."""
  )
  val includedMtCredits: Long,

  @Schema(description = "Date when credits were refilled. (In epoch format.)")
  val creditBalanceRefilledAt: Long,

  @Schema(description = "Date when credits will be refilled. (In epoch format.)")
  val creditBalanceNextRefillAt: Long,

  @Schema(
    description = "Extra credits, which are neither refilled nor reset every month. These credits are " +
      "used when there are no standard credits."
  )
  val extraCreditBalance: Long,

  @Schema(
    description = """How many translations can be stored within your organization."""
  )
  val translationSlotsLimit: Long,

  @Schema(
    description = """How many translations can organization use without additional costs."""
  )
  val planTranslations: Long,

  @Schema(
    description = """How many translations are currently stored within your organization."""
  )
  val currentTranslationSlots: Long,
  val currentTranslations: Long,
  val translationsLimit: Long,
) : RepresentationModel<UsageModel>(), Serializable
