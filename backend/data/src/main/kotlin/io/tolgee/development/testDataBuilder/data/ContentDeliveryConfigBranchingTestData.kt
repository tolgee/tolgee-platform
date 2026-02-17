package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.automations.AutomationAction
import io.tolgee.model.automations.AutomationActionType
import io.tolgee.model.automations.AutomationTrigger
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.model.branching.Branch

class ContentDeliveryConfigBranchingTestData : BaseTestData() {
  var mainBranch: Branch
  var featureBranch: Branch

  val mainBranchCdnConfig =
    projectBuilder.addContentDeliveryConfig {
      name = "Main CDN"
    }

  val featureBranchCdnConfig =
    projectBuilder.addContentDeliveryConfig {
      name = "Feature CDN"
    }

  val defaultServerContentDeliveryConfig =
    projectBuilder.addContentDeliveryConfig {
      name = "Default server"
    }

  val mainCdnAutomation =
    projectBuilder.addAutomation {
      this.triggers.add(
        AutomationTrigger(this)
          .also { it.type = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION },
      )
      this.actions.add(
        AutomationAction(this).also {
          it.type = AutomationActionType.CONTENT_DELIVERY_PUBLISH
          it.contentDeliveryConfig = mainBranchCdnConfig.self
        },
      )
    }

  val featureCdnAutomation =
    projectBuilder.addAutomation {
      this.triggers.add(
        AutomationTrigger(this)
          .also { it.type = AutomationTriggerType.TRANSLATION_DATA_MODIFICATION },
      )
      this.actions.add(
        AutomationAction(this).also {
          it.type = AutomationActionType.CONTENT_DELIVERY_PUBLISH
          it.contentDeliveryConfig = featureBranchCdnConfig.self
        },
      )
    }

  val keyOnMainBranch =
    this.projectBuilder.addKey("key") {
      addTranslation("en", "Hello")
    }

  val keyOnFeatureBranch =
    this.projectBuilder.addKey("feature-key") {
      addTranslation("en", "Feature Hello")
    }

  init {
    projectBuilder.apply {
      self.useBranching = true

      mainBranch =
        addBranch {
          name = "main"
          project = projectBuilder.self
          isProtected = true
          isDefault = true
        }.self

      featureBranch =
        addBranch {
          name = "feature"
          project = projectBuilder.self
          isProtected = false
          isDefault = false
          originBranch = mainBranch
        }.self

      mainBranchCdnConfig.self.branch = mainBranch
      featureBranchCdnConfig.self.branch = featureBranch
      defaultServerContentDeliveryConfig.self.branch = mainBranch

      mainBranchCdnConfig.self.automationActions.addAll(mainCdnAutomation.self.actions)
      featureBranchCdnConfig.self.automationActions.addAll(featureCdnAutomation.self.actions)

      keyOnMainBranch.self.branch = mainBranch
      keyOnFeatureBranch.self.branch = featureBranch
    }
  }
}
