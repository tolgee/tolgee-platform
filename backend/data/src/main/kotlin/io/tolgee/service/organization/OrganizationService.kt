package io.tolgee.service.organization

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.queryResults.organization.OrganizationView
import io.tolgee.dtos.queryResults.organization.PrivateOrganizationView
import io.tolgee.dtos.request.organization.OrganizationDto
import io.tolgee.dtos.request.organization.OrganizationRequestParamsDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.events.BeforeOrganizationDeleteEvent
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.Organization
import io.tolgee.model.Permission
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.OrganizationRoleType
import io.tolgee.model.enums.ProjectPermissionType
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.model.isAdmin
import io.tolgee.repository.OrganizationRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.AvatarService
import io.tolgee.service.QuickStartService
import io.tolgee.service.TenantService
import io.tolgee.service.invitation.InvitationService
import io.tolgee.service.project.ProjectService
import io.tolgee.service.security.PermissionService
import io.tolgee.service.security.UserPreferencesService
import io.tolgee.util.Logging
import io.tolgee.util.SlugGenerator
import io.tolgee.util.logger
import io.tolgee.util.surrogateSafeEnd
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.awt.Dimension
import java.io.InputStream
import io.tolgee.dtos.cacheable.OrganizationDto as CachedOrganizationDto

@Service
@Transactional
class OrganizationService(
  private val organizationRepository: OrganizationRepository,
  private val tenantService: TenantService?,
  private val authenticationFacade: AuthenticationFacade,
  private val slugGenerator: SlugGenerator,
  private val organizationRoleService: OrganizationRoleService,
  private val invitationService: InvitationService,
  private val avatarService: AvatarService,
  @Lazy
  private val userPreferencesService: UserPreferencesService,
  private val tolgeeProperties: TolgeeProperties,
  private val permissionService: PermissionService,
  private val cacheManager: CacheManager,
  private val currentDateProvider: CurrentDateProvider,
  private val eventPublisher: ApplicationEventPublisher,
  private val quickStartService: QuickStartService,
) : Logging {
  private val cache: Cache? by lazy { cacheManager.getCache(Caches.ORGANIZATIONS) }

  @set:Autowired
  lateinit var projectService: ProjectService

  @Transactional
  fun createAsCurrentUser(createDto: OrganizationDto): Organization {
    val userAccount = authenticationFacade.authenticatedUserEntity
    checkUserCanCreateOrganization(userAccount)
    return createWithoutAuthorization(createDto, userAccount)
  }

  @Transactional
  fun createWithoutAuthorization(
    createDto: OrganizationDto,
    userAccount: UserAccount,
  ): Organization {
    if (createDto.slug != null && !validateSlugUniqueness(createDto.slug!!)) {
      throw ValidationException(Message.SLUG_NOT_UNIQUE)
    }

    val name = boundedOrganizationName(createDto.name)

    val slug =
      createDto.slug
        ?: generateSlug(name)

    val basePermission =
      Permission(
        type = ProjectPermissionType.VIEW,
      )

    val organization =
      Organization(
        name = name,
        description = createDto.description,
        slug = slug,
      )

    organization.basePermission = basePermission

    basePermission.organization = organization
    permissionService.create(basePermission)

    organizationRepository.save(organization)
    organizationRoleService.grantOwnerRoleToUser(userAccount, organization)
    return organization
  }

  fun createPreferredWithoutAuthorization(
    userAccount: UserAccount,
    name: String = userAccount.name,
  ): Organization {
    val safeName = organizationNameFor(name, userAccount)
    return this.createWithoutAuthorization(OrganizationDto(name = safeName), userAccount = userAccount)
  }

  private fun organizationNameFor(
    name: String,
    userAccount: UserAccount,
  ): String {
    if (name.length >= 3) {
      return name
    }
    val username = userAccount.username
    val prefix = name.ifEmpty { username.take(surrogateSafeEnd(username, 3)) }
    return "$prefix Organization"
  }

  private fun boundedOrganizationName(name: String): String {
    if (name.length <= Organization.NAME_MAX_LENGTH) {
      return name
    }
    val end = surrogateSafeEnd(name, Organization.NAME_MAX_LENGTH)
    logger.info("Truncating organization name of length ${name.length} to $end characters")
    return name.substring(0, end)
  }

  private fun generateSlug(name: String) =
    slugGenerator.generate(name, 3, 60) {
      this.validateSlugUniqueness(it)
    }

  @Transactional(readOnly = true)
  fun findPreferred(
    userAccountId: Long,
    exceptOrganizationId: Long = 0,
  ): Organization? {
    return organizationRepository
      .findPreferred(
        userId = userAccountId,
        exceptOrganizationId,
        PageRequest.of(0, 1),
      ).content
      .firstOrNull()
  }

  fun findOrCreatePreferred(
    userAccount: UserAccount,
    exceptOrganizationId: Long = 0,
  ): Organization? {
    findPreferred(userAccount.id, exceptOrganizationId)?.let { return it }
    if (!canUserCreateOrganization(userAccount)) {
      return null
    }
    return createPreferredWithoutAuthorization(userAccount)
  }

  fun checkUserCanCreateOrganization(userAccount: UserAccount) {
    organizationCreationRefusal(userAccount)?.let { throw PermissionException(it) }
  }

  fun canUserCreateOrganization(userAccount: UserAccount): Boolean = organizationCreationRefusal(userAccount) == null

  private fun organizationCreationRefusal(userAccount: UserAccount): Message? {
    if (userAccount.isAdmin()) {
      return null
    }

    if (!tolgeeProperties.authentication.userCanCreateOrganizations) {
      return Message.OPERATION_NOT_PERMITTED
    }

    if (userAccount.thirdPartyAuthType == ThirdPartyAuthType.SSO) {
      return Message.SSO_USER_CANNOT_CREATE_ORGANIZATION
    }

    return null
  }

  @Transactional(readOnly = true)
  fun findPermittedPaged(
    pageable: Pageable,
    requestParamsDto: OrganizationRequestParamsDto,
    exceptOrganizationId: Long? = null,
  ): Page<OrganizationView> {
    return findPermittedPaged(
      pageable,
      requestParamsDto.filterCurrentUserOwner,
      requestParamsDto.search,
      exceptOrganizationId,
    )
  }

  @Transactional(readOnly = true)
  fun findPermittedPaged(
    pageable: Pageable,
    filterCurrentUserOwner: Boolean = false,
    search: String? = null,
    exceptOrganizationId: Long? = null,
  ): Page<OrganizationView> {
    return organizationRepository.findAllPermitted(
      userId = authenticationFacade.authenticatedUser.id,
      pageable = pageable,
      roleType = if (filterCurrentUserOwner) OrganizationRoleType.OWNER else null,
      search = search,
      exceptOrganizationId = exceptOrganizationId,
    )
  }

  @Transactional(readOnly = true)
  fun get(id: Long): Organization {
    return organizationRepository.find(id) ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)
  }

  @Transactional(readOnly = true)
  fun find(id: Long): Organization? {
    return organizationRepository.find(id)
  }

  @Transactional(readOnly = true)
  fun get(slug: String): Organization {
    return find(slug) ?: throw NotFoundException(Message.ORGANIZATION_NOT_FOUND)
  }

  @Transactional(readOnly = true)
  fun find(slug: String): Organization? {
    return organizationRepository.findBySlug(slug)
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = [Caches.ORGANIZATIONS], key = "{'id', #id}")
  fun findDto(id: Long): CachedOrganizationDto? {
    return find(id)?.let { CachedOrganizationDto.fromEntity(it) }
  }

  @Transactional(readOnly = true)
  @Cacheable(cacheNames = [Caches.ORGANIZATIONS], key = "{'slug', #slug}")
  fun findDto(slug: String): CachedOrganizationDto? {
    return find(slug)?.let { CachedOrganizationDto.fromEntity(it) }
  }

  @CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "{'id', #id}")
  fun edit(
    id: Long,
    editDto: OrganizationDto,
  ): OrganizationView {
    val organization = this.find(id) ?: throw NotFoundException()

    // Evict slug-based cache entry
    cache?.evict(arrayListOf("slug", organization.slug))

    val newSlug = editDto.slug ?: organization.slug
    if (newSlug != organization.slug && !validateSlugUniqueness(newSlug)) {
      throw ValidationException(Message.SLUG_NOT_UNIQUE)
    }

    organization.name = editDto.name
    organization.description = editDto.description
    organization.slug = newSlug

    organizationRepository.save(organization)
    return OrganizationView.of(organization, OrganizationRoleType.OWNER)
  }

  @Transactional
  @Caching(
    evict = [
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "{'id', #organization.id}"),
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "{'slug', #organization.slug}"),
    ],
  )
  fun delete(organization: Organization) {
    val tenant = organization.ssoTenant
    if (tenant != null && tenant.enabled) {
      tenant.enabled = false
      tenantService?.save(tenant)
    }
    organization.deletedAt = currentDateProvider.date
    save(organization)
    projectService.softDeleteAllInOrganization(organization.id)
    eventPublisher.publishEvent(BeforeOrganizationDeleteEvent(organization))
    organization.preferredBy
      .toList() // we need to clone it so hibernate doesn't change it concurrently
      .forEach {
        it.preferredOrganization =
          findOrCreatePreferred(
            userAccount = it.userAccount,
            exceptOrganizationId = organization.id,
          )
        userPreferencesService.save(it)
      }
  }

  @Transactional
  fun deleteHard(organization: Organization) {
    traceLogMeasureTime("deleteProjects") {
      projectService.findAllInOrganization(organization.id).forEach {
        projectService.deleteProject(it.id)
      }
    }

    traceLogMeasureTime("deleteInvitations") {
      invitationService.getForOrganization(organization).forEach { invitation ->
        invitationService.delete(invitation)
      }
    }

    traceLogMeasureTime("deleteOrganizationRoles") {
      organizationRoleService.onOrganizationDelete(organization)
    }

    traceLogMeasureTime("deleteTheOrganization") {
      this.organizationRepository.fetchData(organization)
      this.organizationRepository.delete(organization)
    }
    traceLogMeasureTime("unlinkAvatarFiles") {
      avatarService.unlinkAvatarFiles(organization)
    }
  }

  @Transactional
  @Caching(
    evict = [
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "#organization.id"),
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "#organization.slug"),
    ],
  )
  fun removeAvatar(organization: Organization) {
    avatarService.removeAvatar(organization)
  }

  @Transactional
  @Caching(
    evict = [
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "#organization.id"),
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "#organization.slug"),
    ],
  )
  fun setAvatar(
    organization: Organization,
    avatar: InputStream,
  ) {
    avatarService.setAvatar(organization, avatar, Dimension(300, 300))
  }

  /**
   * Checks slug uniqueness
   * @return Returns true if valid
   */
  @Transactional(readOnly = true)
  fun validateSlugUniqueness(slug: String): Boolean {
    return !organizationRepository.organizationWithSlugExists(slug)
  }

  @Transactional(readOnly = true)
  fun isThereAnotherOwner(id: Long): Boolean {
    return organizationRoleService.isAnotherOwnerInOrganization(id)
  }

  @Transactional(readOnly = true)
  fun generateSlug(
    name: String,
    oldSlug: String? = null,
  ): String {
    return slugGenerator.generate(name, 3, 60) {
      if (it == oldSlug) {
        return@generate true
      }
      this.validateSlugUniqueness(it)
    }
  }

  /**
   * Returns all organizations which are owned only by the specified user
   */
  @Transactional(readOnly = true)
  fun getAllSingleOwnedByUser(userAccount: UserAccount) = organizationRepository.getAllSingleOwnedByUser(userAccount)

  @Caching(
    evict = [
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "#organization.id"),
      CacheEvict(cacheNames = [Caches.ORGANIZATIONS], key = "#organization.slug"),
    ],
  )
  fun save(organization: Organization) {
    organizationRepository.save(organization)
  }

  @Transactional(readOnly = true)
  fun findAllPaged(
    pageable: Pageable,
    search: String?,
    userId: Long,
  ): Page<OrganizationView> {
    return organizationRepository.findAllViews(pageable, search, userId)
  }

  @Transactional(readOnly = true)
  fun findAllByName(name: String): List<Organization> {
    return organizationRepository.findAllByName(name)
  }

  @Transactional(readOnly = true)
  fun getProjectOwner(projectId: Long): Organization {
    return organizationRepository.getProjectOwner(projectId)
  }

  fun setBasePermission(
    organizationId: Long,
    permissionType: ProjectPermissionType,
  ) {
    // Cache eviction: Not necessary, base permission is not cached here
    val organization = get(organizationId)
    val basePermission = organization.basePermission
    basePermission.type = permissionType
    basePermission.scopes = arrayOf()
    permissionService.save(basePermission)
  }

  @Transactional(readOnly = true)
  fun findPrivateView(
    id: Long,
    currentUserId: Long,
  ): PrivateOrganizationView? {
    return findView(id, currentUserId)?.let {
      val quickStart = quickStartService.findView(currentUserId, id)
      PrivateOrganizationView(it, quickStart)
    }
  }

  @Transactional(readOnly = true)
  fun findView(
    id: Long,
    currentUserId: Long,
  ): OrganizationView? {
    return organizationRepository.findView(id, currentUserId)
  }
}
