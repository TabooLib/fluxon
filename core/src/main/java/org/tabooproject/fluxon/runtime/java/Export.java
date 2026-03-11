package org.tabooproject.fluxon.runtime.java;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 导出一个 API 函数
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Export {

    // 是否为异步函数
    boolean async() default false;

    // 是否为同步函数
    boolean sync() default false;

    // 是否共享到其他 Fluxon 实例（跨 ClassLoader）
    boolean shared() default false;
}
