package io.tolgee.configuration

import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.tolgee.security.api_key_auth.AccessWithApiKey
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod
import java.lang.reflect.Method

@Configuration
class OpenApiConfiguration {

  companion object {
    private const val PROJECT_ID_PARAMETER = "projectId"
  }

  @Bean
  fun openAPI(): OpenAPI? {
    return OpenAPI()
      .info(
        Info().title("Tolgee API ")
          .description("Tolgee Server API reference")
          .version("v1.0")
      )
      .externalDocs(
        ExternalDocumentation()
          .description("Tolgee documentation")
          .url("https://toolkit.tolgee.io")
      )
  }

  @Bean
  fun internalV1OpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = arrayOf("/api/**"),
      excludedPaths = arrayOf("!/v2/billing/**"),
      name = "V1 Internal - for Tolgee Web application"
    )
  }

  @Bean
  fun internalV2OpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = arrayOf("/v2/**"),
      excludedPaths = arrayOf("!/v2/billing/**"),
      name = "V2 Internal - for Tolgee Web application"
    )
  }

  @Bean
  fun internalAllOpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = arrayOf("/v2/**", "/api/**"),
      excludedPaths = arrayOf("!/v2/billing/**"),
      name = "All Internal - for Tolgee Web application"
    )
  }

  @Bean
  fun apiKeyAllOpenApi(): GroupedOpenApi? {
    return apiKeyGroupForPaths(
      paths = arrayOf("/api/**", "/v2/**"),
      excludedPaths = arrayOf("!/v2/billing/**"),
      name = "Accessible with API key"
    )
  }

  @Bean
  fun apiKeyV1OpenApi(): GroupedOpenApi? {
    return apiKeyGroupForPaths(
      paths = arrayOf("/api/**"),
      excludedPaths = arrayOf("!/v2/billing/**"),
      name = "V1 Accessible with API key"
    )
  }

  @Bean
  fun apiKeyV2OpenApi(): GroupedOpenApi? {
    return apiKeyGroupForPaths(
      paths = arrayOf("/v2/**"),
      excludedPaths = arrayOf("!/v2/billing/**"),
      name = "V2 Accessible with API key V2"
    )
  }

  @Bean
  fun billingOpenApi(): GroupedOpenApi? {
    return internalGroupForPaths(
      paths = arrayOf("/v2/billing/**"),
      name = "V2 Billing"
    )
  }

  private fun internalGroupForPaths(
    paths: Array<String>,
    excludedPaths: Array<String> = arrayOf(),
    name: String
  ): GroupedOpenApi? {
    val operationHandlers = HashMap<Operation, HandlerMethod>()
    val handlerPaths = HashMap<Method, MutableList<String>>()

    return GroupedOpenApi.builder().group(name)
      .addOperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
        operationHandlers[operation] = handlerMethod
        operation
      }
      .pathsToMatch(*paths).pathsToExclude(*excludedPaths)
      .addOpenApiCustomiser { openApi ->
        openApi.paths.forEach { (path, value) ->
          value.readOperations().forEach { operation ->
            operationHandlers[operation]?.method?.let { method ->
              handlerPaths[method] = handlerPaths[method].let {
                it?.run {
                  add(path)
                  this
                } ?: mutableListOf(path)
              }
            }
          }
        }
      }
      .addOpenApiCustomiser { openApi ->
        val newPaths = Paths()
        openApi.paths.forEach { pathEntry ->
          val operations = ArrayList<Operation>()
          val newPathItem = PathItem()
          val oldPathItem = pathEntry.value
          oldPathItem.readOperations().forEach { operation ->
            val isParameterConsumed = operation?.parameters?.any {
              it.name == PROJECT_ID_PARAMETER
            } == true
            val pathContainsProjectId = pathEntry.key.contains("{$PROJECT_ID_PARAMETER}")
            val parameterIsMissingAtAll = !pathContainsProjectId && !isParameterConsumed
            val otherMethodPathContainsProjectId = handlerPaths[
              operationHandlers[operation]
                ?.method
            ]?.any { it.contains("{projectId}") }
              ?: false
            // If controller method has another method which request mapping path contains {projectId},
            // this operation is then considered as one for access for API key and removed from
            // internal operations
            val isApiKeyOperation = !pathContainsProjectId && otherMethodPathContainsProjectId

            if ((pathContainsProjectId || parameterIsMissingAtAll) && !isApiKeyOperation) {
              operations.add(operation)
            }

            if (!isParameterConsumed && pathContainsProjectId) {
              val param = Parameter().apply {
                name(PROJECT_ID_PARAMETER)
                `in` = "path"
                required = true
                allowEmptyValue = false
                schema = IntegerSchema().apply { format = "int64" }
              }
              operation.parameters?.apply {
                add(param)
              } ?: let {
                operation.parameters = mutableListOf(param)
              }
            }

            operation.parameters?.removeIf { it.name == "ak" && it.`in` == "query" }
          }

          operations.forEach { operation ->
            newPathItem.operation(oldPathItem.getHttpMethod(operation), operation)
          }

          if (operations.isNotEmpty()) {
            newPaths.addPathItem(pathEntry.key, newPathItem)
          }
        }
        openApi.paths = newPaths
      }.handleLinks()
      .pathsToExclude("/api/project/{$PROJECT_ID_PARAMETER}/sources/**")
      .build()
  }

  fun apiKeyGroupForPaths(paths: Array<String>, excludedPaths: Array<String>, name: String): GroupedOpenApi? {
    val operationHandlers = HashMap<Operation, HandlerMethod>()

    return GroupedOpenApi.builder().group(name)
      .pathsToMatch(*paths)
      .pathsToExclude(*excludedPaths)
      .addOpenApiCustomiser { openApi ->
        val newPaths = Paths()
        openApi.paths.forEach { pathEntry ->
          val operations = ArrayList<Operation>()
          val newPathItem = PathItem()
          val oldPathItem = pathEntry.value
          oldPathItem.readOperations().forEach { operation ->
            val annotation = operationHandlers[operation]
              ?.getMethodAnnotation(AccessWithApiKey::class.java)

            if (annotation != null) {
              val containsProjectIdParam = pathEntry.key
                .contains("{$PROJECT_ID_PARAMETER}")
              if (!pathEntry.key.matches("^/(?:api|v2)/projects?/\\{$PROJECT_ID_PARAMETER}.*".toRegex())) {
                if (!containsProjectIdParam) {
                  operation.parameters.removeIf { it.name == PROJECT_ID_PARAMETER }
                }
                operations.add(operation)
              }
            }
          }

          operations.forEach { operation ->
            newPathItem.operation(oldPathItem.getHttpMethod(operation), operation)
          }

          if (operations.isNotEmpty()) {
            newPaths.addPathItem(pathEntry.key, newPathItem)
          }
        }
        openApi.paths = newPaths

        val usedTags = newPaths.flatMap { it.value.readOperations() }.flatMap { it.tags }
        openApi.tags.removeIf { !usedTags.contains(it.name) }
      }
      .addOperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
        operationHandlers[operation] = handlerMethod
        operation
      }.handleLinks().build()
  }

  private fun GroupedOpenApi.Builder.handleLinks(): GroupedOpenApi.Builder {
    this.addOpenApiCustomiser {
      it.components.schemas.values.forEach {
        it?.properties?.remove("_links")
      }
    }
    return this
  }

  private fun PathItem.getHttpMethod(operation: Operation): PathItem.HttpMethod? {
    return when (operation) {
      this.get -> PathItem.HttpMethod.GET
      this.delete -> PathItem.HttpMethod.DELETE
      this.head -> PathItem.HttpMethod.HEAD
      this.post -> PathItem.HttpMethod.POST
      this.patch -> PathItem.HttpMethod.PATCH
      this.put -> PathItem.HttpMethod.PUT
      this.options -> PathItem.HttpMethod.OPTIONS
      this.trace -> PathItem.HttpMethod.TRACE
      else -> null
    }
  }
}
