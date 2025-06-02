package io.tolgee.ee.api.v2.hateoas.model.glossary

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaries", itemRelation = "glossary")
class SimpleGlossaryModel(
  val id: Long,
  val name: String,
  val baseLanguageTag: String?,
) : RepresentationModel<SimpleGlossaryModel>()
