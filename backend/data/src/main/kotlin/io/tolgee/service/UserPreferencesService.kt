package io.tolgee.service

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.UserPreferences
import io.tolgee.repository.UserPreferencesRepository
import io.tolgee.security.AuthenticationFacade
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.stereotype.Service

@Service
class UserPreferencesService(
  private val authenticationFacade: AuthenticationFacade,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val userAccountService: UserAccountService,
  private val organizationService: OrganizationService
) {
  fun setLanguage(tag: String, userAccount: UserAccount = authenticationFacade.userAccountEntity) {
    val preferences = findOrCreate(userAccount.id)
    preferences.language = tag
    userPreferencesRepository.save(preferences)
  }

  fun setPreferredOrganization(
    organization: Organization,
    userAccount: UserAccount = authenticationFacade.userAccountEntity
  ) {
    val preferences = findOrCreate(userAccount.id)
    preferences.preferredOrganization = organization
    userPreferencesRepository.save(preferences)
  }

  fun findOrCreate(userAccountId: Long = authenticationFacade.userAccount.id): UserPreferences {
    return tryUntilItDoesntBreakConstraint {
      val userAccount = userAccountService.get(authenticationFacade.userAccount.username)
      val preferences = find(userAccountId) ?: UserPreferences(userAccount = userAccount).apply {
        userPreferencesRepository.save(this)
      }

      if (preferences.preferredOrganization == null) {
        preferences.preferredOrganization = findPreferredOrganization()
      }

      preferences
    }
  }

  fun find(userAccountId: Long = authenticationFacade.userAccount.id): UserPreferences? {
    return userPreferencesRepository.findById(userAccountId).orElse(null)
  }

  fun save(prefs: UserPreferences) {
    userPreferencesRepository.save(prefs)
  }

  fun findPreferredOrganization(): Organization? {
    return organizationService.findAllPermitted().firstOrNull()
  }
}
