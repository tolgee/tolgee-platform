package io.tolgee.repository

import io.tolgee.model.UserPreferences
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Lazy
interface UserPreferencesRepository : JpaRepository<UserPreferences, Long> {
  @Query(
    nativeQuery = true,
    value = """
      select cast(project_storage_json -> cast(:projectId as text) -> cast(:fieldName as text) as text)
      from user_preferences
      where user_account_id = :userAccountId
    """,
  )
  fun findProjectStorageFieldJson(
    userAccountId: Long,
    projectId: Long,
    fieldName: String,
  ): String?

  @Transactional
  @Modifying(flushAutomatically = false)
  @Query(
    nativeQuery = true,
    value = """
      update user_preferences
      set project_storage_json = jsonb_set(
        coalesce(project_storage_json, jsonb_build_object()),
        array[cast(:projectId as text)],
        coalesce(project_storage_json -> cast(:projectId as text), jsonb_build_object())
          || jsonb_build_object(cast(:fieldName as text), cast(:valueJson as jsonb))
      )
      where user_account_id = :userAccountId
        and (
          jsonb_exists(coalesce(project_storage_json -> cast(:projectId as text), jsonb_build_object()), :fieldName)
          or (
            select count(*)
            from jsonb_object_keys(coalesce(project_storage_json -> cast(:projectId as text), jsonb_build_object()))
          ) < :maxFields
        )
    """,
  )
  fun setProjectStorageFieldIfBelowFieldLimit(
    userAccountId: Long,
    projectId: Long,
    fieldName: String,
    valueJson: String,
    maxFields: Int,
  ): Int

  @Transactional
  @Modifying(flushAutomatically = false)
  @Query(
    nativeQuery = true,
    value = """
      update user_preferences
      set project_storage_json = project_storage_json #- array[cast(:projectId as text), cast(:fieldName as text)]
      where user_account_id = :userAccountId
        and project_storage_json is not null
    """,
  )
  fun removeProjectStorageField(
    userAccountId: Long,
    projectId: Long,
    fieldName: String,
  ): Int

  @Modifying(flushAutomatically = false)
  @Query(
    nativeQuery = true,
    value = """
      update user_preferences
      set project_storage_json = project_storage_json - cast(:projectId as text)
      where jsonb_exists(project_storage_json, cast(:projectId as text))
    """,
  )
  fun removeProjectStorage(projectId: Long): Int
}
