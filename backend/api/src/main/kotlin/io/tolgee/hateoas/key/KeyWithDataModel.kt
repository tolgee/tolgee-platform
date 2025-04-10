package io.tolgee.hateoas.key

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.hateoas.screenshot.ScreenshotModel
import io.tolgee.hateoas.translations.TranslationModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class KeyWithDataModel(
  @Schema(description = "Id of key record")
  val id: Long,
  @Schema(description = "Name of key", example = "this_is_super_key")
  val name: String,
  @Schema(description = "Namespace of key", example = "homepage")
  val namespace: String?,
  @Schema(
    description = "Description of key",
    example = "This key is used on homepage. It's a label of sign up button.",
  )
  val description: String?,
  @Schema(
    description = "Translations object containing values updated in this request",
    example = "{\"en\": {\"id\": 100000003, \"text\": \"This is super translation!\" }}",
  )
  val translations: Map<String, TranslationModel>,
  @Schema(description = "Tags of key")
  val tags: Set<TagModel>,
  @Schema(description = "Screenshots of the key")
  val screenshots: List<ScreenshotModel>,
  @Schema(description = "If key is pluralized. If it will be reflected in the editor")
  val isPlural: Boolean,
  @Schema(description = "The argument name for the plural")
  val pluralArgName: String?,
  @Schema(description = "Custom values of the key")
  val custom: Map<String, Any?>,
) : RepresentationModel<KeyWithDataModel>(),
  Serializable
