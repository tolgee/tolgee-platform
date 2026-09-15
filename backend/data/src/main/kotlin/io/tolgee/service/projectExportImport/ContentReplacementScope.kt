package io.tolgee.service.projectExportImport

import io.tolgee.configuration.TransactionScopeConfig
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.stereotype.Component

/**
 * Marks the transaction that replaces a project's content wholesale.
 *
 * The wipe half of that replacement is bulk JPQL, which emits no entity events, while the re-insert
 * emits one per row — so a usage check that adds the inserts to a pre-wipe baseline sees content
 * counted twice and refuses a restore of content the instance already held. Transaction-scoped, so
 * it cannot leak between concurrent requests and dies with the transaction even if the import throws.
 *
 * Covers writes whose entity event fires synchronously — persist and remove — plus anything flushed
 * inside the block. A dirty update's event is emitted at flush, so a mutation left to the
 * commit-time flush is *not* covered: flush it inside [replacingContent] instead.
 */
@Scope(TransactionScopeConfig.SCOPE_TRANSACTION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
open class ContentReplacementScope {
  open var isReplacingContent = false

  open fun <T> replacingContent(body: () -> T): T {
    isReplacingContent = true
    try {
      return body()
    } finally {
      isReplacingContent = false
    }
  }
}
