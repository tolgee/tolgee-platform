package io.tolgee.service

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.UserPreferences
import io.tolgee.repository.UserPreferencesRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class UserPreferencesService(
  private val authenticationFacade: AuthenticationFacade,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val userAccountService: UserAccountService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService
) {
  fun setLanguage(tag: String, userAccount: UserAccount) {
    val preferences = findOrCreate(userAccount.id)
    preferences.language = tag
    userPreferencesRepository.save(preferences)
  }

  fun setPreferredOrganization(
    organization: Organization,
    userAccount: UserAccount
  ) {
    val preferences = findOrCreate(userAccount.id)
    preferences.preferredOrganization = organization
    userPreferencesRepository.save(preferences)
  }

  @Async
  fun setPreferredOrganizationAsync(
    organization: Organization,
    userAccount: UserAccount
  ) {
    setPreferredOrganization(organization, userAccount)
  }

  fun findOrCreate(userAccountId: Long): UserPreferences {
    return tryUntilItDoesntBreakConstraint {
      val userAccount = userAccountService.get(userAccountId)
      return@tryUntilItDoesntBreakConstraint find(userAccountId) ?: create(userAccount)
    }
  }

  private fun create(userAccount: UserAccount): UserPreferences {
    val preferences = UserPreferences(userAccount = userAccount, organizationService.findOrCreatePreferred(userAccount))
    return save(preferences)
  }

  fun find(userAccountId: Long = authenticationFacade.userAccount.id): UserPreferences? {
    return userPreferencesRepository.findById(userAccountId).orElse(null)
  }

  fun save(prefs: UserPreferences): UserPreferences {
    return userPreferencesRepository.save(prefs)
  }

  /**
   * Sets different organization as preferred if user has no access to the current one
   */
  fun refreshPreferredOrganization(userAccountId: Long) {
    val preferences = findOrCreate(userAccountId)
    val canUserView = organizationRoleService.canUserView(userAccountId, preferences.preferredOrganization.id)
    if (!canUserView) {
      preferences.preferredOrganization = organizationService.findOrCreatePreferred(
        userAccount = preferences.userAccount
      )
      save(preferences)
    }
  }
}
