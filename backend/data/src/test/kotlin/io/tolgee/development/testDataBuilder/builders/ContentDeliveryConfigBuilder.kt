import java.util.concurrent.atomic.AtomicLong

class ContentDeliveryConfigBuilder {
    // Add this field to generate unique identifiers
    private val uniqueId = AtomicLong(System.currentTimeMillis())
    
    /**
     * Generates a unique string identifier for test data.
     */
    private fun getUniqueString(prefix: String = ""): String {
        return "${prefix}_${uniqueId.incrementAndGet()}"
    }
    
    /**
     * Sets unique data for the content delivery config to avoid conflicts.
     */
    fun withUniqueData(): ContentDeliveryConfigBuilder {
        this.name = getUniqueString("config")
        this.slug = getUniqueString("slug")
        return this
    }
    
    /**
     * Optimized method to build a content delivery config with minimal database operations.
     */
    fun buildAndPersistOptimized(): ContentDeliveryConfig {
        // Use unique name if not already set
        if (name == null) {
            name = getUniqueString("config")
        }
        
        // Use unique slug if not already set
        if (slug == null) {
            slug = getUniqueString("slug")
        }
        
        // Create and save content delivery config
        val config = ContentDeliveryConfig(project!!)
        config.name = name!!
        config.slug = slug!!
        // Set other properties
        
        return contentDeliveryConfigRepository.save(config)
    }
} 