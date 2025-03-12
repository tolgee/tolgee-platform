package io.tolgee.development.testDataBuilder.builders

import io.tolgee.model.Invitation
import org.apache.commons.lang3.RandomStringUtils

class InvitationBuilder : BaseEntityDataBuilder<Invitation, InvitationBuilder>() {
  override val self: Invitation = Invitation(code = RandomStringUtils.randomAlphanumeric(50))
}
