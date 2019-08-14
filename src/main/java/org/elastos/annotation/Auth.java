package org.elastos.annotation;

import java.lang.annotation.*;

/**
 * Created by wanghan on
 * 需要鉴权的controller
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auth {
}
