package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.branching.Branch
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation

class NotificationRecipientsTestData : BaseTestData("notification_author", "Notification project") {
  lateinit var french: Language
  lateinit var german: Language
  lateinit var defaultBranch: Branch
  lateinit var existingKey: Key
  lateinit var existingFrench: Translation
  lateinit var existingGerman: Translation
  lateinit var featureBranch: Branch
  lateinit var branchKey: Key
  lateinit var branchFrench: Translation

  val author: UserAccount get() = user

  init {
    projectBuilder.apply {
      self.useBranching = true
      french =
        addLanguage {
          name = "French"
          tag = "fr"
          originalName = "Français"
          flagEmoji = "🇫🇷"
        }.self
      german =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
          flagEmoji = "🇩🇪"
        }.self
      defaultBranch =
        addBranch {
          name = "main"
          isProtected = true
          isDefault = true
        }.self
      featureBranch =
        addBranch {
          name = "feature-login"
          originBranch = defaultBranch
          isDefault = false
        }.self
      addKey("existing-key").apply {
        existingKey = self
        self.branch = defaultBranch
        addTranslation("en", "Hello")
        existingFrench = addTranslation("fr", "Bonjour").self
        existingGerman = addTranslation("de", "Hallo").self
      }
      addKey("branch-key").apply {
        branchKey = self
        self.branch = featureBranch
        addTranslation("en", "Login")
        branchFrench = addTranslation("fr", "Connexion").self
      }
    }
  }

  val translatorFr =
    addUser("translatorFr") {
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(french)
      viewLanguages = mutableSetOf(french)
    }

  val reviewerFr =
    addUser("reviewerFr") {
      type = ProjectPermissionType.REVIEW
      translateLanguages = mutableSetOf(french)
      viewLanguages = mutableSetOf(french)
      stateChangeLanguages = mutableSetOf(french)
    }

  val reviewerAll = addUser("reviewerAll") { type = ProjectPermissionType.REVIEW }

  val viewerDe =
    addUser("viewerDe") {
      type = ProjectPermissionType.VIEW
      viewLanguages = mutableSetOf(german)
    }

  val orgMember: UserAccount =
    root
      .addUserAccount { username = "orgMember" }
      .self
      .also { member ->
        userAccountBuilder.defaultOrganizationBuilder.addRole {
          user = member
          type = OrganizationRoleType.MEMBER
        }
      }

  val outsider: UserAccount = root.addUserAccount { username = "outsider" }.self

  private fun addUser(
    name: String,
    setPermission: Permission.() -> Unit,
  ): UserAccount {
    val account =
      root
        .addUserAccount {
          username = name
          this.name = name
        }.self
    projectBuilder.addPermission {
      user = account
      setPermission(this)
    }
    return account
  }
}
