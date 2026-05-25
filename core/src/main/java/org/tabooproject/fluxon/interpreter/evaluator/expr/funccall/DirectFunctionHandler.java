package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.FunctionClassEmitter;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ElvisExpression;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.expression.GroupingExpression;
import org.tabooproject.fluxon.parser.expression.IfExpression;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.ListExpression;
import org.tabooproject.fluxon.parser.expression.LogicalExpression;
import org.tabooproject.fluxon.parser.expression.MapExpression;
import org.tabooproject.fluxon.parser.expression.RangeExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.TernaryExpression;
import org.tabooproject.fluxon.parser.expression.UnaryExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.runtime.*;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;

/**
 * 用户定义函数直接引用处理器
 * 编译期已知函数为同一编译单元的用户定义函数时，
 * 直接通过静态字段引用调用 prepareCallDirect，跳过运行时名称查找。
 *
 * @author sky
 */
public class DirectFunctionHandler implements FunctionCallHandler {

    public static final DirectFunctionHandler INSTANCE = new DirectFunctionHandler();

    private DirectFunctionHandler() {
    }

    /**
     * 尝试生成用户函数直接调用。
     * 仅覆盖同步表达式函数，块函数继续走 FunctionContext 返回协议，避免 return 语义分叉。
     */
    public static Type tryEmitDirectInvoke(FunctionCallExpression expr, ParseResult[] args, CodeContext ctx, MethodVisitor mv) {
        FunctionDefinition definition = findDefinition(expr, ctx);
        if (!canUseDirectInvoke(definition)) {
            return null;
        }
        Type inlineResult = tryEmitInlineExpression(definition, args, ctx, mv);
        if (inlineResult != null) {
            return inlineResult;
        }
        String ownerClass = ctx.getUserFunctionOwner(expr.getFunctionName());
        if (ownerClass == null) {
            return null;
        }
        String funcClass = ownerClass + expr.getFunctionName();
        mv.visitFieldInsn(GETSTATIC, ownerClass, expr.getFunctionName(), "L" + funcClass + ";");
        Instructions.loadEnvironment(mv, ctx);
        int argIndex = 0;
        for (Map.Entry<String, Integer> entry : definition.getParameters().entrySet()) {
            ParseResult arg = args[argIndex];
            Type expectedType = FunctionClassEmitter.getDirectParameterType(definition, entry.getValue());
            Type argType = FunctionCallHandlers.emitArgExpression(arg, ctx, mv);
            if (expectedType.isPrimitive()) {
                if (argType.isPrimitive()) {
                    FunctionCallHandlers.emitPrimitiveConversion(argType, expectedType, mv);
                } else {
                    FunctionCallHandlers.emitUnbox(expectedType, mv);
                }
            } else if (argType.isPrimitive()) {
                FunctionCallHandlers.emitBox(argType, mv);
            }
            argIndex++;
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, funcClass, "callDirect", FunctionClassEmitter.getDirectCallDescriptor(definition), false);
        Type returnType = FunctionClassEmitter.getDirectReturnType(definition);
        if (returnType.isPrimitive()) {
            return returnType;
        }
        return Type.OBJECT;
    }

    /**
     * 判断函数调用是否会被展开为纯表达式。
     * 循环缓存规划器依赖这个判断识别可内联调用，避免把纯函数调用误判为外部观察点。
     */
    public static boolean canInlinePureExpression(FunctionCallExpression expr, CodeContext ctx) {
        FunctionDefinition definition = findDefinition(expr, ctx);
        if (!canUseDirectInvoke(definition)) return false;
        if (expr.getArguments().length != definition.getParameters().size()) return false;
        return canInlineExpression(definition.getBody());
    }

    /**
     * 尝试把纯表达式用户函数直接展开到调用点。
     * 参数先写入临时槽位，保证实参与普通函数调用一样只求值一次。
     */
    private static Type tryEmitInlineExpression(FunctionDefinition definition, ParseResult[] args, CodeContext ctx, MethodVisitor mv) {
        if (!canInlineExpression(definition.getBody())) return null;
        if (args.length != definition.getParameters().size()) return null;
        Map<Integer, CodeContext.InlineLocalVariable> locals = new LinkedHashMap<>();
        int argIndex = 0;
        for (Map.Entry<String, Integer> entry : definition.getParameters().entrySet()) {
            int position = entry.getValue();
            Type expectedType = FunctionClassEmitter.getDirectParameterType(definition, position);
            Type argType = FunctionCallHandlers.emitArgExpression(args[argIndex], ctx, mv);
            Type localType = expectedType.isPrimitive() ? expectedType : Type.OBJECT;
            if (localType.isPrimitive()) {
                if (argType.isPrimitive()) {
                    FunctionCallHandlers.emitPrimitiveConversion(argType, localType, mv);
                } else {
                    FunctionCallHandlers.emitUnbox(localType, mv);
                }
            } else if (argType.isPrimitive()) {
                FunctionCallHandlers.emitBox(argType, mv);
            }
            int slot = ctx.allocateLocalVar(localType);
            Instructions.emitStoreLocal(mv, localType, slot);
            locals.put(position, new CodeContext.InlineLocalVariable(localType, slot));
            argIndex++;
        }
        ctx.enterInlineLocalVariableScope(locals);
        try {
            return ctx.getEvaluator(definition.getBody()).generateBytecode(definition.getBody(), ctx, mv);
        } finally {
            ctx.exitInlineLocalVariableScope();
        }
    }

    @Override
    public FunctionContext<?> prepareCall(Interpreter interpreter, FunctionCallExpression expr, int argCount) {
        // 解释模式走动态路径
        return DynamicResolutionHandler.INSTANCE.prepareCall(interpreter, expr, argCount);
    }

    @Override
    public Type finishCall(Interpreter interpreter, FunctionCallExpression expr, FunctionContext<?> ctx) {
        return FunctionCallHandlers.executeSync(interpreter, ctx);
    }

    @Override
    public PrepareCallResult generatePrepareCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, int argCount) {
        String funcName = expr.getFunctionName();
        String ownerClass = ctx.getUserFunctionOwner(funcName);
        String funcClass = ownerClass + funcName;
        // 直接引用静态字段，跳过 Environment 名称查找
        return FunctionCallHandlers.emitPrepareCallDirect(ctx, mv, argCount,
                () -> mv.visitFieldInsn(GETSTATIC, ownerClass, funcName, "L" + funcClass + ";"));
    }

    @Override
    public Type generateFinishCall(FunctionCallExpression expr, CodeContext ctx, MethodVisitor mv, PrepareCallResult prepareResult, Type returnType) {
        mv.visitVarInsn(ALOAD, prepareResult.ctxSlot);
        boolean knownSync = isKnownSync(expr, ctx);
        FunctionCallHandlers.emitFinishCall(returnType, knownSync, mv);
        return knownSync ? returnType : Type.OBJECT;
    }

    /**
     * 查找用户定义函数的定义，判断是否确定为同步调用
     */
    private static boolean isKnownSync(FunctionCallExpression expr, CodeContext ctx) {
        FunctionDefinition fd = findDefinition(expr, ctx);
        if (fd != null) {
            return !fd.isAsync() && !fd.isPrimarySync();
        }
        return true;
    }

    private static FunctionDefinition findDefinition(FunctionCallExpression expr, CodeContext ctx) {
        String funcName = expr.getFunctionName();
        for (Definition def : ctx.getDefinitions()) {
            if (def instanceof FunctionDefinition) {
                FunctionDefinition fd = (FunctionDefinition) def;
                if (fd.getName().equals(funcName)) {
                    return fd;
                }
            }
        }
        return null;
    }

    private static boolean canUseDirectInvoke(FunctionDefinition definition) {
        if (definition == null) return false;
        if (definition instanceof LambdaFunctionDefinition) return false;
        if (definition.isAsync() || definition.isPrimarySync()) return false;
        if (definition.hasVariablesCapturedByChildren()) return false;
        return definition.getBody().getType() != ParseResult.ResultType.STATEMENT;
    }

    private static boolean canInlineExpression(ParseResult node) {
        if (node == null) return true;
        if (node instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) node;
            return canInlineExpression(binary.getLeft()) && canInlineExpression(binary.getRight());
        }
        if (node instanceof LogicalExpression) {
            LogicalExpression logical = (LogicalExpression) node;
            return canInlineExpression(logical.getLeft()) && canInlineExpression(logical.getRight());
        }
        if (node instanceof UnaryExpression) {
            return canInlineExpression(((UnaryExpression) node).getRight());
        }
        if (node instanceof GroupingExpression) {
            return canInlineExpression(((GroupingExpression) node).getExpression());
        }
        if (node instanceof IfExpression) {
            IfExpression ifExpression = (IfExpression) node;
            return canInlineExpression(ifExpression.getCondition())
                    && canInlineExpression(ifExpression.getThenBranch())
                    && canInlineExpression(ifExpression.getElseBranch());
        }
        if (node instanceof TernaryExpression) {
            TernaryExpression ternary = (TernaryExpression) node;
            return canInlineExpression(ternary.getCondition())
                    && canInlineExpression(ternary.getTrueExpr())
                    && canInlineExpression(ternary.getFalseExpr());
        }
        if (node instanceof ElvisExpression) {
            ElvisExpression elvis = (ElvisExpression) node;
            return canInlineExpression(elvis.getCondition()) && canInlineExpression(elvis.getAlternative());
        }
        if (node instanceof ListExpression) {
            for (ParseResult element : ((ListExpression) node).getElements()) {
                if (!canInlineExpression(element)) return false;
            }
            return true;
        }
        if (node instanceof MapExpression) {
            for (MapExpression.MapEntry entry : ((MapExpression) node).getEntries()) {
                if (!canInlineExpression(entry.getKey())) return false;
                if (!canInlineExpression(entry.getValue())) return false;
            }
            return true;
        }
        if (node instanceof RangeExpression) {
            RangeExpression range = (RangeExpression) node;
            return canInlineExpression(range.getStart()) && canInlineExpression(range.getEnd());
        }
        if (node instanceof IndexAccessExpression) {
            IndexAccessExpression index = (IndexAccessExpression) node;
            if (!canInlineExpression(index.getTarget())) return false;
            for (ParseResult item : index.getIndices()) {
                if (!canInlineExpression(item)) return false;
            }
            return true;
        }
        if (node instanceof ReferenceExpression || node instanceof Identifier) {
            return true;
        }
        if (!(node instanceof Expression)) {
            return false;
        }
        return node.getClass().getSimpleName().endsWith("Literal");
    }
}
