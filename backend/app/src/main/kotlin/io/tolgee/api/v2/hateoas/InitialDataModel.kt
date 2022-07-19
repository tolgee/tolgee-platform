package io.tolgee.api.v2.hateoas

import io.tolgee.api.v2.hateoas.organization.OrganizationModel
import io.tolgee.api.v2.hateoas.user_account.UserAccountModel
import io.tolgee.configuration.PublicConfigurationDTO

class InitialDataModel(
  val serverConfiguration: PublicConfigurationDTO,
  var userInfo: UserAccountModel? = null,
  var preferredOrganization: OrganizationModel? = null,
  var languageTag: String? = null
)
