package io.tolgee.core.concepts.architecture

/**
 * This annotation marks classes which act as resources in the
 * [REST sense of the word](https://restful-api-design.readthedocs.io/en/latest/resources.html).
 *
 * This class stands between the implementation of the (e.g., API) connector and our core domain and is
 * responsible for translating business concepts defined in our domain (be it operations or objects) to
 * specific calls on the remote service (or services). Therefore, the inputs and outputs of these objects
 * should only be data defined in the domain.
 *
 * For example, say we create an invoice in billing, and say that business rules dictate that when an
 * invoice is created by us, it must also be created in system A. Furthermore, assume that the process
 * of creation in system A actually entails calling two different endpoints, the first one for, e.g.,
 * authentication, and the second that actually creates the invoice. In this scenario, we have a single
 * resource, `Invoice`, which has a `create` method. This method accepts our domain representation of
 * an invoice and is internally responsible for calling both endpoints. Similarly, things like pagination,
 * throttling, etc. are all handled by the [ExternalResource] instance in a manner that is transparent to
 * its clients, as is mapping to and from domain representations (this will, of course, very likely be
 * internally delegated to a different component, but the important thing is that it is the responsibility
 * of the [ExternalResource], not its client).
 *
 * Keep in mind that this annotation is completely generic, and *not* only tied to REST or API calls.
 * If you have some sort of "thing" that is represented as a file on disk, it should be modeled by an
 * [ExternalResource]. When you get down to it, an [ExternalResource] is basically exactly like a Spring
 * `@Repository` in the DDD sense. However, in contrast to the term "repository", which is an amorphous
 * concept, we feel that phrasing the vocabulary in terms of a "resource" gives more emphasis on the "thing",
 * instead of the process by which it is fetched, and keeping REST in mind guides us how to design and name
 * the methods it exposes.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ExternalResource
