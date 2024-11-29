package io.tolgee.service.invitation

import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.model.Invitation

interface EeInvitationService {
  fun create(params: CreateProjectInvitationParams): Invitation
}
