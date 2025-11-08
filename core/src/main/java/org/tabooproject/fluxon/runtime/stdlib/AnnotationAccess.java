package org.tabooproject.fluxon.runtime.stdlib;

import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.runtime.Function;

public class AnnotationAccess {

    /**
     * 检查函数是否有指定注解
     *
     * @param function 函数
     * @param name     注解名
     * @return 是否有指定注解
     */
    public static boolean hasAnnotation(Function function, String name) {
        for (Annotation annotation : function.getAnnotations()) {
            if (annotation.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
