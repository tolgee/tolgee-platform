package io.tolgee.testing

import io.tolgee.development.testDataBuilder.builders.ProjectBuilder
import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.contentDelivery.ContentStorage
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import io.tolgee.repository.LanguageRepository
import io.tolgee.repository.ProjectRepository
import io.tolgee.repository.contentDelivery.ContentDeliveryConfigRepository
import io.tolgee.repository.contentDelivery.ContentStorageRepository
import io.tolgee.repository.key.KeyRepository
import io.tolgee.repository.translation.TranslationRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Component for building isolated test data.
 * Each test should use this builder to create its own isolated data set.
 */
@Component
class IsolatedTestDataBuilder {
    
    @Autowired
    private lateinit var projectRepository: ProjectRepository
    
    @Autowired
    private lateinit var languageRepository: LanguageRepository
    
    @Autowired
    private lateinit var keyRepository: KeyRepository
    
    @Autowired
    private lateinit var translationRepository: TranslationRepository
    
    @Autowired
    private lateinit var contentDeliveryConfigRepository: ContentDeliveryConfigRepository
    
    @Autowired
    private lateinit var contentStorageRepository: ContentStorageRepository
    
    // Use atomic counter for generating unique IDs
    private val uniqueCounter = AtomicLong(System.currentTimeMillis())
    
    /**
     * Creates a unique identifier for test data to avoid conflicts between tests
     */
    private fun uniqueId(): String {
        return "test_${uniqueCounter.incrementAndGet()}"
    }
    
    /**
     * Creates a complete set of test data with unique identifiers
     * This ensures tests don't interfere with each other
     */
    @Transactional
    fun createTestData(
        projectName: String = "Test Project ${uniqueId()}",
        withLanguages: Boolean = true,
        withKeys: Boolean = false,
        withTranslations: Boolean = false,
        withContentDelivery: Boolean = false
    ): TestDataSet {
        // Create project with unique name and slug
        val project = Project().apply {
            name = projectName
            slug = "test-${UUID.randomUUID()}"
        }
        projectRepository.save(project)
        
        // Create languages if needed
        val languages = if (withLanguages) {
            listOf("en", "cs", "de").map { tag ->
                Language().apply {
                    name = "Language $tag"
                    this.tag = tag
                    this.project = project
                }
            }.also { languageRepository.saveAll(it) }
        } else emptyList()
        
        // Create keys if needed
        val keys = if (withKeys) {
            (1..5).map { i ->
                Key().apply {
                    name = "key_${uniqueId()}_$i"
                    this.project = project
                }
            }.also { keyRepository.saveAll(it) }
        } else emptyList()
        
        // Create translations if needed
        val translations = if (withTranslations && withKeys && withLanguages) {
            val result = mutableListOf<Translation>()
            keys.forEach { key ->
                languages.forEach { language ->
                    result.add(
                        Translation().apply {
                            this.key = key
                            this.language = language
                            text = "Translation for ${key.name} in ${language.tag}"
                        }
                    )
                }
            }
            translationRepository.saveAll(result)
            result
        } else emptyList()
        
        // Create content delivery config if needed
        val contentDeliveryConfig = if (withContentDelivery) {
            ContentDeliveryConfig().apply {
                name = "CD Config ${uniqueId()}"
                slug = "cd-${UUID.randomUUID()}"
                this.project = project
            }.also { contentDeliveryConfigRepository.save(it) }
        } else null
        
        // Create content storage if needed
        val contentStorage = if (withContentDelivery) {
            ContentStorage().apply {
                name = "Storage ${uniqueId()}"
                this.project = project
            }.also { contentStorageRepository.save(it) }
        } else null
        
        return TestDataSet(
            project = project,
            languages = languages,
            keys = keys,
            translations = translations,
            contentDeliveryConfig = contentDeliveryConfig,
            contentStorage = contentStorage
        )
    }
    
    /**
     * Data class to hold a complete set of related test entities.
     */
    data class TestDataSet(
        val project: Project,
        val languages: List<Language>,
        val keys: List<Key>,
        val translations: List<Translation>,
        val contentDeliveryConfig: ContentDeliveryConfig? = null,
        val contentStorage: ContentStorage? = null
    )
} 