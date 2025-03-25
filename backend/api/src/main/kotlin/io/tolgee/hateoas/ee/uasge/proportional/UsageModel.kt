<<<<<<<< HEAD:backend/api/src/main/kotlin/io/tolgee/hateoas/ee/uasge/proportional/UsageModel.kt
<<<<<<<< HEAD:backend/api/src/main/kotlin/io/tolgee/hateoas/ee/uasge/proportional/UsageModel.kt
package io.tolgee.hateoas.ee.uasge.proportional
========
package io.tolgee.ee.api.v2.hateoas.model.uasge
>>>>>>>> 795c0acf1 (feat: wip: initial work on glossaries - db schema, ui prototyping, refractoring;):ee/backend/app/src/main/kotlin/io/tolgee/ee/api/v2/hateoas/model/uasge/UsageModel.kt
========
package io.tolgee.hateoas.ee.uasge
>>>>>>>> 1a95c2a89 (revert some of the refractoring):backend/api/src/main/kotlin/io/tolgee/hateoas/ee/uasge/UsageModel.kt

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import java.io.Serializable
import java.math.BigDecimal

/**
 * Presents usage to show usage for invoices or to show current expected usage.
 *
 * Is suitable for cases, where proportional usage is required.
 */
@Suppress("unused")
open class UsageModel(
  val subscriptionPrice: BigDecimal? = 0.toBigDecimal(),
  @Schema(
    description =
      "Relevant for invoices only. When there are " +
        "applied stripe credits, we need to reduce the total price by this amount.",
  )
  val appliedStripeCredits: BigDecimal? = null,
  val seats: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val translations: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val credits: SumUsageItemModel?,
  val keys: AverageProportionalUsageItemModel = AverageProportionalUsageItemModel(),
  val total: BigDecimal = 0.toBigDecimal(),
) : RepresentationModel<UsageModel>(), Serializable
