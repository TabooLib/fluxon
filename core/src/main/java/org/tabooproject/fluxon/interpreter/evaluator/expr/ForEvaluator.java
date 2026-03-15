package org.tabooproject.fluxon.interpreter.evaluator.expr;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.destructure.DestructuringRegistry;
import org.tabooproject.fluxon.interpreter.evaluator.Evaluator;
import org.tabooproject.fluxon.interpreter.evaluator.ExpressionEvaluator;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.expression.ExpressionType;
import org.tabooproject.fluxon.parser.expression.ForExpression;
import org.tabooproject.fluxon.parser.expression.RangeExpression;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.FunctionContext;
import org.tabooproject.fluxon.runtime.RuntimeScriptBase;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.error.EvaluatorNotFoundError;
import org.tabooproject.fluxon.runtime.error.VoidError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import java.util.Iterator;
import java.util.Map;
import java.util.function.IntFunction;

import static org.objectweb.asm.Opcodes.*;

public class ForEvaluator extends ExpressionEvaluator<ForExpression> {

    @Override
    public ExpressionType getType() {
        return ExpressionType.FOR;
    }

    @Override
    public Type evaluate(Interpreter interpreter, ForExpression result) {
        // 评估集合表达式
        Type ct = interpreter.evaluate(result.getCollection());
        Object collection = interpreter.getResultBoxed(ct);
        // 使用 Operations 类创建迭代器
        Iterator<?> iterator = Intrinsics.createIterator(collection);
        // 获取变量名列表
        Map<String, Integer> variables = result.getVariables();
        boolean bodyIsStatement = result.getBody().getType() == ParseResult.ResultType.STATEMENT;
        // Env-free 路径：循环变量写入 FunctionContext
        FunctionContext<?> ctx = interpreter.activeFunctionContext;
        if (ctx != null && variables.size() == 1) {
            int pos = variables.values().iterator().next();
            while (iterator.hasNext()) {
                ctx.setLocal(pos, iterator.next());
                if (executeLoopBody(interpreter, result.getBody(), bodyIsStatement)) break;
            }
            return Type.VOID;
        }
        Environment env = interpreter.getEnvironment();
        // 提前创建类型提供器，避免循环内每次迭代分配 lambda
        IntFunction<Type> typeProvider = env::getVariableType;
        // 迭代集合元素
        while (iterator.hasNext()) {
            DestructuringRegistry.getInstance().destructure(env, variables, iterator.next(), typeProvider);
            if (executeLoopBody(interpreter, result.getBody(), bodyIsStatement)) break;
        }
        return Type.VOID;
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
            throw new EvaluatorNotFoundError("No evaluator found for collection expression");
        }
        Evaluator<ParseResult> bodyEval = ctx.getEvaluator(result.getBody());
        if (bodyEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for body expression");
        }
        // IntRange 快速路径：单变量 + 集合是 RangeExpression + 变量类型是 int
        if (result.getVariables().size() == 1 && result.getCollection() instanceof RangeExpression) {
            int varPos = result.getVariables().values().iterator().next();
            Type varType = ctx.getVariableType(varPos);
            if (varType == Type.I || varType == Type.Z) {
                return generateIntRangeLoop(result, (RangeExpression) result.getCollection(), ctx, mv, bodyEval, varPos);
            }
        }
        return generateIteratorLoop(result, ctx, mv, collectionEval, bodyEval);
    }

    /**
     * IntRange 特化路径：直接生成原始 int 计数循环
     * 消除 IntRange 对象分配、Iterator 接口调用、autobox/unbox 链
     */
    private Type generateIntRangeLoop(ForExpression result, RangeExpression range, CodeContext ctx, MethodVisitor mv, Evaluator<ParseResult> bodyEval, int varPos) {
        int saved = ctx.getLocalVarIndex();
        Evaluator<ParseResult> startEval = ctx.getEvaluator(range.getStart());
        Evaluator<ParseResult> endEval = ctx.getEvaluator(range.getEnd());
        if (startEval == null || endEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for range expression");
        }
        // 生成 start 和 end 值
        Type st = startEval.generateBytecode(range.getStart(), ctx, mv);
        int startVar = ctx.allocateLocalVar(Type.I);
        emitToInt(st, mv);
        mv.visitVarInsn(ISTORE, startVar);
        Type et = endEval.generateBytecode(range.getEnd(), ctx, mv);
        int endVar = ctx.allocateLocalVar(Type.I);
        emitToInt(et, mv);
        // inclusive: 上界 +1 转换为 exclusive 比较
        if (range.isInclusive()) {
            mv.visitInsn(ICONST_1);
            mv.visitInsn(IADD);
        }
        mv.visitVarInsn(ISTORE, endVar);
        // 循环变量初始化
        int loopVar;
        boolean useJvmSlot = ctx.isEnvFreeMode();
        if (useJvmSlot) {
            loopVar = ctx.getJvmSlot(varPos);
        } else {
            loopVar = ctx.allocateLocalVar(Type.I);
        }
        mv.visitVarInsn(ILOAD, startVar);
        mv.visitVarInsn(ISTORE, loopVar);
        Label loopStart = new Label();
        Label loopEnd = new Label();
        ctx.enterLoop(loopEnd, loopStart);
        // 循环条件: loopVar < endVar
        mv.visitLabel(loopStart);
        mv.visitVarInsn(ILOAD, loopVar);
        mv.visitVarInsn(ILOAD, endVar);
        mv.visitJumpInsn(IF_ICMPGE, loopEnd);
        // 非 env-free 模式需要同步到 Environment
        if (!useJvmSlot) {
            Instructions.loadEnvironment(mv, ctx);
            mv.visitLdcInsn(varPos);
            mv.visitVarInsn(ILOAD, loopVar);
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalInt", "(II)V", false);
        }
        // 循环体
        Type bodyType = bodyEval.generateBytecode(result.getBody(), ctx, mv);
        // 递增并跳回
        finishLoopBodyWithInc(bodyType, mv, ctx, loopVar, loopStart, loopEnd);
        ctx.restoreLocalVarIndex(saved);
        return Type.VOID;
    }

    /**
     * 将栈顶值转换为 int
     */
    private static void emitToInt(Type t, MethodVisitor mv) {
        if (t == Type.I || t == Type.Z) return;
        if (t == Type.D) { mv.visitInsn(D2I); return; }
        if (t == Type.J) { mv.visitInsn(L2I); return; }
        if (t == Type.F) { mv.visitInsn(F2I); return; }
        // Object → Number.intValue()
        mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
    }

    /**
     * 完成循环体，递增循环变量，跳回循环头
     */
    private void finishLoopBodyWithInc(Type bodyType, MethodVisitor mv, CodeContext ctx, int loopVar, Label loopStart, Label loopEnd) {
        if (bodyType != Type.VOID) {
            if (bodyType == Type.D || bodyType == Type.J) {
                mv.visitInsn(POP2);
            } else {
                mv.visitInsn(POP);
            }
        }
        mv.visitIincInsn(loopVar, 1);
        mv.visitJumpInsn(GOTO, loopStart);
        mv.visitLabel(loopEnd);
        ctx.exitLoop();
    }

    /**
     * 通用 Iterator 循环路径
     */
    private Type generateIteratorLoop(ForExpression result, CodeContext ctx, MethodVisitor mv, Evaluator<ParseResult> collectionEval, Evaluator<ParseResult> bodyEval) {
        int saved = ctx.getLocalVarIndex();
        int iteratorVar = ctx.allocateLocalVar(Type.OBJECT);
        int variablesMapVar = ctx.allocateLocalVar(Type.OBJECT);

        // 创建标签用于跳转
        Label whileStart = new Label();
        Label whileEnd = new Label();

        // 评估集合表达式并创建迭代器
        Type ct = collectionEval.generateBytecode(result.getCollection(), ctx, mv);
        if (ct == Type.VOID) {
            throw new VoidError("Void type is not allowed for for loop collection");
        }
        boxing(ct, mv);
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "createIterator", "(" + Type.OBJECT + ")" + ITERATOR, false);
        mv.visitVarInsn(ASTORE, iteratorVar);

        // 将 result.getVariables() 转换为 Map
        Instructions.emitVariablePositionMap(mv, result.getVariables());
        mv.visitVarInsn(ASTORE, variablesMapVar);

        // 注册循环上下文：break 跳到 whileEnd，continue 跳到 whileStart
        ctx.enterLoop(whileEnd, whileStart);
        // while 循环开始标签
        mv.visitLabel(whileStart);

        // 检查条件：iterator.hasNext()
        mv.visitVarInsn(ALOAD, iteratorVar);
        mv.visitMethodInsn(INVOKEINTERFACE, ITERATOR.getPath(), "hasNext", "()Z", true);
        mv.visitJumpInsn(IFEQ, whileEnd); // 如果没有更多元素，跳转到结束

        // 获取下一个元素
        mv.visitVarInsn(ALOAD, iteratorVar);
        mv.visitMethodInsn(INVOKEINTERFACE, ITERATOR.getPath(), "next", "()" + Type.OBJECT, true);

        Map<String, Integer> variables = result.getVariables();
        if (variables.size() == 1) {
            // 单变量：内联赋值，避免 destructure 跨作用域类型冲突
            Map.Entry<String, Integer> entry = variables.entrySet().iterator().next();
            int varPos = entry.getValue();
            Type varType = ctx.getVariableType(varPos);
            if (ctx.isEnvFreeMode()) {
                // Env-free 模式：直接存入 JVM 局部变量
                int jvmSlot = ctx.getJvmSlot(varPos);
                if (varType.isPrimitive()) {
                    mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                    emitUnboxAndStoreJvm(varType, jvmSlot, mv);
                } else {
                    mv.visitVarInsn(ASTORE, jvmSlot);
                }
            } else {
                Instructions.loadEnvironment(mv, ctx);
                mv.visitInsn(SWAP);
                mv.visitLdcInsn(varPos);
                mv.visitInsn(SWAP);
                if (varType.isPrimitive()) {
                    // 拆箱并存入原始槽位
                    mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                    emitSetLocalPrimitive(varType, mv);
                } else {
                    // 存入引用槽位
                    mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", "(I" + Type.OBJECT + ")V", false);
                }
            }
        } else {
            // 多变量：使用 destructure
            mv.visitVarInsn(ALOAD, 0); // this
            mv.visitInsn(SWAP);
            mv.visitVarInsn(ALOAD, variablesMapVar);
            mv.visitInsn(SWAP);
            mv.visitMethodInsn(
                    INVOKESTATIC,
                    Intrinsics.TYPE.getPath(),
                    "destructure",
                    "(" + RuntimeScriptBase.TYPE + MAP + Type.OBJECT + ")V", false);
        }

        // 执行循环体
        // break 和 continue 语句会直接生成跳转指令
        Type bodyType = bodyEval.generateBytecode(result.getBody(), ctx, mv);
        finishLoopBody(bodyType, mv, ctx, whileStart, whileEnd);
        ctx.restoreLocalVarIndex(saved);
        return Type.VOID;
    }

    private static final Type ITERATOR = new Type(Iterator.class);
    private static final Type MAP = new Type(Map.class);

    /**
     * 生成原始类型设置字节码
     * 栈输入: [env, pos, Number]
     * 栈输出: []
     */
    private void emitSetLocalPrimitive(Type type, MethodVisitor mv) {
        String methodName;
        String desc;
        switch (type.getDescriptor()) {
            case "I":
            case "Z":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
                methodName = "setLocalInt";
                desc = "(II)V";
                break;
            case "J":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "longValue", "()J", false);
                methodName = "setLocalLong";
                desc = "(IJ)V";
                break;
            case "D":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "doubleValue", "()D", false);
                methodName = "setLocalDouble";
                desc = "(ID)V";
                break;
            case "F":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "floatValue", "()F", false);
                methodName = "setLocalFloat";
                desc = "(IF)V";
                break;
            default:
                // 非数字原始类型，回退到 Object
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", "(I" + Type.OBJECT + ")V", false);
                return;
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), methodName, desc, false);
    }

    /**
     * Env-free 模式：拆箱 Number 并存入 JVM 局部变量
     * 栈输入: [Number]
     * 栈输出: []
     */
    private void emitUnboxAndStoreJvm(Type type, int jvmSlot, MethodVisitor mv) {
        switch (type.getDescriptor()) {
            case "I":
            case "Z":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
                mv.visitVarInsn(ISTORE, jvmSlot);
                break;
            case "J":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "longValue", "()J", false);
                mv.visitVarInsn(LSTORE, jvmSlot);
                break;
            case "D":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "doubleValue", "()D", false);
                mv.visitVarInsn(DSTORE, jvmSlot);
                break;
            case "F":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "floatValue", "()F", false);
                mv.visitVarInsn(FSTORE, jvmSlot);
                break;
        }
    }

    @Override
    public void analyzeTypes(ForExpression result, TypeAnalyzer analyzer) {
        analyzer.analyzeNode(result.getCollection());
        // 推断集合元素类型并记录变量类型
        Type collectionType = analyzer.inferType(result.getCollection());
        Type elementType = collectionType.getElementType();
        if (elementType != null) {
            // 循环变量类型由集合决定，强制覆盖（避免与同 position 的其他变量合并）
            for (Integer pos : result.getVariables().values()) {
                analyzer.forceType(pos, elementType);
            }
        }
        analyzer.analyzeNode(result.getBody());
    }
}
