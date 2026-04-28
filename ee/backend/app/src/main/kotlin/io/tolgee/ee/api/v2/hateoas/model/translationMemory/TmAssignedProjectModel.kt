package io.tolgee.ee.api.v2.hateoas.model.translationMemory

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation

@Relation(
  collectionRelation = "assignedProjects",
  itemRelation = "assignedProject",
)
class TmAssignedProjectModel(
  val projectId: Long,
  val projectName: String,
  val readAccess: Boolean,
  val writeAccess: Boolean,
  val priority: Int,
  val penalty: Int? = null,
) : RepresentationModel<TmAssignedProjectModel>()
