package io.tolgee.hateoas.automation

import io.tolgee.dtos.response.automations.AutomationActionModel
import io.tolgee.dtos.response.automations.AutomationTriggerModel
import org.springframework.hateoas.RepresentationModel
import org.springframework.hateoas.server.core.Relation
import java.io.Serializable

@Suppress("unused")
@Relation(collectionRelation = "activities", itemRelation = "activity")
class AutomationModel(
  val id: Long,
  val triggers: List<AutomationTriggerModel>,
  val actions: List<AutomationActionModel>,
) : RepresentationModel<AutomationModel>(), Serializable
