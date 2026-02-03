package io.tolgee.development.testDataBuilder.builders

import io.tolgee.development.testDataBuilder.EntityDataBuilder
import kotlin.reflect.full.createType

abstract class BaseEntityDataBuilder<Entity, Builder> : EntityDataBuilder<Entity, Builder> {
  protected inline fun <reified AddedEntity, reified Builder : EntityDataBuilder<AddedEntity, *>> addOperation(
    collection: MutableCollection<Builder>,
    instance: Builder,
    ft: AddedEntity.() -> Unit,
  ): Builder {
    ft(instance.self)
    collection.add(instance)
    return instance
  }

  protected inline fun <reified AddedEntity, reified Builder : EntityDataBuilder<AddedEntity, *>> addOperation(
    collection: MutableCollection<Builder>,
    ft: AddedEntity.() -> Unit,
  ): Builder {
    val instance = Builder::class.constructors.find { it.parameters[0].type == this::class.createType() }!!.call(this)
    addOperation(collection, instance, ft)
    return instance
  }
}
