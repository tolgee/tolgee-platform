import java.util.concurrent.atomic.AtomicLong
import org.springframework.transaction.annotation.Transactional

class TestDataBuilder {
    // Add this field to generate unique identifiers
    private val uniqueId = AtomicLong(System.currentTimeMillis())
    
    /**
     * Generates a unique string identifier for test data.
     * This ensures that each test uses unique data to avoid conflicts.
     */
    fun getUniqueString(prefix: String = ""): String {
        return "${prefix}_${uniqueId.incrementAndGet()}"
    }
    
    /**
     * Creates a project with a unique name to avoid conflicts between tests.
     * This eliminates the need for database truncation between tests.
     */
    @Transactional
    fun createUniqueProject(name: String? = null): Project {
        val projectName = name ?: getUniqueString("project")
        val project = Project()
        project.name = projectName
        project.slug = getUniqueString("slug")
        // Set other required properties
        return projectRepository.save(project)
    }
    
    /**
     * Creates a content delivery config with a unique name.
     */
    @Transactional
    fun createUniqueContentDeliveryConfig(project: Project): ContentDeliveryConfig {
        val config = ContentDeliveryConfig(project)
        config.name = getUniqueString("config")
        config.slug = getUniqueString("slug")
        // Set other required properties
        return contentDeliveryConfigRepository.save(config)
    }
    
    /**
     * Creates a complete project with all related entities using unique identifiers.
     * This method is optimized to minimize database operations.
     */
    @Transactional
    fun createCompleteProjectOptimized(
        projectName: String? = null,
        languages: List<String> = listOf("en", "de"),
        keys: List<String> = listOf("key1", "key2"),
        withContentDelivery: Boolean = false
    ): TestDataSet {
        // Create project with unique name
        val project = createUniqueProject(projectName ?: getUniqueString("project"))
        
        // Create languages in batch
        val languageEntities = languages.map { tag ->
            val language = Language()
            language.tag = tag
            language.name = "Language $tag"
            language.project = project
            language
        }
        languageRepository.saveAll(languageEntities)
        
        // Create keys in batch
        val keyEntities = keys.map { keyName ->
            val key = Key()
            key.name = keyName
            key.project = project
            key
        }
        keyRepository.saveAll(keyEntities)
        
        // Create translations in batch
        val translations = mutableListOf<Translation>()
        for (key in keyEntities) {
            for (language in languageEntities) {
                val translation = Translation()
                translation.key = key
                translation.language = language
                translation.text = "Translation of ${key.name} in ${language.tag}"
                translations.add(translation)
            }
        }
        translationRepository.saveAll(translations)
        
        // Create content delivery config if needed
        val contentDeliveryConfig = if (withContentDelivery) {
            createUniqueContentDeliveryConfig(project)
        } else {
            null
        }
        
        return TestDataSet(
            project = project,
            languages = languageEntities,
            keys = keyEntities,
            translations = translations,
            contentDeliveryConfig = contentDeliveryConfig
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
        val contentDeliveryConfig: ContentDeliveryConfig? = null
    )
} 