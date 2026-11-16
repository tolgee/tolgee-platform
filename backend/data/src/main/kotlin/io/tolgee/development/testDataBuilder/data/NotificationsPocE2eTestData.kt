package io.tolgee.development.testDataBuilder.data

import io.tolgee.model.Language
import io.tolgee.model.Permission
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.TranslationCommentState
import io.tolgee.model.translation.Translation

class NotificationsPocE2eTestData : BaseTestData("marek@example.com", "Mobile App") {
  lateinit var german: Language
  lateinit var french: Language
  lateinit var germanCardError: Translation

  init {
    user.name = "Marek Svoboda"
    projectBuilder.apply {
      german =
        addLanguage {
          name = "German"
          tag = "de"
          originalName = "Deutsch"
          flagEmoji = "🇩🇪"
        }.self
      french =
        addLanguage {
          name = "French"
          tag = "fr"
          originalName = "Français"
          flagEmoji = "🇫🇷"
        }.self
      addKey("checkout.pay_button").apply {
        addTranslation("en", "Pay now")
        addTranslation("de", "Jetzt bezahlen").apply {
          addComment {
            text = "Is this formal or informal German?"
            author = this@NotificationsPocE2eTestData.user
            state = TranslationCommentState.NEEDS_RESOLUTION
          }
        }
        addTranslation("fr", "Payer maintenant")
      }
      addKey("checkout.card_error").apply {
        addTranslation("en", "Your card was declined")
        germanCardError = addTranslation("de", "Ihre Karte wurde abgelehnt").self
      }
      addKey("profile.title").apply {
        addTranslation("en", "Your profile")
      }
    }
  }

  val anna =
    addUser("anna@example.com", "Anna Novak") {
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(german)
      viewLanguages = mutableSetOf(german, englishLanguage)
    }

  val andreas =
    addUser("andreas@example.com", "Andreas Keller") {
      type = ProjectPermissionType.REVIEW
      translateLanguages = mutableSetOf(german, french)
      viewLanguages = mutableSetOf(german, french, englishLanguage)
      stateChangeLanguages = mutableSetOf(german, french)
    }.also { projectBuilder.addTranslationReview(it, germanCardError) }

  val tom =
    addUser("tom@example.com", "Tom Reid") {
      type = ProjectPermissionType.TRANSLATE
      translateLanguages = mutableSetOf(french)
      viewLanguages = mutableSetOf(french, englishLanguage)
    }

  val vera =
    root
      .addUserAccount {
        username = "vera@example.com"
        name = "Vera Lindqvist"
      }.self

  lateinit var helpCenter: Project
  lateinit var helpCenterGerman: Language
  lateinit var helpCenterRefundGerman: Translation

  init {
    root.apply {
      addProject(organizationOwner = userAccountBuilder.defaultOrganizationBuilder.self) {
        name = "Help Center"
        public = true
      }.build {
        helpCenter = self
        val english =
          addLanguage {
            name = "English"
            tag = "en"
            originalName = "English"
            flagEmoji = "🇬🇧"
          }.self
        self.baseLanguage = english
        helpCenterGerman =
          addLanguage {
            name = "German"
            tag = "de"
            originalName = "Deutsch"
            flagEmoji = "🇩🇪"
          }.self
        addPermission {
          user = andreas
          type = ProjectPermissionType.REVIEW
          translateLanguages = mutableSetOf(helpCenterGerman)
          viewLanguages = mutableSetOf(helpCenterGerman, english)
          stateChangeLanguages = mutableSetOf(helpCenterGerman)
        }
        addKey("faq.refund_policy").apply {
          addTranslation("en", "You can ask for a refund within 30 days")
          helpCenterRefundGerman =
            addTranslation("de", "Sie können innerhalb von 30 Tagen eine Erstattung verlangen")
              .apply {
                addComment {
                  text = "Should this be Rückerstattung? Erstattung sounds like insurance to me."
                  author = vera
                  state = TranslationCommentState.NEEDS_RESOLUTION
                }
              }.self
        }
        addKey("faq.contact").apply {
          addTranslation("en", "Contact us")
          addTranslation("de", "Kontakt")
        }
      }
    }
  }

  private fun addUser(
    username: String,
    fullName: String,
    setPermission: Permission.() -> Unit,
  ): UserAccount {
    val account =
      root
        .addUserAccount {
          this.username = username
          name = fullName
        }.self
    projectBuilder.addPermission {
      user = account
      setPermission(this)
    }
    return account
  }
}
