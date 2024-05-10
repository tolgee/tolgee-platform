package io.tolgee.ee.component.slackIntegration

import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.component.SlackErrorProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class SlackErrorProviderImpl(
  private val i18n: I18n,
  private val slackUserLoginUrlProvider: SlackUserLoginUrlProvider,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
  private val tolgeeProperties: TolgeeProperties,
) : SlackErrorProvider {
  override fun getUserNotConnectedError(payload: SlackCommandDto): List<LayoutBlock> {
    val workspace = organizationSlackWorkspaceService.findBySlackTeamId(payload.team_id)

    val url =
      slackUserLoginUrlProvider.getUrl(
        payload.channel_id,
        payload.user_id,
        workspace?.id,
      )

    return withBlocks {
      section {
        markdownText(
          i18n.translate("slack.common.message.not-connected"),
        )
      }
      section {
        markdownText(i18n.translate("slack.common.message.connect-account-instruction"))
      }
      actions {
        button {
          text(i18n.translate("slack.common.text.button.connect"), emoji = true)
          url(url)
          style("primary")
        }
      }
    }
  }

  override fun getInvalidCommandError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.command-not-recognized"))
      }
      section {
        markdownText(i18n.translate("slack.common.message.check-command-solutions"))
      }
      actions {
        helpButton()
      }
    }
  }

  override fun getInvalidLangTagError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.invalid-lang-tag"))
      }
    }
  }

  override fun getInvalidGlobalSubscriptionError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.global-subscription-error"))
      }
    }
  }

  override fun getNotSubscribedYetError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(
          i18n.translate("slack.common.message.not-subscribed-yet"),
        )
      }
      section {
        markdownText(i18n.translate("slack.common.message.not-subscribed-solution"))
      }
      actions {
        helpButton()
      }
    }
  }

  override fun getNoPermissionError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(
          i18n.translate("slack-insufficient-permissions-error"),
        )
      }
    }
  }

  override fun getFeatureDisabledError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(
          i18n.translate("slack.common.message.feature-not-available"),
        )
      }
    }
  }

  override fun getProjectNotFoundError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(
          i18n.translate("slack-project-not-found-error"),
        )
      }
    }
  }

  override fun getWorkspaceNotFoundError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack.common.message.slack-workspace-not-connected-to-any-organization"))
      }

      actions {
        button {
          text(i18n.translate("slack.common.text.button.connect-workspace"), emoji = true)
          url(tolgeeProperties.frontEndUrl + "/preferred-organization?path=apps")
          style("primary")
        }
      }
    }
  }

  override fun getInvalidSignatureError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack-invalid-signature"))
      }
    }
  }

  private fun ActionsBlockBuilder.helpButton() {
    button {
      value("help_btn")
      text(i18n.translate("slack.common.text.button.view-help"), emoji = true)
    }
  }
}
