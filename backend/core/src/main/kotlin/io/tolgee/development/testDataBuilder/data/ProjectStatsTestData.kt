package io.tolgee.development.testDataBuilder.data

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Language
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.TranslationState
import io.tolgee.model.key.Tag

class ProjectStatsTestData : BaseTestData() {
  lateinit var germanLanguage: Language
  lateinit var czechLanguage: Language

  init {
    projectBuilder.apply {

      addLanguages()
      addKeys()

      val organizationOwner =
        root
          .addUserAccount {
            name = "franta"
            username = "franta"
          }.self

      val organizationMember =
        root
          .addUserAccount {
            name = "jindra"
            username = "jindra"
          }.self

      root
        .addOrganization {
          name = "org"
          projectBuilder.self.organizationOwner = this
        }.build buildOrganization@{
          addRole {
            user = organizationOwner
            type = OrganizationRoleType.OWNER
            organization = this@buildOrganization.self
          }
          addRole {
            user = organizationMember
            type = OrganizationRoleType.MEMBER
            organization = this@buildOrganization.self
          }
        }

      addPermission {
        type = ProjectPermissionType.MANAGE
        user = organizationMember
      }
    }
  }

  private fun ProjectBuilder.addLanguages() {
    germanLanguage = addGerman().self
    czechLanguage = addCzech().self
  }

  private fun ProjectBuilder.addKeys() {
    val tag1 =
      Tag().apply {
        name = "Tag1"
        project = this@addKeys.self
      }

    val tag2 =
      Tag().apply {
        name = "Tag2"
        project = this@addKeys.self
      }

    val tag3 =
      Tag().apply {
        name = "Tag3"
        project = this@addKeys.self
      }

    addKey {
      name = "Super key"
    }.build {
      addTranslation {
        language = englishLanguage
        text = "This text has 5 words"
      }
      addTranslation {
        language = germanLanguage
        text = "Another text"
        state = TranslationState.TRANSLATED
      }

      addTranslation {
        language = czechLanguage
        text = "Another text"
        state = TranslationState.TRANSLATED
      }
      addMeta {
        tags.add(tag1)
        tags.add(tag3)
      }
    }

    addKey {
      name = "Key with null translations"
    }.build {
      addTranslation {
        language = englishLanguage
        text = "This text has 5 words"
      }
      addMeta {
        tags.add(tag1)
        tags.add(tag2)
      }
    }

    addKey {
      name = "Key with Untranslated values"
    }.build {
      addTranslation {
        language = englishLanguage
        text = "This text has 5 words"
      }

      addTranslation {
        language = germanLanguage
        text = null
        state = TranslationState.UNTRANSLATED
      }

      addTranslation {
        language = czechLanguage
        text = null
        state = TranslationState.UNTRANSLATED
      }
    }

    addKey {
      name = "Key with reviewed values"
    }.build {
      addTranslation {
        language = englishLanguage
        text = "This text has 5 words"
      }
      addTranslation {
        language = germanLanguage
        text = null
        state = TranslationState.REVIEWED
      }

      addTranslation {
        language = czechLanguage
        text = null
        state = TranslationState.REVIEWED
      }
    }

    addKey {
      name = "Key with mixed values"
    }.build {
      addTranslation {
        language = englishLanguage
        text = "This text has 5 words"
      }
      addTranslation {
        language = germanLanguage
        text = null
        state = TranslationState.TRANSLATED
      }

      addTranslation {
        language = czechLanguage
        text = null
        state = TranslationState.REVIEWED
      }
    }
  }
}
