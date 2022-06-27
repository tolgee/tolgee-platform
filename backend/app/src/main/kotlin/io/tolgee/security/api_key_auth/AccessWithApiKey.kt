package io.tolgee.security.api_key_auth

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.model.enums.ApiScope

@Parameter(
  `in` = ParameterIn.QUERY,
  name = "ak",
  style = ParameterStyle.FORM,
  schema = Schema(type = "string"),
  explode = Explode.TRUE,
  example = "90fqj8f97bsjk0fbrlhkluevge",
  description = "API key provided via query parameter. Will be deprecated in the future."
)
@Parameter(
  `in` = ParameterIn.HEADER,
  name = "X-API-Key",
  style = ParameterStyle.FORM,
  schema = Schema(type = "string"),
  explode = Explode.TRUE,
  example = "90fqj8f97bsjk0fbrlhkluevge",
  description = "API key provided via header. Safer since headers are not stored in server logs."
)
annotation class AccessWithApiKey(val scopes: Array<ApiScope> = [])
