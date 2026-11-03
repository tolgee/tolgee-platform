package io.tolgee.hateoas.initialData

import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.api.OnboardingQuestionnaireVersion
import io.tolgee.api.publicConfiguration.PublicConfigurationDTO
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.hateoas.auth.AuthInfoModel
import io.tolgee.hateoas.organization.PrivateOrganizationModel
import io.tolgee.hateoas.qa.QaCheckCategoryModel
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
  var qaCheckCategories: List<QaCheckCategoryModel>? = null,
  @Schema(
    description =
      "Whether the user has contributed to a community (public) project they are not a member of. " +
        "Drives the \"Community projects\" entry in the organization switcher.",
  )
  var hasCommunityContributions: Boolean = false,
  @Schema(
    description =
      "Whether the user has at least one counted contribution in a community (public) project, as a member or " +
        "not. This is the rule that makes a community profile visible by default.",
  )
  var hasPublicContribution: Boolean = false,
  var onboardingSurveyVersion: OnboardingQuestionnaireVersion? = null,
)
