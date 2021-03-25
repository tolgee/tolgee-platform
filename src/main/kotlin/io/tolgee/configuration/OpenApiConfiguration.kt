package io.tolgee.configuration

import io.swagger.v3.oas.models.Operation
import io.tolgee.openapi_fixtures.OpenApiApiKeyPaths
import io.tolgee.security.api_key_auth.AccessWithApiKey
import org.reflections.Reflections
import org.springdoc.core.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod

@Configuration
open class OpenApiConfiguration {
    @Bean
    open fun internalOpenApi(): GroupedOpenApi? {
        val paths = arrayOf("/api/repository/**")
        val reflections = Reflections("io.tolgee.controllers")
        val apiPaths = reflections.getTypesAnnotatedWith(OpenApiApiKeyPaths::class.java)
                .flatMap { it.getAnnotation(OpenApiApiKeyPaths::class.java).value.toList() }

        return GroupedOpenApi.builder().group("internal")
                .pathsToMatch(*paths)
                .pathsToExclude(*apiPaths.toTypedArray())
                .build()
    }

    @Bean
    open fun apiKeyOpenApi(): GroupedOpenApi? {
        return GroupedOpenApi.builder().group("With API key")
                .pathsToMatch(*apiKeyPaths.toTypedArray())
                .addOpenApiCustomiser { it.paths }
                .addOperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
                    if (handlerMethod.hasMethodAnnotation(AccessWithApiKey::class.java)) {

                    }
                    operation
                }.build()
    }

    private val apiKeyPaths: List<String>
        get() {
            val reflections = Reflections("io.tolgee.controllers")
            return reflections.getTypesAnnotatedWith(OpenApiApiKeyPaths::class.java)
                    .flatMap { it.getAnnotation(OpenApiApiKeyPaths::class.java).value.toList() }
        }
}
