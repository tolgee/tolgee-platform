package io.tolgee.service

import io.tolgee.model.translationAgency.TranslationAgency

interface TranslationAgencyService {
  fun findById(id: Long): TranslationAgency
}
