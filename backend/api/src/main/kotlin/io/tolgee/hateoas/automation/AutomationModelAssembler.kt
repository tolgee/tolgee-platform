package io.tolgee.hateoas.automation

import io.tolgee.api.v2.controllers.AutomationController
import io.tolgee.dtos.response.automations.AutomationActionModel
import io.tolgee.dtos.response.automations.AutomationTriggerModel
import io.tolgee.model.automations.Automation
import io.tolgee.model.automations.AutomationAction
import org.springframework.context.ApplicationContext
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport
import org.springframework.stereotype.Component

@Component
class AutomationModelAssembler(
  private val applicationContext: ApplicationContext
) : RepresentationModelAssemblerSupport<Automation, AutomationModel>(
  AutomationController::class.java, AutomationModel::class.java
) {
  override fun toModel(entity: Automation): AutomationModel {
    val triggers = getTriggers(entity)
    val actions = getActions(entity)
    return AutomationModel(entity.id, triggers, actions)
  }

  private fun getActions(entity: Automation) =
    entity.actions.map {
      val model = AutomationActionModel(it.id, it.type)
      it.getProcessor().fillModel(model, it)
      model
    }

  private fun getTriggers(entity: Automation) =
    entity.triggers.map {
      AutomationTriggerModel(it.id, it.type, it.activityType, it.debounceDurationInMs)
    }

  private fun AutomationAction.getProcessor() =
    applicationContext.getBean(this.type.processor.java)
}
