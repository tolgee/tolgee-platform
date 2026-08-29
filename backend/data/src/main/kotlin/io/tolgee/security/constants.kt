package io.tolgee.security

const val PROJECT_API_KEY_PREFIX = "tgpak_"
const val PAT_PREFIX = "tgpat_"
const val BILLING_API_KEY_PREFIX = "tgbak_"

/** OAuth 2.1 access token issued by Tolgee's own authorization server, presented as `Authorization: Bearer`. */
const val OAUTH_ACCESS_TOKEN_PREFIX = "tgoat_"

/** OAuth 2.1 refresh token; never presented to the API, prefixed so secret scanners recognise a leak. */
const val OAUTH_REFRESH_TOKEN_PREFIX = "tgort_"
