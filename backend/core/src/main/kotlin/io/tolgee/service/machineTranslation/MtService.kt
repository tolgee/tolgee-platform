package io.tolgee.service.machineTranslation

import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MtService(
  private val applicationContext: ApplicationContext,
) {
  @Transactional
  fun getMachineTranslations(
    projectId: Long,
    isBatch: Boolean,
    paramsModifier: MachineTranslationParams.() -> Unit,
  ): List<MtTranslatorResult> {
    val params = MachineTranslationParams()
    params.paramsModifier()
    return getMachineTranslations(projectId, isBatch, params)
  }

  @Transactional
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
