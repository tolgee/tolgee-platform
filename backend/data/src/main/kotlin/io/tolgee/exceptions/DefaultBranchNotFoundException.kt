package io.tolgee.exceptions

import io.tolgee.constants.Message

class DefaultBranchNotFoundException : NotFoundException(Message.BRANCH_NOT_FOUND)
