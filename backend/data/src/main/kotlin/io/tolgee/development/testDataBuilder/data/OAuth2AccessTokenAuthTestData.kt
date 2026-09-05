package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.batch.BatchJob
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment

/**
 * The full spread an OAuth token is narrowed against: users whose server role outranks their membership, a
 * language-restricted membership, rows the default user authored themselves, a second project in the same
 * organization and a third owned by a stranger.
 */
class OAuth2AccessTokenAuthTestData : BaseTestData() {
  val viewOnlyUser = root.addUserAccount { username = "oauth_view_only_user" }.self

  val strangerBuilder = root.addUserAccount { username = "oauth_stranger_user" }

  val adminUser =
    root
      .addUserAccount {
        username = "oauth_admin_user"
        role = UserAccount.Role.ADMIN
      }.self

  val supporterUser =
    root
      .addUserAccount {
        username = "oauth_supporter_user"
        role = UserAccount.Role.SUPPORTER
      }.self

  val langRestrictedAdmin =
    root
      .addUserAccount {
        username = "oauth_lang_admin"
        role = UserAccount.Role.ADMIN
      }.self

  val viewRestrictedAdmin =
    root
      .addUserAccount {
        username = "oauth_view_lang_admin"
        role = UserAccount.Role.ADMIN
      }.self

  lateinit var strangersProject: Project

  lateinit var foreignProject: Project

  lateinit var german: Language

  lateinit var ownComment: TranslationComment

  lateinit var ownCommentTranslation: Translation

  lateinit var ownBatchJob: BatchJob

  init {
    strangersProject =
      root
        .addProject {
          name = "oauth_strangers_project"
          organizationOwner = strangerBuilder.defaultOrganizationBuilder.self
        }.self

    german =
      projectBuilder
        .addLanguage {
          name = "German"
          tag = "de"
          originalName = "German"
        }.self

    projectBuilder.addPermission {
      user = langRestrictedAdmin
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(german)
    }

    projectBuilder.addPermission {
      user = viewOnlyUser
      type = ProjectPermissionType.VIEW
    }

    projectBuilder.addPermission {
      user = viewRestrictedAdmin
      type = ProjectPermissionType.VIEW
      viewLanguages = mutableSetOf(german)
    }

    projectBuilder
      .addKey { name = "oauth-own-comment-key" }
      .build {
        addTranslation {
          language = german
          text = "Wert"
        }
        addTranslation {
          language = projectBuilder.self.baseLanguage!!
          text = "value"
          ownCommentTranslation = this
        }.build {
          ownComment =
            addComment {
              text = "comment by token owner"
              author = this@OAuth2AccessTokenAuthTestData.user
            }.self
        }
      }

    foreignProject =
      root
        .addProject {
          name = FOREIGN_PROJECT_NAME
          organizationOwner = projectBuilder.self.organizationOwner
        }.build {
          addLanguage {
            name = "English"
            tag = "en"
          }
        }.self

    ownBatchJob =
      projectBuilder
        .addBatchJob {
          author = this@OAuth2AccessTokenAuthTestData.user
          totalItems = 1
        }.self
  }

  companion object {
    const val FOREIGN_PROJECT_NAME = "oauth-foreign-project"
  }
}
