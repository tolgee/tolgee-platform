package io.tolgee.ee.service.branching

import io.tolgee.model.key.Key
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.translation.Translation
import org.springframework.stereotype.Component
import java.util.LinkedHashMap

@Component
class BranchMergeKeyCloneFactory {
  fun cloneForMerge(target: Key): Key {
    val clone =
      Key(target.name).apply {
        id = target.id
        namespace = target.namespace
        project = target.project
        branch = target.branch
        isPlural = target.isPlural
        pluralArgName = target.pluralArgName
      }

    target.keyMeta?.let { meta ->
      val clonedMeta =
        KeyMeta(key = clone).apply {
          description = meta.description
          custom = meta.custom?.let { LinkedHashMap(it) }
          tags.addAll(meta.tags)
        }
      clone.keyMeta = clonedMeta
    }

    target.translations.forEach { translation ->
      val clonedTranslation =
        Translation(translation.text).apply {
          id = translation.id
          key = clone
          language = translation.language
          state = translation.state
          auto = translation.auto
          outdated = translation.outdated
          mtProvider = translation.mtProvider
          labels.addAll(translation.labels)
        }
      clone.translations.add(clonedTranslation)
    }

    return clone
  }
}
