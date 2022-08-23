package io.tolgee.security.apiKeyAuth

import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.Explode
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.enums.ParameterStyle
import io.swagger.v3.oas.annotations.media.Schema
import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.model.enums.ApiScope

@Parameter(
  `in` = ParameterIn.QUERY,
  name = "ak",
  style = ParameterStyle.FORM,
  schema = Schema(type = "string"),
  explode = Explode.TRUE,
  example = "tgpak_gm2dcxzynjvdqm3fozwwgmdjmvwdgojqonvxamldnu4hi5lp",
  description = "API key provided via query parameter. Will be deprecated in the future."
)
@Parameter(
  `in` = ParameterIn.HEADER,
  name = API_KEY_HEADER_NAME,
  style = ParameterStyle.FORM,
  schema = Schema(type = "string"),
  explode = Explode.TRUE,
  example = "tgpak_gm2dcxzynjvdqm3fozwwgmdjmvwdgojqonvxamldnu4hi5lp",
  description = "API key provided via header. Safer since headers are not stored in server logs."
)
annotation class AccessWithApiKey(val scopes: Array<ApiScope> = [])
