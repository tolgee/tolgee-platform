package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.hateoas.ee.contentStorage.AzureContentStorageConfigModel
import io.tolgee.hateoas.ee.contentStorage.ContentStorageModel
import io.tolgee.hateoas.ee.contentStorage.IContentStorageModelAssembler
import io.tolgee.hateoas.ee.contentStorage.S3ContentStorageConfigModel
import io.tolgee.model.contentDelivery.ContentStorage
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
class ContentStorageModelAssembler :
  RepresentationModelAssembler<ContentStorage, ContentStorageModel>,
  IContentStorageModelAssembler {
  override fun toModel(entity: ContentStorage): ContentStorageModel {
    return ContentStorageModel(
      id = entity.id,
      name = entity.name,
      publicUrlPrefix = entity.publicUrlPrefix,
      s3ContentStorageConfig =
        entity.s3ContentStorageConfig?.let {
          S3ContentStorageConfigModel(
            bucketName = it.bucketName,
            endpoint = it.endpoint,
            signingRegion = it.signingRegion,
          )
        },
      azureContentStorageConfig =
        entity.azureContentStorageConfig?.let {
          AzureContentStorageConfigModel(containerName = it.containerName)
        },
    )
  }
}
