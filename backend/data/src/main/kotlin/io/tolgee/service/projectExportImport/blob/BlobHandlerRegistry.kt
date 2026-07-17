package io.tolgee.service.projectExportImport.blob

import org.springframework.stereotype.Component

/**
 * Resolves the [BlobHandler] for an entity class, if any. Most OWNED entities own no external bytes
 * and have no handler; the export serializer simply skips them.
 */
@Component
class BlobHandlerRegistry(
  handlers: List<BlobHandler>,
) {
  // Keyed by the JVM binary name (java.name), matching the lookup key (EntityType.javaType.name) — not
  // KClass.qualifiedName, which differs for a nested @Entity (`Outer.Inner` vs `Outer$Inner`).
  private val byClassName: Map<String, BlobHandler> =
    handlers.associateBy { it.entityClass.java.name }

  fun handlerFor(entityClassName: String): BlobHandler? = byClassName[entityClassName]
}
