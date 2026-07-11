package io.tolgee.repository

import io.tolgee.model.UserPreferences
import org.springframework.context.annotation.Lazy
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
@Lazy
interface UserPreferencesRepository : JpaRepository<UserPreferences, Long>
