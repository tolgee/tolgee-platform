package io.tolgee.component.automations.processors.slackIntegration

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
        markdownText(i18n.translate("help-subscribe"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-command"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-events"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-all-event"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-new-key-event"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-base-changed-event"))
      }

      section {
        markdownText(i18n.translate("help-subscribe-translation-change-event"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-unsubscribe"))
      }

      section {
        markdownText(i18n.translate("help-unsubscribe-command"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-show-subscriptions"))
      }
      section {
        markdownText(i18n.translate("help-show-subscriptions-command"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-connect-tolgee"))
      }
      section {
        markdownText(i18n.translate("help-connect-tolgee-command"))
      }

      divider()
      section {
        markdownText(i18n.translate("help-disconnect-tolgee"))
      }
      section {
        markdownText(i18n.translate("help-disconnect-tolgee-command"))
      }
    }
}
