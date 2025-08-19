package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeUtils;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.interpreter.error.EvaluatorNotFoundException;
import org.tabooproject.fluxon.interpreter.error.VoidValueException;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ForExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Iterator;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

public class ForEvaluator extends ExpressionEvaluator<ForExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FOR;
    }

    @Override
    public Object evaluate(Interpreter interpreter, ForExpression result) {
        // 评估集合表达式
        Object collection = interpreter.evaluate(result.getCollection());
        // 使用 Operations 类创建迭代器
        Iterator<?> iterator = Intrinsics.createIterator(collection);
        // 获取变量名列表
        Map<String, Integer> variables = result.getVariables();
        Object last = null;
        // 迭代集合元素
        while (iterator.hasNext()) {
            // 使用解构器注册表执行解构
            DestructuringRegistry.getInstance().destructure(interpreter.getEnvironment(), variables, iterator.next());
            // 执行循环体
            try {
                last = interpreter.evaluate(result.getBody());
            } catch (ContinueException ignored) {
            } catch (BreakException ignored) {
                break;
            }
        }
        return last;
    }

    /*
            评估集合表达式并创建迭代器
            |
            创建变量Map（在循环外部）
            |
            注册循环上下文（break -> whileEnd, continue -> whileStart）
            |
            V
            whileStart:
            检查 iterator.hasNext()
            |
            +--> 如果为假，跳到 whileEnd
            |
            获取 iterator.next() 元素
            |
            调用 Intrinsics.destructureAndSetVars 设置变量
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
    public Type generateBytecode(ForExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取评估器注册表
        Evaluator<ParseResult> collectionEval = ctx.getEvaluator(result.getCollection());
        if (collectionEval == null) {
            throw new EvaluatorNotFoundException("No evaluator found for collection expression");
        }
        Evaluator<ParseResult> bodyEval = ctx.getEvaluator(result.getBody());
        if (bodyEval == null) {
            throw new EvaluatorNotFoundException("No evaluator found for body expression");
        }

        // 分配局部变量存储迭代器和变量Map
        int iteratorVar = ctx.allocateLocalVar(Type.OBJECT);
        int variablesMapVar = ctx.allocateLocalVar(Type.OBJECT);

        // 创建标签用于跳转
        Label whileStart = new Label();
        Label whileEnd = new Label();

        // 评估集合表达式并创建迭代器
        if (collectionEval.generateBytecode(result.getCollection(), ctx, mv) == Type.VOID) {
            throw new VoidValueException("Void type is not allowed for for loop collection");
        }
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "createIterator", "(" + Type.OBJECT + ")" + ITERATOR, false);
        mv.visitVarInsn(ASTORE, iteratorVar);

        // 将 result.getVariables() 转换为 Map
        BytecodeUtils.generateVariablePositionMap(mv, result.getVariables());
        mv.visitVarInsn(ASTORE, variablesMapVar);

        // 注册循环上下文：break 跳到 whileEnd，continue 跳到 whileStart
        ctx.enterLoop(whileEnd, whileStart);
        // while 循环开始标签
        mv.visitLabel(whileStart);

        // 检查条件：iterator.hasNext()
        mv.visitVarInsn(ALOAD, iteratorVar);
        mv.visitMethodInsn(INVOKEINTERFACE, ITERATOR.getPath(), "hasNext", "()Z", true);
        mv.visitJumpInsn(IFEQ, whileEnd); // 如果没有更多元素，跳转到结束

        // 执行解构操作：准备参数
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(ALOAD, variablesMapVar); // 变量 Map
        
        // 获取下一个元素
        mv.visitVarInsn(ALOAD, iteratorVar);
        mv.visitMethodInsn(INVOKEINTERFACE, ITERATOR.getPath(), "next", "()" + Type.OBJECT, true);

        // 调用解构方法
        mv.visitMethodInsn(
                INVOKESTATIC,
                Intrinsics.TYPE.getPath(),
                "destructure",
                "(" + RuntimeScriptBase.TYPE + MAP + Type.OBJECT + ")V", false);

        // 执行循环体
        // break 和 continue 语句会直接生成跳转指令
        if (bodyEval.generateBytecode(result.getBody(), ctx, mv) != Type.VOID) {
            mv.visitInsn(POP);
        }
        // 跳回循环开始
        mv.visitJumpInsn(GOTO, whileStart);
        // while 循环结束标签
        mv.visitLabel(whileEnd);
        // 退出循环上下文
        ctx.exitLoop();
        return Type.VOID;
    }

    private static final Type ITERATOR = new Type(Iterator.class);
    private static final Type MAP = new Type(Map.class);
}
