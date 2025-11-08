package org.tabooproject.fluxon.parser;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;

/**
 * 待解析的函数调用
 * 用于在解析阶段收集所有函数调用，在解析完成后统一解析
 */
public class PendingFunctionCall {
    
    private final FunctionCallExpression expression;
    private final Token nameToken;
    
    public PendingFunctionCall(FunctionCallExpression expression, Token nameToken) {
        this.expression = expression;
        this.nameToken = nameToken;
    }
    
    public FunctionCallExpression getExpression() {
        return expression;
    }
    
    public Token getNameToken() {
        return nameToken;
    }
    
    public String getFunctionName() {
        return expression.getCallee();
    }
    
    public int getArgumentCount() {
        return expression.getArguments().length;
    }
}
