package io.tolgee.ee.service.slackIntegration

import io.tolgee.constants.Message
import io.tolgee.ee.repository.slackIntegration.SlackUserConnectionRepository
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.SlackUserConnection
import io.tolgee.util.Logging
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SlackUserConnectionService(
  private val slackUserConnectionRepository: SlackUserConnectionRepository,
) : Logging {
  fun get(id: Long): SlackUserConnection {
    return slackUserConnectionRepository.findById(id).get()
  }

  fun find(id: Long): SlackUserConnection? {
    return slackUserConnectionRepository.findById(id).orElse(null)
  }

  fun findByUserAccountId(id: Long): SlackUserConnection? {
    return slackUserConnectionRepository.findByUserAccountId(id)
  }

  fun findBySlackId(
    slackId: String,
    slackTeamId: String,
  ): SlackUserConnection? {
    return slackUserConnectionRepository.findBySlackUserIdAndSlackTeamId(slackId, slackTeamId)
  }

  fun save(slackUserConnection: SlackUserConnection): SlackUserConnection {
    return slackUserConnectionRepository.save(slackUserConnection)
  }

  fun isUserConnected(
    slackId: String,
    slackTeamId: String,
  ) = findBySlackId(slackId, slackTeamId) != null

  @Transactional
  fun createOrUpdate(
    userAccount: UserAccount,
    slackId: String,
    slackTeamId: String,
  ): SlackUserConnection {
    val old = findBySlackId(slackId, slackTeamId) ?: return createUnsafe(userAccount, slackId, slackTeamId)

    if (old.userAccount.id == userAccount.id && old.slackUserId == slackId) {
      return old
    }

    delete(old)
    return createUnsafe(userAccount, slackId, slackTeamId)
  }

  private fun createUnsafe(
    userAccount: UserAccount,
    slackId: String,
    slackTeamId: String?,
  ): SlackUserConnection {
    val slackUserConnection = SlackUserConnection()
    slackUserConnection.slackUserId = slackId
    slackUserConnection.userAccount = userAccount
    slackUserConnection.slackTeamId = slackTeamId ?: ""
    try {
      return slackUserConnectionRepository.saveAndFlush(slackUserConnection)
    } catch (e: DataIntegrityViolationException) {
      throw BadRequestException(Message.TOLGEE_ACCOUNT_ALREADY_CONNECTED)
    }
  }

  private fun delete(connection: SlackUserConnection) {
    return slackUserConnectionRepository.delete(connection)
  }

  @Transactional
  fun delete(
    slackId: String,
    slackTeamId: String,
  ): Boolean {
    val subscription = findBySlackId(slackId, slackTeamId) ?: return false
    slackUserConnectionRepository.delete(subscription)
    return true
  }
}
