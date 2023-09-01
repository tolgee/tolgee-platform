package io.tolgee.service

import io.tolgee.model.QuickStart
import io.tolgee.repository.QuickStartRepository
import io.tolgee.security.AuthenticationFacade
import org.springframework.stereotype.Component

@Component
class QuickStartService (
  private val quickStartRepository: QuickStartRepository,
  private val authenticationFacade: AuthenticationFacade
) {
  fun completeStep(step: String) : QuickStart? {
    val userAccount = authenticationFacade.userAccountEntity

    val quickStart = quickStartRepository.findByUserAccount(userAccount)
    if (quickStart?.completedSteps?.let { !it.contains(step) } == true) {
      quickStart.completedSteps.add(step)
      quickStartRepository.save(quickStart)
    }
    return quickStart
  }


  fun finish() : QuickStart? {
    val userAccount = authenticationFacade.userAccountEntity
    val quickStart = quickStartRepository.findByUserAccount(userAccount)

    if (quickStart?.open == true) {
      quickStart.open = false
      quickStartRepository.save(quickStart)
    }
    return quickStart
  }
}
