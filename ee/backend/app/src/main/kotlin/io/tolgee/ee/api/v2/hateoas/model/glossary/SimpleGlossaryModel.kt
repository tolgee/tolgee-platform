package io.tolgee.ee.api.v2.hateoas.model.glossary

import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaries", itemRelation = "glossary")
class SimpleGlossaryModel(
  val id: Long,
  val name: String,
  @Schema(
    description = "Language tag for default translations for terms",
    example = "en",
  )
  val baseLanguageTag: String,
) : RepresentationModel<SimpleGlossaryModel>()
