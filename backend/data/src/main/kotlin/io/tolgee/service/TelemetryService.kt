package io.tolgee.service

import io.tolgee.component.HttpClient
import io.tolgee.configuration.tolgee.TelemetryProperties
import io.tolgee.dtos.TelemetryReportRequest
import org.springframework.http.HttpMethod
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@Service
class TelemetryService(
  private val instanceIdService: InstanceIdService,
  private val entityManager: EntityManager,
  private val httpClient: HttpClient,
  private val telemetryProperties: TelemetryProperties
) {
  companion object {
    const val TELEMETRY_REPORT_PERIOD_MS = 24 * 60 * 60 * 1000L
  }

  @Scheduled(fixedDelayString = """${'$'}{tolgee.telemetry.report-period-ms:$TELEMETRY_REPORT_PERIOD_MS}""")
  @Transactional
  fun report() {
    if (!telemetryProperties.enabled) return
    val data: TelemetryReportRequest = getTelemetryData()
    if (data.projectsCount == 0L) return
    if (data.usersCount == 0L) return
    httpClient.requestForJson(
      "${telemetryProperties.server}/v2/public/telemetry/report",
      data,
      HttpMethod.POST,
      Unit::class.java
    )
  }

  private fun getTelemetryData(): TelemetryReportRequest {
    return TelemetryReportRequest().apply {
      instanceId = instanceIdService.getInstanceId()
      projectsCount = getProjectsCount()
      translationsCount = getTranslationsCount()
      languagesCount = getLanguagesCount()
      distinctLanguagesCount = getDistinctLanguagesCount()
      usersCount = getUsersCount()
    }
  }

  private fun getUsersCount(): Long {
    return entityManager.createQuery(
      """
      select count(u) from UserAccount u
    """,
      Long::class.javaObjectType
    ).singleResult
  }

  private fun getDistinctLanguagesCount(): Long {
    return entityManager.createQuery(
      """
      select count(distinct l.tag) from Language l
    """,
      Long::class.javaObjectType
    ).singleResult
  }

  private fun getLanguagesCount(): Long {
    return entityManager.createQuery(
      """
      select count(l) from Language l
    """,
      Long::class.javaObjectType
    ).singleResult
  }

  private fun getTranslationsCount(): Long {
    return entityManager.createQuery(
      """
      select count(t) from Translation t
    """,
      Long::class.javaObjectType
    ).singleResult
  }

  private fun getProjectsCount(): Long {
    return entityManager.createQuery(
      """
      select count(p) from Project p
    """,
      Long::class.javaObjectType
    ).singleResult
  }
}
