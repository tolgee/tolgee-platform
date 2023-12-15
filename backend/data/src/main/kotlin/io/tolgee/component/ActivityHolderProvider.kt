package io.tolgee.component

import io.tolgee.activity.ActivityHolder
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.support.ScopeNotActiveException
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * Class providing Activity Holder, while caching it in ThreadLocal.
 * I also registers a transaction synchronization, which clears the ThreadLocal after transaction
 * is completed, this enables us to be safe even when running activities in main thred.
 */
@Component
class ActivityHolderProvider(private val applicationContext: ApplicationContext) {
  private val threadLocal = ThreadLocal<ActivityHolder?>()

  fun getActivityHolder(): ActivityHolder {
    // Get the activity holder from ThreadLocal.
    var activityHolder = threadLocal.get()
    if (activityHolder == null) {
      // If no activity holder exists for this thread, fetch one and store it in ThreadLocal.
      activityHolder = fetchActivityHolder()
      threadLocal.set(activityHolder)
    }
    return activityHolder
  }

  private fun fetchActivityHolder(): ActivityHolder {
    return try {
      applicationContext.getBean("requestActivityHolder", ActivityHolder::class.java).also {
        it.activityRevision
      }
    } catch (e: ScopeNotActiveException) {
      val transactionActivityHolder =
        applicationContext.getBean("transactionActivityHolder", ActivityHolder::class.java)
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(
          object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
              clearThreadLocal()
            }
          }
        )
        return transactionActivityHolder
      }

      throw IllegalStateException("Transaction synchronization is not active.")
    }
  }

  @PreDestroy
  fun clearThreadLocal() {
    threadLocal.remove()
  }
}
