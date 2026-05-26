package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.FunctionClassEmitter;
import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.parser.expression.IndexAccessExpression;
import org.tabooproject.fluxon.parser.expression.RangeExpression;
import org.tabooproject.fluxon.parser.expression.ReferenceExpression;
import org.tabooproject.fluxon.parser.expression.TransparentExpression;
import org.tabooproject.fluxon.parser.expression.literal.Identifier;
import org.tabooproject.fluxon.parser.expression.literal.Literal;
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
        if (args.length != definition.getParameters().size()) {
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
                    Instructions.emitPrimitiveConversion(argType, expectedType, mv);
                } else {
                    Instructions.emitUnbox(mv, expectedType);
                }
            } else if (argType.isPrimitive()) {
                Instructions.emitBox(mv, argType);
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
        return canInlineExpression(definition.getBody(), InlineMode.DIRECT);
    }

    /**
     * 循环缓存只接受不会在中途抛出常规运行时异常的内联表达式，否则异常被外层 catch 捕获时会丢失已发生的写入。
     */
    public static boolean canInlineCacheSafeExpression(FunctionCallExpression expr, CodeContext ctx) {
        FunctionDefinition definition = findDefinition(expr, ctx);
        if (!canUseDirectInvoke(definition)) return false;
        if (expr.getArguments().length != definition.getParameters().size()) return false;
        return canInlineExpression(definition.getBody(), InlineMode.CACHE_SAFE);
    }

    /**
     * 尝试把纯表达式用户函数直接展开到调用点。
     * 参数先写入临时槽位，保证实参与普通函数调用一样只求值一次。
     */
    private static Type tryEmitInlineExpression(FunctionDefinition definition, ParseResult[] args, CodeContext ctx, MethodVisitor mv) {
        if (!canInlineExpression(definition.getBody(), InlineMode.DIRECT)) return null;
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
                    Instructions.emitPrimitiveConversion(argType, localType, mv);
                } else {
                    Instructions.emitUnbox(mv, localType);
                }
            } else if (argType.isPrimitive()) {
                Instructions.emitBox(mv, argType);
            }
            int slot = ctx.allocateLocalVar(localType);
            Instructions.emitStoreLocal(mv, localType, slot);
            locals.put(position, new CodeContext.InlineLocalVariable(localType, slot));
            argIndex++;
        }
        ctx.enterInlineLocalVariableScope(locals);
        try {
            // 内联展开仍然保留函数体源码行号，运行时错误才能指向函数定义而不是调用点。
            Instructions.emitLineNumber(definition.getBody(), mv);
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
        if (!FunctionClassEmitter.canUseDirectReturnBody(definition.getBody())) return false;
        return FunctionClassEmitter.getDirectReturnType(definition) != Type.VOID;
    }

    private static boolean canInlineExpression(ParseResult node, InlineMode mode) {
        if (node == null) return true;
        if (node instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) node;
            if (mode == InlineMode.CACHE_SAFE && isMayThrowArithmetic(binary.getOperator().getType())) return false;
            return canInlineChildren(node, mode);
        }
        if (node instanceof RangeExpression) {
            if (mode == InlineMode.CACHE_SAFE) return false;
            return canInlineChildren(node, mode);
        }
        if (node instanceof IndexAccessExpression) {
            if (mode == InlineMode.CACHE_SAFE) return false;
            return canInlineChildren(node, mode);
        }
        if (node instanceof ReferenceExpression || node instanceof Identifier) {
            return true;
        }
        if (!(node instanceof Expression)) {
            return false;
        }
        if (node instanceof Literal) return true;
        if (!(node instanceof TransparentExpression)) return false;
        return canInlineChildren(node, mode);
    }

    private static boolean canInlineChildren(ParseResult node, InlineMode mode) {
        final boolean[] allowed = {true};
        node.forEachChild(child -> {
            if (allowed[0] && !canInlineExpression(child, mode)) {
                allowed[0] = false;
            }
        });
        return allowed[0];
    }

    private static boolean isMayThrowArithmetic(TokenType type) {
        return type == TokenType.DIVIDE || type == TokenType.MODULO;
    }

    private enum InlineMode {
        DIRECT,
        CACHE_SAFE
    }
}
