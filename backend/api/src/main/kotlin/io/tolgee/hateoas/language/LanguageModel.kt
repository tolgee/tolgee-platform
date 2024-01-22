package io.tolgee.hateoas.language

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Suppress("unused")
@Relation(collectionRelation = "languages", itemRelation = "language")
open class LanguageModel(
  val id: Long,
  @Schema(example = "Czech", description = "Language name in english")
  val name: String,
  @Schema(example = "cs-CZ", description = "Language tag according to BCP 47 definition")
  var tag: String,
  @Schema(example = "čeština", description = "Language name in this language")
  var originalName: String? = null,
  @Schema(example = "\uD83C\uDDE8\uD83C\uDDFF", description = "Language flag emoji as UTF-8 emoji")
  var flagEmoji: String? = null,
  @Schema(example = "false", description = "Whether is base language of project")
  var base: Boolean,
) : RepresentationModel<LanguageModel>()
