package io.tolgee.service.projectExportImport.sidechannel

import io.tolgee.service.projectExportImport.model.ExportZipLayout
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

class SideChannelHandlerRegistryTest {
  @Test
  fun `rejects a handler whose entryName is not under sidechannels`() {
    assertThatThrownBy { SideChannelHandlerRegistry(listOf(fakeHandler("top-level.json", String::class))) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessageContaining(ExportZipLayout.SIDE_CHANNELS_DIR)
  }

  @Test
  fun `rejects two handlers claiming the same zip entry`() {
    assertThatThrownBy {
      SideChannelHandlerRegistry(
        listOf(
          fakeHandler("${ExportZipLayout.SIDE_CHANNELS_DIR}a.json", String::class),
          fakeHandler("${ExportZipLayout.SIDE_CHANNELS_DIR}a.json", Int::class),
        ),
      )
    }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("same zip entry")
  }

  @Test
  fun `rejects two handlers claiming the same entity`() {
    assertThatThrownBy {
      SideChannelHandlerRegistry(
        listOf(
          fakeHandler("${ExportZipLayout.SIDE_CHANNELS_DIR}a.json", String::class),
          fakeHandler("${ExportZipLayout.SIDE_CHANNELS_DIR}b.json", String::class),
        ),
      )
    }.isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("same entity")
  }

  @Test
  fun `exposes handlers sorted by entryName for a reproducible export`() {
    val registry =
      SideChannelHandlerRegistry(
        listOf(
          fakeHandler("${ExportZipLayout.SIDE_CHANNELS_DIR}b.json", String::class),
          fakeHandler("${ExportZipLayout.SIDE_CHANNELS_DIR}a.json", Int::class),
        ),
      )
    assertThat(registry.handlersInWriteOrder.map { it.entryName })
      .containsExactly("${ExportZipLayout.SIDE_CHANNELS_DIR}a.json", "${ExportZipLayout.SIDE_CHANNELS_DIR}b.json")
  }

  private fun fakeHandler(
    entry: String,
    klass: KClass<*>,
  ) = object : SideChannelHandler {
    override val entityClass = klass
    override val entryName = entry

    override fun collectForExport(projectId: Long) = emptyList<Any>()

    override fun restore(
      json: ByteArray,
      context: SideChannelImportContext,
    ) {}
  }
}
