package org.tabooproject.fluxon.interpreter.bytecode;

/**
 * 用于加载动态生成的类的类加载器
 */
public class FluxonClassLoader extends ClassLoader {

    // 无参构造器，使用系统类加载器作为父加载器
    public FluxonClassLoader() {
        super();
    }
    
    public FluxonClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> defineClass(String name, byte[] bytecode) {
        return defineClass(name, bytecode, 0, bytecode.length);
    }
}