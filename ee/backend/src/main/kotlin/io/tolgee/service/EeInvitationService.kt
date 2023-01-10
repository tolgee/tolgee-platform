package io.tolgee.service

import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.model.Invitation
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EeInvitationService(
  private val eePermissionService: EePermissionService,
  private val invitationService: InvitationService,
) {
  @Transactional
  fun create(params: CreateProjectInvitationParams): Invitation {
    return invitationService.create(params) { invitation ->
      eePermissionService.createForInvitation(
        invitation = invitation,
        params
      )
    }
  }
}
