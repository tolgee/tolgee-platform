package io.tolgee.exceptions

import java.io.Serializable

class ImportConflictNotResolvedException(params: MutableList<Serializable>) :
  BadRequestException(io.tolgee.constants.Message.CONFLICT_IS_NOT_RESOLVED, params)
