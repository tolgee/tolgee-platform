package io.tolgee.component

import io.tolgee.activity.ActivityHolder
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder

/**
 * Class providing Activity Holder, while caching it in ThreadLocal.
 * I also registers a transaction synchronization, which clears the ThreadLocal after transaction
 * is completed, this enables us to be safe even when running activities in main thred.
 */
@Component
class ActivityHolderProvider(
  private val applicationContext: ApplicationContext,
) {
  private val threadLocal = ThreadLocal<Pair<Scope, ActivityHolder?>>()

  fun getActivityHolder(): ActivityHolder {
    // Get the activity holder from ThreadLocal.
    var (scope, activityHolder) = threadLocal.get() ?: (null to null)

    val currentScope = getCurrentScope()
    if (scope != currentScope) {
      // If the scope has changed, clear the ThreadLocal and fetch a new activity holder.
      clearThreadLocal()
      activityHolder = null
    }

    if (activityHolder == null) {
      // If no activity holder exists for this thread, fetch one and store it in ThreadLocal.
      activityHolder = fetchActivityHolder()
      threadLocal.set(currentScope to activityHolder)
    }
    return activityHolder
  }

  private fun fetchActivityHolder(): ActivityHolder {
    return try {
      applicationContext.getBean("requestActivityHolder", ActivityHolder::class.java).also {
        it.activityRevision
      }
    } catch (e: ScopeNotActiveException) {
      applicationContext.getBean("transactionActivityHolder", ActivityHolder::class.java)
    }.also {
      it.destroyer = this::clearThreadLocal
    }
  }

  private fun getCurrentScope(): Scope {
    if (RequestContextHolder.getRequestAttributes() == null) {
      return Scope.TRANSACTION
    }
    return Scope.REQUEST
  }

  @PreDestroy
  fun clearThreadLocal() {
    threadLocal.remove()
  }

  enum class Scope {
    REQUEST,
    TRANSACTION,
  }
}
