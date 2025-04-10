package io.tolgee.hateoas.key.namespace

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "namespaces", itemRelation = "namespace")
open class UsedNamespaceModel(
  @Schema(description = "The id of namespace. Null for default namespace.", example = "10000048")
  val id: Long?,
  @Schema(description = "Name of namespace. Null if default.", example = "homepage")
  val name: String?,
) : RepresentationModel<UsedNamespaceModel>(),
  Serializable
