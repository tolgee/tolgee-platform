package io.tolgee.hateoas.automation

import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "automations", itemRelation = "automation")
class AutomationModel(
  val id: Long,
  val name: String,
  val triggers: List<AutomationTriggerModel>,
  val actions: List<AutomationActionModel>,
) : RepresentationModel<AutomationModel>(),
  Serializable
