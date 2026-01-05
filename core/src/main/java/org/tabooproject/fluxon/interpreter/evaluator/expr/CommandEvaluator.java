package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.parser.CommandExecutor;
import org.tabooproject.fluxon.parser.expression.CommandExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;
import static org.tabooproject.fluxon.runtime.Type.STRING;

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
            return executor.execute(interpreter.getEnvironment(), expr.getParsedData());
        } catch (FluxonRuntimeError ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Error executing command '" + expr.getCommandName() + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public Type generateBytecode(CommandExpression expr, CodeContext ctx, MethodVisitor mv) {
        String commandName = expr.getCommandName();
        Object parsedData = expr.getParsedData();
        // 将 parsedData 存储到 CodeContext，获取索引
        int dataIndex = ctx.addCommandData(parsedData);
        // 1. 加载 command name
        mv.visitLdcInsn(commandName);
        // 2. 加载 environment
        BytecodeUtils.loadEnvironment(mv, ctx);
        // 3. 调用 this.getCommandData(dataIndex) 获取 parsedData
        mv.visitVarInsn(ALOAD, 0);  // this
        mv.visitLdcInsn(dataIndex);
        mv.visitMethodInsn(INVOKEVIRTUAL, RuntimeScriptBase.TYPE.getPath(), "getCommandData", "(I)" + OBJECT, false);
        // 4. 调用 Intrinsics.executeCommand(String, Environment, Object)
        mv.visitMethodInsn(
            INVOKESTATIC,
            Intrinsics.TYPE.getPath(),
            "executeCommand",
            "(" + STRING + Environment.TYPE + OBJECT + ")" + OBJECT,
            false
        );
        return OBJECT;
    }
}
