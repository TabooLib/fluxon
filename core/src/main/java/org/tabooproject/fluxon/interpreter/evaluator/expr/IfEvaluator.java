package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.error.EvaluatorNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VoidValueException;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.IfExpression;
import org.tabooproject.fluxon.runtime.Type;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.stdlib.Operations.isTrue;

public class IfEvaluator extends ExpressionEvaluator<IfExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.IF;
    }

    @Override
    public Object evaluate(Interpreter interpreter, IfExpression result) {
        if (isTrue(interpreter.evaluate(result.getCondition()))) {
            return interpreter.evaluate(result.getThenBranch());
        } else if (result.getElseBranch() != null) {
            return interpreter.evaluate(result.getElseBranch());
        } else {
            return null;
        }
    }

    /*
            条件判断
            |
            +--> 如果为假，跳到elseLabel
            |
            执行then分支
            |
            +--> 跳到endLabel
            |
            elseLabel:
            执行else分支
            |
            V
            endLabel:
            继续执行后续代码
     */
    @Override
    public Type generateBytecode(IfExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取评估器注册表
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        if (conditionEval == null) {
            throw new EvaluatorNotFoundException("No evaluator found for expression");
        }
        Evaluator<ParseResult> thenEval = ctx.getEvaluator(result.getThenBranch());
        if (thenEval == null) {
            throw new EvaluatorNotFoundException("No evaluator found for expression");
        }
        Evaluator<ParseResult> elseEval = result.getElseBranch() != null ? ctx.getEvaluator(result.getElseBranch()) : null;

        // 创建局部变量用于存储分支结果
        int storeId = ctx.allocateLocalVar(Type.OBJECT);
        // 创建标签用于跳转
        Label elseLabel = new Label();
        Label endLabel = new Label();
        // 评估条件表达式
        generateCondition(ctx, mv, result.getCondition(), conditionEval, elseLabel);

        // then 分支代码
        if (thenEval.generateBytecode(result.getThenBranch(), ctx, mv) == Type.VOID) {
            // 压入空对象
            mv.visitInsn(ACONST_NULL);
        }
        mv.visitVarInsn(ASTORE, storeId);
        mv.visitJumpInsn(GOTO, endLabel);

        // else 分支标签
        mv.visitLabel(elseLabel);
        // 生成 else 分支的字节码（如果存在）
        if (elseEval != null) {
            if (elseEval.generateBytecode(result.getElseBranch(), ctx, mv) == Type.VOID) {
                // 压入空对象
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitVarInsn(ASTORE, storeId);
        } else {
            mv.visitInsn(ACONST_NULL);
            mv.visitVarInsn(ASTORE, storeId);
        }

        // 结束标签
        mv.visitLabel(endLabel);
        // 将分支结果加载到栈顶
        mv.visitVarInsn(ALOAD, storeId);
        return Type.OBJECT;
    }
}
