package io.tolgee.component

import com.slack.api.model.block.LayoutBlock
import io.tolgee.dtos.request.slack.SlackCommandDto

interface SlackErrorProvider {
  fun getInvalidSignatureError(): List<LayoutBlock>

  fun getWorkspaceNotFoundError(): List<LayoutBlock>

  fun getProjectNotFoundError(): List<LayoutBlock>

  fun getFeatureDisabledError(): List<LayoutBlock>

  fun getNoPermissionError(): List<LayoutBlock>

  fun getNotSubscribedYetError(): List<LayoutBlock>

  fun getInvalidGlobalSubscriptionError(): List<LayoutBlock>

  fun getInvalidLangTagError(): List<LayoutBlock>

  fun getInvalidCommandError(): List<LayoutBlock>

  fun getUserNotConnectedError(payload: SlackCommandDto): List<LayoutBlock>
}
