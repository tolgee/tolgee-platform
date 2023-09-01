package io.tolgee.service.security

import io.tolgee.component.CurrentDateProvider
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.request.UserUpdatePasswordRequestDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.events.OnUserCountChanged
import io.tolgee.events.user.OnUserCreated
import io.tolgee.events.user.OnUserUpdated
import io.tolgee.exceptions.AuthenticationException
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import io.tolgee.exceptions.PermissionException
import io.tolgee.model.UserAccount
import io.tolgee.model.views.ExtendedUserAccountInProject
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.repository.UserAccountRepository
import io.tolgee.service.AvatarService
import io.tolgee.service.EmailVerificationService
import io.tolgee.service.organization.OrganizationService
import org.apache.commons.lang3.time.DateUtils
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*
import javax.persistence.EntityManager

@Service
class UserAccountService(
  private val userAccountRepository: UserAccountRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val tolgeeProperties: TolgeeProperties,
  private val avatarService: AvatarService,
  private val passwordEncoder: PasswordEncoder,
  @Lazy
  private val organizationService: OrganizationService,
  private val entityManager: EntityManager,
  private val currentDateProvider: CurrentDateProvider,
) {
  @Autowired
  lateinit var emailVerificationService: EmailVerificationService

  @Autowired
  @Lazy
  lateinit var permissionService: PermissionService

  private val emailValidator = EmailValidator()

  fun findActive(username: String): UserAccount? {
    return userAccountRepository.findActive(username)
  }

  operator fun get(username: String): UserAccount {
    return this.findActive(username) ?: throw NotFoundException(Message.USER_NOT_FOUND)
  }

  fun findActive(id: Long): UserAccount? {
    return userAccountRepository.findActive(id)
  }

  fun findInitialUser(): UserAccount? {
    return userAccountRepository.findInitialUser()
  }

  fun get(id: Long): UserAccount {
    return this.findActive(id) ?: throw NotFoundException(Message.USER_NOT_FOUND)
  }

  @Cacheable(cacheNames = [Caches.USER_ACCOUNTS], key = "#id")
  fun findDto(id: Long): UserAccountDto? {
    return userAccountRepository.findActive(id)?.let {
      UserAccountDto.fromEntity(it)
    }
  }

  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun createUser(userAccount: UserAccount): UserAccount {
    userAccountRepository.saveAndFlush(userAccount)
    applicationEventPublisher.publishEvent(OnUserCreated(this, userAccount))
    applicationEventPublisher.publishEvent(OnUserCountChanged(this))
    return userAccount
  }

  @Transactional
  fun createUser(request: SignUpDto, role: UserAccount.Role = UserAccount.Role.USER): UserAccount {
    dtoToEntity(request).let {
      it.role = role
      this.createUser(it)
      return it
    }
  }

  @Transactional
  fun createInitialUser(request: SignUpDto): UserAccount {
    dtoToEntity(request).let {
      it.role = UserAccount.Role.ADMIN
      it.isInitialUser = true
      this.createUser(it)

      // TODO: remove this on Tolgee 4 release
      transferLegacyNoAuthUser()
      return it
    }
  }

  @CacheEvict(Caches.USER_ACCOUNTS, key = "#id")
  @Transactional
  fun delete(id: Long) {
    val user = this.get(id)
    delete(user)
    this.applicationEventPublisher.publishEvent(OnUserCountChanged(this))
  }

  @CacheEvict(Caches.USER_ACCOUNTS, key = "#userAccount.id")
  @Transactional
  fun delete(userAccount: UserAccount) {
    userAccount.emailVerification?.let {
      entityManager.remove(it)
    }
    userAccount.apiKeys?.forEach {
      entityManager.remove(it)
    }
    userAccount.pats?.forEach {
      entityManager.remove(it)
    }
    userAccount.permissions.forEach {
      entityManager.remove(it)
    }
    userAccount.preferences?.let {
      entityManager.remove(it)
    }
    organizationService.getAllSingleOwnedByUser(userAccount).forEach {
      it.preferredBy.removeIf { preferences ->
        preferences.userAccount.id == userAccount.id
      }
      organizationService.delete(it)
    }
    userAccount.organizationRoles.forEach {
      entityManager.remove(it)
    }
    userAccountRepository.softDeleteUser(userAccount)
    applicationEventPublisher.publishEvent(OnUserCountChanged(this))
  }

  fun dtoToEntity(request: SignUpDto): UserAccount {
    val encodedPassword = passwordEncoder.encode(request.password!!)
    return UserAccount(name = request.name, username = request.email, password = encodedPassword)
  }

  fun findByThirdParty(type: String, id: String): Optional<UserAccount> {
    return userAccountRepository.findThirdByThirdParty(id, type)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setAccountType(userAccount: UserAccount, accountType: UserAccount.AccountType): UserAccount {
    userAccount.accountType = accountType
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setResetPasswordCode(userAccount: UserAccount, code: String?): UserAccount {
    userAccount.resetPasswordCode = passwordEncoder.encode(code)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setUserPassword(userAccount: UserAccount, password: String?): UserAccount {
    userAccount.tokensValidNotBefore = DateUtils.truncate(Date(), Calendar.SECOND)
    userAccount.password = passwordEncoder.encode(password)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  fun isResetCodeValid(userAccount: UserAccount, code: String?): Boolean {
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
  fun enableMfaTotp(userAccount: UserAccount, key: ByteArray): UserAccount {
    userAccount.totpKey = key
    userAccount.tokensValidNotBefore = DateUtils.truncate(Date(), Calendar.SECOND)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun disableMfaTotp(userAccount: UserAccount): UserAccount {
    userAccount.totpKey = null
    // note: if support for more MFA methods is added, this should be only done if no other MFA method is enabled
    userAccount.mfaRecoveryCodes = emptyList()
    userAccount.tokensValidNotBefore = DateUtils.truncate(Date(), Calendar.SECOND)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun consumeMfaRecoveryCode(userAccount: UserAccount, token: String): UserAccount {
    if (!userAccount.mfaRecoveryCodes.contains(token)) {
      throw AuthenticationException(Message.INVALID_OTP_CODE)
    }

    userAccount.mfaRecoveryCodes = userAccount.mfaRecoveryCodes.minus(token)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setMfaRecoveryCodes(userAccount: UserAccount, codes: List<String>): UserAccount {
    userAccount.mfaRecoveryCodes = codes
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userAccount.id")
  fun removeAvatar(userAccount: UserAccount) {
    avatarService.removeAvatar(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userAccount.id")
  fun setAvatar(userAccount: UserAccount, avatar: InputStream) {
    avatarService.setAvatar(userAccount, avatar)
  }

  fun getAllInOrganization(
    organizationId: Long,
    pageable: Pageable,
    search: String?
  ): Page<UserAccountWithOrganizationRoleView> {
    return userAccountRepository.getAllInOrganization(organizationId, pageable, search = search ?: "")
  }

  fun getAllInProject(
    projectId: Long,
    pageable: Pageable,
    search: String?,
    exceptUserId: Long? = null
  ): Page<UserAccountInProjectView> {
    return userAccountRepository.getAllInProject(projectId, pageable, search = search, exceptUserId)
  }

  fun getAllInProjectWithPermittedLanguages(
    projectId: Long,
    pageable: Pageable,
    search: String?,
    exceptUserId: Long? = null
  ): Page<ExtendedUserAccountInProject> {
    val users = getAllInProject(projectId, pageable, search, exceptUserId)
    val organizationBasePermission = organizationService.getProjectOwner(projectId = projectId).basePermission

    val permittedLanguageMap = permissionService.getPermittedTranslateLanguagesForUserIds(
      users.content.map { it.id },
      projectId
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
        avatarHash = it.avatarHash
      )
    }
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun update(userAccount: UserAccount, dto: UserUpdateRequestDto): UserAccount {
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
    updateUserEmail(userAccount, dto)
    userAccount.name = dto.name

    publishUserInfoUpdatedEvent(old, userAccount)
    return userAccountRepository.save(userAccount)
  }

  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun updatePassword(userAccount: UserAccount, dto: UserUpdatePasswordRequestDto): UserAccount {
    if (userAccount.accountType == UserAccount.AccountType.MANAGED) {
      throw BadRequestException(Message.OPERATION_UNAVAILABLE_FOR_ACCOUNT_TYPE)
    }

    val matches = passwordEncoder.matches(dto.currentPassword, userAccount.password)
    if (!matches) throw PermissionException()

    userAccount.tokensValidNotBefore = DateUtils.truncate(Date(), Calendar.SECOND)
    userAccount.password = passwordEncoder.encode(dto.password)
    return userAccountRepository.save(userAccount)
  }

  private fun updateUserEmail(
    userAccount: UserAccount,
    dto: UserUpdateRequestDto
  ) {
    if (userAccount.username != dto.email) {
      if (!emailValidator.isValid(dto.email, null)) {
        // todo: Allow to specify STANDARD_VALIDATION typed errors to show errors on specific fields
        throw ValidationException(Message.VALIDATION_EMAIL_IS_NOT_VALID)
      }

      this.findActive(dto.email)?.let { throw ValidationException(Message.USERNAME_ALREADY_EXISTS) }
      if (tolgeeProperties.authentication.needsEmailVerification) {
        emailVerificationService.createForUser(userAccount, dto.callbackUrl, dto.email)
      } else {
        userAccount.username = dto.email
      }
    }
  }

  private fun publishUserInfoUpdatedEvent(
    old: UserAccountDto,
    userAccount: UserAccount
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

  fun findAllWithDisabledPaged(pageable: Pageable, search: String?): Page<UserAccount> {
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
    this.applicationEventPublisher.publishEvent(OnUserCountChanged(this))
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#userId")
  fun enable(userId: Long) {
    val user = this.userAccountRepository.findDisabled(userId)
    user.disabledAt = null
    this.save(user)
    this.applicationEventPublisher.publishEvent(OnUserCountChanged(this))
  }

  val isAnyUserAccount: Boolean
    get() = userAccountRepository.count() > 0

  private fun transferLegacyNoAuthUser() {
    val legacyImplicitUser = findActive("___implicit_user")
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
}
