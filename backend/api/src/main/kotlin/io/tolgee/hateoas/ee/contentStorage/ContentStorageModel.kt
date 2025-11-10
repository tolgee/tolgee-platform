package io.tolgee.hateoas.ee.contentStorage

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "contentStorages", itemRelation = "contentStorage")
class ContentStorageModel(
  val id: Long,
  val name: String,
  val publicUrlPrefix: String?,
  val azureContentStorageConfig: AzureContentStorageConfigModel?,
  val s3ContentStorageConfig: S3ContentStorageConfigModel?,
) : RepresentationModel<ContentStorageModel>(),
  Serializable
