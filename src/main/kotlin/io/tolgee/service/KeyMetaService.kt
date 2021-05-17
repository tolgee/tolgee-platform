package io.tolgee.service

import io.tolgee.model.key.KeyCodeReference
import io.tolgee.model.key.KeyComment
import io.tolgee.model.key.KeyMeta
import io.tolgee.model.key.WithKeyMetaReference
import io.tolgee.repository.KeyCommentRepository
import io.tolgee.repository.KeyCoreReferenceRepository
import io.tolgee.repository.KeyMetaRepository
import io.tolgee.security.AuthenticationFacade
import org.springframework.stereotype.Service

@Service
class KeyMetaService(
        private val keyMetaRepository: KeyMetaRepository,
        private val keyCodeReferenceRepository: KeyCoreReferenceRepository,
        private val keyCommentRepository: KeyCommentRepository,
        private val authenticationFacade: AuthenticationFacade
) {
    fun saveAll(entities: Iterable<KeyMeta>): MutableList<KeyMeta> = keyMetaRepository.saveAll(entities)

    fun saveAllComments(entities: Iterable<KeyComment>): MutableList<KeyComment> =
            keyCommentRepository.saveAll(entities)

    fun saveAllCodeReferences(entities: Iterable<KeyCodeReference>): MutableList<KeyCodeReference> =
            keyCodeReferenceRepository.saveAll(entities)

    fun import(target: KeyMeta, source: KeyMeta) {
        target.comments.import(target, source.comments) { a, b ->
            a.text == b.text && a.fromImport == b.fromImport
        }
        target.codeReferences.import(target, source.codeReferences) { a, b ->
            a.line == b.line && a.path == b.path
        }
    }

    private inline fun <T : WithKeyMetaReference> MutableList<T>.import(
            target: KeyMeta,
            other: MutableCollection<T>,
            equalsFn: (a: T, b: T) -> Boolean
    ) {
        val toRemove = mutableListOf<WithKeyMetaReference>()
        other.forEach { otherItem ->
            if (!this.any { equalsFn(it, otherItem) }) {
                this.add(otherItem)
                toRemove.add(otherItem)
                otherItem.keyMeta = target
            }
        }
        other.removeAll(toRemove)
    }
}
