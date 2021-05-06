package io.tolgee.exceptions

import io.tolgee.constants.Message
import java.io.Serializable

class ImportConflictNotResolvedException(params: MutableList<Serializable?>) :
        BadRequestException(Message.CONFLICT_IS_NOT_RESOLVED, params) {
}
