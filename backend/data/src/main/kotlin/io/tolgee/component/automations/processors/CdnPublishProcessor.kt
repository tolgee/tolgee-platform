package io.tolgee.component.automations.processors

import io.tolgee.batch.RequeueWithDelayException
import io.tolgee.component.automations.AutomationProcessor
import io.tolgee.component.cdn.CdnUploader
import io.tolgee.constants.Message
import io.tolgee.exceptions.FileStoreException
import io.tolgee.model.automations.AutomationAction
import io.tolgee.security.ProjectHolder
import io.tolgee.service.security.SecurityService
import org.springframework.stereotype.Component

@Component
class CdnPublishProcessor(
  val cdnUploader: CdnUploader,
  val securityService: SecurityService,
  val projectHolder: ProjectHolder,
) : AutomationProcessor {
  override fun process(action: AutomationAction, activityRevisionId: Long?) {
    try {
      val exporter = action.cdn ?: throw IllegalStateException("Wrong params passed to cdn publish processor")
      cdnUploader.upload(cdnId = exporter.id)
    } catch (e: Throwable) {
      when (e) {
        is FileStoreException -> throw RequeueWithDelayException(
          Message.CANNOT_STORE_FILE_TO_CONTENT_STORAGE,
          cause = e,
        )
        else -> throw RequeueWithDelayException(Message.UNEXPECTED_ERROR_WHILE_PUBLISHING_TO_CDN, cause = e)
      }
    }
  }
}
