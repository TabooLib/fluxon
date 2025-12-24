package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.expression.ListExpression;
import org.tabooproject.fluxon.parser.expression.MapExpression;
import org.tabooproject.fluxon.parser.expression.literal.Literal;
import org.tabooproject.fluxon.parser.type.BlockParser;
import org.tabooproject.fluxon.parser.type.ExpressionParser;
import org.tabooproject.fluxon.parser.type.ListParser;

import java.util.*;

/**
 * 函数定义语句宏
 * <p>
 * 处理 sync def、async def、def 函数定义，以及前置的注解。
 * 仅支持顶层语句。
 */
public class FunctionDefinitionMacro implements StatementMacro {

    @Override
    public boolean matchesTopLevel(Parser parser) {
        // 匹配注解或函数定义关键字
        return parser.checkAny(TokenType.AT, TokenType.SYNC, TokenType.ASYNC, TokenType.DEF);
    }

    @Override
    public ParseResult parseTopLevel(Parser parser) {
        // 自行收集注解
        List<Annotation> annotations = new ArrayList<>();
        while (parser.check(TokenType.AT)) {
            annotations.add(parseAnnotation(parser));
        }
        // 解析函数定义
        if (parser.match(TokenType.SYNC)) {
            parser.match(TokenType.DEF);
            return parse(parser, false, true, annotations);
        }
        if (parser.match(TokenType.ASYNC)) {
            parser.match(TokenType.DEF);
            return parse(parser, true, false, annotations);
        }
        if (parser.match(TokenType.DEF)) {
            return parse(parser, false, false, annotations);
        }
        // 有注解但没有函数定义
        if (!annotations.isEmpty()) {
            throw new RuntimeException("Annotations can only be applied to function definitions");
        }
        throw parser.createParseException("Expected function definition", parser.peek());
    }

    @Override
    public int priority() {
        return 200;
    }

    /**
     * 解析单个注解
     */
    private Annotation parseAnnotation(Parser parser) {
        parser.consume(TokenType.AT, "Expected '@' for annotation");
        String name = parser.consume(TokenType.IDENTIFIER, "Expected annotation name").getLexeme();
        // 检查是否有属性
        if (parser.match(TokenType.LEFT_PAREN)) {
            Map<String, Object> attributes = parseAnnotationAttributes(parser);
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after annotation attributes");
            return new Annotation(name, attributes);
        }
        parser.match(TokenType.SEMICOLON); // 可选的分号
        return new Annotation(name);
    }

    /**
     * 解析注解属性列表
     */
    private Map<String, Object> parseAnnotationAttributes(Parser parser) {
        Map<String, Object> attributes = new HashMap<>();
        if (!parser.check(TokenType.RIGHT_PAREN)) {
            do {
                Token keyToken = parser.consume(TokenType.IDENTIFIER, "Expected attribute name");
                String key = keyToken.getLexeme();
                parser.consume(TokenType.ASSIGN, "Expected '=' after attribute name");
                // 如果是列表
                if (parser.match(TokenType.LEFT_BRACKET)) {
                    ParseResult value = ListParser.parse(parser);
                    // 列表
                    if (value instanceof ListExpression) {
                        ListExpression listExpr = (ListExpression) value;
                        List<Object> literalValues = new ArrayList<>();
                        for (ParseResult element : listExpr.getElements()) {
                            if (element instanceof Literal) {
                                literalValues.add(((Literal) element).getSourceValue());
                            } else {
                                parser.error("Expected a literal value for list element in attribute '" + key + "'");
                            }
                        }
                        attributes.put(key, literalValues);
                    }
                    // 映射
                    else if (value instanceof MapExpression) {
                        MapExpression mapExpr = (MapExpression) value;
                        Map<Object, Object> literalValues = new HashMap<>();
                        for (MapExpression.MapEntry entry : mapExpr.getEntries()) {
                            ParseResult entryKey = entry.getKey();
                            ParseResult entryValue = entry.getValue();
                            if (entryKey instanceof Literal && entryValue instanceof Literal) {
                                literalValues.put(((Literal) entryKey).getSourceValue(), ((Literal) entryValue).getSourceValue());
                            } else {
                                parser.error("Expected literal values for map key and value in attribute '" + key + "'");
                            }
                        }
                        attributes.put(key, literalValues);
                    }
                } else {
                    ParseResult value = ExpressionParser.parsePrimary(parser);
                    if (value instanceof Literal) {
                        attributes.put(key, ((Literal) value).getSourceValue());
                    } else {
                        parser.error("Expected a literal value for attribute '" + key + "'");
                    }
                }
            } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_PAREN));
        }
        return attributes;
    }

    /**
     * 解析函数定义
     * 关键特性：
     * 1. 允许无括号参数定义，如：def factorial n = { ... }
     * 2. 允许省略大括号
     * 3. 支持注解，如：@listener(event = "onStart") def handleStart() = { ... }
     *
     * @param parser        解析器
     * @param isAsync       是否为异步函数
     * @param isPrimarySync 是否为主线程同步函数
     * @param annotations   函数的注解列表
     * @return 函数定义解析结果
     */
    private ParseResult parse(Parser parser, boolean isAsync, boolean isPrimarySync, List<Annotation> annotations) {
        // 解析函数名
        Token nameToken = parser.consume(TokenType.IDENTIFIER, "Expected function name");
        String functionName = nameToken.getLexeme();
        // 创建函数标记
        parser.getSymbolEnvironment().setCurrentFunction(functionName);
        // 解析参数列表（函数定义支持无括号多参数）
        LinkedHashMap<String, Integer> parameters = parseParameters(
                parser,
                true,  // 函数定义支持无括号多参数
                TokenType.ASSIGN, TokenType.LEFT_BRACE  // 遇到等号或左大括号停止
        );
        // 消费可选的等于号
        parser.match(TokenType.ASSIGN);
        // 将函数添加到当前作用域
        Callable function = parser.getFunction(functionName);
        if (function != null) {
            // 函数已存在，添加新的参数数量
            List<Integer> paramCounts = new ArrayList<>(function.getParameterCounts());
            if (!paramCounts.contains(parameters.size())) {
                paramCounts.add(parameters.size());
            }
            parser.defineUserFunction(functionName, new SymbolFunction(null, functionName, paramCounts));
        } else {
            // 函数不存在，创建新条目
            parser.defineUserFunction(functionName, new SymbolFunction(null, functionName, parameters.size()));
        }

        // 解析函数体
        ParseResult body;
        // 如果有左大括号，则解析为 Block 函数体
        if (parser.match(TokenType.LEFT_BRACE)) {
            body = BlockParser.parse(parser);
        } else {
            body = ExpressionParser.parse(parser);
        }
        // 可选的分号
        parser.match(TokenType.SEMICOLON);

        // 函数局部变量
        Set<String> localVariables = parser.getSymbolEnvironment().getLocalVariables().get(functionName);
        if (localVariables == null) {
            localVariables = new HashSet<>();
        }
        // 退出函数标记
        parser.getSymbolEnvironment().setCurrentFunction(null);
        return new FunctionDefinition(functionName, parameters, body, isAsync, isPrimarySync, annotations, localVariables);
    }

    /**
     * 解析参数列表
     * 支持三种语法：
     * 1. 带括号：(x, y, z)
     * 2. 无括号多参数：x y z 或 x, y, z
     * 3. 单参数简写：x
     *
     * @param parser                       解析器
     * @param allowUnparenthesizedMultiple 是否允许无括号的多参数
     * @param stopTokens                   停止解析的 token 类型
     * @return 参数名到索引的映射
     */
    public static LinkedHashMap<String, Integer> parseParameters(Parser parser, boolean allowUnparenthesizedMultiple, TokenType... stopTokens) {
        LinkedHashMap<String, Integer> parameters = new LinkedHashMap<>();
        // 带括号的参数列表: (x, y, z)
        if (parser.match(TokenType.LEFT_PAREN)) {
            if (!parser.check(TokenType.RIGHT_PAREN)) {
                do {
                    addParameter(parser, parameters);
                } while (parser.match(TokenType.COMMA) && !parser.check(TokenType.RIGHT_PAREN));
            }
            parser.consume(TokenType.RIGHT_PAREN, "Expected ')' after parameters");
            return parameters;
        }
        // 无括号参数：需要检查是否允许
        if (!allowUnparenthesizedMultiple) {
            return parameters;
        }
        // 无括号的参数列表: x, y, z 或 x y z
        while (parser.check(TokenType.IDENTIFIER)) {
            addParameter(parser, parameters);
            // 检查是否遇到停止符
            if (isStopToken(parser.peek().getType(), stopTokens)) {
                break;
            }
            // 可选的逗号分隔
            if (!parser.match(TokenType.COMMA)) {
                // 如果没有逗号，检查下一个是否还是标识符（空格分隔）
                if (!parser.check(TokenType.IDENTIFIER)) {
                    break;
                }
            }
        }
        return parameters;
    }

    /**
     * 添加单个参数
     */
    private static void addParameter(Parser parser, LinkedHashMap<String, Integer> parameters) {
        Token paramToken = parser.consume(TokenType.IDENTIFIER, "Expected parameter name");
        String paramName = paramToken.getLexeme();
        // 检查参数重复
        if (parameters.containsKey(paramName)) {
            parser.error("Duplicate parameter name: " + paramName);
        }
        // 注册参数为局部变量
        parser.defineVariable(paramName);
        int index = parser.getSymbolEnvironment().getLocalVariable(paramName);
        parameters.put(paramName, index);
    }

    /**
     * 检查当前 token 是否是停止符
     */
    private static boolean isStopToken(TokenType token, TokenType... stopTokens) {
        if (stopTokens == null) {
            return false;
        }
        for (TokenType stopToken : stopTokens) {
            if (token == stopToken) {
                return true;
            }
        }
        return false;
    }
}
