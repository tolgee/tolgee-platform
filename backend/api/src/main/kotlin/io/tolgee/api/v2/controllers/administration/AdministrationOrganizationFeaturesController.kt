package io.tolgee.api.v2.controllers.administration

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.api.v2.controllers.IController
import io.tolgee.component.enabledFeaturesProvider.EnabledFeaturesProvider
import io.tolgee.constants.Feature
import io.tolgee.constants.FeatureFlag
import io.tolgee.dtos.request.organization.SetOrganizationFeaturesRequest
import io.tolgee.hateoas.administration.FeatureFlagStateModel
import io.tolgee.hateoas.administration.OrganizationFeaturesModel
import io.tolgee.hateoas.administration.PlanFeatureStateModel
import io.tolgee.openApiDocs.OpenApiSelfHostedExtension
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.service.featureFlags.FeatureFlagService
import io.tolgee.service.featureFlags.OrganizationFeatureGrantService
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
  private val featureFlagService: FeatureFlagService,
  private val enabledFeaturesProvider: EnabledFeaturesProvider,
  private val authenticationFacade: AuthenticationFacade,
) : IController {
  @GetMapping
  @Operation(
    summary = "Get organization features",
    description =
      "Returns feature flags and plan features of the organization, " +
        "including those granted by a server administrator.",
  )
  @RequiresSuperAuthentication
  fun getOrganizationFeatures(
    @PathVariable organizationId: Long,
  ): OrganizationFeaturesModel {
    organizationService.get(organizationId)
    return getModel(organizationId)
  }

  @PutMapping
  @Operation(
    summary = "Set organization features",
    description =
      "Sets feature flags enabled for the organization " +
        "and plan features granted to it regardless of its plan.",
  )
  @RequiresSuperAuthentication
  fun setOrganizationFeatures(
    @PathVariable organizationId: Long,
    @RequestBody @Valid
    request: SetOrganizationFeaturesRequest,
  ): OrganizationFeaturesModel {
    organizationService.get(organizationId)
    organizationFeatureGrantService.setGrants(
      organizationId = organizationId,
      featureFlags = request.featureFlags,
      planFeatures = request.grantedFeatures,
      actor = authenticationFacade.authenticatedUserEntity,
    )
    return getModel(organizationId)
  }

  private fun getModel(organizationId: Long): OrganizationFeaturesModel {
    val grants = organizationFeatureGrantService.getGrants(organizationId)
    val enabledForAll = featureFlagService.getEnabledForAll()
    val fromPlan =
      organizationFeatureGrantService.withoutGrants { enabledFeaturesProvider.get(organizationId).toSet() }

    return OrganizationFeaturesModel(
      featureFlags =
        FeatureFlag.entries.map {
          FeatureFlagStateModel(
            flag = it,
            granted = it in grants.featureFlags,
            enabledForAll = it in enabledForAll,
          )
        },
      planFeatures =
        Feature.entries.map {
          PlanFeatureStateModel(
            feature = it,
            fromPlan = it in fromPlan,
            granted = it in grants.planFeatures,
          )
        },
      planFeatureGrantsAllowed = organizationFeatureGrantService.arePlanFeatureGrantsAllowed(),
    )
  }
}
