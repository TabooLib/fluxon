package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.CommandExecutor;
import org.tabooproject.fluxon.parser.expression.CommandExpression;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;
import org.tabooproject.fluxon.runtime.Type;

/**
 * Command 表达式求值器
 * 执行自定义 command 的逻辑
 */
@SuppressWarnings("unchecked")
public class CommandEvaluator extends Evaluator<CommandExpression> {

    @Override
    public Object evaluate(Interpreter interpreter, CommandExpression expr) {
        try {
            // 直接调用解析时捕获的 executor
            CommandExecutor<Object> executor = (CommandExecutor<Object>) expr.getExecutor();
            return executor.execute(interpreter, expr.getParsedData());
        } catch (FluxonRuntimeError ex) {
            throw ex;
        } catch (Exception ex) {
            // 包装为通用运行时异常
            throw new RuntimeException("Error executing command '" + expr.getCommandName() + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public Type generateBytecode(CommandExpression result, CodeContext ctx, MethodVisitor mv) {
        // Command 不支持编译为字节码
        throw new UnsupportedOperationException(
            "Command expressions are not supported in compiled mode. " +
            "Commands can only be used in interpreted scripts."
        );
    }
}
