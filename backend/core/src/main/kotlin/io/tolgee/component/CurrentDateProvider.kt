package io.tolgee.component

import io.tolgee.development.OnDateForced
import io.tolgee.model.ForcedServerDateTime
import io.tolgee.util.Logging
import io.tolgee.util.executeInNewTransaction
import io.tolgee.util.logger
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope
import org.springframework.data.auditing.AuditingHandler
import org.springframework.data.auditing.DateTimeProvider
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import java.sql.Timestamp
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.Date
import java.util.Optional

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CurrentDateProvider(
  @Suppress("SpringJavaInjectionPointsAutowiringInspection") @Lazy
  auditingHandler: AuditingHandler,
  private val entityManager: EntityManager,
  private val applicationEventPublisher: ApplicationEventPublisher,
  private val transactionManager: PlatformTransactionManager,
) : Logging,
  DateTimeProvider {
  var forcedDate: Date? = null
    set(value) {
      if (field != value) {
        logger.debug("Forcing date to: {} (old value: {})", value, field)
        field = value
        updateEntity(field)
        applicationEventPublisher.publishEvent(OnDateForced(this, value))
      }
    }

  private fun updateEntity(forcedDate: Date?) {
    executeInNewTransaction(transactionManager) {
      getServerTimeEntity()?.let { entity ->
        forcedDate?.let { date ->
          entity.time = date
          entityManager.persist(entity)
        } ?: let { _ ->
          entityManager.remove(entity)
        }
      } ?: let {
        forcedDate?.let { date ->
          entityManager.persist(
            ForcedServerDateTime().apply {
              time = date
            },
          )
        }
      }
    }
  }

  init {
    forcedDate = getForcedTime()
    auditingHandler.setDateTimeProvider(this)
  }

  fun forceDateString(
    dateString: String,
    pattern: String = "yyyy-MM-dd HH:mm:ss z",
  ) {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    val parsed = ZonedDateTime.parse(dateString, formatter).toInstant().toEpochMilli()
    forcedDate = Date(parsed)
  }

  fun move(duration: Duration) {
    forcedDate = Date(date.time + duration.toMillis())
  }

  val date: Date
    get() {
      return forcedDate ?: Date()
    }

  override fun getNow(): Optional<TemporalAccessor> {
    return Optional.of(date.toInstant())
  }

  private fun getServerTimeEntity(): ForcedServerDateTime? =
    entityManager
      .createQuery(
        "select st from ForcedServerDateTime st where st.id = 1",
        ForcedServerDateTime::class.java,
      ).resultList
      .singleOrNull()

  private fun getForcedTime(): Timestamp? =
    entityManager
      .createNativeQuery(
        "select st.time from public.forced_server_date_time st where st.id = 1",
        Timestamp::class.java,
      ).resultList
      .singleOrNull() as Timestamp?
}
