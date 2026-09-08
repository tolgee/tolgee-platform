package io.tolgee.ee.service.connectedApps

import io.tolgee.component.CurrentDateProvider
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.enums.AuthAuditEventType
import io.tolgee.model.oauth2.OAuth2Grant
import io.tolgee.repository.oauth2.OAuth2GrantRepository
import io.tolgee.security.oauth2.OAuth2ClientRegistry
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.AuthAuditService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ConnectedAppService(
  private val grantRepository: OAuth2GrantRepository,
  private val clientRegistry: OAuth2ClientRegistry,
  private val projectService: ProjectService,
  private val authAuditService: AuthAuditService,
  private val currentDateProvider: CurrentDateProvider,
) {
  fun find(
    userAccountId: Long,
    pageable: Pageable,
  ): Page<ConnectedAppView> {
    val clientIds = clientRegistry.clients.map { it.clientId }
    if (clientIds.isEmpty()) return Page.empty<ConnectedAppView>(pageable)
    val page = grantRepository.findConnected(userAccountId, clientIds, currentDateProvider.date, pageable)
    return PageImpl(toViews(page.content), pageable, page.totalElements)
  }

  @Transactional
  fun revoke(
    id: Long,
    userAccountId: Long,
  ) {
    val grant = ownGrant(id, userAccountId)
    record(grant)
    grantRepository.delete(grant)
  }

  @Transactional
  fun revokeAllForUser(userAccountId: Long): Int {
    val clientIds = clientRegistry.clients.map { it.clientId }
    if (clientIds.isNotEmpty()) {
      grantRepository.findConnected(userAccountId, clientIds, currentDateProvider.date).forEach { record(it) }
    }
    return grantRepository.deleteAllByUserAccountId(userAccountId)
  }

  /**
   * Revocation deletes the row, so ownership cannot be re-checked afterwards. Missing, foreign and
   * already-revoked are therefore all reported the same way: a distinct status would tell a caller
   * which grant ids exist on other accounts.
   */
  private fun ownGrant(
    id: Long,
    userAccountId: Long,
  ): OAuth2Grant {
    val grant = grantRepository.findById(id).orElse(null) ?: throw NotFoundException()
    if (grant.userAccount.id != userAccountId) throw NotFoundException()
    return grant
  }

  private fun record(grant: OAuth2Grant) {
    authAuditService.record(
      type = AuthAuditEventType.OAUTH_GRANT_REVOKED,
      userAccountId = grant.userAccount.id,
      targetId = grant.id,
      data = mutableMapOf("clientId" to grant.clientId, "initiator" to "USER"),
    )
  }

  private fun toViews(grants: List<OAuth2Grant>): List<ConnectedAppView> {
    val boundIds = grants.mapNotNull { it.boundProjectIds() }.flatten().toSet()
    val names = projectService.findAll(boundIds).associate { it.id to it.name }
    return grants.map { grant ->
      val ids = grant.boundProjectIds()
      ConnectedAppView(
        grant = grant,
        clientName = clientRegistry.find(grant.clientId)?.name ?: grant.clientId,
        allProjects = ids == null,
        projects = ids.orEmpty().map { ConnectedAppProject(it, names[it] ?: it.toString()) },
      )
    }
  }
}
