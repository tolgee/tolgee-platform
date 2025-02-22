package io.tolgee.events

import org.springframework.context.ApplicationEvent

class OnAfterMachineTranslationEvent(
  source: Any,
  val organizationId: Long,
  /**
   * The actual total price of translation actually consumed
   */
  val actualSumPriceInCents: Int,
) : ApplicationEvent(source)
