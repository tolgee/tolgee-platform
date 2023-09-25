package io.tolgee.hateoas

import io.tolgee.configuration.PublicConfigurationDTO
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.ee.api.v2.hateoas.eeSubscription.EeSubscriptionModel
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.user_account.PrivateUserAccountModel

class InitialDataModel(
  val serverConfiguration: PublicConfigurationDTO,
  var userInfo: PrivateUserAccountModel? = null,
  var preferredOrganization: PrivateOrganizationModel? = null,
  var languageTag: String? = null,
  val eeSubscription: EeSubscriptionModel? = null,
  var announcement: AnnouncementDto? = null,
)
