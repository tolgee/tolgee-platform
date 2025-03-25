<<<<<<<< HEAD:backend/api/src/main/kotlin/io/tolgee/hateoas/ee/uasge/proportional/AverageProportionalUsageItemModel.kt
<<<<<<<< HEAD:backend/api/src/main/kotlin/io/tolgee/hateoas/ee/uasge/proportional/AverageProportionalUsageItemModel.kt
package io.tolgee.hateoas.ee.uasge.proportional
========
package io.tolgee.ee.api.v2.hateoas.model.uasge
>>>>>>>> 795c0acf1 (feat: wip: initial work on glossaries - db schema, ui prototyping, refractoring;):ee/backend/app/src/main/kotlin/io/tolgee/ee/api/v2/hateoas/model/uasge/AverageProportionalUsageItemModel.kt
========
package io.tolgee.hateoas.ee.uasge
>>>>>>>> 1a95c2a89 (revert some of the refractoring):backend/api/src/main/kotlin/io/tolgee/hateoas/ee/uasge/AverageProportionalUsageItemModel.kt

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "invoices", itemRelation = "invoice")
open class AverageProportionalUsageItemModel(
  val total: BigDecimal = 0.toBigDecimal(),
  val unusedQuantity: BigDecimal = 0.toBigDecimal(),
  val usedQuantity: BigDecimal = 0.toBigDecimal(),
  val usedQuantityOverPlan: BigDecimal = 0.toBigDecimal(),
) : RepresentationModel<AverageProportionalUsageItemModel>(), Serializable
