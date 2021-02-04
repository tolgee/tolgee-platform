package io.tolgee.security.api_key_auth

import io.tolgee.constants.ApiScope

annotation class AllowAccessWithApiKey(val scopes: Array<ApiScope> = [])