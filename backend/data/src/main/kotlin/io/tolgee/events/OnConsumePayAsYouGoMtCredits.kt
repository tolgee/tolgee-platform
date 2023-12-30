package io.tolgee.events

import io.tolgee.service.machineTranslation.MtCreditBucketService
import org.springframework.context.ApplicationEvent

class OnConsumePayAsYouGoMtCredits(
  source: MtCreditBucketService,
  val organizationId: Long,
  val credits: Long,
) : ApplicationEvent(source)
