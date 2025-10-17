package io.tolgee.hateoas.initialData

import io.tolgee.api.publicConfiguration.PublicConfigurationDTO
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.hateoas.auth.AuthInfoModel
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.sso.PublicSsoTenantModel
import io.tolgee.hateoas.userAccount.PrivateUserAccountModel

class InitialDataModel(
  val serverConfiguration: PublicConfigurationDTO,
  var authInfo: AuthInfoModel? = null,
  var userInfo: PrivateUserAccountModel? = null,
  var ssoInfo: PublicSsoTenantModel? = null,
  var preferredOrganization: PrivateOrganizationModel? = null,
  var languageTag: String? = null,
  var announcement: AnnouncementDto? = null,
  var eeSubscription: InitialDataEeSubscriptionModel? = null,
)
