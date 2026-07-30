package io.tolgee.hateoas.key

import io.tolgee.service.key.KeySearchResultView
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
class KeySearchSearchResultModel(
  view: KeySearchResultView,
  override val deletedByUserUsername: String?,
) : RepresentationModel<KeySearchSearchResultModel>(),
  KeySearchResultView by view
