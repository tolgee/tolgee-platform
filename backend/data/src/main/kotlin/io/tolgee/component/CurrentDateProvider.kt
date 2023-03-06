package io.tolgee.component

import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.data.auditing.AuditingHandler
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.stereotype.Component
import java.time.temporal.TemporalAccessor
import java.util.*

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CurrentDateProvider(
  @Lazy
  auditingHandler: AuditingHandler
) : DateTimeProvider {
  init {
    auditingHandler.setDateTimeProvider(this)
  }

  var forcedDate: Date? = null

  val date: Date
    get() {
      return forcedDate ?: Date()
    }

  override fun getNow(): Optional<TemporalAccessor> {
    return Optional.of(date.toInstant())
  }
}
