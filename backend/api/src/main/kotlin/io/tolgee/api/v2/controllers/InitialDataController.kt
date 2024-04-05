package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.EeSubscriptionProvider
import io.tolgee.component.PreferredOrganizationFacade
import io.tolgee.hateoas.InitialDataModel
import io.tolgee.hateoas.ee.IEeSubscriptionModelAssembler
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.security.UserPreferencesService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
  private val userController: V2UserController,
  private val userPreferencesService: UserPreferencesService,
  private val preferredOrganizationFacade: PreferredOrganizationFacade,
  private val eeSubscriptionModelAssembler: IEeSubscriptionModelAssembler,
  private val eeSubscriptionProvider: EeSubscriptionProvider,
  private val announcementController: AnnouncementController,
) : IController {
  @GetMapping(value = [""])
  @Operation(summary = "Get initial data", description = "Returns initial data required by the UI to load")
  fun get(): InitialDataModel {
    val data =
      InitialDataModel(
        serverConfiguration = configurationController.getPublicConfiguration(),
        eeSubscription =
          eeSubscriptionProvider.findSubscriptionDto()?.let {
            eeSubscriptionModelAssembler.toModel(
              it,
            )
          },
      )

    val userAccount = authenticationFacade.authenticatedUserOrNull
    if (userAccount != null) {
      data.userInfo = userController.getInfo()
      data.preferredOrganization = preferredOrganizationFacade.getPreferred()
      data.languageTag = userPreferencesService.find(userAccount.id)?.language
      data.announcement = announcementController.getLatest()
    }

    return data
  }
}
