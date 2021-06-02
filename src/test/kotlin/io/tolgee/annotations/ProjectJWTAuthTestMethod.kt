package io.tolgee.annotations

import io.tolgee.constants.ApiScope
import org.testng.annotations.Test

@Test
annotation class ProjectJWTAuthTestMethod(
        val scopes: Array<ApiScope> = [ApiScope.TRANSLATIONS_EDIT, ApiScope.KEYS_EDIT, ApiScope.TRANSLATIONS_VIEW]
)
