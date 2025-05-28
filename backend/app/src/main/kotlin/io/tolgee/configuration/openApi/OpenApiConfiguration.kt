package io.tolgee.configuration.openApi

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.tolgee.configuration.openApi.OpenApiGroupBuilder.Companion.PROJECT_ID_PARAMETER
import io.tolgee.openApiDocs.OpenApiHideFromPublicDocs
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfiguration {
  @Bean
  fun openAPI(): OpenAPI? {
    return OpenAPI()
      .info(
        Info().title("Tolgee API")
          .description("Tolgee Platform REST API reference")
          .version("v1.0"),
      )
      .externalDocs(
        ExternalDocumentation()
          .description("Tolgee documentation")
          .url("https://tolgee.io"),
      )
  }

  @Bean
  fun internalV1OpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = arrayOf("/api/**"),
      excludedPaths = BILLING + arrayOf(API_REPOSITORY),
      name = "V1 Internal - for Tolgee Web application",
    )
  }

  @Bean
  fun internalV2OpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = arrayOf("/v2/**"),
      excludedPaths = BILLING + arrayOf(API_REPOSITORY),
      name = "V2 Internal - for Tolgee Web application",
    )
  }

  @Bean
  fun internalAllOpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = arrayOf("/v2/**", "/api/**"),
      excludedPaths = BILLING + arrayOf(API_REPOSITORY),
      name = "All Internal - for Tolgee Web application",
    )
  }

  @Bean
  fun apiKeyAllOpenApi(): GroupedOpenApi? {
    return pakGroupForPaths(
      paths = arrayOf("/api/**", "/v2/**"),
      excludedPaths = BILLING + arrayOf(API_REPOSITORY),
      name = "Accessible with Project API key (All)",
    )
  }

  @Bean
  fun apiKeyV1OpenApi(): GroupedOpenApi? {
    return pakGroupForPaths(
      paths = arrayOf("/api/**"),
      excludedPaths = BILLING + arrayOf(API_REPOSITORY),
      name = "Accessible with Project API key (V1)",
    )
  }

  @Bean
  fun apiKeyV2OpenApi(): GroupedOpenApi? {
    return pakGroupForPaths(
      paths = arrayOf("/v2/**"),
      excludedPaths = BILLING + arrayOf(API_REPOSITORY),
      name = "Accessible with Project API key (V2)",
    )
  }

  @Bean
  fun allPublicApi(): GroupedOpenApi? {
    return publicApiGroupForPaths(
      paths = arrayOf("/v2/**", "/api/**"),
      excludedPaths = arrayOf(API_REPOSITORY),
      name = "Public API (All)",
    )
  }

  @Bean
  fun billingOpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = BILLING,
      excludedPaths = arrayOf("/v2/public/billing/webhook"),
      name = "V2 Billing",
    )
  }

  private fun internalGroupForPaths(
    paths: Array<String>,
    excludedPaths: Array<String> = arrayOf(),
    name: String,
  ): GroupedOpenApi {
    return OpenApiGroupBuilder(name) {
      builder.pathsToExclude(*excludedPaths, "/api/project/{$PROJECT_ID_PARAMETER}/sources/**")
      builder.pathsToMatch(*paths)
      customizeOperations { operation, _, path ->
        val isParameterConsumed =
          operation.isProjectIdConsumed()
        val pathContainsProjectId = path.contains("{$PROJECT_ID_PARAMETER}")
        val parameterIsMissingAtAll = !pathContainsProjectId && !isParameterConsumed
        val otherMethodPathContainsProjectId =
          handlerPaths[
            operationHandlers[operation.operationId]
              ?.method,
          ]?.any { it.contains("{projectId}") }
            ?: false
        // If controller method has another method which request mapping path contains {projectId},
        // this operation is then considered as one for access for API key and removed from
        // internal operations
        val isApiKeyOperation = !pathContainsProjectId && otherMethodPathContainsProjectId

        if (!isParameterConsumed && pathContainsProjectId) {
          operation.addProjectIdPathParam()
        }

        if ((pathContainsProjectId || parameterIsMissingAtAll) && !isApiKeyOperation) {
          return@customizeOperations operation
        }

        return@customizeOperations null
      }
    }.result
  }

  private fun Operation.isProjectIdConsumed() =
    parameters?.any {
      it.name == PROJECT_ID_PARAMETER && it.`in` == "path"
    } == true

  private fun Operation.addProjectIdPathParam() {
    val param =
      Parameter().apply {
        name(PROJECT_ID_PARAMETER)
        `in` = "path"
        required = true
        allowEmptyValue = false
        schema = IntegerSchema().apply { format = "int64" }
      }
    parameters?.apply {
      add(param)
    } ?: this@OpenApiConfiguration.let {
      parameters = mutableListOf(param)
    }
  }

  fun pakGroupForPaths(
    paths: Array<String>,
    excludedPaths: Array<String>,
    name: String,
  ): GroupedOpenApi? {
    return OpenApiGroupBuilder(name) {
      builder.pathsToExclude(*excludedPaths)
        .pathsToMatch(*paths)
      customizeOperations { operation, handler, path ->
        if (isApiAccessAllowed(handler)) {
          if (!path.matches(PATH_WITH_PROJECT_ID_REGEX)) {
            if (!containsProjectIdParam(path)) {
              operation.parameters?.removeIf { it.name == PROJECT_ID_PARAMETER && it.`in` != "query" }
            }
            return@customizeOperations operation
          }
        }
        return@customizeOperations null
      }
    }.result
  }

  fun publicApiGroupForPaths(
    paths: Array<String>,
    excludedPaths: Array<String>,
    name: String,
  ): GroupedOpenApi? {
    return OpenApiGroupBuilder(name) {
      builder.pathsToExclude(*excludedPaths)
        .pathsToMatch(*paths)
      customizeOperations { operation, handler, path ->
        val isProjectPath = isProjectPath(path)
        val containsProjectIdParam = containsProjectIdParam(path)
        if (isProjectPath && !containsProjectIdParam) {
          return@customizeOperations null
        }

        if (!operation.isProjectIdConsumed() && isProjectPath && containsProjectIdParam) {
          operation.addProjectIdPathParam()
        }

        if (handler.isHidden(path)) {
          return@customizeOperations null
        }

        return@customizeOperations operation
      }
    }.result
  }

  fun HandlerMethod.isHidden(path: String): Boolean {
    val annotation = getMethodHideAnnotation() ?: getClassHideAnnotation() ?: return false

    return path in annotation.paths || annotation.paths.isEmpty()
  }

  private fun HandlerMethod.getClassHideAnnotation(): OpenApiHideFromPublicDocs? =
    method.declaringClass.getAnnotation(
      OpenApiHideFromPublicDocs::class.java,
    )

  private fun HandlerMethod.getMethodHideAnnotation(): OpenApiHideFromPublicDocs? =
    method.getAnnotation(OpenApiHideFromPublicDocs::class.java)

  companion object {
    private const val API_REPOSITORY = "/api/repository/**"
    private const val BILLING_MAIN = "/v2/**/billing/**"
    private const val BILLING_LICENSING = "/v2/**/licensing/**"
    private const val BILLING_TELEMETRY = "/v2/**/telemetry/**"
    private const val BILLING_TRANSLATOR = "/v2/**/translator/**"
    private val BILLING =
      arrayOf(
        BILLING_MAIN,
        BILLING_LICENSING,
        BILLING_TELEMETRY,
        BILLING_TRANSLATOR,
      )
    private val PATH_WITH_PROJECT_ID_REGEX = "^/(?:api|v2)/projects?/\\{$PROJECT_ID_PARAMETER}.*".toRegex()
  }
}
