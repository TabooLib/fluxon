package org.tabooproject.fluxon.parser.error;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.parser.SourceExcerpt;

/**
 * 没有找到函数异常
 */
public class FunctionNotFoundException extends ParseException {

    private final String name;

    public FunctionNotFoundException(String name, Token token, SourceExcerpt excerpt) {
        super(name, token, excerpt);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
