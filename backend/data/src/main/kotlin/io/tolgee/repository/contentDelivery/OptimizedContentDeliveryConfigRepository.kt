package io.tolgee.repository.contentDelivery

import io.tolgee.model.contentDelivery.ContentDeliveryConfig
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface OptimizedContentDeliveryConfigRepository : JpaRepository<ContentDeliveryConfig, Long> {
    
    /**
     * Optimized query to fetch ContentDeliveryConfig with minimal joins
     */
    @Query("""
        SELECT c FROM ContentDeliveryConfig c
        WHERE c.project.id = :projectId AND c.id = :contentDeliveryConfigId
    """)
    fun findByProjectIdAndIdOptimized(
        projectId: Long,
        contentDeliveryConfigId: Long
    ): ContentDeliveryConfig?
    
    /**
     * Optimized query to check if a ContentDeliveryConfig exists
     */
    @Query("""
        SELECT COUNT(c) > 0 FROM ContentDeliveryConfig c
        WHERE c.project.id = :projectId AND c.id = :contentDeliveryConfigId
    """)
    fun existsByProjectIdAndId(
        projectId: Long,
        contentDeliveryConfigId: Long
    ): Boolean
    
    /**
     * Optimized query to count ContentDeliveryConfigs for a project
     */
    @Query("""
        SELECT COUNT(c) FROM ContentDeliveryConfig c
        WHERE c.project.id = :projectId
    """)
    fun countByProjectId(projectId: Long): Int
    
    @Query("""
        SELECT c FROM ContentDeliveryConfig c
        LEFT JOIN FETCH c.project
        LEFT JOIN FETCH c.autoPublish
        LEFT JOIN FETCH c.contentStorage
        WHERE c.project.id = :projectId
    """)
    fun findAllByProjectIdOptimized(projectId: Long): List<ContentDeliveryConfig>
    
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM ContentDeliveryConfig c
        WHERE c.project.id = :projectId
    """)
    fun deleteAllByProjectId(projectId: Long): Int
    
    // Batch operations for better performance
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO content_delivery_config (project_id, name, slug, format, auto_publish_id)
        VALUES (:projectId, :name, :slug, :format, :autoPublishId)
    """, nativeQuery = true)
    fun batchInsert(projectId: Long, name: String, slug: String, format: String, autoPublishId: Long?): Int
} 