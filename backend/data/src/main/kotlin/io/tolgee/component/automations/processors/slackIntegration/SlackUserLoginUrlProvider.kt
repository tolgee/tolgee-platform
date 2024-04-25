package io.tolgee.component.automations.processors.slackIntegration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.tolgee.component.Aes
import io.tolgee.component.FrontendUrlProvider
import io.tolgee.constants.Message
import io.tolgee.dtos.request.slack.SlackUserLoginDto
import io.tolgee.exceptions.BadRequestException
import org.springframework.stereotype.Component
import java.util.*

@Component
class SlackUserLoginUrlProvider(
  private val frontendUrlProvider: FrontendUrlProvider,
  private val objectMapper: ObjectMapper,
  private val aes: Aes,
) {
  fun getUrl(
    slackChannelId: String,
    slackUserId: String,
    workspaceId: Long?,
  ): String {
    val dto = getDto(slackChannelId, slackUserId, workspaceId)
    val encryptedData = encryptData(dto)
    return "${frontendUrlProvider.url}/slack/connect?data=$encryptedData"
  }

  private fun encryptData(dto: SlackUserLoginDto): String {
    val stringData = objectMapper.writeValueAsString(dto)
    val encrypted = aes.encrypt(stringData.toByteArray())
    return Base64.getUrlEncoder().encode(encrypted).toString(Charsets.UTF_8)
  }

  fun decryptData(data: String): SlackUserLoginDto {
    try {
      val bytes = Base64.getUrlDecoder().decode(data)
      val decrypted = aes.decrypt(bytes)
      return objectMapper.readValue<SlackUserLoginDto>(decrypted)
    } catch (e: Exception) {
      throw BadRequestException(Message.CANNOT_PARSE_ENCRYPTED_SLACK_LOGIN_DATA)
    }
  }

  private fun getDto(
    slackChannelId: String,
    slackUserId: String,
    workspaceId: Long?,
  ): SlackUserLoginDto {
    return SlackUserLoginDto(
      slackUserId = slackUserId,
      slackChannelId = slackChannelId,
      workspaceId = workspaceId,
    )
  }
}
