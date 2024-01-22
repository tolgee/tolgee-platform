package io.tolgee.controllers.internal

import io.swagger.v3.oas.annotations.Hidden
import io.tolgee.configuration.tolgee.E2eRuntimeMutable
import io.tolgee.configuration.tolgee.TolgeeProperties
import io.tolgee.dtos.request.SetPropertyDto
import io.tolgee.exceptions.BadRequestException
import io.tolgee.exceptions.NotFoundException
import jakarta.validation.Valid
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.hasAnnotation

@RestController
@CrossOrigin(origins = ["*"])
@Hidden
@RequestMapping(value = ["internal/properties"])
@Transactional
class PropertiesController(
  val tolgeeProperties: TolgeeProperties,
) {
  @PutMapping(value = ["/set"])
  @Transactional
  fun setProperty(
    @RequestBody @Valid
    setPropertyDto: SetPropertyDto,
  ) {
    val name = setPropertyDto.name
    var instance: Any = tolgeeProperties
    name.split(".").let { namePath ->
      namePath.forEachIndexed { idx, property ->
        val isLast = idx == namePath.size - 1
        val props = instance::class.declaredMemberProperties
        val prop = props.find { it.name == property } ?: throw NotFoundException()
        if (isLast) {
          (prop as? KMutableProperty1<Any, Any?>)?.let {
            if (!it.hasAnnotation<E2eRuntimeMutable>()) {
              io.tolgee.constants.Message.PROPERTY_NOT_MUTABLE
            }
            it.set(instance, setPropertyDto.value)
            return
          } ?: throw BadRequestException(io.tolgee.constants.Message.PROPERTY_NOT_MUTABLE)
        }
        instance = (prop as KProperty1<Any, Any?>).get(instance)!!
      }
    }
  }
}
