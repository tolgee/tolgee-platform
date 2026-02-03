package io.tolgee.security

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class ProjectNotSelectedException : BadRequestException(Message.PROJECT_NOT_SELECTED)
