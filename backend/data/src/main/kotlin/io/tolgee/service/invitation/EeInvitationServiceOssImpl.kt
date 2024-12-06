package io.tolgee.service.invitation

import io.tolgee.constants.Feature
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.exceptions.NotImplementedInOss
import io.tolgee.model.Invitation
import org.springframework.stereotype.Component

@Component
class EeInvitationServiceOssImpl : EeInvitationService {
  override fun create(params: CreateProjectInvitationParams): Invitation {
    throw NotImplementedInOss(Feature.GRANULAR_PERMISSIONS)
  }
}
