package io.tolgee.ee.service.aiReview.scoring

import io.tolgee.constants.Message
import io.tolgee.exceptions.FailedDependencyException

class ReviewParseException : FailedDependencyException(Message.LLM_PROVIDER_NOT_RETURNED_JSON)
