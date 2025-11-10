package io.tolgee.configuration.openApi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.tolgee.API_KEY_HEADER_NAME
import io.tolgee.security.authentication.AllowApiAccess
import io.tolgee.security.authentication.RequiresSuperAuthentication
import io.tolgee.security.authorization.IsGlobalRoute
import org.springframework.web.method.HandlerMethod

class OpenApiSecurityHelper(
  private val groupBuilder: OpenApiGroupBuilder,
) {
  fun handleSecurity() {
    addSecurityToOperations()
    addSecuritySchemas()
  }

  private fun addSecurityToOperations() {
    groupBuilder.customizeOperations { operation, handlerMethod, path, _ ->
      if (path.matches(OpenApiGroupBuilder.PUBLIC_ENDPOINT_REGEX)) {
        return@customizeOperations operation
      }

      if (handlerMethod.hasMethodAnnotation(AllowApiAccess::class.java)) {
        operation.addHeaderApiKeySecurity()
        operation.addQueryParamApiKeySecurity()
      }

      addJwtAuth(handlerMethod, operation, path)

      return@customizeOperations operation
    }
  }

  private fun addJwtAuth(
    handlerMethod: HandlerMethod,
    operation: Operation,
    path: String,
  ) {
    val pakOnly =
      groupBuilder.isProjectPath(path) &&
        !groupBuilder.containsProjectIdParam(path) &&
        !handlerMethod.hasMethodAnnotation(
          IsGlobalRoute::class.java,
        )

    if (pakOnly) {
      wasPakOnly = true
      return
    }
    if (handlerMethod.hasMethodAnnotation(RequiresSuperAuthentication::class.java)) {
      operation.addSuperJwtTokenSecurity()
      return
    }

    operation.addJwtTokenSecurity()
  }

  private var wasPakOnly = false

  private fun addSecuritySchemas() {
    groupBuilder.builder.addOpenApiCustomizer { it ->
      val schemas = it.getSecuritySchemas()
      if (usedSecuritySchemas.contains(JWT_TOKEN)) {
        schemas[JWT_TOKEN] =
          SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
      }
      if (usedSecuritySchemas.contains(SUPER_JWT_TOKEN)) {
        schemas[SUPER_JWT_TOKEN] =
          SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .description(
              "Super JWT token is required for sensitive operations when user has set 2FA. You can obtain " +
                "super JWT token via `/v2/user/generate-super-token` endpoint. But be careful! Super JWT Token " +
                "has super powers!",
            )
      }
      if (usedSecuritySchemas.contains(API_KEY_IN_HEADER)) {
        schemas[API_KEY_IN_HEADER] =
          SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.HEADER)
            .description(getPakOnlyDescription())
            .name(API_KEY_HEADER_NAME)
      }
      if (usedSecuritySchemas.contains(API_KEY_IN_QUERY_PARAM)) {
        val pakOnlyDescription = getPakOnlyDescription()?.let { " $it" } ?: ""
        schemas[API_KEY_IN_QUERY_PARAM] =
          SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .`in`(SecurityScheme.In.QUERY)
            .description(
              "It's not recommended to use API key in query param, " +
                "since it can be stored in logs.$pakOnlyDescription",
            ).name("ak")
      }
    }
  }

  private fun getPakOnlyDescription(): String? {
    if (wasPakOnly) {
      return "Project API key is required for endpoints accessing specific " +
        "project without {projectId} specified in path. " +
        "Project API key contains encoded project ID. " +
        "For other endpoints, you can use Personal Access Token as well."
    }
    return null
  }

  private fun Operation.addJwtTokenSecurity() {
    this.addSecurityRequirement(jwtSecurityRequirement)
  }

  private fun Operation.addSuperJwtTokenSecurity() {
    this.addSecurityRequirement(superJwtSecurityRequirement)
  }

  private fun Operation.addHeaderApiKeySecurity() {
    this.addSecurityRequirement(apiKeyHeaderSecurityRequirement)
  }

  private fun Operation.addQueryParamApiKeySecurity() {
    this.addSecurityRequirement(apiKeyQueryParamSecurityRequirement)
  }

  private val usedSecuritySchemas = mutableListOf<String>()

  private val jwtSecurityRequirement by lazy {
    usedSecuritySchemas.add(JWT_TOKEN)
    SecurityRequirement().addList(JWT_TOKEN)
  }

  private val superJwtSecurityRequirement by lazy {
    usedSecuritySchemas.add(SUPER_JWT_TOKEN)
    SecurityRequirement().addList(SUPER_JWT_TOKEN)
  }

  private val apiKeyHeaderSecurityRequirement by lazy {
    usedSecuritySchemas.add(API_KEY_IN_HEADER)
    SecurityRequirement().addList(API_KEY_IN_HEADER)
  }

  private val apiKeyQueryParamSecurityRequirement by lazy {
    usedSecuritySchemas.add(API_KEY_IN_QUERY_PARAM)
    SecurityRequirement().addList(API_KEY_IN_QUERY_PARAM)
  }

  private fun Operation.addSecurityRequirement(securityRequirement: SecurityRequirement) {
    this.getOrInitSecurity().add(securityRequirement)
  }

  private fun OpenAPI.getSecuritySchemas(): MutableMap<String, SecurityScheme> {
    this.components.securitySchemes = this.components.securitySchemes ?: mutableMapOf()
    return this.components.securitySchemes
  }

  private fun Operation.getOrInitSecurity(): MutableList<SecurityRequirement> {
    this.security = this.security ?: mutableListOf()
    return this.security
  }

  companion object {
    const val API_KEY_IN_HEADER = "ApiKeyInHeader"
    const val JWT_TOKEN = "JwtToken"
    const val SUPER_JWT_TOKEN = "SuperJwtToken"
    const val API_KEY_IN_QUERY_PARAM = "ApiKeyInQueryParam"
  }
}
