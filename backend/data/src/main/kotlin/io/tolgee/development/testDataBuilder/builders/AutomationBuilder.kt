package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import io.tolgee.model.automations.Automation

class AutomationBuilder(
  val projectBuilder: ProjectBuilder,
) : EntityDataBuilder<Automation, AutomationBuilder> {
  override var self = Automation(projectBuilder.self)
}
