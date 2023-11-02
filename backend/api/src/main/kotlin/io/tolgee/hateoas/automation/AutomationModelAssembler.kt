package io.tolgee.hateoas.automation

import io.tolgee.api.v2.controllers.AutomationController
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationAction
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class AutomationModelAssembler(
  private val fillers: List<AutomationActionModelFiller>
) : RepresentationModelAssemblerSupport<Automation, AutomationModel>(
  AutomationController::class.java, AutomationModel::class.java
) {

  val fillersByType by lazy {
    fillers.associateBy { it.type }
  }

  override fun toModel(entity: Automation): AutomationModel {
    val triggers = getTriggers(entity)
    val actions = getActions(entity)
    return AutomationModel(entity.id, entity.name, triggers, actions)
  }

  private fun getActions(entity: Automation) =
    entity.actions.map {
      val model = AutomationActionModel(it.id, it.type)
      fillActionModel(it, model)
      model
    }

  private fun fillActionModel(
    entity: AutomationAction,
    model: AutomationActionModel
  ) {
    val filler = fillersByType[entity.type]
      ?: throw IllegalStateException(
        "No filler for ${entity.type}." +
          " Implement ${AutomationActionModelFiller::class.simpleName} for the type."
      )

    filler.fill(model, entity)
  }

  private fun getTriggers(entity: Automation) =
    entity.triggers.map {
      AutomationTriggerModel(it.id, it.type, it.activityType, it.debounceDurationInMs)
    }
}
