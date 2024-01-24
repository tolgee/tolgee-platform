package io.tolgee.ee.service

import io.tolgee.api.EeInvitationService
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.misc.CreateProjectInvitationParams
import io.tolgee.model.Invitation
import io.tolgee.service.InvitationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EeInvitationServiceImpl(
  private val eePermissionService: EePermissionService,
  private val invitationService: InvitationService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
) : EeInvitationService {
  @Transactional
  override fun create(params: CreateProjectInvitationParams): Invitation {
    enabledFeaturesProvider.checkFeatureEnabled(params.project.organizationOwner.id, Feature.GRANULAR_PERMISSIONS)
    return invitationService.create(params) { invitation ->
      eePermissionService.createForInvitation(
        invitation = invitation,
        params,
      )
    }
  }
}
