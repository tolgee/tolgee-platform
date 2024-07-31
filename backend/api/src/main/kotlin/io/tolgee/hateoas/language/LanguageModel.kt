package io.tolgee.hateoas.language

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.ILanguageModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "languages", itemRelation = "language")
open class LanguageModel(
  override val id: Long,
  @Schema(example = "Czech", description = "Language name in english")
  override val name: String,
  @Schema(example = "cs-CZ", description = "Language tag according to BCP 47 definition")
  override var tag: String,
  @Schema(example = "čeština", description = "Language name in this language")
  override var originalName: String?,
  @Schema(example = "\uD83C\uDDE8\uD83C\uDDFF", description = "Language flag emoji as UTF-8 emoji")
  override var flagEmoji: String?,
  @Schema(example = "false", description = "Whether is base language of project")
  var base: Boolean,
) : RepresentationModel<LanguageModel>(), ILanguageModel
