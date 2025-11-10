/*
 * Copyright (c) 2020. Tolgee
 */

package io.tolgee.api.v2.controllers.organization

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.tolgee.component.mtBucketSizeProvider.PayAsYouGoCreditsProvider
import io.tolgee.component.translationsLimitProvider.LimitsProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.isAdmin
import io.tolgee.dtos.queryResults.organization.OrganizationView
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.dtos.request.organization.OrganizationRequestParamsDto
import io.tolgee.dtos.request.organization.SetOrganizationRoleDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.hateoas.organization.OrganizationModel
import io.tolgee.hateoas.organization.OrganizationModelAssembler
import io.tolgee.hateoas.organization.PublicUsageModel
import io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModel
import io.tolgee.hateoas.organization.UserAccountWithOrganizationRoleModelAssembler
import io.tolgee.model.Project
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.openApiDocs.OpenApiOrderExtension
import io.tolgee.security.OrganizationHolder
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.AuthTokenType
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.IsGlobalRoute
import io.tolgee.security.authorization.RequiresOrganizationRole
import io.tolgee.security.authorization.UseDefaultPermissions
import io.tolgee.service.ImageUploadService
import io.tolgee.service.machineTranslation.mtCreditsConsumption.MtCreditsService
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.service.organization.OrganizationStatsService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.UserAccountService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedResourcesAssembler
import org.springframework.data.web.SortDefault
import org.springframework.hateoas.PagedModel
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@CrossOrigin(origins = ["*"])
@RequestMapping(value = ["/v2/organizations"])
@Tag(name = "Organizations")
@Suppress("SpringJavaInjectionPointsAutowiringInspection")
class OrganizationController(
  private val organizationService: OrganizationService,
  private val arrayResourcesAssembler: PagedResourcesAssembler<OrganizationView>,
  private val arrayUserResourcesAssembler: PagedResourcesAssembler<
    Pair<UserAccountWithOrganizationRoleView, List<Project>>,
  >,
  private val organizationModelAssembler: OrganizationModelAssembler,
  private val userAccountWithOrganizationRoleModelAssembler: UserAccountWithOrganizationRoleModelAssembler,
  private val tolgeeProperties: TolgeeProperties,
  private val authenticationFacade: AuthenticationFacade,
  private val organizationRoleService: OrganizationRoleService,
  private val userAccountService: UserAccountService,
  private val imageUploadService: ImageUploadService,
  private val mtCreditConsumer: MtCreditsService,
  private val organizationStatsService: OrganizationStatsService,
  private val limitsProvider: LimitsProvider,
  private val projectService: ProjectService,
  private val payAsYouGoCreditsProvider: PayAsYouGoCreditsProvider,
  private val organizationHolder: OrganizationHolder,
) {
  @PostMapping
  @Transactional
  @Operation(summary = "Create organization")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @IsGlobalRoute
  @OpenApiOrderExtension(1)
  fun create(
    @RequestBody @Valid
    dto: OrganizationDto,
  ): ResponseEntity<OrganizationModel> {
    if (!this.tolgeeProperties.authentication.userCanCreateOrganizations &&
      !authenticationFacade.authenticatedUser.isAdmin()
    ) {
      throw PermissionException()
    }
    if (authenticationFacade.authenticatedUserEntity.thirdPartyAuthType === ThirdPartyAuthType.SSO &&
      !authenticationFacade.authenticatedUser.isAdmin()
    ) {
      throw PermissionException(Message.SSO_USER_CANNOT_CREATE_ORGANIZATION)
    }
    this.organizationService.create(dto).let {
      return ResponseEntity(
        organizationModelAssembler.toModel(OrganizationView.of(it, OrganizationRoleType.OWNER)),
        HttpStatus.CREATED,
      )
    }
  }

  @GetMapping("/{id:[0-9]+}")
  @Operation(summary = "Get one organization")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @OpenApiOrderExtension(2)
  fun get(
    @PathVariable("id") id: Long,
  ): OrganizationModel? {
    val organization = organizationService.get(id)
    val roleType = organizationRoleService.findType(id)
    return OrganizationView.of(organization, roleType).toModel()
  }

  @GetMapping("/{slug:.*[a-z].*}")
  @Operation(summary = "Get organization by slug")
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @UseDefaultPermissions
  @OpenApiOrderExtension(3)
  fun get(
    @PathVariable("slug") slug: String,
  ): OrganizationModel {
    val organization = organizationService.get(slug)
    val roleType = organizationRoleService.findType(organization.id)
    return OrganizationView.of(organization, roleType).toModel()
  }

  @GetMapping("")
  @Operation(
    summary = "Get all permitted organizations",
    description = "Returns all organizations, which is current user allowed to view",
  )
  @AllowApiAccess(AuthTokenType.ONLY_PAT)
  @IsGlobalRoute
  @OpenApiOrderExtension(4)
  fun getAll(
    @ParameterObject
    @SortDefault(sort = ["id"])
    pageable: Pageable,
    @ParameterObject
    params: OrganizationRequestParamsDto,
  ): PagedModel<OrganizationModel>? {
    val organizations = organizationService.findPermittedPaged(pageable, params)
    return arrayResourcesAssembler.toModel(organizations, organizationModelAssembler)
  }

  @PutMapping("/{id:[0-9]+}")
  @Operation(summary = "Update organization data")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresSuperAuthentication
  @OpenApiOrderExtension(5)
  fun update(
    @PathVariable("id")
    id: Long,
    @RequestBody @Valid
    dto: OrganizationDto,
  ): OrganizationModel {
    return this.organizationService.edit(id, editDto = dto).toModel()
  }

  @DeleteMapping("/{id:[0-9]+}")
  @Operation(summary = "Delete organization", description = "Deletes organization and all its data including projects")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresSuperAuthentication
  @OpenApiOrderExtension(6)
  fun delete(
    @PathVariable("id") id: Long,
  ) {
    val org = organizationService.get(id)
    organizationService.delete(org)
  }

  @GetMapping("/{id:[0-9]+}/users")
  @Operation(
    summary = "Get all users in organization",
    description =
      "Returns all users in organization. " +
        "The result also contains users who are only members of projects in the organization.",
  )
  @RequiresOrganizationRole
  @RequiresSuperAuthentication
  fun getAllUsers(
    @PathVariable("id") id: Long,
    @ParameterObject
    @SortDefault(sort = ["name", "username"], direction = Sort.Direction.ASC)
    pageable: Pageable,
    @RequestParam("search") search: String?,
  ): PagedModel<UserAccountWithOrganizationRoleModel> {
    val allInOrganization = userAccountService.getAllInOrganization(id, pageable, search)
    val userIds = allInOrganization.content.map { it.id }
    val projectsWithDirectPermission = projectService.getProjectsWithDirectPermissions(id, userIds)
    val pairs =
      allInOrganization.content.map { user ->
        user to (projectsWithDirectPermission[user.id] ?: emptyList())
      }

    val data = PageImpl(pairs, allInOrganization.pageable, allInOrganization.totalElements)

    return arrayUserResourcesAssembler.toModel(data, userAccountWithOrganizationRoleModelAssembler)
  }

  @PutMapping("/{id:[0-9]+}/leave")
  @Operation(summary = "Leave organization", description = "Remove current user from organization")
  @UseDefaultPermissions
  @RequiresSuperAuthentication
  fun leaveOrganization(
    @PathVariable("id") id: Long,
  ) {
    organizationService.find(id)?.let {
      if (!organizationService.isThereAnotherOwner(id)) {
        throw ValidationException(Message.ORGANIZATION_HAS_NO_OTHER_OWNER)
      }
      organizationRoleService.leave(id)
    } ?: throw NotFoundException()
  }

  @PutMapping("/{organizationId:[0-9]+}/users/{userId:[0-9]+}/set-role")
  @Operation(summary = "Set user role", description = "Sets user role in organization. Owner or Member.")
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresSuperAuthentication
  fun setUserRole(
    @PathVariable organizationId: Long,
    @PathVariable("userId") userId: Long,
    @RequestBody dto: SetOrganizationRoleDto,
  ) {
    if (authenticationFacade.authenticatedUser.id == userId) {
      throw BadRequestException(Message.CANNOT_SET_YOUR_OWN_ROLE)
    }
    organizationRoleService.setMemberRole(organizationHolder.organization.id, userId, dto)
  }

  @DeleteMapping("/{organizationId:[0-9]+}/users/{userId:[0-9]+}")
  @Operation(
    summary = "Remove user from organization",
    description = (
      "Remove user from organization. " +
        "If user is managed by the organization, their account is disabled instead."
    ),
  )
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  @RequiresSuperAuthentication
  fun removeUser(
    @PathVariable organizationId: Long,
    @PathVariable("userId") userId: Long,
  ) {
    organizationRoleService.removeOrDeactivateUser(userId, organizationHolder.organization.id)
  }

  @PutMapping("/{id:[0-9]+}/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
  @Operation(summary = "Upload organizations avatar")
  @ResponseStatus(HttpStatus.OK)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun uploadAvatar(
    @RequestParam("avatar") avatar: MultipartFile,
    @PathVariable id: Long,
  ): OrganizationModel {
    imageUploadService.validateIsImage(avatar)
    val organization = organizationService.get(id)
    val roleType = organizationRoleService.getType(organization.id)
    organizationService.setAvatar(organization, avatar.inputStream)
    return organizationModelAssembler.toModel(OrganizationView.of(organization, roleType))
  }

  @DeleteMapping("/{id:[0-9]+}/avatar")
  @Operation(summary = "Delete organization avatar")
  @ResponseStatus(HttpStatus.OK)
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun removeAvatar(
    @PathVariable id: Long,
  ): OrganizationModel {
    val organization = organizationService.get(id)
    val roleType = organizationRoleService.getType(organization.id)
    organizationService.removeAvatar(organization)
    return organizationModelAssembler.toModel(OrganizationView.of(organization, roleType))
  }

  @PutMapping("/{organizationId:[0-9]+}/set-base-permissions/{permissionType}")
  @Operation(
    summary = "Set organization base permission",
    description = "Sets default (level-based) permission for organization",
  )
  @RequiresOrganizationRole(OrganizationRoleType.OWNER)
  fun setBasePermissions(
    @PathVariable organizationId: Long,
    @PathVariable permissionType: ProjectPermissionType,
  ) {
    organizationService.setBasePermission(organizationHolder.organization.id, permissionType)
  }

  @GetMapping(value = ["/{organizationId:[0-9]+}/usage"])
  @Operation(summary = "Get current organization usage")
  @RequiresOrganizationRole
  fun getUsage(
    @PathVariable organizationId: Long,
  ): PublicUsageModel {
    val organization = organizationHolder.organizationEntity
    val creditBalances = mtCreditConsumer.getCreditBalances(organization.id)
    val currentPayAsYouGoMtCredits = payAsYouGoCreditsProvider.getUsedPayAsYouGoCredits(organization)
    val availablePayAsYouGoMtCredits = payAsYouGoCreditsProvider.getPayAsYouGoAvailableCredits(organization)
    val currentTranslations = organizationStatsService.getTranslationCount(organization.id)
    val currentSeats = organizationStatsService.getSeatCountToCountSeats(organization.id)
    val currentKeys = organizationStatsService.getKeyCount(organization.id)
    val limits = limitsProvider.getLimits(organization.id)

    return PublicUsageModel(
      isPayAsYouGo = limits.isPayAsYouGo,
      organizationId = organization.id,
      creditBalance = creditBalances.creditBalance / 100,
      includedMtCredits = creditBalances.bucketSize / 100,
      creditBalanceRefilledAt = creditBalances.refilledAt.time,
      creditBalanceNextRefillAt = creditBalances.nextRefillAt.time,
      currentPayAsYouGoMtCredits = currentPayAsYouGoMtCredits,
      availablePayAsYouGoMtCredits = availablePayAsYouGoMtCredits,
      currentTranslations = currentTranslations,
      includedTranslations = limits.strings.included,
      translationsLimit = limits.strings.limit,
      includedKeys = limits.keys.included,
      keysLimit = limits.keys.limit,
      includedSeats = limits.seats.included,
      seatsLimit = limits.seats.limit,
      currentKeys = currentKeys,
      currentSeats = currentSeats,
      usedMtCredits = creditBalances.usedCredits / 100,
    )
  }

  private fun OrganizationView.toModel(): OrganizationModel {
    return this@OrganizationController.organizationModelAssembler.toModel(this)
  }
}
