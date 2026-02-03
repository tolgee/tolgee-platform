package io.tolgee.core.concepts.architecture

import io.tolgee.core.concepts.security.AuthorizationWitness
import io.tolgee.core.concepts.types.InputMarker
import io.tolgee.core.concepts.types.OutputMarker
import org.springframework.stereotype.Service

/**
 * A toplevel service that directly corresponds to an operation performed by some user. These services should serve as
 * entrypoints to anything that happens in the application and orchestrate other services, possibly including other
 * scenarios, to reach a certain goal. As such, they should take care of delimiting transactions and defining
 * security policies.
 *
 * A scenario accepts an [InputMarker] [I] and returns an [OutputMarker] [O] conditional on the existence of an
 * [AuthorizationWitness] [A].
 */
fun interface Scenario<I : InputMarker, O : OutputMarker, A : AuthorizationWitness> {
  fun A.execute(input: I): O
}

/**
 * A semantic specialization of [Service].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Service
annotation class ScenarioService
