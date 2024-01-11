package io.tolgee.hateoas

import io.tolgee.configuration.PublicConfigurationDTO
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.hateoas.ee.eeSubscription.EeSubscriptionModel
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.userAccount.PrivateUserAccountModel

class InitialDataModel(
  val serverConfiguration: PublicConfigurationDTO,
  var userInfo: PrivateUserAccountModel? = null,
  var preferredOrganization: PrivateOrganizationModel? = null,
  var languageTag: String? = null,
  val eeSubscription: EeSubscriptionModel? = null,
  var announcement: AnnouncementDto? = null,
)
