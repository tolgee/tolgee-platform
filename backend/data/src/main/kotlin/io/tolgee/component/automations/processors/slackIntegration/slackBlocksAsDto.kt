package io.tolgee.component.automations.processors.slackIntegration

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.slack.api.model.block.LayoutBlock
import io.tolgee.dtos.response.SlackMessageDto

/**
 * When serializing the LayoutBlock to JSON, our default mapper keeps the null values in the json,
 * which makes Slack to throw an error. This extension function removes the null values from the data.
 */
val List<LayoutBlock>.asSlackMessageDto: SlackMessageDto
  get() {
    val mapper = jacksonObjectMapper()
    val withoutNulls =
      mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
        .writeValueAsString(this)

    val resultBlocks = mapper.readValue<List<Any>>(withoutNulls)

    return SlackMessageDto(blocks = resultBlocks)
  }
