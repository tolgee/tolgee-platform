package io.tolgee.service.dataImport

import io.tolgee.events.OnImportSoftDeleted
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class AsyncImportHardDeleter(
  private val importService: ImportService,
) {
  @Async
  @TransactionalEventListener
  fun hardDelete(event: OnImportSoftDeleted) {
    importService.findDeleted(importId = event.importId)?.let {
      importService.hardDeleteImport(it)
    }
  }
}
