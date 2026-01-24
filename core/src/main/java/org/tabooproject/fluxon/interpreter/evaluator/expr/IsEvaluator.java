package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.IsExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

/**
 * Is 类型检查表达式求值器
 * <p>
 * 处理 `is` 运算符的求值和字节码生成
 */
public class IsEvaluator extends ExpressionEvaluator<IsExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.IS_CHECK;
    }

    @Override
    public Type evaluate(Interpreter interpreter, IsExpression expr) {
        Type t = interpreter.evaluate(expr.getLeft());
        Object obj = interpreter.getResultBoxed(t);
        interpreter.resultRef = Intrinsics.isInstanceOf(obj, expr.getTargetClass());
        return Type.BOOLEAN;
    }

    @Override
    public Type generateBytecode(IsExpression expr, CodeContext ctx, MethodVisitor mv) {
        // 获取左侧表达式的求值器
        Evaluator<ParseResult> leftEval = ctx.getEvaluator(expr.getLeft());
        if (leftEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for left operand of is expression");
        }
        // 生成左侧表达式的字节码（栈：[] -> [obj]）
        Type leftType = leftEval.generateBytecode(expr.getLeft(), ctx, mv);
        if (leftType == Type.VOID) {
            throw new VoidError("Void type is not allowed for is expression left operand");
        }
        boxing(leftType, mv);
        // 使用 BytecodeUtils 生成 INSTANCEOF 检查字节码（栈：[obj] -> [int]）
        Instructions.emitInstanceofCheck(mv, expr.getTargetClass());
        return Type.Z;
    }

    @Override
    public void analyzeTypes(IsExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getLeft());
    }

    @Override
    public Type inferResultType(IsExpression result, TypeAnalyzer analyzer) {
        return Type.Z;
    }
}
