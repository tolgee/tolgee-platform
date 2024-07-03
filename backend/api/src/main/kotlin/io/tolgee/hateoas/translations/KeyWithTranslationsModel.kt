package io.tolgee.hateoas.translations

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.hateoas.screenshot.ScreenshotModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
open class KeyWithTranslationsModel(
  @Schema(description = "Id of key record")
  val keyId: Long,
  @Schema(description = "Name of key", example = "this_is_super_key")
  val keyName: String,
  @Schema(description = "Is this key a plural?", example = "true")
  val keyIsPlural: Boolean,
  @Schema(description = "The placeholder name for plural parameter", example = "value")
  val keyPluralArgName: String?,
  @Schema(description = "The namespace id of the key", example = "100000282")
  val keyNamespaceId: Long?,
  @Schema(description = "The namespace of the key", example = "homepage")
  val keyNamespace: String?,
  @Schema(description = "The namespace of the key", example = "homepage")
  val keyDescription: String?,
  @Schema(description = "Tags of key")
  val keyTags: List<TagModel>,
  @Schema(description = "Count of screenshots provided for the key", example = "1")
  val screenshotCount: Long,
  @Schema(description = "Key screenshots. Not provided when API key hasn't screenshots.view scope permission.")
  val screenshots: List<ScreenshotModel>?,
  @Schema(description = "There is a context available for this key")
  val contextPresent: Boolean,
  @Schema(
    description = "Translations object",
    example = """
    {
      "en": {
        "id": 100000003, 
        "text": "This is super translation!"
        "state": "TRANSLATED",
        "commentCount": 1
      }
    }
    """,
  )
  val translations: Map<String, TranslationViewModel>,
  @Schema(description = "Tasks related to this key")
  val tasks: List<KeyTaskViewModel>?,
) : RepresentationModel<KeyWithTranslationsModel>()
