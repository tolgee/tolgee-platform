package io.tolgee.ee.service.slackIntegration

import io.tolgee.model.UserAccount
import io.tolgee.model.slackIntegration.SlackUserConnection
import io.tolgee.repository.slackIntegration.SlackUserConnectionRepository
import io.tolgee.util.Logging
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

  fun findBySlackId(slackId: String): SlackUserConnection? {
    return slackUserConnectionRepository.findBySlackUserId(slackId)
  }

  fun save(slackUserConnection: SlackUserConnection): SlackUserConnection {
    return slackUserConnectionRepository.save(slackUserConnection)
  }

  fun isUserConnected(slackId: String) = findBySlackId(slackId) != null

  @Transactional
  fun createOrUpdate(
    userAccount: UserAccount,
    slackId: String,
  ): SlackUserConnection {
    val old = findBySlackId(slackId) ?: return createUnsafe(userAccount, slackId)

    if (old.userAccount.id == userAccount.id && old.slackUserId == slackId) {
      return old
    }

    delete(old)
    return createUnsafe(userAccount, slackId)
  }

  private fun createUnsafe(
    userAccount: UserAccount,
    slackId: String,
  ): SlackUserConnection {
    val slackUserConnection = SlackUserConnection()
    slackUserConnection.slackUserId = slackId
    slackUserConnection.userAccount = userAccount
    return slackUserConnectionRepository.saveAndFlush(slackUserConnection)
  }

  private fun delete(connection: SlackUserConnection) {
    return slackUserConnectionRepository.delete(connection)
  }

  @Transactional
  fun delete(slackId: String): Boolean {
    val subscription = findBySlackId(slackId) ?: return false
    slackUserConnectionRepository.delete(subscription)
    return true
  }
}
