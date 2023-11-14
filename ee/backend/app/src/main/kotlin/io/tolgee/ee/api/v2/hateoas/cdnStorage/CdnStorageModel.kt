package io.tolgee.ee.api.v2.hateoas.cdnStorage

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "cdnStorages", itemRelation = "cdnStorage")
class CdnStorageModel(
  val id: Long,
  val name: String,
  val publicUrlPrefix: String?,
  val azureCdnConfig: AzureCdnConfigModel?,
  val s3CdnConfig: S3CdnConfigModel?
) : RepresentationModel<CdnStorageModel>(), Serializable
