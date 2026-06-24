package io.tolgee.service.projectExportImport

/**
 * Excludes a single persistent column of an OWNED entity from project export/import. Use it for
 * columns that must not cross instances:
 *  - instance-local secrets, and
 *  - soft-FK columns stored as a raw id rather than a JPA association (e.g. `Translation.promptId`),
 *    which the metamodel walk sees as a plain scalar and would copy as a stale id that dangles on the
 *    target instance.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class DoNotExport
