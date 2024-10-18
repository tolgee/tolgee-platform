package io.tolgee.service.dataImport

import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.stereotype.Service

@Service
class ImportDeleteService(
  private val entityManager: EntityManager,
) {
  fun deleteImport(importId: Long) {
    entityManager.unwrap(Session::class.java).doWork { connection ->
      deleteImportTranslations(connection, importId)
      deleteImportLanguages(connection, importId)
      deleteImportKeyMetaTags(connection, importId)
      deleteImportKeyMetaComments(connection, importId)
      deleteImportKeyMetaCodeReferences(connection, importId)
      deleteImportKeyMeta(connection, importId)
      deleteImportKeys(connection, importId)
      deleteImportFileIssueParams(connection, importId)
      deleteImportFileIssues(connection, importId)
      deleteImportFiles(connection, importId)
      deleteTheImport(connection, importId)
    }
  }

  fun executeUpdate(
    connection: java.sql.Connection,
    query: String,
    importId: Long,
  ) {
    @Suppress("SqlSourceToSinkFlow")
    connection.prepareStatement(query).use { statement ->
      statement.setLong(1, importId)
      statement.executeUpdate()
    }
  }

  fun deleteImportTranslations(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_translation it " +
        "using import_language il, import_file if " +
        "where it.language_id = il.id and il.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportLanguages(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_language il " +
        "using import_file if " +
        "where il.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeys(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_key ik " +
        "using import_file if " +
        "where ik.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMetaTags(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_meta_tags kmt " +
        "using key_meta km, import_key ik, import_file if " +
        "where kmt.key_metas_id = km.id and km.import_key_id = ik.id " +
        "and ik.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMetaComments(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_comment kc " +
        "using key_meta km, import_key ik, import_file if " +
        "where kc.key_meta_id = km.id and km.import_key_id = ik.id " +
        "and ik.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMetaCodeReferences(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_code_reference kcr " +
        "using key_meta km, import_key ik, import_file if " +
        "where kcr.key_meta_id = km.id and km.import_key_id = ik.id " +
        "and ik.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportKeyMeta(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from key_meta km " +
        "using import_key ik, import_file if " +
        "where km.import_key_id = ik.id and ik.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportFileIssueParams(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_file_issue_param ifip " +
        "using import_file_issue ifi, import_file if " +
        "where ifip.issue_id = ifi.id and ifi.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportFileIssues(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_file_issue ifi " +
        "using import_file if " +
        "where ifi.file_id = if.id and if.import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteImportFiles(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query =
      "delete from import_file " +
        "where import_id = ?"
    executeUpdate(connection, query, importId)
  }

  fun deleteTheImport(
    connection: java.sql.Connection,
    importId: Long,
  ) {
    val query = "delete from import where id = ?"
    executeUpdate(connection, query, importId)
  }
}
