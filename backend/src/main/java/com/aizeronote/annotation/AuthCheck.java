package com.aizeronote.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * Required role value (see {@link com.aizeronote.model.enums.UserRoleEnum}). Empty means any logged-in user.
     */
    String mustRole() default "";
}
