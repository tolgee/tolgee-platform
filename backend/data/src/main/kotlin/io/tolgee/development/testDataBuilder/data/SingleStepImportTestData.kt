package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.Scope
import io.tolgee.model.enums.TranslationState

class SingleStepImportTestData : BaseTestData() {
  val germanLanguage = projectBuilder.addGerman()

  fun addConflictTranslation() {
    val key =
      projectBuilder.addKey {
        this.name = "test"
      }
    projectBuilder.addTranslation {
      this.key = key.self
      this.text = "conflict!"
      this.language = englishLanguage
    }
  }

  fun addReviewedTranslation() {
    val key =
      projectBuilder.addKey {
        this.name = "test"
      }
    projectBuilder.addTranslation {
      this.key = key.self
      this.text = "conflict!"
      state = TranslationState.REVIEWED
      this.language = germanLanguage.self
    }
  }

  fun setUserScopes(scopes: Array<Scope>) {
    userAccountBuilder.defaultOrganizationBuilder.data.roles.first().self.type = OrganizationRoleType.MEMBER
    projectBuilder.data.permissions.first().self.scopes = scopes
  }
}
