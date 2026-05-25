package org.tabooproject.fluxon.interpreter.evaluator.expr.funccall;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.Interpreter;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.bytecode.emitter.FunctionClassEmitter;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.FunctionCallExpression;
import org.tabooproject.fluxon.runtime.*;

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
        String ownerClass = ctx.getUserFunctionOwner(expr.getFunctionName());
        if (ownerClass == null) {
            return null;
        }
        String funcClass = ownerClass + expr.getFunctionName();
        mv.visitFieldInsn(GETSTATIC, ownerClass, expr.getFunctionName(), "L" + funcClass + ";");
        Instructions.loadEnvironment(mv, ctx);
        for (ParseResult arg : args) {
            Type argType = FunctionCallHandlers.emitArgExpression(arg, ctx, mv);
            if (argType.isPrimitive()) {
                FunctionCallHandlers.emitBox(argType, mv);
            }
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, funcClass, "callDirect", FunctionClassEmitter.getDirectCallDescriptor(definition), false);
        Type returnType = inferDirectReturnType(definition);
        if (returnType.isPrimitive()) {
            FunctionCallHandlers.emitUnbox(returnType, mv);
            return returnType;
        }
        return Type.OBJECT;
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

    private static Type inferDirectReturnType(FunctionDefinition definition) {
        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.initFromParameterTypes(definition.getParameterTypes());
        analyzer.analyzeNode(definition.getBody());
        return analyzer.inferType(definition.getBody());
    }
}
