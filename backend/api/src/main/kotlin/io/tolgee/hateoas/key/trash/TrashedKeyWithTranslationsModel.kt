package io.tolgee.hateoas.key.trash

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.v2.hateoas.invitation.TagModel
import io.tolgee.hateoas.screenshot.ScreenshotModel
import io.tolgee.hateoas.translations.TranslationViewModel
import io.tolgee.hateoas.userAccount.SimpleUserAccountModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable
import java.util.Date

@Suppress("unused")
@Relation(collectionRelation = "keys", itemRelation = "key")
class TrashedKeyWithTranslationsModel(
  @Schema(description = "Id of key record")
  val id: Long,
  @Schema(description = "Name of key", example = "this_is_super_key")
  val name: String,
  @Schema(description = "Namespace of key", example = "homepage")
  val namespace: String?,
  @Schema(description = "When the key was deleted")
  val deletedAt: Date,
  @Schema(description = "When the key will be permanently deleted")
  val permanentDeleteAt: Date,
  @Schema(description = "Description of the key")
  val description: String?,
  @Schema(description = "Tags of key")
  val tags: List<TagModel>,
  @Schema(description = "Translations object keyed by language tag")
  val translations: Map<String, TranslationViewModel>,
  @Schema(description = "Screenshots of the key")
  val screenshots: List<ScreenshotModel>,
  @Schema(description = "Whether the key is plural")
  val isPlural: Boolean,
  @Schema(description = "User who deleted the key")
  val deletedBy: SimpleUserAccountModel?,
) : RepresentationModel<TrashedKeyWithTranslationsModel>(),
  Serializable
