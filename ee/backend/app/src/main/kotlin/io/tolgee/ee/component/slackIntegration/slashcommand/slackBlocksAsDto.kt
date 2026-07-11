package io.tolgee.ee.component.slackIntegration.slashcommand

import com.slack.api.model.block.LayoutBlock
import com.slack.api.util.json.GsonFactory
import io.tolgee.dtos.response.SlackMessageDto

val SlackMessageDto?.asSlackResponseString: String?
  get() {
    this ?: return null
    return getJsonWithGsonAnonymInnerClassHandling(this)
  }

val List<LayoutBlock>.asSlackResponseString: String?
  get() {
    return SlackMessageDto(blocks = this).asSlackResponseString
  }

/**
 * This is taken from the Slack SDK
 */
private fun getJsonWithGsonAnonymInnerClassHandling(data: Any): String {
  return GSON.toJson(data)
}

private val GSON = GsonFactory.createSnakeCase()
