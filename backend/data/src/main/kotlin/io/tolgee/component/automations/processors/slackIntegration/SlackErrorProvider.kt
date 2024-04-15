package io.tolgee.component.automations.processors.slackIntegration

import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.withBlocks
import io.tolgee.dtos.request.slack.SlackCommandDto
import io.tolgee.service.slackIntegration.OrganizationSlackWorkspaceService
import io.tolgee.util.I18n
import org.springframework.stereotype.Component

@Component
class SlackErrorProvider(
  private val i18n: I18n,
  private val slackUserLoginUrlProvider: SlackUserLoginUrlProvider,
  private val organizationSlackWorkspaceService: OrganizationSlackWorkspaceService,
) {
  fun getUserNotConnectedError(payload: SlackCommandDto): List<LayoutBlock> {
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
          i18n.translate("slack-not-connected-message"),
        )
      }
      section {
        markdownText(i18n.translate("connect-account-instruction"))
      }
      actions {
        button {
          text(i18n.translate("connect-button-text"), emoji = true)
          url(url)
          style("primary")
        }
      }
    }
  }

  fun getInvalidCommandError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("command-not-recognized"))
      }
      section {
        markdownText(i18n.translate("check-command-solutions"))
      }
      actions {
        helpButton()
      }
    }
  }

  fun getNotSubscribedYetError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(
          i18n.translate("not-subscribed-yet-message"),
        )
      }
      section {
        markdownText(i18n.translate("not-subscribed-solution"))
      }
      actions {
        helpButton()
      }
    }
  }

  fun getNoPermissionError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(
          i18n.translate("slack-insufficient-permissions-error"),
        )
      }
    }
  }

  fun getProjectNotFoundError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(
          i18n.translate("slack-project-not-found-error"),
        )
      }
    }
  }

  fun getWorkspaceNotFoundError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack-workspace-not-connected-to-any-organization"))
      }
    }
  }

  fun getInvalidSignatureError(): List<LayoutBlock> {
    return withBlocks {
      section {
        markdownText(i18n.translate("slack-invalid-signature"))
      }
    }
  }

  private fun ActionsBlockBuilder.helpButton() {
    button {
      value("help_btn")
      text(i18n.translate("view-help-button-text"), emoji = true)
    }
  }
}
