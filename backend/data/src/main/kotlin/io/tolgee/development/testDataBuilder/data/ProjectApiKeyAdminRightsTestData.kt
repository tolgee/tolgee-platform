package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType

/**
 * Four elevated accounts standing in different relations to one project: two with no membership at all, one holding a
 * plain preset, and one restricted to a single language — so what a key minted by each of them may reach can be told
 * apart from what its owner's server role would reach.
 */
class ProjectApiKeyAdminRightsTestData : BaseTestData() {
  val outsideAdmin =
    root
      .addUserAccount {
        username = "pak_outside_admin"
        role = UserAccount.Role.ADMIN
      }.self

  val outsideSupporter =
    root
      .addUserAccount {
        username = "pak_outside_supporter"
        role = UserAccount.Role.SUPPORTER
      }.self

  val memberAdmin =
    root
      .addUserAccount {
        username = "pak_member_admin"
        role = UserAccount.Role.ADMIN
      }.self

  val languageRestrictedAdmin =
    root
      .addUserAccount {
        username = "pak_lang_restricted_admin"
        role = UserAccount.Role.ADMIN
      }.self

  lateinit var german: Language

  init {
    projectBuilder.addPermission {
      user = memberAdmin
      type = ProjectPermissionType.VIEW
    }

    german =
      projectBuilder
        .addLanguage {
          name = "German"
          tag = "de"
          originalName = "German"
        }.self

    projectBuilder.addPermission {
      user = languageRestrictedAdmin
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(german)
    }

    projectBuilder
      .addKey { name = EXISTING_KEY }
      .build {
        addTranslation {
          language = projectBuilder.self.baseLanguage!!
          text = "value"
        }
      }
  }

  companion object {
    const val EXISTING_KEY = "pak-admin-rights-key"
  }
}
