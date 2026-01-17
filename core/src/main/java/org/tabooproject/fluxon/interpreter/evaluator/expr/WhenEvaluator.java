package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.WhenExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.List;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

public class WhenEvaluator extends ExpressionEvaluator<WhenExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.WHEN;
    }

    @Override
    public Object evaluate(Interpreter interpreter, WhenExpression result) {
        // 获取并评估主题对象（如果有）
        Object subject = null;
        if (result.getSubject() != null) {
            subject = interpreter.evaluate(result.getSubject());
        }
        // 遍历所有分支，只执行匹配的分支
        for (WhenExpression.WhenBranch branch : result.getBranches()) {
            // 如果是 else 分支（没有条件），直接返回其结果
            if (branch.getCondition() == null && branch.getMatchType() != WhenExpression.MatchType.IS) {
                return interpreter.evaluate(branch.getResult());
            }
            // 评估分支条件（IS 类型不需要条件）
            Object condition = null;
            if (branch.getMatchType() != WhenExpression.MatchType.IS) {
                condition = interpreter.evaluate(branch.getCondition());
            }
            // 执行分支匹配
            if (Intrinsics.matchWhenBranch(subject, condition, branch.getMatchType(), branch.getTargetClass())) {
                return interpreter.evaluate(branch.getResult());
            }
        }
        // 如果没有匹配的分支，返回 null
        return null;
    }

    @Override
    public Type generateBytecode(WhenExpression expr, CodeContext ctx, MethodVisitor mv) {
        // 评估主题对象（如果有）
        if (expr.getSubject() != null) {
            Evaluator<ParseResult> subjectEval = ctx.getEvaluator(expr.getSubject());
            if (subjectEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for when expression subject");
            }
            Type subjectType = subjectEval.generateBytecode(expr.getSubject(), ctx, mv);
            if (subjectType == VOID) {
                throw new VoidError("Void type is not allowed for when expression subject");
            }
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        // 存储 subject 到局部变量
        int subjectVar = ctx.allocateLocalVar(OBJECT);
        mv.visitVarInsn(ASTORE, subjectVar);
        // 创建所有分支的标签
        List<WhenExpression.WhenBranch> branches = expr.getBranches();
        Label[] branchLabels = new Label[branches.size()];
        Label endLabel = new Label();
        for (int i = 0; i < branches.size(); i++) {
            branchLabels[i] = new Label();
        }
        // 遍历所有分支进行条件判断
        for (int i = 0; i < branches.size(); i++) {
            WhenExpression.WhenBranch branch = branches.get(i);
            // 如果是 else 分支（没有条件且不是 IS 类型），直接跳转到分支执行
            if (branch.getCondition() == null && branch.getMatchType() != WhenExpression.MatchType.IS) {
                mv.visitJumpInsn(GOTO, branchLabels[i]);
                break;
            }
            // 对于 IS 类型，使用内联的 INSTANCEOF 字节码
            if (branch.getMatchType() == WhenExpression.MatchType.IS) {
                Class<?> targetClass = branch.getTargetClass();
                if (targetClass == null) {
                    throw new RuntimeException("IS match type requires a targetClass");
                }
                // 加载 subject（栈：[] -> [obj]）
                mv.visitVarInsn(ALOAD, subjectVar);
                // 使用 BytecodeUtils 生成 INSTANCEOF 检查（栈：[obj] -> [int]）
                Instructions.emitInstanceofCheck(mv, targetClass);
                // 如果匹配（int != 0），跳转到对应分支
                mv.visitJumpInsn(IFNE, branchLabels[i]);
                continue;
            }
            // 其他匹配类型：使用 Intrinsics.matchWhenBranch()
            // 加载 subject
            mv.visitVarInsn(ALOAD, subjectVar);
            // 评估分支条件
            Evaluator<ParseResult> conditionEval = ctx.getEvaluator(branch.getCondition());
            if (conditionEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for when expression condition");
            }
            Type conditionType = conditionEval.generateBytecode(branch.getCondition(), ctx, mv);
            if (conditionType == VOID) {
                throw new VoidError("Void type is not allowed for when expression condition");
            }
            // 加载 matchType
            mv.visitFieldInsn(GETSTATIC, MATCH_TYPE.getPath(), branch.getMatchType().name(), MATCH_TYPE.getDescriptor());
            // 传递 null 作为 targetClass（非 IS 类型不需要）
            mv.visitInsn(ACONST_NULL);
            // 调用 matchWhenBranch(Object, Object, MatchType, Class<?>)
            mv.visitMethodInsn(INVOKESTATIC,
                    Intrinsics.TYPE.getPath(),
                    "matchWhenBranch",
                    "(" + OBJECT + OBJECT + MATCH_TYPE + CLASS + ")Z",
                    false
            );
            // 如果匹配，跳转到对应分支
            mv.visitJumpInsn(IFNE, branchLabels[i]);
        }
        // 如果没有匹配的分支，返回 null
        mv.visitInsn(ACONST_NULL);
        mv.visitJumpInsn(GOTO, endLabel);
        // 生成各分支的执行代码
        for (int i = 0; i < branches.size(); i++) {
            mv.visitLabel(branchLabels[i]);
            Evaluator<ParseResult> branchEval = ctx.getEvaluator(branches.get(i).getResult());
            if (branchEval == null) {
                throw new EvaluatorNotFoundError("No evaluator found for when expression branch result");
            }
            Type branchType = branchEval.generateBytecode(branches.get(i).getResult(), ctx, mv);
            // 如果分支返回 void，则推送 null 作为返回值
            if (branchType == VOID) {
                mv.visitInsn(ACONST_NULL);
            }
            mv.visitJumpInsn(GOTO, endLabel);
        }
        mv.visitLabel(endLabel);
        return OBJECT;
    }

    private static final Type MATCH_TYPE = new Type(WhenExpression.MatchType.class);
}
