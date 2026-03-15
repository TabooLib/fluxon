package org.tabooproject.fluxon.runtime;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注静态方法为 Fluxon 系统函数或扩展函数
 * 扫描器自动注册为 NativeFunction（解释器用）+ DirectBinding（编译器直接 INVOKESTATIC）
 * 设置 target 后视为扩展函数，方法第一个参数为 target 类型
 *
 * @author sky
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FluxonFunction {

    /**
     * 系统函数标记（target 默认值）
     * 不可实例化，仅作为 sentinel 区分系统函数和扩展函数
     */
    final class SystemFunction {
        private SystemFunction() {
        }
    }

    /**
     * 函数名，默认使用方法名
     */
    String value() default "";

    /**
     * 命名空间
     */
    String namespace() default "";

    /**
     * 扩展函数的目标类型
     * 默认 SystemFunction.class 表示系统函数；设置为具体类型（包括 Object.class）表示扩展函数
     */
    Class<?> target() default SystemFunction.class;
}
