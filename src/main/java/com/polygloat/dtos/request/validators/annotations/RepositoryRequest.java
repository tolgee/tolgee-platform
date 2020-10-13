package com.polygloat.dtos.request.validators.annotations;

import com.polygloat.dtos.request.validators.RepositoryValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = RepositoryValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RepositoryRequest {
    String message() default "REPOSITORY_DEFAULT_ERROR";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

