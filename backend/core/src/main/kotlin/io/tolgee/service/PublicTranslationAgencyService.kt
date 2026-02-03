package io.tolgee.service

import io.tolgee.constants.Feature
import io.tolgee.constants.Message
import io.tolgee.exceptions.BadRequestException
import io.tolgee.model.translationAgency.TranslationAgency
import org.springframework.stereotype.Component

@Component
class PublicTranslationAgencyService : TranslationAgencyService {
  override fun findById(id: Long): TranslationAgency {
    throw BadRequestException(Message.FEATURE_NOT_ENABLED, listOf(Feature.ORDER_TRANSLATION))
  }
}
