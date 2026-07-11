package io.tolgee.ee.component.slackIntegration.slashcommand

import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.ee.component.slackIntegration.SlackUserLoginUrlProvider
import io.tolgee.ee.service.slackIntegration.SlackConfigReadService
import io.tolgee.model.slackIntegration.OrganizationSlackWorkspace
import io.tolgee.model.slackIntegration.SlackConfig
import io.tolgee.model.slackIntegration.SlackEventType
import io.tolgee.service.language.LanguageService
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class SlackSlackCommandBlocksProvider(
  private val i18n: I18n,
  private val slackConfigReadService: SlackConfigReadService,
  private val languageService: LanguageService,
  private val slackUserLoginUrlProvider: SlackUserLoginUrlProvider,
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

  fun getListOfSubscriptionsBlocks(
    userId: String,
    channelId: String,
  ): List<LayoutBlock> {
    val configList = slackConfigReadService.getAllByChannelId(channelId)

    val blocks =
      withBlocks {
        header {
          text("Subscription Details", emoji = true)
        }
        divider()

        configList.forEach { config ->
          section {
            markdownText("*Project Name:* ${config.project.name} (id: ${config.project.id})")
          }
          if (config.isGlobalSubscription) {
            section {
              markdownText("*Global Subscription:* Yes")
            }

            val events = config.events.joinToString(", ") { "$it" }

            val allEventMeaning =
              if (config.events.contains(SlackEventType.ALL)) {
                getEventAllMeaning()
              } else {
                ""
              }

            section {
              markdownText("*Events:* `$events` $allEventMeaning")
            }

            context {
              markdownText(
                i18n.translate("slack.common.message.list-subscriptions-global-subscription-meaning"),
              )
            }
          }
          config.preferences.forEach {
            section {
              if (it.languageTag == null) {
                return@section
              }
              val language = languageService.getByTag(it.languageTag!!, config.project)
              val flagEmoji = language.flagEmoji

              val fullName = language.name
              val events = it.events.joinToString(", ") { "$it" }
              val allEventMeaning =
                if (it.events.contains(SlackEventType.ALL)) {
                  getEventAllMeaning()
                } else {
                  ""
                }
              markdownText(
                "*Subscribed Languages:*\n- $fullName $flagEmoji : on `$events` $allEventMeaning",
              )
            }
          }
          divider()
        }
      }

    return blocks
  }

  fun getLoginRedirectBlocks(
    slackChannelId: String,
    slackId: String,
    workspace: OrganizationSlackWorkspace?,
    slackTeamId: String,
  ): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.not-connected"))
      }

      section {
        markdownText(i18n.translate("slack.common.message.connect-account-instruction"))
      }

      actions {
        button {
          text(i18n.translate("slack.common.text.button.connect"), emoji = true)
          value("connect_slack")
          url(slackUserLoginUrlProvider.getUrl(slackChannelId, slackId, workspace?.id, slackTeamId))
          actionId("button_connect_slack")
          style("primary")
        }
      }
    }
  }

  fun getSuccessfullySubscribedBlocks(config: SlackConfig): List<LayoutBlock> =
    withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.subscribed-successfully").format(config.project.name))
      }
      val subscriptionInfo =
        buildString {
          if (config.isGlobalSubscription) {
            val events = config.events.joinToString(", ") { "$it" }
            append(
              i18n
                .translate(
                  "slack.common.message.subscribed-successfully-global-subscription",
                ).format(events),
            )
          }

          val languageInfo =
            config.preferences
              .mapNotNull { pref ->
                pref.languageTag?.let { tag ->
                  val language = languageService.getByTag(tag, config.project)
                  val flagEmoji = language.flagEmoji
                  val fullName = language.name
                  val events = pref.events.joinToString(", ") { "$it" }
                  " - $fullName $flagEmoji : on `$events`"
                }
              }.joinToString("\n")

          if (languageInfo.isNotEmpty()) {
            if (config.isGlobalSubscription) {
              append("\n\n").append(i18n.translate("slack.common.message.also-subscribed-to"))
            } else {
              append(
                "\n\n",
              ).append(i18n.translate("slack.common.message.subscribed-successfully-not-global-subscription"))
            }
            append("\n").append(languageInfo)
          }
        }

      section {
        markdownText(subscriptionInfo)
      }
    }

  private fun getEventAllMeaning() =
    "(" +
      i18n.translate(
        "slack.common.message.list-subscriptions-all-events",
      ) + ")"

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
