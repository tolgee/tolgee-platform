package io.tolgee.api.v2.controllers.machineTranslation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.dtos.MtCreditBalanceDto
import io.tolgee.hateoas.machineTranslation.CreditBalanceModel
import io.tolgee.security.ProjectHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService
import io.tolgee.service.organization.OrganizationService
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2"])
@Tag(name = "Machine translation credits")
class MtCreditsController(
  private val projectHolder: ProjectHolder,
  private val mtCreditsService: MtCreditsService,
  private val organizationService: OrganizationService,
) {
  @GetMapping("/projects/{projectId:\\d+}/machine-translation-credit-balance")
  @Operation(
    summary = "Get credit balance for project",
    description = "Returns machine translation credit balance for specified project",
  )
  @UseDefaultPermissions
  @AllowApiAccess
  fun getProjectCredits(
    @PathVariable projectId: Long,
  ): CreditBalanceModel {
    return mtCreditsService.getCreditBalances(projectHolder.project.organizationOwnerId).model
  }

  @GetMapping("/organizations/{organizationId:\\d+}/machine-translation-credit-balance")
  @Operation(
    summary = "Get credit balance for organization",
    description = "Returns machine translation credit balance for organization",
  )
  @RequiresOrganizationRole
  @AllowApiAccess
  fun getOrganizationCredits(
    @PathVariable organizationId: Long,
  ): CreditBalanceModel {
    val organization = organizationService.get(organizationId)
    return mtCreditsService.getCreditBalances(organization.id).model
  }

  private val MtCreditBalanceDto.model
    get() =
      CreditBalanceModel(
        creditBalance = this.creditBalance / 100,
        bucketSize = this.bucketSize / 100,
      )
}
