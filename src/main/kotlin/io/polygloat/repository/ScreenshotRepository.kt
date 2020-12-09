package io.polygloat.repository

import io.polygloat.model.Key
import io.polygloat.model.Screenshot
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ScreenshotRepository : JpaRepository<Screenshot, Long> {
    fun findAllByKey(key: Key, pageRequest: Pageable? = null): List<Screenshot>
}