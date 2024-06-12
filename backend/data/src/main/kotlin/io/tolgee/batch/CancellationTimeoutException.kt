package io.tolgee.batch

import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException

class CancellationTimeoutException : BadRequestException(Message.BATCH_JOB_CANCELLATION_TIMEOUT)
