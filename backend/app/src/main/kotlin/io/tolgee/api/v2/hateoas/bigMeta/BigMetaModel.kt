package io.tolgee.api.v2.hateoas.language

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "bigMeta", itemRelation = "bigMetas")
open class BigMetaModel(
  var namespace: String? = null,

  var keyName: String,

  @field:Schema(description = "Distance between keys (number between 0 and 10000)")
  var distance: Long? = null,
) : RepresentationModel<BigMetaModel>()
