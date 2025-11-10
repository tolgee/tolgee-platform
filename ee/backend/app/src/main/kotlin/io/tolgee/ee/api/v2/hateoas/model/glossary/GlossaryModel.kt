package io.tolgee.ee.api.v2.hateoas.model.glossary

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.hateoas.organization.SimpleOrganizationModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaries", itemRelation = "glossary")
class GlossaryModel(
  val id: Long,
  val name: String,
  @Schema(
    description = "Language tag for default translations for terms",
    example = "en",
  )
  val baseLanguageTag: String,
  val organizationOwner: SimpleOrganizationModel,
) : RepresentationModel<GlossaryModel>()
