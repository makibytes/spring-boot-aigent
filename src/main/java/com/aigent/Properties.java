package com.aigent;

import java.lang.annotation.*;

/** Container annotation for repeatable {@link Property}. */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Properties {
    Property[] value();
}
