package org.tabooproject.fluxon.parser;

import java.util.Collections;

/**
 * 支持任意参数数量的符号函数
 */
public class VarargsSymbolFunction extends SymbolFunction {

    public VarargsSymbolFunction(String namespace, String name) {
        super(namespace, name, Collections.emptyList());
    }

    @Override
    public boolean supportsParameterCount(int count) {
        return true;
    }

    @Override
    public int getMaxParameterCount() {
        return Integer.MAX_VALUE;
    }
}