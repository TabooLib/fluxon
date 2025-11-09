package org.tabooproject.fluxon.parser.error;

import org.jetbrains.annotations.Nullable;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.parser.SourceExcerpt;

/**
 * 没有找到函数异常
 */
public class FunctionNotFoundException extends ParseException {

    private final String name;
    private final Class<?> extensionClass;
    private final int index;

    public FunctionNotFoundException(String name, Token token) {
        this(name, token, null);
    }
    
    public FunctionNotFoundException(String name, Token token, SourceExcerpt excerpt) {
        super("Function not found: " + name, token, excerpt);
        this.name = name;
        this.extensionClass = null;
        this.index = -1;
    }

    public FunctionNotFoundException(String name, Class<?> extensionClass, int index, Token token) {
        this(name, extensionClass, index, token, null);
    }
    
    public FunctionNotFoundException(String name, Class<?> extensionClass, int index, Token token, SourceExcerpt excerpt) {
        super("Function not found: " + name + ", context: " + extensionClass + ", index: " + index, token, excerpt);
        this.name = name;
        this.extensionClass = extensionClass;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public Class<?> getExtensionClass() {
        return extensionClass;
    }

    public int getIndex() {
        return index;
    }
}
