package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.DestructuringAssignExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 解构赋值表达式求值器
 * 处理 (var1, var2, ...) = expression 语法
 */
public class DestructuringAssignmentEvaluator extends ExpressionEvaluator<DestructuringAssignExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.DESTRUCTURING_ASSIGNMENT;
    }

    @Override
    public Object evaluate(Interpreter interpreter, DestructuringAssignExpression result) {
        // 评估右侧表达式
        Object value = interpreter.evaluate(result.getValue());
        // 使用解构器注册表执行解构
        DestructuringRegistry.getInstance().destructure(
                interpreter.getEnvironment(),
                result.getVariables(),
                value
        );
        // 返回被解构的值
        return value;
    }

    @Override
    public Type generateBytecode(DestructuringAssignExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取右侧表达式的评估器
        Evaluator<ParseResult> valueEval = ctx.getEvaluator(result.getValue());
        if (valueEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for destructuring value expression");
        }

        // 分配局部变量存储变量Map
        int variablesMapVar = ctx.allocateLocalVar(Type.OBJECT);

        // 评估右侧表达式
        if (valueEval.generateBytecode(result.getValue(), ctx, mv) == Type.VOID) {
            throw new VoidError("Void type is not allowed for destructuring assignment");
        }

        // 复制栈顶值（用于返回）
        mv.visitInsn(DUP);

        // 将 result.getVariables() 转换为 Map
        BytecodeUtils.generateVariablePositionMap(mv, result.getVariables());
        mv.visitVarInsn(ASTORE, variablesMapVar);

        // 交换顺序：value, map -> map, value -> this, map, value
        mv.visitVarInsn(ALOAD, variablesMapVar);  // stack: value, value, map
        mv.visitInsn(SWAP);                        // stack: value, map, value

        // 调用解构方法：Intrinsics.destructure(this, map, value)
        // 需要 this 在栈顶
        mv.visitVarInsn(ALOAD, 0);                 // stack: value, map, value, this
        mv.visitInsn(SWAP);                        // stack: value, map, this, value
        // 现在需要重新排列成 this, map, value
        // 使用局部变量临时存储
        int tempValue = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, tempValue);        // stack: value, map, this
        mv.visitInsn(SWAP);                        // stack: value, this, map
        mv.visitVarInsn(ALOAD, tempValue);         // stack: value, this, map, value

        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "destructure",
                "(" + RuntimeScriptBase.TYPE + MAP + Type.OBJECT + ")V",
                false
        );

        // 栈顶剩余原始值作为表达式返回值
        return Type.OBJECT;
    }

    private static final Type MAP = new Type(Map.class);
}
