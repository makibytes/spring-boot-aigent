package com.aigent;

import java.lang.annotation.*;

/**
 * Describes what a method does in plain language.
 * This is the north star for the AI implementation engine — read it first.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Intent {
    String value();
}
