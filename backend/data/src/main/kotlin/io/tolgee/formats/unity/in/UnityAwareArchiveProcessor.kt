package io.tolgee.formats.unity.`in`

import io.tolgee.dtos.dataImport.ImportFileDto
import io.tolgee.service.dataImport.processors.ImportArchiveProcessor
import io.tolgee.service.dataImport.processors.ZipTypeProcessor

/**
 * Unzips via the generic [ZipTypeProcessor], then — only when the archive holds a Unity Localization
 * collection — merges its `.asset` files into synthetic `.unity` files. Non-Unity zips pass through
 * unchanged.
 */
class UnityAwareArchiveProcessor(
  private val zipTypeProcessor: ZipTypeProcessor,
  private val unityArchiveProcessor: UnityArchiveProcessor,
) : ImportArchiveProcessor {
  override fun process(file: ImportFileDto): Collection<ImportFileDto> {
    val unpacked = zipTypeProcessor.process(file)
    if (!unityArchiveProcessor.isUnity(unpacked)) {
      return unpacked
    }
    return unityArchiveProcessor.merge(unpacked)
  }
}
