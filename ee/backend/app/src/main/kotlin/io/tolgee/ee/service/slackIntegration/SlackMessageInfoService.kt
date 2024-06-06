package io.tolgee.ee.service.slackIntegration

import io.tolgee.ee.repository.slackIntegration.SlackMessageInfoRepository
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.slackIntegration.SavedSlackMessage
import io.tolgee.model.slackIntegration.SlackMessageInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SlackMessageInfoService(
  private val slackMessageInfoRepository: SlackMessageInfoRepository,
) {
  fun create(slackMessageInfo: SlackMessageInfo): SlackMessageInfo {
    return slackMessageInfoRepository.save(slackMessageInfo)
  }

  fun update(
    slackMessageInfo: SlackMessageInfo,
    authorContext: String,
  ): SlackMessageInfo {
    val slackMessageInfoEntity = get(slackMessageInfo.id)
    slackMessageInfoEntity.authorContext = authorContext
    return slackMessageInfoRepository.save(slackMessageInfo)
  }

  fun get(id: Long): SlackMessageInfo {
    return slackMessageInfoRepository.findById(id).orElseThrow { NotFoundException() }
  }

  fun delete(slackMessageInfo: SlackMessageInfo) {
    slackMessageInfoRepository.delete(slackMessageInfo)
  }

  fun deleteAll(slackMessageInfo: List<SlackMessageInfo>) {
    slackMessageInfoRepository.deleteAll(slackMessageInfo)
  }

  fun create(
    savedSlackMessage: SavedSlackMessage,
    it: String,
    authorContext: String,
  ): SlackMessageInfo {
    val slackMessageInfo =
      SlackMessageInfo(
        slackMessage = savedSlackMessage,
        languageTag = it,
      )
    slackMessageInfo.authorContext = authorContext
    return create(slackMessageInfo)
  }
}
