package com.aigent;

import java.lang.annotation.*;

/** Container annotation for repeatable {@link Example}. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Examples {
    Example[] value();
}
