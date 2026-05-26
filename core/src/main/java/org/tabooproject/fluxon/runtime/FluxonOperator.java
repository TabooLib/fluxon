package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.lexer.TokenType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注静态方法为 Fluxon 运算符重载
 * 方法第一个参数为 target 类型，第二个参数为右操作数。
 *
 * @author sky
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FluxonOperator {

    /**
     * 绑定的 Fluxon 运算符
     */
    TokenType value();

    /**
     * 运算符左操作数类型
     */
    Class<?> target();

    /**
     * 运算完成后返回左操作数
     * 用于 Collection += value 这类原地修改 API，避免把 add() 的 boolean 写回变量。
     */
    boolean returnsTarget() default false;
}
