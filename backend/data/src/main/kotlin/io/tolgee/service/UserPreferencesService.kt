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
  private val userPreferencesRepository: UserPreferencesRepository
) {
  fun setLanguage(tag: String, userAccount: UserAccount = authenticationFacade.userAccountEntity) {
    val preferences = findOrCreate(userAccount)
    preferences.language = tag
    userPreferencesRepository.save(preferences)
  }

  fun setPreferredOrganization(
    organization: Organization,
    userAccount: UserAccount = authenticationFacade.userAccountEntity
  ) {
    val preferences = findOrCreate(userAccount)
    preferences.preferredOrganization = organization
    userPreferencesRepository.save(preferences)
  }

  fun findOrCreate(userAccount: UserAccount = authenticationFacade.userAccountEntity): UserPreferences {
    return tryUntilItDoesntBreakConstraint {
      userPreferencesRepository.findById(userAccount).orElseGet {
        UserPreferences(userAccount = userAccount).apply {
          userPreferencesRepository.save(this)
        }
      }
    }
  }

  fun save(prefs: UserPreferences) {
    userPreferencesRepository.save(prefs)
  }
}
