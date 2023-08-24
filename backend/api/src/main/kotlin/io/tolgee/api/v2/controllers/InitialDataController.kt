package io.tolgee.api.v2.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.PreferredOrganizationFacade
import io.tolgee.dtos.response.AnnouncementDto
import io.tolgee.ee.api.v2.hateoas.eeSubscription.EeSubscriptionModelAssembler
import io.tolgee.ee.service.EeSubscriptionService
import io.tolgee.hateoas.InitialDataModel
import io.tolgee.security.AuthenticationFacade
import io.tolgee.service.AnnouncementService
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
  ]
)
@Tag(name = "Initial data")
class InitialDataController(
  private val configurationController: ConfigurationController,
  private val authenticationFacade: AuthenticationFacade,
  private val userController: V2UserController,
  private val userPreferencesService: UserPreferencesService,
  private val preferredOrganizationFacade: PreferredOrganizationFacade,
  private val eeSubscriptionModelAssembler: EeSubscriptionModelAssembler,
  private val eeSubscriptionService: EeSubscriptionService,
  private val announcementService: AnnouncementService
) : IController {
  @GetMapping(value = [""])
  @Operation(description = "Returns initial data always required by frontend")
  fun get(): InitialDataModel {

    val data = InitialDataModel(
      serverConfiguration = configurationController.getPublicConfiguration(),
      eeSubscription = eeSubscriptionService.findSubscriptionEntity()?.let { eeSubscriptionModelAssembler.toModel(it) }
    )

    val userAccount = authenticationFacade.userAccountOrNull
    if (userAccount != null) {
      data.userInfo = userController.getInfo()
      data.preferredOrganization = preferredOrganizationFacade.getPreferred()
      data.languageTag = userPreferencesService.find(userAccount.id)?.language
      data.announcement = announcementService.getAnnouncement()?.let { AnnouncementDto.fromEntity(it) }
    }

    return data
  }
}
