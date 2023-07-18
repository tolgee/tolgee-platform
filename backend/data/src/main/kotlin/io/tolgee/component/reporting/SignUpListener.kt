package io.tolgee.component.reporting

import io.tolgee.dtos.cacheable.UserAccountDto
import io.tolgee.events.user.OnUserCreated
import io.tolgee.model.UserAccount
import io.tolgee.service.organization.OrganizationService
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SignUpListener(
  private val businessEventPublisher: BusinessEventPublisher,
  private val organizationService: OrganizationService
) {
  @TransactionalEventListener(OnUserCreated::class)
  fun listen(onUserCreated: OnUserCreated) {
    publishBusinessEvent(onUserCreated.userAccount)
  }

  private fun publishBusinessEvent(user: UserAccount) {
    val organization = organizationService.findPreferred(userAccountId = user.id)
    businessEventPublisher.publish(
      OnBusinessEventToCaptureEvent(
        eventName = "SIGN_UP",
        organizationId = organization?.id,
        organizationName = organization?.name,
        userAccountId = user.id,
        userAccountDto = UserAccountDto.fromEntity(user)
      )
    )
  }
}
