package io.tolgee.component

import io.tolgee.development.OnDateForced
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.data.auditing.AuditingHandler
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CurrentDateProvider(
  @Lazy
  auditingHandler: AuditingHandler,
  private val applicationEventPublisher: ApplicationEventPublisher
) : DateTimeProvider {
  init {
    auditingHandler.setDateTimeProvider(this)
  }

  var forcedDate: Date? = null
    set(value) {
      field = value
      applicationEventPublisher.publishEvent(OnDateForced(this, value))
    }

  fun forceDateString(dateString: String, pattern: String = "yyyy-MM-dd HH:mm:ss z") {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    val parsed = ZonedDateTime.parse(dateString, formatter).toInstant().toEpochMilli()
    forcedDate = Date(parsed)
  }

  val date: Date
    get() {
      return forcedDate ?: Date()
    }

  override fun getNow(): Optional<TemporalAccessor> {
    return Optional.of(date.toInstant())
  }
}
