package io.tolgee.exceptions

import java.io.Serializable

class ImportConflictNotResolvedException(
  params: List<Serializable>,
) : BadRequestException(io.tolgee.constants.Message.CONFLICT_IS_NOT_RESOLVED, params)
