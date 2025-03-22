import java.util.concurrent.atomic.AtomicLong

class ProjectBuilder {
    // Add this field to generate unique identifiers
    private val uniqueId = AtomicLong(System.currentTimeMillis())
    
    /**
     * Generates a unique string identifier for test data.
     */
    private fun getUniqueString(prefix: String = ""): String {
        return "${prefix}_${uniqueId.incrementAndGet()}"
    }
    
    /**
     * Sets a unique name for the project to avoid conflicts between tests.
     */
    fun withUniqueData(): ProjectBuilder {
        this.name = getUniqueString("project")
        this.slug = getUniqueString("slug")
        return this
    }
    
    /**
     * Optimized method to build a project with minimal database operations.
     */
    fun buildAndPersistOptimized(): Project {
        // Use unique name if not already set
        if (name == null) {
            name = getUniqueString("project")
        }
        
        // Use unique slug if not already set
        if (slug == null) {
            slug = getUniqueString("slug")
        }
        
        // Create and save project
        val project = Project()
        project.name = name!!
        project.slug = slug
        // Set other properties
        
        return projectRepository.save(project)
    }
} 