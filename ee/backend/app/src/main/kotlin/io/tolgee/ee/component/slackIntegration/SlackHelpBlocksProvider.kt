package io.tolgee.ee.component.slackIntegration

import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class SlackHelpBlocksProvider(
  private val i18n: I18n,
) {
  companion object {
    const val DOCUMENTATION_URL = "https://tolgee.io/platform/integrations/slack_integration/about"
  }

  fun getHelpBlocks() =
    withBlocks {
      section {
        markdownText(i18n.translate("slack.help.message.intro"))
      }
      divider()

      section {
        markdownText(i18n.translate("slack.help.message.subscribe-to-project"))
      }

      section {
        fields {
          markdownText(i18n.translate("slack.help.message.subscribe"))

          markdownText(i18n.translate("slack.help.message.subscribe-command"))
        }
      }

      section {
        fields {
          markdownText(i18n.translate("slack.help.message.unsubscribe"))

          markdownText(i18n.translate("slack.help.message.unsubscribe-command"))
        }
      }

      section {
        fields {
          markdownText(i18n.translate("slack.help.message.show-subscriptions"))
          markdownText(i18n.translate("slack.help.message.show-subscriptions-command"))
        }
      }

      section {
        markdownText(i18n.translate("slack.help.message.default-subscription"))
      }

      actions {
        advancedCommandsButton()
      }

      divider()

      section {
        markdownText(i18n.translate("slack.help.message.account"))
      }

      section {
        fields {
          markdownText(i18n.translate("slack.help.message.connect-tolgee"))
          markdownText(i18n.translate("slack.help.message.connect-tolgee-command"))
        }
      }

      section {
        fields {
          markdownText(i18n.translate("slack.help.message.disconnect-tolgee"))
          markdownText(i18n.translate("slack.help.message.disconnect-tolgee-command"))
        }
      }

      divider()

      section {
        markdownText(i18n.translate("slack.help.message.more"))
      }

      actions {
        docsButton()
      }
    }

  fun getAdvancedSubscriptionHelpBlocks() =
    withBlocks {
      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-intro"))
      }

      divider()

      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-language"))
      }

      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-language-info"))
      }

      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-language-example"))
      }

      divider()

      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-events"))
      }

      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-events-info"))
      }

      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-events-usage"))
      }

      section {
        markdownText(i18n.translate("slack.help.message.advanced-subscribe-events-example"))
      }
    }

  private fun ActionsBlockBuilder.advancedCommandsButton() {
    button {
      text(i18n.translate("slack.help.text.button.advanced-subscribe"), emoji = true)
      value("help_advanced_subscribe_btn")
    }
  }

  private fun ActionsBlockBuilder.contactSupportButton() {
    button {
      text(i18n.translate("slack.help.text.button.contact-support"), emoji = true)
      actionId("contact_support_btn")
    }
  }

  private fun ActionsBlockBuilder.docsButton() {
    button {
      url(DOCUMENTATION_URL)
      text(i18n.translate("slack.help.text.button.docs"), emoji = true)
      actionId("docs_btn")
    }
  }
}
