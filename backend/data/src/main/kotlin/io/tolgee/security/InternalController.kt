package io.tolgee.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@ConditionalOnProperty("tolgee.internal.controller-enabled")
annotation class InternalController
