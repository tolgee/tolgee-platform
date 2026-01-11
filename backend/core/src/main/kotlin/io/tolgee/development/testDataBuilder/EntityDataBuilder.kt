package io.tolgee.development.testDataBuilder

interface EntityDataBuilder<T, out BuilderType> {
  val self: T

  fun self(ft: T.() -> Unit): T {
    ft(self)
    return self
  }

  fun build(ft: BuilderType.() -> Unit): BuilderType {
    @Suppress("UNCHECKED_CAST")
    ft(this as BuilderType)
    return this
  }
}
