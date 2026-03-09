package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.WhileExpression;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;

public class WhileEvaluator extends ExpressionEvaluator<WhileExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.WHILE;
    }

    @Override
    public Type evaluate(Interpreter interpreter, WhileExpression result) {
        boolean bodyIsStatement = result.getBody().getType() == ParseResult.ResultType.STATEMENT;
        while (true) {
            Type ct = interpreter.evaluate(result.getCondition());
            if (!interpreter.isResultTrue(ct)) break;
            if (executeLoopBody(interpreter, result.getBody(), bodyIsStatement)) break;
        }
        return Type.VOID;
    }

    /*
            注册循环上下文（break -> whileEnd, continue -> whileStart）
            |
            V
            whileStart:
            评估条件表达式
            |
            调用 Intrinsics.isTrue 判断条件
            |
            +--> 如果为假，跳到 whileEnd
            |
            执行循环体（break/continue 直接跳转）
            |
            跳回 whileStart
            |
            V
            whileEnd:
            退出循环上下文
     */
    @Override
    public Type generateBytecode(WhileExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取评估器注册表
        Evaluator<ParseResult> conditionEval = ctx.getEvaluator(result.getCondition());
        if (conditionEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for condition expression");
        }
        Evaluator<ParseResult> bodyEval = ctx.getEvaluator(result.getBody());
        if (bodyEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for body expression");
        }

        // 创建标签用于跳转
        Label whileStart = new Label();
        Label whileEnd = new Label();
        // 注册循环上下文：break 跳到 whileEnd，continue 跳到 whileStart
        ctx.enterLoop(whileEnd, whileStart);
        // while 循环开始标签
        mv.visitLabel(whileStart);
        // 评估条件表达式
        generateCondition(ctx, mv, result.getCondition(), conditionEval, whileEnd);

        // 执行循环体
        // break 和 continue 语句会直接生成跳转指令
        Type bodyType = bodyEval.generateBytecode(result.getBody(), ctx, mv);
        finishLoopBody(bodyType, mv, ctx, whileStart, whileEnd);
        return Type.VOID;
    }

    @Override
    public void analyzeTypes(WhileExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getCondition());
        analyzer.analyzeNode(result.getBody());
    }
}
