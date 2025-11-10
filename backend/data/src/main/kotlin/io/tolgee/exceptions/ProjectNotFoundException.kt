package io.tolgee.exceptions

import io.tolgee.constants.Message

class ProjectNotFoundException(
  id: Long,
) : NotFoundException(Message.PROJECT_NOT_FOUND, id)
