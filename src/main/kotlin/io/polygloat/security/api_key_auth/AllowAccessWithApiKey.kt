package io.polygloat.security.api_key_auth

import io.polygloat.constants.ApiScope

annotation class AllowAccessWithApiKey(val scopes: Array<ApiScope> = [])