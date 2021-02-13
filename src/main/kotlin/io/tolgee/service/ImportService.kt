package io.tolgee.service

import io.tolgee.dtos.ImportDto
import io.tolgee.model.Repository
import io.tolgee.model.Key
import io.tolgee.model.Translation
import io.tolgee.repository.KeyRepository
import io.tolgee.repository.TranslationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.OutputStream
import java.util.stream.Collectors
import kotlin.collections.ArrayList

@Service
open class ImportService(
        private val languageService: LanguageService,
        private val keyService: KeyService,
        private val keyRepository: KeyRepository,
        private val translationRepository: TranslationRepository
) {

    @Autowired
    private lateinit var translationService: TranslationService;

    @Transactional
    open fun import(repository: Repository, dto: ImportDto, emitter: OutputStream) {
        val language = languageService.getOrCreate(repository, dto.languageAbbreviation)
        val allKeys = keyService.getAll(repository.id).stream().collect(Collectors.toMap({ it.name }, { it }))
        val allTranslations = translationService.getAllByLanguageId(language.id)
                .stream()
                .collect(Collectors.toMap({ it.key.id }, { it }))

        val keysToSave = ArrayList<Key>();
        val translationsToSave = ArrayList<Translation>()

        for ((index, entry) in dto.data!!.entries.withIndex()) {
            val key = allKeys[entry.key] ?: {
                val keyToSave = Key(name = entry.key, repository = repository)
                keysToSave.add(keyToSave)
                keyToSave

            }()

            val translation = allTranslations[key.id] ?: Translation()
            translation.key = key
            translation.language = language
            translation.text = entry.value
            translationsToSave.add(translation)
            emitter.write(index);
        }

        keyRepository.saveAll(keysToSave)
        translationRepository.saveAll(translationsToSave)
    }
}