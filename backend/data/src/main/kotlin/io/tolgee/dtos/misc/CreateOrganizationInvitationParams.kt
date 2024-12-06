package io.tolgee.dtos.misc

import io.tolgee.model.Organization
import io.tolgee.model.enums.OrganizationRoleType

data class CreateOrganizationInvitationParams(
  var organization: Organization,
  var type: OrganizationRoleType,
  override val email: String? = null,
  override val name: String? = null,
  override val agencyId: Long? = null,
) : CreateInvitationParams
