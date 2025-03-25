package io.tolgee.hateoas.ee

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.math.BigDecimal

@Suppress("unused")
@Relation(collectionRelation = "plans", itemRelation = "plan")
open class PrepareSetLicenseKeyModel(
  val perSetPrice: BigDecimal,
  val currentSeats: Long,
  val total: Long,
) : RepresentationModel<PrepareSetLicenseKeyModel>(), Serializable
