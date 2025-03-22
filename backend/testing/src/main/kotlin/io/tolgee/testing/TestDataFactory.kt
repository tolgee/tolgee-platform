package io.tolgee.testing

import io.tolgee.model.Language
import io.tolgee.model.Project
import io.tolgee.model.UserAccount
import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import io.tolgee.model.key.Key
import io.tolgee.model.translation.Translation
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * Factory for creating test data with unique identifiers to avoid conflicts between tests.
 */
class TestDataFactory {
    private val uniqueCounter = AtomicLong(System.currentTimeMillis())
    
    /**
     * Generate a unique ID
     */
    fun uniqueId(): Long = uniqueCounter.incrementAndGet()
    
    /**
     * Generate a unique string with optional prefix
     */
    fun uniqueString(prefix: String = ""): String = "$prefix-${UUID.randomUUID()}"
    
    /**
     * Create a project with unique name
     */
    fun createProject(name: String? = null): Project {
        return Project().apply {
            this.name = name ?: uniqueString("Project")
            this.organizationOwner = null
        }
    }
    
    /**
     * Create a user with unique username and email
     */
    fun createUser(username: String? = null, email: String? = null): UserAccount {
        val uniqueUsername = username ?: uniqueString("user")
        return UserAccount().apply {
            this.username = uniqueUsername
            this.name = uniqueUsername
            this.email = email ?: "$uniqueUsername@example.com"
        }
    }
    
    /**
     * Create a language with unique tag
     */
    fun createLanguage(tag: String? = null, name: String? = null): Language {
        val uniqueTag = tag ?: uniqueString("lang")
        return Language().apply {
            this.tag = uniqueTag
            this.name = name ?: uniqueTag.uppercase()
        }
    }
    
    /**
     * Create a key with unique name
     */
    fun createKey(name: String? = null, project: Project): Key {
        return Key().apply {
            this.name = name ?: uniqueString("key")
            this.project = project
        }
    }
    
    /**
     * Create a translation
     */
    fun createTranslation(text: String? = null, key: Key, language: Language): Translation {
        return Translation().apply {
            this.text = text ?: uniqueString("translation")
            this.key = key
            this.language = language
        }
    }
    
    /**
     * Create a content delivery config with unique name and slug
     */
    fun createContentDeliveryConfig(name: String? = null, project: Project): ContentDeliveryConfig {
        return ContentDeliveryConfig().apply {
            this.name = name ?: uniqueString("config")
            this.slug = uniqueString("slug")
            this.project = project
        }
    }
} 