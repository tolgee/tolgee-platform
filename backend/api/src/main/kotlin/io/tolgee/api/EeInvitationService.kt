package io.tolgee.api

import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.model.Invitation
import org.springframework.transaction.annotation.Transactional

interface EeInvitationService {
  @Transactional
  fun create(params: CreateProjectInvitationParams): Invitation
}
