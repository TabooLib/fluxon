package org.tabooproject.fluxon.parser.macro;

import org.tabooproject.fluxon.lexer.Token;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.*;
import org.tabooproject.fluxon.parser.expression.CommandExpression;

import java.util.Objects;

/**
 * Command 语法宏
 * <p>
 * 匹配已注册的 command 标识符，优先级高于普通标识符。
 * 内部使用 {@link CommandRegistry} 来查找和执行 command。
 */
@SuppressWarnings("unchecked")
public class CommandSyntaxMacro implements SyntaxMacro {

    @Override
    public boolean matches(Parser parser) {
        if (!parser.check(TokenType.IDENTIFIER)) {
            return false;
        }
        String name = parser.peek().getLexeme();
        CommandRegistry registry = parser.getContext().getCommandRegistry();
        return registry.hasCommand(name);
    }

    @Override
    public Trampoline<ParseResult> parse(Parser parser, Trampoline.Continuation<ParseResult> continuation) {
        Token token = parser.consume();
        String name = token.getLexeme();
        CommandRegistry registry = parser.getContext().getCommandRegistry();
        CommandHandler<?> handler = registry.get(name);
        try {
            // 调用 CommandParser 解析参数
            CommandParser<Object> commandParser = (CommandParser<Object>) Objects.requireNonNull(handler).getParser();
            Object parsedData = commandParser.parse(parser, token);
            // 捕获 CommandExecutor 到 AST 节点
            CommandExecutor<Object> commandExecutor = (CommandExecutor<Object>) handler.getExecutor();
            return continuation.apply(parser.attachSource(new CommandExpression(name, parsedData, commandExecutor), token));
        } catch (ParseException ex) {
            // 重新抛出，让 Parser 统一处理
            throw ex;
        } catch (Exception ex) {
            // 包装为 RuntimeException
            throw new RuntimeException("Error parsing command '" + name + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public int priority() {
        return 1000; // 高于普通标识符
    }
}
