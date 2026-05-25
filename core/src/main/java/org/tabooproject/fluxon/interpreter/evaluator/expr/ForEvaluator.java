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

import java.util.HashMap;
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

        Type rangeLoopType = tryGenerateIntRangeLoop(result, ctx, mv, bodyEval);
        if (rangeLoopType != null) {
            return rangeLoopType;
        }

        // 分配局部变量存储迭代器和变量Map
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
                    emitUnboxNumber(varType, mv);
                    ReferenceEvaluator.emitSetLocal(varType, mv);
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
     * 为单变量 int range 生成计数循环。
     * 动态起止值、解构和非 range 集合保持通用 Iterator 路径，避免改变运行时错误边界。
     */
    private Type tryGenerateIntRangeLoop(ForExpression result, CodeContext ctx, MethodVisitor mv, Evaluator<ParseResult> bodyEval) {
        if (!(result.getCollection() instanceof RangeExpression) || result.getVariables().size() != 1) {
            return null;
        }
        RangeExpression range = (RangeExpression) result.getCollection();
        TypeAnalyzer analyzer = ctx.getTypeAnalyzer();
        Type startType = analyzer != null ? analyzer.inferType(range.getStart()) : Type.OBJECT;
        Type endType = analyzer != null ? analyzer.inferType(range.getEnd()) : Type.OBJECT;
        if (!isIntRangeEndpoint(startType) || !isIntRangeEndpoint(endType)) {
            return null;
        }
        Evaluator<ParseResult> startEval = ctx.getEvaluator(range.getStart());
        Evaluator<ParseResult> endEval = ctx.getEvaluator(range.getEnd());
        if (startEval == null || endEval == null) {
            throw new EvaluatorNotFoundError("No evaluator found for range endpoint");
        }

        int saved = ctx.getLocalVarIndex();
        Integer constantStart = analyzer != null ? analyzer.inferIntConstant(range.getStart()) : null;
        Integer constantEnd = analyzer != null ? analyzer.inferIntConstant(range.getEnd()) : null;
        if (constantStart != null && constantEnd != null) {
            return emitConstantIntRangeLoop(result, range, constantStart, constantEnd, ctx, mv, bodyEval, saved);
        }
        int startVar = ctx.allocateLocalVar(Type.I);
        int endVar = ctx.allocateLocalVar(Type.I);
        int stepVar = ctx.allocateLocalVar(Type.I);
        int loopVar = ctx.allocateLocalVar(Type.I);
        Label descending = new Label();
        Label afterStep = new Label();
        Label condition = new Label();
        Label negativeCondition = new Label();
        Label body = new Label();
        Label increment = new Label();
        Label loopEnd = new Label();

        emitIntEndpoint(range.getStart(), startEval, ctx, mv);
        mv.visitVarInsn(ISTORE, startVar);
        emitIntEndpoint(range.getEnd(), endEval, ctx, mv);
        mv.visitVarInsn(ISTORE, endVar);

        mv.visitVarInsn(ILOAD, startVar);
        mv.visitVarInsn(ILOAD, endVar);
        mv.visitJumpInsn(IF_ICMPGT, descending);
        mv.visitInsn(ICONST_1);
        mv.visitJumpInsn(GOTO, afterStep);
        mv.visitLabel(descending);
        mv.visitInsn(ICONST_M1);
        mv.visitLabel(afterStep);
        mv.visitVarInsn(ISTORE, stepVar);

        if (!range.isInclusive()) {
            mv.visitVarInsn(ILOAD, endVar);
            mv.visitVarInsn(ILOAD, stepVar);
            mv.visitInsn(ISUB);
            mv.visitVarInsn(ISTORE, endVar);
        }

        mv.visitVarInsn(ILOAD, startVar);
        mv.visitVarInsn(ISTORE, loopVar);
        RangeLoopCacheState cacheState = createRangeLoopCacheState(result, loopVar, ctx);
        emitRangeLoopEnter(cacheState, ctx, mv);
        ctx.enterLoop(loopEnd, increment);
        mv.visitLabel(condition);
        emitDynamicRangeCondition(loopVar, endVar, stepVar, negativeCondition, body, loopEnd, mv);
        mv.visitLabel(body);
        emitRangeLoopBody(result, bodyEval, cacheState, ctx, mv);
        mv.visitLabel(increment);
        mv.visitVarInsn(ILOAD, loopVar);
        mv.visitVarInsn(ILOAD, stepVar);
        mv.visitInsn(IADD);
        mv.visitVarInsn(ISTORE, loopVar);
        mv.visitJumpInsn(GOTO, condition);
        emitRangeLoopExit(loopEnd, cacheState, ctx, mv);
        ctx.restoreLocalVarIndex(saved);
        return Type.VOID;
    }

    private Type emitConstantIntRangeLoop(
            ForExpression result,
            RangeExpression range,
            int start,
            int end,
            CodeContext ctx,
            MethodVisitor mv,
            Evaluator<ParseResult> bodyEval,
            int saved
    ) {
        int step = start <= end ? 1 : -1;
        int effectiveEnd = range.isInclusive() ? end : end - step;
        int loopVar = ctx.allocateLocalVar(Type.I);
        RangeLoopCacheState cacheState = createRangeLoopCacheState(result, loopVar, ctx);
        emitRangeLoopEnter(cacheState, ctx, mv);
        Label condition = new Label();
        Label increment = new Label();
        Label loopEnd = new Label();
        mv.visitLdcInsn(start);
        mv.visitVarInsn(ISTORE, loopVar);
        ctx.enterLoop(loopEnd, increment);
        mv.visitLabel(condition);
        mv.visitVarInsn(ILOAD, loopVar);
        mv.visitLdcInsn(effectiveEnd);
        mv.visitJumpInsn(step > 0 ? IF_ICMPGT : IF_ICMPLT, loopEnd);
        emitRangeLoopBody(result, bodyEval, cacheState, ctx, mv);
        mv.visitLabel(increment);
        mv.visitIincInsn(loopVar, step);
        mv.visitJumpInsn(GOTO, condition);
        emitRangeLoopExit(loopEnd, cacheState, ctx, mv);
        ctx.restoreLocalVarIndex(saved);
        return Type.VOID;
    }

    private void emitDynamicRangeCondition(int loopVar, int endVar, int stepVar, Label negativeCondition, Label body, Label loopEnd, MethodVisitor mv) {
        mv.visitVarInsn(ILOAD, stepVar);
        mv.visitJumpInsn(IFLE, negativeCondition);
        mv.visitVarInsn(ILOAD, loopVar);
        mv.visitVarInsn(ILOAD, endVar);
        mv.visitJumpInsn(IF_ICMPGT, loopEnd);
        mv.visitJumpInsn(GOTO, body);
        mv.visitLabel(negativeCondition);
        mv.visitVarInsn(ILOAD, loopVar);
        mv.visitVarInsn(ILOAD, endVar);
        mv.visitJumpInsn(IF_ICMPLT, loopEnd);
    }

    private RangeLoopCacheState createRangeLoopCacheState(ForExpression result, int loopVar, CodeContext ctx) {
        Map.Entry<String, Integer> entry = result.getVariables().entrySet().iterator().next();
        int varPos = entry.getValue();
        Type varType = ctx.getVariableType(varPos);
        LoopRootCachePlanner.Plan rootCachePlan = LoopRootCachePlanner.planForBody(result.getBody(), ctx);
        Map<Integer, CodeContext.InlineLocalVariable> loopLocals = null;
        int loopValueWriteBackVar = -1;
        int loopExecutedVar = -1;
        if (LoopRootCachePlanner.canUseLocalVariableCache(result.getBody(), varPos, ctx)) {
            // 纯循环体内没有外部观察点，循环变量可延迟到退出循环时一次性写回。
            loopLocals = new HashMap<>();
            loopLocals.put(varPos, new CodeContext.InlineLocalVariable(varType, loopVar));
            loopValueWriteBackVar = ctx.allocateLocalVar(Type.I);
            loopExecutedVar = ctx.allocateLocalVar(Type.I);
        }
        return new RangeLoopCacheState(varPos, varType, loopVar, rootCachePlan, loopLocals, loopValueWriteBackVar, loopExecutedVar);
    }

    private void emitRangeLoopEnter(RangeLoopCacheState state, CodeContext ctx, MethodVisitor mv) {
        if (state.rootCachePlan != null) {
            LoopRootCachePlanner.emitLoadCaches(state.rootCachePlan, ctx, mv);
        }
        // 顶层常量只允许初始化当前循环 cache，进入循环后不能继续影响嵌套循环。
        ctx.clearRootConstantValues();
        if (state.loopExecutedVar >= 0) {
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, state.loopValueWriteBackVar);
            mv.visitInsn(ICONST_0);
            mv.visitVarInsn(ISTORE, state.loopExecutedVar);
        }
    }

    private void emitRangeLoopBody(ForExpression result, Evaluator<ParseResult> bodyEval, RangeLoopCacheState state, CodeContext ctx, MethodVisitor mv) {
        if (state.loopLocals != null) {
            mv.visitVarInsn(ILOAD, state.loopVar);
            mv.visitVarInsn(ISTORE, state.loopValueWriteBackVar);
            mv.visitInsn(ICONST_1);
            mv.visitVarInsn(ISTORE, state.loopExecutedVar);
        } else {
            emitStoreRangeLoopVariable(state.varPos, state.varType, state.loopVar, ctx, mv);
        }
        if (state.rootCachePlan != null) {
            ctx.enterRootVariableCacheScope(state.rootCachePlan.caches);
        }
        if (state.loopLocals != null) {
            ctx.enterInlineLocalVariableScope(state.loopLocals);
        }
        Type bodyType = bodyEval.generateBytecode(result.getBody(), ctx, mv);
        if (state.loopLocals != null) {
            ctx.exitInlineLocalVariableScope();
        }
        if (state.rootCachePlan != null) {
            ctx.exitRootVariableCacheScope();
        }
        if (bodyType != Type.VOID) {
            mv.visitInsn((bodyType == Type.J || bodyType == Type.D) ? POP2 : POP);
        }
    }

    private void emitRangeLoopExit(Label loopEnd, RangeLoopCacheState state, CodeContext ctx, MethodVisitor mv) {
        mv.visitLabel(loopEnd);
        ctx.exitLoop();
        if (state.loopLocals != null) {
            Label skipLoopValueWriteBack = new Label();
            mv.visitVarInsn(ILOAD, state.loopExecutedVar);
            mv.visitJumpInsn(IFEQ, skipLoopValueWriteBack);
            emitStoreRangeLoopVariable(state.varPos, state.varType, state.loopValueWriteBackVar, ctx, mv);
            mv.visitLabel(skipLoopValueWriteBack);
        }
        if (state.rootCachePlan != null) {
            LoopRootCachePlanner.emitWriteBackCaches(state.rootCachePlan, ctx, mv);
        }
    }

    private static final class RangeLoopCacheState {
        private final int varPos;
        private final Type varType;
        private final int loopVar;
        private final LoopRootCachePlanner.Plan rootCachePlan;
        private final Map<Integer, CodeContext.InlineLocalVariable> loopLocals;
        private final int loopValueWriteBackVar;
        private final int loopExecutedVar;

        private RangeLoopCacheState(
                int varPos,
                Type varType,
                int loopVar,
                LoopRootCachePlanner.Plan rootCachePlan,
                Map<Integer, CodeContext.InlineLocalVariable> loopLocals,
                int loopValueWriteBackVar,
                int loopExecutedVar
        ) {
            this.varPos = varPos;
            this.varType = varType;
            this.loopVar = loopVar;
            this.rootCachePlan = rootCachePlan;
            this.loopLocals = loopLocals;
            this.loopValueWriteBackVar = loopValueWriteBackVar;
            this.loopExecutedVar = loopExecutedVar;
        }
    }

    private static boolean isIntRangeEndpoint(Type type) {
        return type == Type.I || type == Type.J || type == Type.F || type == Type.D
                || type == Type.INT || type == Type.LONG || type == Type.FLOAT || type == Type.DOUBLE
                || type == Type.NUMBER;
    }

    private static void emitIntEndpoint(ParseResult endpoint, Evaluator<ParseResult> evaluator, CodeContext ctx, MethodVisitor mv) {
        Type type = evaluator.generateBytecode(endpoint, ctx, mv);
        if (type == Type.VOID) {
            throw new VoidError("Void type is not allowed for range endpoint");
        }
        if (!type.isPrimitive()) {
            mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
            return;
        }
        if (type == Type.J) {
            mv.visitInsn(L2I);
        } else if (type == Type.F) {
            mv.visitInsn(F2I);
        } else if (type == Type.D) {
            mv.visitInsn(D2I);
        }
    }

    private static void emitStoreRangeLoopVariable(int varPos, Type varType, int loopVar, CodeContext ctx, MethodVisitor mv) {
        if (ctx.isEnvFreeMode()) {
            int jvmSlot = ctx.getJvmSlot(varPos);
            emitLoadRangeLoopValue(varType, loopVar, mv);
            mv.visitVarInsn(storeOpcode(varType.isPrimitive() ? varType : Type.OBJECT), jvmSlot);
            return;
        }
        Instructions.loadEnvironment(mv, ctx);
        mv.visitLdcInsn(varPos);
        emitLoadRangeLoopValue(varType, loopVar, mv);
        if (varType.isPrimitive()) {
            ReferenceEvaluator.emitSetLocal(varType, mv);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", "(I" + Type.OBJECT + ")V", false);
        }
    }

    private static void emitLoadRangeLoopValue(Type varType, int loopVar, MethodVisitor mv) {
        mv.visitVarInsn(ILOAD, loopVar);
        if (varType == Type.J) {
            mv.visitInsn(I2L);
        } else if (varType == Type.F) {
            mv.visitInsn(I2F);
        } else if (varType == Type.D) {
            mv.visitInsn(I2D);
        } else if (!varType.isPrimitive()) {
            boxing(Type.I, mv);
        }
    }

    private static void emitUnboxNumber(Type type, MethodVisitor mv) {
        switch (type.getDescriptor()) {
            case "I":
            case "Z":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
                break;
            case "J":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "longValue", "()J", false);
                break;
            case "D":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "doubleValue", "()D", false);
                break;
            case "F":
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "floatValue", "()F", false);
                break;
            default:
                break;
        }
    }

    /**
     * Env-free 模式：拆箱 Number 并存入 JVM 局部变量
     * 栈输入: [Number]
     * 栈输出: []
     */
    private void emitUnboxAndStoreJvm(Type type, int jvmSlot, MethodVisitor mv) {
        emitUnboxNumber(type, mv);
        Instructions.emitStoreLocal(mv, type, jvmSlot);
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
