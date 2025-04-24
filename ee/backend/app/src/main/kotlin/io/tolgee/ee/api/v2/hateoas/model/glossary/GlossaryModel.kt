package io.tolgee.ee.api.v2.hateoas.model.glossary

import io.tolgee.hateoas.organization.SimpleOrganizationModel
import io.tolgee.hateoas.project.SimpleProjectModel
import org.springframework.hateoas.CollectionModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(collectionRelation = "glossaries", itemRelation = "glossary")
class GlossaryModel(
  val id: Long,
  val name: String,
  val baseLanguageTag: String?,
  val organizationOwner: SimpleOrganizationModel,
  val assignedProjects: CollectionModel<SimpleProjectModel>,
) : RepresentationModel<GlossaryModel>()
