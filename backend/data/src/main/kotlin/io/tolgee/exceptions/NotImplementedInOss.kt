package io.tolgee.exceptions

import io.tolgee.constants.Feature
import io.tolgee.constants.Message

class NotImplementedInOss(feature: Feature) :
  BadRequestException(Message.THIS_FEATURE_IS_NOT_IMPLEMENTED_IN_OSS, listOf(feature))
