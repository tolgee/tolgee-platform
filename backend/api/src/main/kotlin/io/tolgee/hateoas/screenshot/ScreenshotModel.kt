package io.tolgee.hateoas.screenshot

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.util.Date

@Suppress("unused")
@Relation(collectionRelation = "screenshots", itemRelation = "screenshot")
open class ScreenshotModel(
  val id: Long,
  @Schema(
    description = """File name, which may be downloaded from the screenshot path.

When images are secured. Encrypted timestamp is appended to the filename.    
  """,
  )
  val filename: String,
  @Schema(
    description = """Thumbnail file name, which may be downloaded from the screenshot path.

When images are secured. Encrypted timestamp is appended to the filename.    
  """,
  )
  val thumbnail: String,
  val middleSized: String?,
  val fileUrl: String,
  val middleSizedUrl: String?,
  val thumbnailUrl: String,
  val createdAt: Date?,
  val keyReferences: List<KeyInScreenshotModel>,
  val location: String?,
  val width: Int?,
  val height: Int?,
) : RepresentationModel<ScreenshotModel>()
