package org.tabooproject.fluxon.parser.parselet;

import org.tabooproject.fluxon.ast.SourceLocation;
import org.tabooproject.fluxon.ast.expression.CallExpr;
import org.tabooproject.fluxon.ast.expression.Expr;
import org.tabooproject.fluxon.ast.expression.LiteralExpr;
import org.tabooproject.fluxon.ast.expression.VariableExpr;
import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.Parser;
import org.tabooproject.fluxon.parser.PrattParser;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数调用解析器，支持两种调用格式：带括号 func(arg1, arg2) 和不带括号 func arg1 arg2
 */
public class CallParselet extends BaseInfixParselet {
    private final PrattParser prattParser;

    public CallParselet(PrattParser prattParser) {
        this.prattParser = prattParser;
    }

    /**
     * 解析函数调用表达式
     * 
     * @param left 左侧表达式（函数名）
     * @param token 当前词法单元（标识符或左括号）
     * @return 函数调用表达式
     */
    @Override
    public Expr parse(Expr left, Token token) {
        List<Expr> arguments = new ArrayList<>();
        SourceLocation location = left.getLocation();
        
        // 获取函数名，在 Fluxon 中函数名是字符串字面量
        String functionName;
        Expr firstArg = null;
        
        // 如果左侧表达式是字符串字面量，则作为函数名
        if (left instanceof LiteralExpr && ((LiteralExpr) left).getType() == LiteralExpr.LiteralType.STRING) {
            functionName = (String) ((LiteralExpr) left).getValue();
        }
        else if (left instanceof CallExpr) {
            // 处理嵌套函数调用，如 "print factorial &x"
            functionName = token.getValue();
            firstArg = left;
        } else {
            // 其他情况，默认使用标识符作为函数名
            functionName = token.getValue();
            firstArg = left;
        }
        
        // 处理两种函数调用格式
        if (token.getType() == TokenType.LEFT_PAREN) {
            // 带括号的函数调用：func(arg1, arg2)
            if (!prattParser.parser.check(TokenType.RIGHT_PAREN)) {
                do {
                    arguments.add(prattParser.parseExpression(0));
                } while (prattParser.parser.match(TokenType.COMMA));
            }
            
            // 消费右括号
            Token end = prattParser.parser.consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
            
            // 更新源代码位置
            location = new SourceLocation(
                    location.getStartLine(),
                    location.getStartColumn(),
                    end.getLine(),
                    end.getColumn()
            );
        } else if (token.getType() == TokenType.IDENTIFIER) {
            // 不带括号的函数调用：func arg1 arg2

            // 如果有第一个参数，添加到参数列表
            if (firstArg != null) {
                arguments.add(firstArg);
                
                // 将 token 作为第二个参数
                arguments.add(prattParser.parseIdentifier(token));
            }
 else {
                arguments.add(prattParser.parseIdentifier(token));
            }
            
            // 解析后续参数，直到遇到优先级更高的操作符或语句结束
            while (prattParser.getPrecedence() < getPrecedence()) {
                arguments.add(prattParser.parseExpression(getPrecedence()));
            }
            
            // 更新源代码位置
            if (!arguments.isEmpty()) {
                Expr lastArg = arguments.get(arguments.size() - 1);
                location = new SourceLocation(
                        location.getStartLine(),
                        location.getStartColumn(),
                        lastArg.getLocation().getEndLine(),
                        lastArg.getLocation().getEndColumn()
                );
            }
        }
        
        // 创建函数调用表达式节点
        return new CallExpr(functionName, arguments, location);
    }
}
