package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.ClassWriter;

/**
 * 自定义 ClassWriter，使用提供的 ClassLoader 来加载类
 */
public class FluxonClassWriter extends ClassWriter {
    private final ClassLoader classLoader;

    public FluxonClassWriter(int flags, ClassLoader classLoader) {
        super(flags);
        this.classLoader = classLoader != null ? classLoader : FluxonClassWriter.class.getClassLoader();
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            // 尝试使用默认实现
            return super.getCommonSuperClass(type1, type2);
        } catch (RuntimeException e) {
            // 如果失败，使用提供的 ClassLoader
            try {
                Class<?> c1 = loadClass(type1);
                Class<?> c2 = loadClass(type2);

                if (c1.isAssignableFrom(c2)) {
                    return type1;
                }
                if (c2.isAssignableFrom(c1)) {
                    return type2;
                }
                if (c1.isInterface() || c2.isInterface()) {
                    return "java/lang/Object";
                }

                // 查找公共父类
                Class<?> current = c1;
                do {
                    current = current.getSuperclass();
                    if (current == null) {
                        return "java/lang/Object";
                    }
                } while (!current.isAssignableFrom(c2));

                return current.getName().replace('.', '/');

            } catch (Exception ex) {
                // 如果还是失败，返回 Object
                return "java/lang/Object";
            }
        }
    }

    private Class<?> loadClass(String type) throws ClassNotFoundException {
        String className = type.replace('/', '.');
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            // 尝试线程上下文类加载器
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null && contextLoader != classLoader) {
                try {
                    return Class.forName(className, false, contextLoader);
                } catch (ClassNotFoundException ignored) {
                }
            }
            // 最后尝试系统类加载器
            return Class.forName(className);
        }
    }
}
