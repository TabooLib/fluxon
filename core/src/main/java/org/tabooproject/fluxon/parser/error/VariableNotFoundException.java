package org.tabooproject.fluxon.parser.error;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.ParseException;
import org.tabooproject.fluxon.parser.SourceExcerpt;

import java.util.List;

/**
 * 没有找到变量异常
 */
public class VariableNotFoundException extends ParseException {

    private final List<String> vars;

    public VariableNotFoundException(String message, List<String> vars, Token token, SourceExcerpt excerpt) {
        super(message + ", LocalVars: " + vars, token, excerpt);
        this.vars = vars;
    }

    /**
     * 获取当前可用变量
     */
    public List<String> getVars() {
        return vars;
    }
}
