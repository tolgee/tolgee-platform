<<<<<<<< HEAD:backend/api/src/main/kotlin/io/tolgee/hateoas/ee/uasge/proportional/SumUsageItemModel.kt
package io.tolgee.hateoas.ee.uasge.proportional
========
package io.tolgee.ee.api.v2.hateoas.model.uasge
>>>>>>>> 795c0acf1 (feat: wip: initial work on glossaries - db schema, ui prototyping, refractoring;):ee/backend/app/src/main/kotlin/io/tolgee/ee/api/v2/hateoas/model/uasge/SumUsageItemModel.kt

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "invoices", itemRelation = "invoice")
open class SumUsageItemModel(
  val total: BigDecimal = 0.toBigDecimal(),
  val unusedQuantity: Long = 0,
  val usedQuantity: Long = 0,
  val usedQuantityOverPlan: Long = 0,
) : RepresentationModel<SumUsageItemModel>(), Serializable
