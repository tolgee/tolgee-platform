package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class SlackHelpBlocksProvider(
  private val i18n: I18n,
) {
  fun getHelpBlocks() =
    withBlocks {
      section {
        markdownText(i18n.translate("help-intro"))
      }
      divider()

      section {
        markdownText(i18n.translate("help-subscribe-to-project"))
      }

      section {
        fields {
          markdownText(i18n.translate("help-subscribe"))

          markdownText(i18n.translate("help-subscribe-command"))
        }
      }

      section {
        fields {
          markdownText(i18n.translate("help-unsubscribe"))

          markdownText(i18n.translate("help-unsubscribe-command"))
        }
      }

      section {
        fields {
          markdownText(i18n.translate("help-show-subscriptions"))
          markdownText(i18n.translate("help-show-subscriptions-command"))
        }
      }

      section {
        markdownText(i18n.translate("help-subscribe-default-subscription"))
      }

      actions {
        advancedCommandsButton()
      }

      divider()

      section {
        markdownText(i18n.translate("help-account"))
      }

      section {
        fields {
          markdownText(i18n.translate("help-connect-tolgee"))
          markdownText(i18n.translate("help-connect-tolgee-command"))
        }
      }

      section {
        fields {
          markdownText(i18n.translate("help-disconnect-tolgee"))
          markdownText(i18n.translate("help-disconnect-tolgee-command"))
        }
      }

      divider()

      section {
        markdownText(i18n.translate("help-more"))
      }

      actions {
        contactSupportButton()

        docsButton()
      }
    }

  fun getAdvancedSubscriptionHelpBlocks() =
    withBlocks {
      section {
        markdownText(i18n.translate("help-advanced-subscribe-intro"))
      }

      divider()

      section {
        markdownText(i18n.translate("help-advanced-subscribe-language"))
      }

      section {
        markdownText(i18n.translate("help-advanced-subscribe-language-info"))
      }

      section {
        markdownText(i18n.translate("help-advanced-subscribe-language-example"))
      }

      divider()

      section {
        markdownText(i18n.translate("help-advanced-subscribe-events"))
      }

      section {
        markdownText(i18n.translate("help-advanced-subscribe-events-info"))
      }

      section {
        markdownText(i18n.translate("help-advanced-subscribe-events-usage"))
      }

      section {
        markdownText(i18n.translate("help-advanced-subscribe-events-example"))
      }
    }

  private fun ActionsBlockBuilder.advancedCommandsButton() {
    button {
      text(i18n.translate("help-button-advanced-subscribe"), emoji = true)
      value("help_advanced_subscribe_btn")
    }
  }

  private fun ActionsBlockBuilder.contactSupportButton() {
    button {
      text(i18n.translate("help-button-contact-support"), emoji = true)
      actionId("contact_support_btn")
    }
  }

  private fun ActionsBlockBuilder.docsButton() {
    button {
      text(i18n.translate("help-button-docs"), emoji = true)
      actionId("docs_btn")
    }
  }
}
