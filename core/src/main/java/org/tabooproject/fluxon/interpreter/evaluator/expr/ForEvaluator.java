package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.BreakException;
import org.tabooproject.fluxon.interpreter.ContinueException;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.EvaluatorRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ForExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.stdlib.Operations;

import java.util.Iterator;
import java.util.List;

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
        Iterator<?> iterator = Operations.createIterator(collection);
        // 获取变量名列表
        List<String> variables = result.getVariables();
        // 创建新环境进行迭代
        interpreter.enterScope();
        Object last = null;
        try {
            Environment environment = interpreter.getEnvironment();
            // 迭代集合元素
            while (iterator.hasNext()) {
                // 使用解构器注册表执行解构
                DestructuringRegistry.getInstance().destructure(environment, variables, iterator.next());
                // 执行循环体
                try {
                    last = interpreter.evaluate(result.getBody());
                } catch (ContinueException ignored) {
                } catch (BreakException ignored) {
                    break;
                }
            }
        } finally {
            interpreter.exitScope();
        }
        return last;
    }

    /*
            评估集合表达式并创建迭代器
            |
            创建变量名数组（在循环外部）
            |
            V
            whileStart:
            检查 iterator.hasNext()
            |
            +--> 如果为假，跳到 whileEnd
            |
            获取 iterator.next() 元素
            |
            调用 Operations.destructureAndSetVars 设置变量
            |
            执行循环体（丢弃返回值）
            |
            跳回 whileStart
            |
            V
            whileEnd:
            返回 null
     */
    @Override
    public Type generateBytecode(ForExpression result, CodeContext ctx, MethodVisitor mv) {
        // 获取评估器注册表
        EvaluatorRegistry registry = EvaluatorRegistry.getInstance();
        Evaluator<ParseResult> collectionEval = registry.getEvaluator(result.getCollection());
        if (collectionEval == null) {
            throw new RuntimeException("No evaluator found for collection expression");
        }
        Evaluator<ParseResult> bodyEval = registry.getEvaluator(result.getBody());
        if (bodyEval == null) {
            throw new RuntimeException("No evaluator found for body expression");
        }

        // 分配局部变量存储迭代器和变量名数组
        int iteratorVar = ctx.allocateLocalVar(Type.OBJECT);
        int variablesArrayVar = ctx.allocateLocalVar(Type.OBJECT);

        // 创建标签用于跳转
        Label whileStart = new Label();
        Label whileEnd = new Label();

        // 评估集合表达式并创建迭代器
        collectionEval.generateBytecode(result.getCollection(), ctx, mv);
        mv.visitMethodInsn(INVOKESTATIC, Operations.TYPE.getPath(), "createIterator", "(" + Type.OBJECT + ")Ljava/util/Iterator;", false);
        mv.visitVarInsn(ASTORE, iteratorVar);

        // 创建变量名数组（在循环外部）
        List<String> variables = result.getVariables();
        mv.visitLdcInsn(variables.size());
        mv.visitTypeInsn(ANEWARRAY, "java/lang/String");
        for (int i = 0; i < variables.size(); i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            mv.visitLdcInsn(variables.get(i));
            mv.visitInsn(AASTORE);
        }
        mv.visitVarInsn(ASTORE, variablesArrayVar);

        // while 循环开始标签
        mv.visitLabel(whileStart);

        // 检查条件：iterator.hasNext()
        mv.visitVarInsn(ALOAD, iteratorVar);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z", true);
        mv.visitJumpInsn(IFEQ, whileEnd); // 如果没有更多元素，跳转到结束

        // 执行解构操作：准备参数
        mv.visitVarInsn(ALOAD, 0); // this
        mv.visitVarInsn(ALOAD, variablesArrayVar); // 变量名数组
        
        // 获取下一个元素
        mv.visitVarInsn(ALOAD, iteratorVar);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()" + Type.OBJECT, true);
        
        // 调用解构方法
        mv.visitMethodInsn(
                INVOKESTATIC,
                Operations.TYPE.getPath(),
                "destructureAndSetVars",
                "(L" + RuntimeScriptBase.class.getName().replace('.', '/') + ";[Ljava/lang/String;" + Type.OBJECT + ")V", false);

        // 执行循环体
        Type bodyType = bodyEval.generateBytecode(result.getBody(), ctx, mv);
        // 如果循环体有返回值，则丢弃它
        if (bodyType != Type.VOID) {
            mv.visitInsn(POP);
        }
        // 跳回循环开始
        mv.visitJumpInsn(GOTO, whileStart);
        // while 循环结束标签
        mv.visitLabel(whileEnd);
        return Type.VOID;
    }
}
