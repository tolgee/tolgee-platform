package io.tolgee.api.v2.controllers.administration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.dtos.request.organization.SetOrganizationFeaturesRequest
import io.tolgee.hateoas.administration.OrganizationFeaturesModel
import io.tolgee.hateoas.administration.PlanFeatureStateModel
import io.tolgee.openApiDocs.OpenApiSelfHostedExtension
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.organization.OrganizationFeatureGrantChangeService
import io.tolgee.service.organization.OrganizationFeatureGrantService
import io.tolgee.service.organization.OrganizationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/administration/organizations/{organizationId:[0-9]+}/features"])
@Tag(name = "Server Administration")
@OpenApiSelfHostedExtension
class AdministrationOrganizationFeaturesController(
  private val organizationService: OrganizationService,
  private val organizationFeatureGrantService: OrganizationFeatureGrantService,
  private val organizationFeatureGrantChangeService: OrganizationFeatureGrantChangeService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  @GetMapping
  @Operation(
    summary = "Get organization features",
    description =
      "Returns plan features of the organization, " +
        "including those granted by a server administrator.",
  )
  @RequiresSuperAuthentication
  fun getOrganizationFeatures(
    @PathVariable organizationId: Long,
  ): OrganizationFeaturesModel {
    checkOrganizationExists(organizationId)
    return getModel(organizationId)
  }

  @PutMapping
  @Operation(
    summary = "Set organization features",
    description = "Sets plan features granted to the organization regardless of its plan.",
  )
  @RequiresSuperAuthentication
  fun setOrganizationFeatures(
    @PathVariable organizationId: Long,
    @RequestBody @Valid
    request: SetOrganizationFeaturesRequest,
  ): OrganizationFeaturesModel {
    checkOrganizationExists(organizationId)
    organizationFeatureGrantChangeService.setGrants(
      organizationId = organizationId,
      grantedFeatures = request.grantedFeatures,
      actor = authenticationFacade.authenticatedUserEntity,
    )
    return getModel(organizationId)
  }

  private fun checkOrganizationExists(organizationId: Long) {
    organizationService.get(organizationId)
  }

  private fun getModel(organizationId: Long): OrganizationFeaturesModel {
    val grants = organizationFeatureGrantService.getStoredGrants(organizationId)
    val fromPlan = enabledFeaturesProvider.getPlanFeatures(organizationId).toSet()

    return OrganizationFeaturesModel(
      planFeatures =
        Feature.entries.map {
          PlanFeatureStateModel(
            feature = it,
            fromPlan = it in fromPlan,
            granted = it in grants.features,
            hidden = it.hidden,
          )
        },
      planFeatureGrantsAllowed = organizationFeatureGrantService.arePlanFeatureGrantsAllowed(),
    )
  }
}
