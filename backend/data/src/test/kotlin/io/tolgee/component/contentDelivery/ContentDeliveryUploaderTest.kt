package io.tolgee.component.contentDelivery

import io.tolgee.component.CurrentDateProvider
import io.tolgee.component.contentDelivery.cachePurging.ContentDeliveryCachePurgingProvider
import io.tolgee.component.fileStorage.FileStorage
import io.tolgee.events.OnContentDeliveryPublished
import io.tolgee.model.Project
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.service.contentDelivery.ContentDeliveryConfigService
import io.tolgee.service.export.ExportService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.context.ApplicationEventPublisher
import java.util.Date

class ContentDeliveryUploaderTest {
  @Test
  fun `publishes event with the published snapshot`() {
    val project = mock<Project> { on { id } doReturn 10L }
    val config =
      mock<ContentDeliveryConfig> {
        on { id } doReturn 55L
        on { this.project } doReturn project
        on { name } doReturn "Production CDN"
        on { slug } doReturn "abc123"
        on { zip } doReturn false
        on { contentStorage } doReturn null
        on { pruneBeforePublish } doReturn false
      }

    val configService = mock<ContentDeliveryConfigService> { on { get(55L) } doReturn config }
    val storage = mock<FileStorage>()
    val storageProvider =
      mock<ContentDeliveryFileStorageProvider> {
        on { getContentStorageWithDefaultClient() } doReturn storage
      }
    val exportService =
      mock<ExportService> {
        on { export(any(), any()) } doReturn mapOf("en.json" to "{}".byteInputStream())
      }
    val purgingProvider = mock<ContentDeliveryCachePurgingProvider> { on { purgings } doReturn listOf() }
    val dateProvider = mock<CurrentDateProvider> { on { date } doReturn Date(1718539200000) }
    val eventPublisher = mock<ApplicationEventPublisher>()

    val uploader =
      ContentDeliveryUploader(
        storageProvider,
        exportService,
        configService,
        purgingProvider,
        dateProvider,
        eventPublisher,
      )

    uploader.upload(55L)

    val captor = argumentCaptor<OnContentDeliveryPublished>()
    verify(eventPublisher).publishEvent(captor.capture())
    val data = captor.firstValue.data
    assertThat(data.projectId).isEqualTo(10L)
    assertThat(data.id).isEqualTo(55L)
    assertThat(data.slug).isEqualTo("abc123")
    assertThat(data.lastPublished).isEqualTo(1718539200000)
    assertThat(data.files).containsExactly("en.json")
  }
}
