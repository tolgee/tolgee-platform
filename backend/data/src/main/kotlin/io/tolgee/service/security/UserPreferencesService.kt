package io.tolgee.service.security

import io.tolgee.model.Organization
import io.tolgee.model.UserAccount
import io.tolgee.model.UserPreferences
import io.tolgee.repository.UserPreferencesRepository
import io.tolgee.security.authentication.AuthenticationFacade
import io.tolgee.service.organization.OrganizationRoleService
import io.tolgee.service.organization.OrganizationService
import io.tolgee.util.tryUntilItDoesntBreakConstraint
import org.springframework.stereotype.Service

@Service
class UserPreferencesService(
  private val authenticationFacade: AuthenticationFacade,
  private val userPreferencesRepository: UserPreferencesRepository,
  private val userAccountService: UserAccountService,
  private val organizationService: OrganizationService,
  private val organizationRoleService: OrganizationRoleService,
) {
  fun setLanguage(
    tag: String,
    userAccount: UserAccount,
  ) {
    val preferences = findOrCreate(userAccount.id)
    preferences.language = tag
    userPreferencesRepository.save(preferences)
  }

  fun setPreferredOrganization(
    organization: Organization,
    userAccount: UserAccount,
  ) {
    val preferences = find(userAccount.id) ?: create(userAccount, organization)
    preferences.preferredOrganization = organization
    userPreferencesRepository.save(preferences)
  }

  /**
   * Updates a specific field within the user's stored JSON preferences.
   *
   * If the user's storage JSON does not exist, it creates one, updates the specified field,
   * and saves the changes to the repository.
   */
  fun setStorageJsonField(
    fieldName: String,
    value: Any?,
    userAccount: UserAccount,
  ) {
    val preferences = findOrCreate(userAccount.id)
    val currentStorage = preferences.storageJson?.toMutableMap() ?: mutableMapOf()
    if (value != null) {
      currentStorage[fieldName] = value
    } else {
      currentStorage.remove(fieldName)
    }
    preferences.storageJson = currentStorage
    userPreferencesRepository.save(preferences)
  }

  fun findOrCreate(userAccountId: Long): UserPreferences {
    return tryUntilItDoesntBreakConstraint {
      val userAccount = userAccountService.get(userAccountId)
      return@tryUntilItDoesntBreakConstraint find(userAccountId) ?: create(userAccount)
    }
  }

  private fun create(
    userAccount: UserAccount,
    preferredOrganization: Organization? = null,
  ): UserPreferences {
    val preferences =
      UserPreferences(
        userAccount = userAccount,
        preferredOrganization = preferredOrganization ?: organizationService.findOrCreatePreferred(userAccount),
      )
    return save(preferences)
  }

  fun find(userAccountId: Long = authenticationFacade.authenticatedUser.id): UserPreferences? {
    val preferences = userPreferencesRepository.findById(userAccountId).orElse(null) ?: return null
    preferences.tryRefreshPreferredOrganizationWhenNull()
    return preferences
  }

  fun save(prefs: UserPreferences): UserPreferences {
    return userPreferencesRepository.save(prefs)
  }

  fun UserPreferences.tryRefreshPreferredOrganizationWhenNull() {
    if (this.preferredOrganization == null) {
      this.preferredOrganization = this@UserPreferencesService.refreshPreferredOrganization(this)
    }
  }

  /**
   * Sets different organization as preferred if user has no access to the current one
   */
  fun refreshPreferredOrganization(preferences: UserPreferences): Organization? {
    val canUserView =
      preferences.preferredOrganization?.let { po ->
        organizationRoleService.canUserView(
          preferences.userAccount.id,
          po.id,
        )
      } ?: false

    if (!canUserView) {
      preferences.preferredOrganization =
        organizationService.findOrCreatePreferred(
          userAccount = preferences.userAccount,
        )
      save(preferences)
    }

    return preferences.preferredOrganization
  }

  /**
   * Sets different organization as preferred if user has no access to the current one
   */
  fun refreshPreferredOrganization(userAccountId: Long): Organization? {
    val preferences = findOrCreateNoRefreshPreferred(userAccountId)
    return refreshPreferredOrganization(preferences)
  }

  private fun findOrCreateNoRefreshPreferred(userAccountId: Long): UserPreferences {
    return tryUntilItDoesntBreakConstraint {
      val userAccount = userAccountService.get(userAccountId)
      return@tryUntilItDoesntBreakConstraint findNoRefreshPreferred(userAccountId) ?: create(userAccount)
    }
  }

  private fun findNoRefreshPreferred(userAccountId: Long): UserPreferences? {
    return userPreferencesRepository.findById(userAccountId).orElse(null) ?: return null
  }
}
