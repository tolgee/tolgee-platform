package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.PreferredOrganizationFacade
import io.tolgee.hateoas.InitialDataModel
import io.tolgee.hateoas.sso.PublicSsoTenantModelAssembler
import io.tolgee.hateoas.userAccount.PrivateUserAccountModelAssembler
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.TenantService
import io.tolgee.service.security.UserPreferencesService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// TODO: Check whether is matters that I removed the eeSubscription from initial data
@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(
  value = [
    "/v2/public/initial-data",
  ],
)
@Tag(name = "Initial data")
@OpenApiHideFromPublicDocs
class InitialDataController(
  private val configurationController: ConfigurationController,
  private val authenticationFacade: AuthenticationFacade,
  private val userPreferencesService: UserPreferencesService,
  private val preferredOrganizationFacade: PreferredOrganizationFacade,
//  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
//  private val eeSubscriptionModelAssembler: IEeSubscriptionModelAssembler?,
//  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
//  private val eeSubscriptionProvider: EeSubscriptionProvider?,
  private val announcementController: AnnouncementController,
  private val tenantService: TenantService,
  private val privateUserAccountModelAssembler: PrivateUserAccountModelAssembler,
  private val publicSsoTenantModelAssembler: PublicSsoTenantModelAssembler,
) : IController {
  @GetMapping(value = [""])
  @Operation(summary = "Get initial data", description = "Returns initial data required by the UI to load")
  fun get(): InitialDataModel {
    val data =
      InitialDataModel(
        serverConfiguration = configurationController.getPublicConfiguration(),
//        eeSubscription =
//          eeSubscriptionProvider?.findSubscriptionDto()?.let {
//            eeSubscriptionModelAssembler?.toModel(
//              it,
//            )
//          },
      )

    val userAccount = authenticationFacade.authenticatedUserOrNull
    if (userAccount != null) {
      val userAccountView = authenticationFacade.authenticatedUserView
      val tenant = tenantService.getEnabledConfigByDomainOrNull(userAccount.domain)
      data.userInfo = privateUserAccountModelAssembler.toModel(userAccountView)
      data.ssoInfo = tenant?.let { publicSsoTenantModelAssembler.toModel(it) }
      data.preferredOrganization = preferredOrganizationFacade.getPreferred()
      data.languageTag = userPreferencesService.find(userAccount.id)?.language
      data.announcement = announcementController.getLatest()
    }

    return data
  }
}
