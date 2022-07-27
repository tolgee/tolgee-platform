package io.tolgee.service

import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.constants.Caches
import io.tolgee.constants.Message
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.dtos.request.UserUpdateRequestDto
import io.tolgee.dtos.request.auth.SignUpDto
import io.tolgee.dtos.request.validators.exceptions.ValidationException
import io.tolgee.events.user.OnUserCreated
import io.tolgee.events.user.OnUserUpdated
import io.tolgee.exceptions.NotFoundException
import io.tolgee.model.UserAccount
import io.tolgee.model.views.UserAccountInProjectView
import io.tolgee.model.views.UserAccountInProjectWithLanguagesView
import io.tolgee.model.views.UserAccountWithOrganizationRoleView
import io.tolgee.repository.UserAccountRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.InputStream
import java.util.*

@Service
class UserAccountService(
  private val userAccountRepository: UserAccountRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val tolgeeProperties: TolgeeProperties,
  private val avatarService: AvatarService,
) {
  @Autowired
  lateinit var emailVerificationService: EmailVerificationService

  @Autowired
  @Lazy
  lateinit var permissionService: PermissionService

  fun findOptional(username: String?): Optional<UserAccount> {
    return userAccountRepository.findByUsername(username)
  }

  fun find(username: String): UserAccount? {
    return userAccountRepository.findByUsername(username).orElse(null)
  }

  fun get(username: String): UserAccount {
    return userAccountRepository
      .findByUsername(username)
      .orElseThrow { NotFoundException(Message.USER_NOT_FOUND) }
  }

  operator fun get(id: Long): Optional<UserAccount> {
    return userAccountRepository.findById(id)
  }

  @Cacheable(cacheNames = [Caches.USER_ACCOUNTS], key = "#id")
  fun getDto(id: Long): UserAccountDto? {
    return userAccountRepository.findById(id).orElse(null)?.let {
      UserAccountDto.fromEntity(it)
    }
  }

  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun createUser(userAccount: UserAccount): UserAccount {
    userAccountRepository.saveAndFlush(userAccount)
    applicationEventPublisher.publishEvent(OnUserCreated(this, userAccount))
    return userAccount
  }

  fun createUser(request: SignUpDto, role: UserAccount.Role = UserAccount.Role.USER): UserAccount {
    dtoToEntity(request).let {
      it.role = role
      this.createUser(it)
      return it
    }
  }

  @CacheEvict(Caches.USER_ACCOUNTS, key = "#userAccount.id")
  fun delete(userAccount: UserAccount) {
    userAccountRepository.delete(userAccount)
  }

  fun dtoToEntity(request: SignUpDto): UserAccount {
    val encodedPassword = encodePassword(request.password!!)
    return UserAccount(name = request.name, username = request.email, password = encodedPassword)
  }

  @get:Cacheable(cacheNames = [Caches.USER_ACCOUNTS], key = "'implicit'")
  val implicitUser: UserAccount
    get() {
      val username = "___implicit_user"
      return userAccountRepository.findByUsername(username).orElseGet {
        val account = UserAccount(name = "No auth user", username = username, role = UserAccount.Role.ADMIN)
        this.createUser(account)
        account
      }
    }

  fun findByThirdParty(type: String?, id: String?): Optional<UserAccount> {
    return userAccountRepository.findByThirdPartyAuthTypeAndThirdPartyAuthId(type!!, id!!)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setResetPasswordCode(userAccount: UserAccount, code: String?): UserAccount {
    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    userAccount.resetPasswordCode = bCryptPasswordEncoder.encode(code)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun setUserPassword(userAccount: UserAccount, password: String?): UserAccount {
    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    userAccount.password = bCryptPasswordEncoder.encode(password)
    return userAccountRepository.save(userAccount)
  }

  @Transactional
  fun isResetCodeValid(userAccount: UserAccount, code: String?): Boolean {
    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    return bCryptPasswordEncoder.matches(code, userAccount.resetPasswordCode)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun removeResetCode(userAccount: UserAccount): UserAccount {
    userAccount.resetPasswordCode = null
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
  ): Page<UserAccountInProjectWithLanguagesView> {
    val users = getAllInProject(projectId, pageable, search, exceptUserId)
    val permittedLanguageMap = permissionService.getPermittedTranslateLanguagesForUserIds(
      users.content.map { it.id },
      projectId
    )
    return users.map {
      UserAccountInProjectWithLanguagesView(
        id = it.id,
        name = it.name,
        username = it.username,
        organizationRole = it.organizationRole,
        organizationBasePermissions = it.organizationBasePermissions,
        directPermissions = it.directPermissions,
        permittedLanguageIds = permittedLanguageMap[it.id]
      )
    }
  }

  fun encodePassword(rawPassword: String): String {
    val bCryptPasswordEncoder = BCryptPasswordEncoder()
    return bCryptPasswordEncoder.encode(rawPassword)
  }

  @Transactional
  @CacheEvict(cacheNames = [Caches.USER_ACCOUNTS], key = "#result.id")
  fun update(userAccount: UserAccount, dto: UserUpdateRequestDto): UserAccount {
    val old = UserAccountDto.fromEntity(userAccount)
    updateUserEmail(userAccount, dto)
    updatePassword(dto, userAccount)
    userAccount.name = dto.name
    publishUserInfoUpdatedEvent(old, userAccount)
    return userAccountRepository.save(userAccount)
  }

  private fun updatePassword(dto: UserUpdateRequestDto, userAccount: UserAccount) {
    dto.password?.let {
      if (!it.isEmpty()) {
        userAccount.password = encodePassword(it)
      }
    }
  }

  private fun updateUserEmail(
    userAccount: UserAccount,
    dto: UserUpdateRequestDto
  ) {
    if (userAccount.username != dto.email) {
      findOptional(dto.email).ifPresent { throw ValidationException(Message.USERNAME_ALREADY_EXISTS) }
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

  fun getAllByIds(ids: Set<Long>): MutableList<UserAccount> {
    return userAccountRepository.findAllById(ids)
  }

  val isAnyUserAccount: Boolean
    get() = userAccountRepository.count() > 0
}
