package io.tolgee.api.v2.hateoas.language

import io.tolgee.api.v2.hateoas.key.KeyModel
import io.tolgee.model.keyBigMeta.BigMetaType
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "bigMeta", itemRelation = "bigMetas")
open class BigMetaModel(
  var id: Long,

  var namespace: String? = null,

  var keyName: String,

  var location: String? = null,

  var key: KeyModel? = null,

  var type: BigMetaType = BigMetaType.SCRAPE,

  var contextData: Any? = null,
) : RepresentationModel<BigMetaModel>()
