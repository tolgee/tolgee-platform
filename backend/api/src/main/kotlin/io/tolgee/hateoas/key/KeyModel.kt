package io.tolgee.hateoas.key

import io.tolgee.api.IKeyModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class KeyModel(
  override val id: Long,
  override val name: String,
  override val namespace: String?,
  override val description: String?,
  override val custom: Map<String, Any?>?,
) : RepresentationModel<KeyModel>(), Serializable, IKeyModel
