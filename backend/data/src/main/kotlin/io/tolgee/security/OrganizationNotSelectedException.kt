package io.tolgee.security

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class OrganizationNotSelectedException : BadRequestException(Message.ORGANIZATION_NOT_SELECTED)
