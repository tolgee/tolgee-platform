package io.tolgee.hateoas.dataImport

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "namespaces", itemRelation = "namespace")
open class ImportNamespaceModel(
  @Schema(
    description = "The id of namespace. When null, namespace doesn't exist and will be created by import.",
    example = "10000048",
  )
  val id: Long?,
  @Schema(description = "", example = "homepage")
  val name: String,
) : RepresentationModel<ImportNamespaceModel>(),
  Serializable
