package io.tolgee.ee.development

import io.tolgee.development.testDataBuilder.data.BaseTestData

class AiPromptCustomizationTestData : BaseTestData() {
  val czech =
    this.projectBuilder.addCzech().self.also {
      it.aiTranslatorPromptDescription = "Czech description"
    }

  val french =
    this.projectBuilder.addFrench().self.also {
      it.aiTranslatorPromptDescription = "French description"
    }

  init {
    this.projectBuilder.self.aiTranslatorPromptDescription = "Project description"
  }
}
