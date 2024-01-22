package io.tolgee.service.dataImport

import io.tolgee.model.dataImport.Import
import org.apache.commons.lang3.time.DateUtils
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class RemoveExpiredImportService(
  @Lazy
  private val importService: ImportService,
  private val currentDateProvider: io.tolgee.component.CurrentDateProvider,
) {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun removeIfExpired(import: Import?): Import? {
    if (import == null) {
      return null
    }

    if (import.createdAt == null) {
      return null
    }

    val minDate = DateUtils.addHours(currentDateProvider.date, -2)
    if (minDate > import.createdAt) {
      importService.deleteImport(import)
      return null
    }
    return import
  }
}
