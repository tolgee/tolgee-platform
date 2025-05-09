package io.tolgee.ee.api.v2.hateoas.assemblers

import io.tolgee.hateoas.ee.contentStorage.AzureContentStorageConfigModel
import io.tolgee.hateoas.ee.contentStorage.ContentStorageModel
import io.tolgee.hateoas.ee.contentStorage.ContentStorageModelAssembler
import io.tolgee.hateoas.ee.contentStorage.S3ContentStorageConfigModel
import io.tolgee.model.contentDelivery.ContentStorage
import org.springframework.context.annotation.Primary
import org.springframework.hateoas.server.RepresentationModelAssembler
import org.springframework.stereotype.Component

@Component
@Primary
class ContentStorageModelAssemblerEeImpl :
  RepresentationModelAssembler<ContentStorage, ContentStorageModel>,
  ContentStorageModelAssembler {
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
            path = it.path ?: "",
          )
        },
      azureContentStorageConfig =
        entity.azureContentStorageConfig?.let {
          AzureContentStorageConfigModel(containerName = it.containerName)
        },
    )
  }
}
