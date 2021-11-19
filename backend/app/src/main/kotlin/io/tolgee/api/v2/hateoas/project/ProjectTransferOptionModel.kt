package io.tolgee.api.v2.hateoas.project

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "transferOptions", itemRelation = "transferOption")
open class ProjectTransferOptionModel(
  val name: String?,
  val username: String? = null,
  val id: Long,
  val type: TransferOptionType
) : RepresentationModel<ProjectTransferOptionModel>() {
  enum class TransferOptionType {
    USER, ORGANIZATION
  }
}
