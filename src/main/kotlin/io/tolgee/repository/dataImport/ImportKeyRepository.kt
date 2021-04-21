package io.tolgee.repository.dataImport

import io.tolgee.model.dataImport.ImportKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ImportKeyRepository : JpaRepository<ImportKey, Long> {
    @Query("select distinct ik from ImportKey ik join ik.files if join if.import im where im.id = :importId")
    fun findAllByImport(importId: Long): List<ImportKey>
}
