package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ErrorPropagationExpression;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

import static org.objectweb.asm.Opcodes.*;

/**
 * 错误传播求值器
 * expr? — null 或异常时设置 return 信号，从当前函数返回 null
 *
 * @author sky
 */
public class ErrorPropagationEvaluator extends ExpressionEvaluator<ErrorPropagationExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.ERROR_PROPAGATION;
    }

    @Override
    public Type evaluate(Interpreter interpreter, ErrorPropagationExpression result) {
        try {
            Type t = interpreter.evaluate(result.getOperand());
            if (interpreter.hasReturn) return Type.VOID;
            Object value = interpreter.getResultBoxed(t);
            if (value == null) {
                interpreter.hasReturn = true;
                interpreter.returnValue = null;
                return Type.VOID;
            }
            interpreter.resultRef = value;
            return Type.OBJECT;
        } catch (Exception ex) {
            interpreter.hasReturn = true;
            interpreter.returnValue = null;
            return Type.VOID;
        }
    }

    @Override
    public Type generateBytecode(ErrorPropagationExpression result, CodeContext ctx, MethodVisitor mv) {
        Evaluator<ParseResult> operandEval = ctx.getEvaluator(result.getOperand());
        if (operandEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for error propagation operand");
        }
        // 评估内部表达式
        Type operandType = operandEval.generateBytecode(result.getOperand(), ctx, mv);
        if (operandType.isPrimitive()) {
            // 原始类型不可能为 null，直接装箱返回
            boxing(operandType, mv);
            return Type.OBJECT;
        }
        boolean isTopLevel = ctx.getExpectedReturnType() != null;
        if (operandType == Type.VOID) {
            // void 视为 null → return null
            emitReturnNull(isTopLevel, mv);
            return Type.OBJECT; // 不可达但需要返回类型
        }
        // 引用类型：检查 null
        Label notNull = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, notNull);
        // null → return null
        mv.visitInsn(POP);
        emitReturnNull(isTopLevel, mv);
        // 非 null → 继续
        mv.visitLabel(notNull);
        return Type.OBJECT;
    }

    /**
     * 生成 return null 的字节码
     * 顶层脚本直接 ARETURN null，函数体内通过 FunctionContext.setReturnRef 设置返回值
     */
    private static void emitReturnNull(boolean isTopLevel, MethodVisitor mv) {
        if (isTopLevel) {
            mv.visitInsn(ACONST_NULL);
            mv.visitInsn(ARETURN);
        } else {
            mv.visitVarInsn(ALOAD, 1); // FunctionContext
            mv.visitInsn(ACONST_NULL);
            mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnRef", "(" + Type.OBJECT + ")V", false);
            mv.visitInsn(RETURN);
        }
    }

    @Override
    public void analyzeTypes(ErrorPropagationExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getOperand());
    }
}
