package org.tabooproject.fluxon.parser;

/**
 * 支持任意参数数量的符号函数
 */
public class VarargsSymbolFunction extends SymbolFunction {

    public VarargsSymbolFunction(String namespace, String name) {
        super(namespace, name, -1);
    }

    @Override
    public int getParameterCount() {
        return -1; // 表示任意参数数量
    }
}
