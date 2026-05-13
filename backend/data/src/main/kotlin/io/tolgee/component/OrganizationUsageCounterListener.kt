package io.tolgee.component

import io.tolgee.activity.data.RevisionType
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.events.OnProjectActivityEvent
import io.tolgee.model.SoftDeletable
import io.tolgee.model.activity.ActivityModifiedEntity
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.service.organization.OrganizationUsageCounterService
import io.tolgee.util.BypassableListener
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Maintains the per-organization usage counter (`organization_usage_counter`) once per
 * transaction by reading deltas from the activity event's modified-entities map.
 *
 * Uses `OnProjectActivityEvent` (one event per transaction per project) rather than
 * `EntityPreCommitEvent` (one event per entity). On a 10k-key bulk import that
 * difference is ~20k method invocations vs 1.
 *
 * Plain `@EventListener` (synchronous): `OnProjectActivityEvent` is published from inside
 * Hibernate's `BeforeTransactionCompletionProcess`, by which point Spring's BEFORE_COMMIT
 * phase has already started. A `@TransactionalEventListener(BEFORE_COMMIT)` here gets
 * registered too late and silently skipped — matching what the sibling
 * `StringsKeysUsageListener` already does. The handler runs inside the original
 * transaction, so a transaction rollback also rolls back the counter UPDATE.
 */
@Component
class OrganizationUsageCounterListener(
  private val counterService: OrganizationUsageCounterService,
  private val tolgeeProperties: TolgeeProperties,
) : BypassableListener {
  override var bypass: Boolean = false

  @EventListener
  fun onActivity(event: OnProjectActivityEvent) {
    if (!tolgeeProperties.orgCounter.enabled || bypass) return
    val orgId = event.activityRevision.organizationId ?: return

    var keyDelta = 0L
    var translationDelta = 0L

    event.modifiedEntities[Key::class]?.values?.forEach { keyDelta += computeKeyDelta(it) }
    event.modifiedEntities[Translation::class]?.values?.forEach {
      translationDelta += computeTranslationDelta(it)
    }

    if (keyDelta != 0L || translationDelta != 0L) {
      counterService.applyDelta(orgId, keyDelta, translationDelta)
    }
  }

  private fun computeKeyDelta(entity: ActivityModifiedEntity): Long =
    when (entity.revisionType) {
      RevisionType.ADD -> 1L
      RevisionType.DEL -> -1L
      RevisionType.MOD -> deletedAtDelta(entity)
    }

  private fun computeTranslationDelta(entity: ActivityModifiedEntity): Long {
    val textMod = entity.modifications["text"]
    return when (entity.revisionType) {
      RevisionType.ADD -> if (!(textMod?.new as? String).isNullOrEmpty()) 1L else 0L
      RevisionType.DEL -> if (!(textMod?.old as? String).isNullOrEmpty()) -1L else 0L
      RevisionType.MOD -> {
        if (textMod == null) {
          // No text change; check soft-delete transitions (translations are hard-deleted
          // today, but the rule mirrors keys for safety).
          return deletedAtDelta(entity)
        }
        val oldEmpty = (textMod.old as? String).isNullOrEmpty()
        val newEmpty = (textMod.new as? String).isNullOrEmpty()
        when {
          oldEmpty && !newEmpty -> 1L
          !oldEmpty && newEmpty -> -1L
          else -> 0L
        }
      }
    }
  }

  private fun deletedAtDelta(entity: ActivityModifiedEntity): Long {
    val mod = entity.modifications[SoftDeletable::deletedAt.name] ?: return 0L
    return when {
      mod.old == null && mod.new != null -> -1L
      mod.old != null && mod.new == null -> 1L
      else -> 0L
    }
  }
}
