package io.tolgee.service.projectExportImport

import io.tolgee.model.Language
import io.tolgee.model.Screenshot
import io.tolgee.model.TranslationSuggestion
import io.tolgee.model.branching.Branch
import io.tolgee.model.branching.snapshot.KeyMetaSnapshot
import io.tolgee.model.branching.snapshot.KeySnapshot
import io.tolgee.model.branching.snapshot.TranslationSnapshot
import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyCodeReference
import io.tolgee.model.key.KeyComment
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.Namespace
import io.tolgee.model.key.Tag
import io.tolgee.model.key.screenshotReference.KeyScreenshotReference
import io.tolgee.model.qa.LanguageQaConfig
import io.tolgee.model.qa.ProjectQaConfig
import io.tolgee.model.qa.TranslationQaIssue
import io.tolgee.model.task.Task
import io.tolgee.model.task.TaskKey
import io.tolgee.model.translation.Label
import io.tolgee.model.translation.Translation
import io.tolgee.model.translation.TranslationComment
import kotlin.reflect.KClass

/**
 * The per-OWNED-type project-scoped JPQL that discovers the rows to export, each keyed by a single
 * `:projectId` parameter. Row discovery is per-type rather than a graph walk because some OWNED types
 * have no navigable inverse path from `Project` (`ProjectQaConfig`, `LanguageQaConfig`, unassigned
 * `Label`s).
 *
 * Soft-delete is NOT enforced globally: there is no `@SQLRestriction` on `Language`/`Key`/`Branch`
 * (only on the `Project.branches` collection), so each query must itself filter `deletedAt IS NULL` at
 * every hop through a soft-deletable entity — otherwise a child of a soft-deleted key/language/branch
 * would be exported and dangle on import. `Screenshot` has no `project`/`key` FK, so it is collected
 * through a `DISTINCT` two-hop join via `KeyScreenshotReference`.
 */
object ProjectScopedCollectorQueries {
  val queriesByClassName: Map<String, String> =
    buildMap {
      query(Language::class, "select e from Language e where e.project.id = :projectId and e.deletedAt is null")
      query(Namespace::class, "select e from Namespace e where e.project.id = :projectId")
      // A key on a soft-deleted branch is deleted content (hidden everywhere in the app), so it and its
      // children are excluded from the export — not exported with the branch nulled, which would resurrect
      // them onto the default branch on import (colliding with real keys). The branch hop is nullable
      // (NULL = legacy default branch), so it must be a LEFT JOIN — an implicit inner join via
      // `...branch.deletedAt` would silently drop every row whose key/task has no branch at all.
      query(
        Key::class,
        "select e from Key e left join e.branch b " +
          "where e.project.id = :projectId and e.deletedAt is null and (b is null or b.deletedAt is null)",
      )
      query(
        KeyMeta::class,
        "select e from KeyMeta e join e.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and (b is null or b.deletedAt is null)",
      )
      query(
        KeyComment::class,
        "select e from KeyComment e join e.keyMeta.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and (b is null or b.deletedAt is null)",
      )
      query(
        KeyCodeReference::class,
        "select e from KeyCodeReference e join e.keyMeta.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and (b is null or b.deletedAt is null)",
      )
      query(
        Translation::class,
        "select e from Translation e join e.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and e.language.deletedAt is null " +
          "and (b is null or b.deletedAt is null)",
      )
      query(
        TranslationComment::class,
        "select e from TranslationComment e join e.translation.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and e.translation.language.deletedAt is null " +
          "and (b is null or b.deletedAt is null)",
      )
      query(
        TranslationSuggestion::class,
        "select e from TranslationSuggestion e join e.key k left join k.branch b " +
          "where e.project.id = :projectId and k.deletedAt is null and e.language.deletedAt is null " +
          "and (b is null or b.deletedAt is null)",
      )
      query(Tag::class, "select e from Tag e where e.project.id = :projectId")
      query(Label::class, "select e from Label e where e.project.id = :projectId")
      query(
        Screenshot::class,
        "select distinct s from KeyScreenshotReference r join r.screenshot s join r.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and (b is null or b.deletedAt is null)",
      )
      query(
        KeyScreenshotReference::class,
        "select e from KeyScreenshotReference e join e.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and (b is null or b.deletedAt is null)",
      )
      query(Branch::class, "select e from Branch e where e.project.id = :projectId and e.deletedAt is null")
      query(
        Task::class,
        "select e from Task e left join e.branch b " +
          "where e.project.id = :projectId and e.language.deletedAt is null and (b is null or b.deletedAt is null)",
      )
      query(
        TaskKey::class,
        "select e from TaskKey e join e.key k left join e.task.branch tb left join k.branch kb " +
          "where e.task.project.id = :projectId and e.task.language.deletedAt is null and k.deletedAt is null " +
          "and (tb is null or tb.deletedAt is null) and (kb is null or kb.deletedAt is null)",
      )
      query(ProjectQaConfig::class, "select e from ProjectQaConfig e where e.project.id = :projectId")
      query(
        LanguageQaConfig::class,
        "select e from LanguageQaConfig e where e.language.project.id = :projectId and e.language.deletedAt is null",
      )
      query(
        TranslationQaIssue::class,
        "select e from TranslationQaIssue e join e.translation.key k left join k.branch b " +
          "where k.project.id = :projectId and k.deletedAt is null and e.translation.language.deletedAt is null " +
          "and (b is null or b.deletedAt is null)",
      )
      query(
        KeySnapshot::class,
        "select e from KeySnapshot e join e.branch b where e.project.id = :projectId and b.deletedAt is null",
      )
      query(
        TranslationSnapshot::class,
        "select e from TranslationSnapshot e join e.keySnapshot ks join ks.branch b " +
          "where ks.project.id = :projectId and b.deletedAt is null",
      )
      query(
        KeyMetaSnapshot::class,
        "select e from KeyMetaSnapshot e join e.keySnapshot ks join ks.branch b " +
          "where ks.project.id = :projectId and b.deletedAt is null",
      )
    }

  private fun MutableMap<String, String>.query(
    klass: KClass<*>,
    jpql: String,
  ) {
    // Key by the JVM binary name (java.name) to match the lookup, which uses EntityType.javaType.name.
    val name = klass.java.name
    val previous = put(name, jpql)
    require(previous == null) { "$name has more than one collector query" }
  }
}
