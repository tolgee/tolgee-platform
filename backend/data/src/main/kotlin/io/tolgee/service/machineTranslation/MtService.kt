package io.tolgee.service.machineTranslation

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MtService(
  private val applicationContext: ApplicationContext,
) {
  /**
   * noRollbackFor: batch chunk processing catches provider exceptions and commits the chunk
   * transaction with the successfully translated items. Without it, an exception passing
   * through this boundary marks the shared transaction rollback-only, and the whole chunk
   * (including already translated items) gets rolled back to the savepoint.
   */
  @Transactional(noRollbackFor = [Exception::class])
  fun getMachineTranslations(
    projectId: Long,
    isBatch: Boolean,
    paramsModifier: MachineTranslationParams.() -> Unit,
  ): List<MtTranslatorResult> {
    val params = MachineTranslationParams()
    params.paramsModifier()
    return getMachineTranslations(projectId, isBatch, params)
  }

  @Transactional(noRollbackFor = [Exception::class])
  fun getMachineTranslations(
    projectId: Long,
    isBatch: Boolean,
    params: MachineTranslationParams,
  ): List<MtTranslatorResult> {
    return MtTranslator(projectId, applicationContext, isBatch).translate(listOf(params))
  }

  @Transactional
  fun getMtTranslator(
    projectId: Long,
    isBatch: Boolean,
  ): MtTranslator {
    return MtTranslator(projectId, applicationContext, isBatch)
  }
}
