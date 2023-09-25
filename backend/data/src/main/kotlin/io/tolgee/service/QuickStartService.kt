package io.tolgee.service

import io.tolgee.component.demoProject.DemoProjectCreator
import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.model.Organization
import io.tolgee.model.QuickStart
import io.tolgee.model.UserAccount
import io.tolgee.repository.QuickStartRepository
import org.springframework.context.ApplicationContext
import org.springframework.data.crossstore.ChangeSetPersister
import org.springframework.stereotype.Component

@Component
class QuickStartService(
  private val quickStartRepository: QuickStartRepository,
  private val applicationContext: ApplicationContext
) {
  fun create(userAccount: UserAccount, organization: Organization) {
    val quickStart = QuickStart(userAccount)
    quickStart.organization = organization
    quickStartRepository.save(quickStart)
    DemoProjectCreator(organization, applicationContext).createDemoProject()
  }

  fun completeStep(userAccount: UserAccountDto, step: String): QuickStart? {
    val quickStart = quickStartRepository.findByUserAccountId(userAccount.id)
    if (quickStart?.completedSteps?.let { !it.contains(step) } == true) {
      quickStart.completedSteps.add(step)
      quickStartRepository.save(quickStart)
    }
    return quickStart
  }

  fun setFinishState(userAccount: UserAccountDto, finished: Boolean): QuickStart {
    val quickStart = quickStartRepository.findByUserAccountId(userAccount.id)
      ?: throw ChangeSetPersister.NotFoundException()
    quickStart.finished = finished
    quickStartRepository.save(quickStart)
    return quickStart
  }

  fun setOpenState(userAccount: UserAccountDto, open: Boolean): QuickStart {
    val quickStart = quickStartRepository.findByUserAccountId(userAccount.id)
      ?: throw ChangeSetPersister.NotFoundException()
    quickStart.open = open
    quickStartRepository.save(quickStart)
    return quickStart
  }

  fun find(userAccountId: Long, organizationId: Long?): QuickStart? {
    organizationId ?: return null
    return quickStartRepository.findByUserAccountIdAndOrganizationId(userAccountId, organizationId)
  }
}
