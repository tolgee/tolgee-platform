package io.tolgee.development.testDataBuilder

import kotlin.reflect.full.createType

abstract class BaseEntityDataBuilder<Entity> : EntityDataBuilder<Entity> {
  protected inline fun <reified Builder : EntityDataBuilder<*>> addOperation(
    collection: MutableCollection<Builder>,
    instance: Builder,
    ft: Builder.() -> Unit
  ): Builder {
    collection.add(instance)
    ft(instance)
    return instance
  }

  protected inline fun <reified Builder : EntityDataBuilder<*>> addOperation(
    collection: MutableCollection<Builder>,
    ft: Builder.() -> Unit
  ): Builder {
    val instance = Builder::class.constructors.find { it.parameters[0].type == this::class.createType() }!!.call(this)
    addOperation(collection, instance, ft)
    return instance
  }
}
