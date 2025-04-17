package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.demoProject.DemoProjectData
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.queryResults.UserAccountView
import io.tolgee.dtos.request.UserUpdatePasswordRequestDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.dtos.request.task.UserAccountFilters
import io.tolgee.dtos.request.userAccount.UserAccountPermissionsFilters
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.events.OnUserCountChanged
import io.tolgee.events.user.OnUserCreated
import io.tolgee.events.user.OnUserUpdated
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.UserAccount
import io.tolgee.model.enums.ThirdPartyAuthType
import io.tolgee.model.notifications.Notification
import io.tolgee.model.notifications.NotificationType
import io.tolgee.model.views.ExtendedUserAccountInProject
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.AiPlaygroundResultService
import io.tolgee.service.AvatarService
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.notification.NotificationService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.util.Logging
import io.tolgee.util.addMinutes
import jakarta.persistence.EntityManager
import jakarta.servlet.http.HttpServletRequest
import org.apache.commons.lang3.time.DateUtils
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*

@Service
class UserAccountService(
  @Lazy
  private val userAccountRepository: UserAccountRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val tolgeeProperties: TolgeeProperties,
  private val avatarService: AvatarService,
  private val passwordEncoder: PasswordEncoder,
  @Lazy
  private val organizationService: OrganizationService,
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
  private val cacheManager: CacheManager,
  @Suppress("SelfReferenceConstructorParameter")
  @Lazy
  private val self: UserAccountService,
) : Logging {
  @Autowired
  @Lazy
  private lateinit var aiPlaygroundResultService: AiPlaygroundResultService

  @Autowired
  lateinit var emailVerificationService: EmailVerificationService

  @Autowired
  @Lazy
  lateinit var permissionService: PermissionService

  @Autowired
  private lateinit var notificationService: NotificationService

  private val emailValidator = EmailValidator()

  fun findActive(username: String): UserAccount? {
    return userAccountRepository.findActive(username)
  }

  operator fun get(username: String): UserAccount {
    return this.findActive(username) ?: throw NotFoundException(Message.USER_NOT_FOUND)
  }

  @Transactional
  fun findActive(id: Long): UserAccount? {
    return userAccountRepository.findActive(id)
  }

  @Transactional
  fun findInitialUser(): UserAccount? {
    return userAccountRepository.findInitialUser()
  }

  @Transactional
  fun get(id: Long): UserAccount {
    return this.findActive(id) ?: throw NotFoundException(Message.USER_NOT_FOUND)
  }

  @Cacheable(cacheNames = [Caches.USER_ACCOUNTS], key = "#id")
  @Transactional
  fun findDto(id: Long): UserAccountDto? {
    return userAccountRepository.findActive(id)?.let {
      UserAccountDto.fromEntity(it)
    }
  }

  @Transactional
  fun findAll(ids: List<Long>): List<UserAccountDto> {
    return userAccountRepository.findAllById(ids).map { UserAccountDto.fromEntity(it) }
  }

  @Transactional
  fun getDto(id: Long): UserAccountDto {
    return self.findDto(id) ?: throw NotFoundException(Message.USER_NOT_FOUND)
  }

  fun getOrCreateDemoUsers(demoUsers: List<DemoProjectData.DemoUser>): Map<String, UserAccount> {
    val usernames = demoUsers.map { it.username }
    val existingUsers = userAccountRepository.findDemoByUsernames(usernames)
    return demoUsers.associate { demoUser ->
      val existingUser =
        existingUsers.find { existingUser -> demoUser.username == existingUser.username }
          ?: createDemoUser(demoUser)
      demoUser.username to existingUser
    }
  }

  private fun createDemoUser(demoUser: DemoProjectData.DemoUser): UserAccount {
    val user = UserAccount()
    user.username = demoUser.username
    user.name = demoUser.name
    user.isDemo = true
    user.disabledAt = currentDateProvider.date

    setDemoUserAvatar(demoUser, user)

    return save(user)
  }

  private fun setDemoUserAvatar(
    demoUser: DemoProjectData.DemoUser,
    user: UserAccount,
  ) {
    val stream =
      javaClass.getResourceAsStream("/demoProject/userAvatars/${demoUser.avatarFileName}")
        ?: throw IllegalArgumentException("Demo user avatar doesn't exist")

    avatarService.setAvatar(user, stream)
  }

  @Transactional
  fun createUser(
    userAccount: UserAccount,
    userSource: String? = null,
  ): UserAccount {
    applicationEventPublisher.publishEvent(OnUserCreated(this, userAccount, userSource))
    userAccountRepository.saveAndFlush(userAccount)
    applicationEventPublisher.publishEvent(OnUserCountChanged(decrease = false, this))
    return userAccount
  }

  @Transactional
  fun createUserWithPassword(
    userAccount: UserAccount,
    rawPassword: String,
  ): UserAccount {
    userAccountRepository.saveAndFlush(userAccount)
    userAccount.password = passwordEncoder.encode(rawPassword)
    applicationEventPublisher.publishEvent(OnUserCreated(this, userAccount))
    applicationEventPublisher.publishEvent(OnUserCountChanged(decrease = false, this))
    return userAccount
  }

  @CacheEvict(Caches.USER_ACCOUNTS, key = "#id")
  @Transactional
  fun delete(id: Long) {
    val user = this.get(id)
    delete(user)
    this.applicationEventPublisher.publishEvent(OnUserCountChanged(decrease = true, this))
  }

  @CacheEvict(Caches.USER_ACCOUNTS, key = "#userAccount.id")
  @Transactional
  fun delete(userAccount: UserAccount) {
    traceLogMeasureTime("deleteUser") {
      val toDelete =
        userAccountRepository.findWithFetchedEmailVerificationAndPermissions(userAccount.id)
          ?: throw NotFoundException()
      deleteWithFetchedData(toDelete)
    }
  }

  private fun deleteWithFetchedData(toDelete: UserAccount) {
    toDelete.emailVerification?.let {
      entityManager.remove(it)
    }
    toDelete.apiKeys?.forEach {
      entityManager.remove(it)
    }
    toDelete.pats?.forEach {
      entityManager.remove(it)
    }
    toDelete.permissions.forEach {
      entityManager.remove(it)
    }
    toDelete.preferences?.let {
      entityManager.remove(it)
    }
    toDelete.invitations?.forEach {
      entityManager.remove(it)
    }
    organizationService.getAllSingleOwnedByUser(toDelete).forEach {
      it.preferredBy.removeIf { preferences ->
        preferences.userAccount.id == toDelete.id
      }
      organizationService.delete(it)
    }
    toDelete.organizationRoles.forEach {
      entityManager.remove(it)
    }
    aiPlaygroundResultService.deleteResultsByUser(toDelete.id)
    userAccountRepository.softDeleteUser(toDelete, currentDateProvider.date)
    applicationEventPublisher.publishEvent(OnUserCountChanged(decrease = true, this))
  }

  @Transactional
  fun deleteByUserNames(usernames: List<String>) {
    val data = userAccountRepository.findActiveWithFetchedDataByUserNames(usernames)
    data.forEach {
      deleteWithFetchedData(it)
      cacheManager.getCache(Caches.USER_ACCOUNTS)?.evict(it.id)
    }
  }

  fun findByThirdParty(
    type: ThirdPartyAuthType,
    id: String,
  ): UserAccount? {
    return userAccountRepository.findThirdByThirdParty(id, type)
  }

  fun findEnabledBySsoDomain(
    type: String,
    idSub: String,
  ): UserAccount? = userAccountRepository.findEnabledBySsoDomain(idSub, type)

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setAccountType(
    userAccount: UserAccount,
    accountType: UserAccount.AccountType,
  ): UserAccount {
    userAccount.accountType = accountType
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setResetPasswordCode(
    userAccount: UserAccount,
    code: String?,
  ): UserAccount {
    userAccount.resetPasswordCode = passwordEncoder.encode(code)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setUserPassword(
    userAccount: UserAccount,
    password: String?,
  ): UserAccount {
    resetTokensValidNotBefore(userAccount)
    userAccount.password = passwordEncoder.encode(password)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  fun isResetCodeValid(
    userAccount: UserAccount,
    code: String?,
  ): Boolean {
    return passwordEncoder.matches(code, userAccount.resetPasswordCode)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun removeResetCode(userAccount: UserAccount): UserAccount {
    userAccount.resetPasswordCode = null
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun enableMfaTotp(
    userAccount: UserAccount,
    key: ByteArray,
  ): UserAccount {
    userAccount.totpKey = key
    resetTokensValidNotBefore(userAccount)
    val savedUser = userAccountRepository.save(userAccount)
    notificationService.notify(
      Notification().apply {
        this.user = userAccount
        this.type = NotificationType.MFA_ENABLED
        this.originatingUser = userAccount
      },
    )
    return savedUser
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun disableMfaTotp(userAccount: UserAccount): UserAccount {
    userAccount.totpKey = null
    // note: if support for more MFA methods is added, this should be only done if no other MFA method is enabled
    userAccount.mfaRecoveryCodes = emptyList()
    resetTokensValidNotBefore(userAccount)
    val savedUser = userAccountRepository.save(userAccount)
    notificationService.notify(
      Notification().apply {
        this.user = userAccount
        this.type = NotificationType.MFA_DISABLED
        this.originatingUser = userAccount
      },
    )
    return savedUser
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun consumeMfaRecoveryCode(
    userAccount: UserAccount,
    token: String,
  ): UserAccount {
    if (!userAccount.mfaRecoveryCodes.contains(token)) {
      throw AuthenticationException(Message.INVALID_OTP_CODE)
    }

    userAccount.mfaRecoveryCodes = userAccount.mfaRecoveryCodes.minus(token)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setMfaRecoveryCodes(
    userAccount: UserAccount,
    codes: List<String>,
  ): UserAccount {
    userAccount.mfaRecoveryCodes = codes
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userAccount.id")
  fun updateSsoSession(
    userAccount: UserAccount,
    refreshToken: String?,
  ): UserAccount {
    userAccount.ssoRefreshToken = refreshToken
    userAccount.ssoSessionExpiry = getCurrentSsoExpiration(userAccount.thirdPartyAuthType)
    return userAccountRepository.save(userAccount)
  }

  fun getCurrentSsoExpiration(type: ThirdPartyAuthType?): Date? {
    return currentDateProvider.date.addMinutes(
      when (type) {
        ThirdPartyAuthType.SSO -> tolgeeProperties.authentication.ssoOrganizations.sessionExpirationMinutes
        ThirdPartyAuthType.SSO_GLOBAL -> tolgeeProperties.authentication.ssoGlobal.sessionExpirationMinutes
        else -> return null
      },
    )
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userAccount.id")
  fun removeAvatar(userAccount: UserAccount) {
    avatarService.removeAvatar(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userAccount.id")
  fun setAvatar(
    userAccount: UserAccount,
    avatar: InputStream,
  ) {
    avatarService.setAvatar(userAccount, avatar)
  }

  fun getAllInOrganization(
    organizationId: Long,
    pageable: Pageable,
    search: String?,
  ): Page<UserAccountWithOrganizationRoleView> {
    return userAccountRepository.getAllInOrganization(organizationId, pageable, search = search ?: "")
  }

  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
    search: String?,
    exceptUserId: Long? = null,
    filters: UserAccountFilters = UserAccountFilters(),
  ): Page<UserAccountInProjectView> {
    return userAccountRepository.getAllInProject(
      projectId,
      pageable,
      search = search,
      exceptUserId,
      filters,
    )
  }

  fun getAllInProjectWithPermittedLanguages(
    projectId: Long,
    pageable: Pageable,
    search: String?,
    exceptUserId: Long? = null,
    filters: UserAccountFilters = UserAccountFilters(),
  ): Page<ExtendedUserAccountInProject> {
    val users = getAllInProject(projectId, pageable, search, exceptUserId, filters)
    val organizationBasePermission = organizationService.getProjectOwner(projectId = projectId).basePermission

    val permittedLanguageMap =
      permissionService.getPermittedTranslateLanguagesForUserIds(
        users.content.map { it.id },
        projectId,
      )
    return users.map {
      ExtendedUserAccountInProject(
        id = it.id,
        name = it.name,
        username = it.username,
        organizationRole = it.organizationRole,
        directPermission = it.directPermission,
        organizationBasePermission = organizationBasePermission,
        permittedLanguageIds = permittedLanguageMap[it.id],
        avatarHash = it.avatarHash,
      )
    }
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun update(
    userAccount: UserAccount,
    dto: UserUpdateRequestDto,
    request: HttpServletRequest,
  ): UserAccount {
    // Current password required to change email or password
    if (dto.email != userAccount.username) {
      if (userAccount.accountType == UserAccount.AccountType.MANAGED) {
        throw BadRequestException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
      }

      if (dto.currentPassword?.isNotEmpty() != true) throw BadRequestException(Message.CURRENT_PASSWORD_REQUIRED)
      val matches = passwordEncoder.matches(dto.currentPassword, userAccount.password)
      if (!matches) throw BadRequestException(Message.WRONG_CURRENT_PASSWORD)
    }

    val old = UserAccountDto.fromEntity(userAccount)
    updateUserEmail(userAccount, dto, request)
    userAccount.name = dto.name

    publishUserInfoUpdatedEvent(old, userAccount)
    return userAccountRepository.save(userAccount)
  }

  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun updatePassword(
    userAccount: UserAccount,
    dto: UserUpdatePasswordRequestDto,
  ): UserAccount {
    if (userAccount.accountType == UserAccount.AccountType.MANAGED) {
      throw BadRequestException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }

    val matches = passwordEncoder.matches(dto.currentPassword, userAccount.password)
    if (!matches) throw PermissionException(Message.WRONG_CURRENT_PASSWORD)

    resetTokensValidNotBefore(userAccount)
    userAccount.password = passwordEncoder.encode(dto.password)
    userAccount.passwordChanged = true
    val savedUser = userAccountRepository.save(userAccount)
    notificationService.notify(
      Notification().apply {
        this.user = userAccount
        this.type = NotificationType.PASSWORD_CHANGED
        this.originatingUser = userAccount
      },
    )
    return savedUser
  }

  private fun updateUserEmail(
    userAccount: UserAccount,
    dto: UserUpdateRequestDto,
    request: HttpServletRequest,
  ) {
    if (userAccount.username != dto.email) {
      if (!emailValidator.isValid(dto.email, null)) {
        // todo: Allow to specify STANDARD_VALIDATION typed errors to show errors on specific fields
        throw ValidationException(Message.VALIDATION_EMAIL_IS_NOT_VALID)
      }

      this.findActive(dto.email)?.let { throw ValidationException(Message.USERNAME_ALREADY_EXISTS) }
      if (tolgeeProperties.authentication.needsEmailVerification) {
        emailVerificationService.resendEmailVerification(userAccount, request, dto.callbackUrl, dto.email)
      } else {
        userAccount.username = dto.email
      }
    }
  }

  fun invalidateTokens(userAccount: UserAccount): UserAccount {
    resetTokensValidNotBefore(userAccount)
    return userAccountRepository.save(userAccount)
  }

  private fun resetTokensValidNotBefore(userAccount: UserAccount) {
    userAccount.tokensValidNotBefore = DateUtils.truncate(currentDateProvider.date, Calendar.SECOND)
  }

  private fun publishUserInfoUpdatedEvent(
    old: UserAccountDto,
    userAccount: UserAccount,
  ) {
    val event = OnUserUpdated(this, old, UserAccountDto.fromEntity(userAccount))
    applicationEventPublisher.publishEvent(event)
  }

  fun saveAll(userAccounts: Collection<UserAccount>): MutableList<UserAccount> =
    userAccountRepository.saveAll(userAccounts)

  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun save(user: UserAccount): UserAccount {
    return userAccountRepository.save(user)
  }

  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun saveAndFlush(user: UserAccount): UserAccount {
    return userAccountRepository.saveAndFlush(user)
  }

  fun getAllByIdsIncludingDeleted(ids: Set<Long>): MutableList<UserAccount> {
    return userAccountRepository.getAllByIdsIncludingDeleted(ids)
  }

  fun findAllWithDisabledPaged(
    pageable: Pageable,
    search: String?,
  ): Page<UserAccount> {
    return userAccountRepository.findAllWithDisabledPaged(search, pageable)
  }

  fun countAll(): Long {
    return userAccountRepository.count()
  }

  fun countAllEnabled(): Long {
    return userAccountRepository.countAllEnabled()
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userId")
  fun disable(userId: Long) {
    val user = this.get(userId)
    user.disabledAt = currentDateProvider.date
    this.save(user)
    this.applicationEventPublisher.publishEvent(OnUserCountChanged(decrease = true, this))
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userId")
  fun enable(userId: Long) {
    val user = this.userAccountRepository.findDisabled(userId)
    user.disabledAt = null
    this.save(user)
    this.applicationEventPublisher.publishEvent(OnUserCountChanged(decrease = false, this))
  }

  fun transferLegacyNoAuthUser() {
    val legacyImplicitUser =
      findActive("___implicit_user")
        ?: return

    // We need to migrate what's owned by `___implicit_user` to the initial user
    val initialUser = findInitialUser() ?: throw IllegalStateException("Initial user does not exist?")

    legacyImplicitUser.apiKeys?.forEach {
      it.userAccount = initialUser
      initialUser.apiKeys!!.add(it)
      entityManager.merge(it)
    }

    legacyImplicitUser.pats?.forEach {
      it.userAccount = initialUser
      initialUser.pats!!.add(it)
      entityManager.merge(it)
    }

    legacyImplicitUser.permissions.forEach {
      it.user = initialUser
      initialUser.permissions.add(it)
      entityManager.merge(it)
    }

    legacyImplicitUser.preferences?.let {
      it.userAccount = initialUser
      entityManager.merge(it)
    }

    legacyImplicitUser.organizationRoles.forEach {
      it.user = initialUser
      initialUser.organizationRoles.add(it)
      entityManager.merge(it)
    }

    legacyImplicitUser.apiKeys?.clear()
    legacyImplicitUser.pats?.clear()
    legacyImplicitUser.permissions.clear()
    legacyImplicitUser.organizationRoles.clear()

    entityManager.flush()
    userAccountRepository.save(initialUser)

    userAccountRepository.deleteById(legacyImplicitUser.id)
  }

  fun findActiveView(id: Long): UserAccountView? = userAccountRepository.findActiveView(id)

  fun findWithMinimalPermissions(
    filters: UserAccountPermissionsFilters,
    projectId: Long,
    search: String?,
    pageable: Pageable,
  ): PageImpl<UserAccount> {
    val ids =
      userAccountRepository.findUsersWithMinimalPermissions(
        filters.filterId ?: listOf(),
        filters.filterMinimalScopeExtended,
        filters.filterMinimalPermissionType,
        projectId,
        filters.filterViewLanguageId,
        filters.filterEditLanguageId,
        filters.filterStateLanguageId,
        search,
        pageable,
      )
    return PageImpl(userAccountRepository.findAllById(ids.content), pageable, ids.totalElements)
  }
}
