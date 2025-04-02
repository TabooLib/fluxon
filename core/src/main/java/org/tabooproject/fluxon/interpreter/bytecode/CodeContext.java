package org.tabooproject.fluxon.interpreter.bytecode;

public class CodeContext {

    private final String className;
    private final String superClassName;

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
}
