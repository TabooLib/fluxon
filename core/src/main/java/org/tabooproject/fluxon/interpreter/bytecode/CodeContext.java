package org.tabooproject.fluxon.interpreter.bytecode;

import org.tabooproject.fluxon.runtime.Type;

public class CodeContext {

    // 类名和父类名
    private final String className;
    private final String superClassName;

    // 局部变量表
    private int localVarIndex = 0;

    public CodeContext(String className, String superClassName) {
        this.className = className;
        this.superClassName = superClassName;
    }

    public String getClassName() {
        return className;
    }

    public String getSuperClassName() {
        return superClassName;
    }

    public int allocateLocalVar(Type type) {
        String descriptor = type.getDescriptor();
        // 根据类型增加索引
        if ("J".equals(descriptor) || "D".equals(descriptor)) {
            localVarIndex += 2;
        } else {
            localVarIndex += 1;
        }
        return localVarIndex;
    }

    public int getLocalVarIndex() {
        return localVarIndex;
    }
}
