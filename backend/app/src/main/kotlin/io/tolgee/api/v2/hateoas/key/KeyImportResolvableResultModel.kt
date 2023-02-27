package io.tolgee.api.v2.hateoas.key

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.screenshot.ScreenshotModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class KeyImportResolvableResultModel(
  @Schema(description = "List of keys")
  val keys: List<KeyModel>,

  @Schema(description = "Map uploadedImageId to screenshot")
  val screenshots: Map<Long, ScreenshotModel>,
) : RepresentationModel<KeyImportResolvableResultModel>(), Serializable
