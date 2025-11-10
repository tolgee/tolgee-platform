package io.tolgee.component.automations

import io.tolgee.activity.data.ActivityType
import io.tolgee.batch.BatchJobService
import io.tolgee.batch.BatchOperationParams
import io.tolgee.batch.data.BatchJobType
import io.tolgee.batch.request.AutomationBjRequest
import io.tolgee.dtos.cacheable.automations.AutomationActionDto
import io.tolgee.dtos.cacheable.automations.AutomationDto
import io.tolgee.dtos.cacheable.automations.AutomationTriggerDto
import io.tolgee.model.Project
import io.tolgee.model.automations.AutomationTriggerType
import io.tolgee.service.automations.AutomationService
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class AutomationsBatchJobCreator(
  val batchJobService: BatchJobService,
  val automationService: AutomationService,
  val entityManager: EntityManager,
) {
  fun executeActivityAutomation(
    projectId: Long,
    type: ActivityType,
    activityRevisionId: Long,
  ) {
    startBatchJobForAutomations(projectId, AutomationTriggerType.ACTIVITY, type, activityRevisionId)
  }

  fun executeTranslationDataModificationAutomation(
    projectId: Long,
    activityRevisionId: Long,
  ) {
    startBatchJobForAutomations(
      projectId,
      AutomationTriggerType.TRANSLATION_DATA_MODIFICATION,
      null,
      activityRevisionId,
    )
  }

  fun startBatchJobForAutomations(
    projectId: Long,
    triggerType: AutomationTriggerType,
    activityType: ActivityType? = null,
    activityRevisionId: Long,
  ) {
    val automations =
      automationService.getProjectAutomations(projectId, triggerType, activityType)

    val automationTriggersMap =
      getAutomationTriggersMap(automations)

    automationTriggersMap.forEach { (trigger, automation) ->
      automation.actions.forEach { action ->
        val debouncingKeyProvider: ((BatchOperationParams) -> Any)? =
          action.type.debouncingKeyProvider?.let { actionProvider ->
            { batchOperationParams -> actionProvider(batchOperationParams, action, trigger) }
          }
        startAutomationBatchJob(trigger, action, projectId, activityRevisionId, debouncingKeyProvider)
      }
    }
  }

  private fun getAutomationTriggersMap(automations: List<AutomationDto>) =
    automations.flatMap { automation -> automation.triggers.map { it to automation } }

  private fun startAutomationBatchJob(
    trigger: AutomationTriggerDto,
    action: AutomationActionDto,
    projectId: Long,
    activityRevisionId: Long,
    debouncingKeyProvider: ((BatchOperationParams) -> Any)? = null,
  ) {
    batchJobService.startJob(
      AutomationBjRequest(trigger.id, action.id, activityRevisionId),
      project = entityManager.getReference(Project::class.java, projectId),
      author = null,
      type = BatchJobType.AUTOMATION,
      isHidden = true,
      debounceDuration = trigger.debounceDurationInMs?.let { Duration.ofMillis(it) },
      debouncingKeyProvider = debouncingKeyProvider,
    )
  }
}
