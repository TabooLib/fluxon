package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.compiler.TypeAnalyzer;
import org.tabooproject.fluxon.interpreter.evaluator.expr.funccall.FunctionCallHandlers;
import org.tabooproject.fluxon.interpreter.bytecode.BytecodeGenerator;
import org.tabooproject.fluxon.interpreter.bytecode.CodeContext;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.definition.Definition;
import org.tabooproject.fluxon.parser.definition.FunctionDefinition;
import org.tabooproject.fluxon.parser.definition.LambdaFunctionDefinition;
import org.tabooproject.fluxon.parser.expression.AssignExpression;
import org.tabooproject.fluxon.parser.expression.EnvironmentBoundaryExpression;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Block;
import org.tabooproject.fluxon.parser.statement.ExpressionStatement;
import org.tabooproject.fluxon.parser.statement.ReturnStatement;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;

import java.util.*;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 函数类生成器
 * 生成实现 Function 接口的类（用户函数和 Lambda 共用）
 */
public class FunctionClassEmitter extends ClassEmitter {

    private static final Type MAP = new Type(Map.class);
    private static final Type LIST = new Type(List.class);
    private static final Type ARRAYS = new Type(Arrays.class);
    private static final Type ANNOTATION = new Type(Annotation.class);
    private static final Type COLLECTIONS = new Type(Collections.class);

    private final FunctionDefinition funcDef;
    private final String parentClassName;
    private final BytecodeGenerator generator;
    private final String fileName;
    private final String source;
    private final Set<Integer> assignedLocalPositions;

    /**
     * 构造函数类生成器
     *
     * @param funcDef         函数定义（可以是普通函数或 Lambda）
     * @param parentClassName 父类名（主类名或所属函数类名）
     * @param fileName        源文件名
     * @param source          源代码
     * @param generator       字节码生成器（用于委托表达式/语句生成）
     * @param classLoader     类加载器
     */
    public FunctionClassEmitter(FunctionDefinition funcDef, String parentClassName, String fileName, String source, BytecodeGenerator generator, ClassLoader classLoader) {
        super(parentClassName + funcDef.getName(), RuntimeScriptBase.TYPE.getPath(), new String[]{Function.TYPE.getPath()}, classLoader);
        this.funcDef = funcDef;
        this.parentClassName = parentClassName;
        this.generator = generator;
        this.fileName = fileName;
        this.source = source;
        this.assignedLocalPositions = collectAssignedLocalPositions(funcDef.getBody());
    }

    @Override
    public EmitResult emit() {
        List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>();
        CodeContext funcCtx = createFunctionCodeContext();
        CodeContext compiledCtx = funcCtx;
        boolean directCallMethod = canUseDirectCallMethod();
        // 类声明
        beginClass(ACC_PUBLIC, fileName);
        emitSourceMetadataFields(source, fileName);
        // 添加 parameters 和 annotations 静态字段
        emitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "parameters", MAP.getDescriptor(), null);
        emitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "annotations", LIST.getDescriptor(), null);
        if (!funcDef.getParameterTypes().isEmpty()) {
            emitField(ACC_PRIVATE | ACC_FINAL | ACC_STATIC, "signature", FunctionSignature.TYPE.getDescriptor(), null);
        }
        // 生成构造函数
        emitDefaultConstructor();
        // 实现 Function 接口方法
        emitFunctionInterfaceMethods(lambdaDefinitions, funcCtx, directCallMethod);
        if (directCallMethod) {
            compiledCtx = emitDirectCallMethod();
            lambdaDefinitions.addAll(compiledCtx.getLambdaDefinitions());
        }
        // 为此函数类的 lambda 创建静态字段
        List<LambdaFunctionDefinition> ownedLambdas = getOwnedLambdas(className, lambdaDefinitions);
        for (LambdaFunctionDefinition lambdaDef : ownedLambdas) {
            emitLambdaFieldDeclaration(lambdaDef);
        }
        // 声明编译期优化相关的静态字段
        emitCompiledFunctionFields(compiledCtx);
        // 生成静态初始化块
        emitStaticInit(ownedLambdas, compiledCtx);
        // 生成 clone 方法
        emitCloneMethod();
        return new EmitResult(endClass(), lambdaDefinitions);
    }

    private void emitFunctionInterfaceMethods(List<LambdaFunctionDefinition> lambdaDefinitions, CodeContext funcCtx, boolean directCallMethod) {
        emitGetNameMethod();
        emitGetNamespaceMethod();
        emitGetSignatureMethod();
        emitIsAsyncMethod();
        emitIsPrimarySyncMethod();
        emitGetAnnotationsMethod();
        emitCallMethod(lambdaDefinitions, funcCtx, directCallMethod);
    }

    private CodeContext createFunctionCodeContext() {
        CodeContext funcCtx = new CodeContext(className, RuntimeScriptBase.TYPE.getPath());
        funcCtx.setCurrentFunction(funcDef);
        // 传播定义列表和用户函数注册表，使函数体内可查询兄弟函数属性（如 async）并直接引用静态字段
        funcCtx.addDefinitions(generator.getDefinitions());
        for (Definition def : generator.getDefinitions()) {
            if (def instanceof FunctionDefinition) {
                FunctionDefinition fd = (FunctionDefinition) def;
                if (fd.isRegisterToRoot()) {
                    funcCtx.registerUserFunction(fd.getName(), parentClassName);
                }
            }
        }
        return funcCtx;
    }

    /**
     * 判断此函数是否可以生成直接调用方法。
     * 直接调用只覆盖同步表达式函数，显式 return 的块函数仍保留 FunctionContext 返回协议。
     */
    private boolean canUseDirectCallMethod() {
        if (!canUseEnvFreeMode()) return false;
        if (funcDef.isAsync() || funcDef.isPrimarySync()) return false;
        if (!canUseDirectReturnBody(funcDef.getBody())) return false;
        return getDirectReturnType(funcDef) != Type.VOID;
    }

    private void emitGetNameMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getName", "()" + STRING, null, null);
        mv.visitCode();
        mv.visitLdcInsn(funcDef.getName());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitGetNamespaceMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getNamespace", "()" + STRING, null, null);
        mv.visitCode();
        mv.visitInsn(ACONST_NULL);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitGetSignatureMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getSignature", "()" + FunctionSignature.TYPE.getDescriptor(), null, null);
        mv.visitCode();
        Map<Integer, Class<?>> parameterTypes = funcDef.getParameterTypes();
        if (parameterTypes.isEmpty()) {
            mv.visitInsn(ACONST_NULL);
        } else {
            mv.visitFieldInsn(GETSTATIC, className, "signature", FunctionSignature.TYPE.getDescriptor());
        }
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitIsAsyncMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isAsync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isAsync() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitIsPrimarySyncMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "isPrimarySync", "()Z", null, null);
        mv.visitCode();
        mv.visitInsn(funcDef.isPrimarySync() ? ICONST_1 : ICONST_0);
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitGetAnnotationsMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "getAnnotations", "()" + LIST, null, null);
        mv.visitCode();
        mv.visitFieldInsn(GETSTATIC, className, "annotations", LIST.getDescriptor());
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitCallMethod(List<LambdaFunctionDefinition> lambdaDefinitions, CodeContext funcCtx, boolean directCallMethod) {
        // 生成 Function.call(FunctionContext) 方法（void 返回）
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "(" + FunctionContext.TYPE + ")V", null, null);
        mv.visitCode();
        if (directCallMethod) {
            emitCallDirectBridge(mv, funcCtx);
            mv.visitMaxs(0, funcCtx.getLocalVarIndex() + 1);
            mv.visitEnd();
            return;
        }
        // 初始化代码上下文，预留 slot 0 (this) 和 slot 1 (FunctionContext 参数)
        reserveReceiverAndArgumentSlots(funcCtx);
        funcCtx.setTypeAnalyzer(createFunctionTypeAnalyzer());
        emitContextPoolLocal(mv, funcCtx);
        // 绑定参数到环境
        boolean envFree = canUseEnvFreeMode();
        if (envFree) {
            funcCtx.enableEnvFreeMode(funcDef.getLocalVariables().size());
            emitParameterBindingEnvFree(mv, funcCtx);
        } else {
            emitParameterBinding(mv, funcCtx);
        }
        // 生成函数体字节码
        emitFunctionBody(mv, funcCtx);
        mv.visitMaxs(0, funcCtx.getLocalVarIndex() + 1);
        mv.visitEnd();
        // 收集函数体中发现的 Lambda 定义
        lambdaDefinitions.addAll(funcCtx.getLambdaDefinitions());
    }

    private CodeContext emitDirectCallMethod() {
        String descriptor = getDirectCallDescriptor(funcDef);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "callDirect", descriptor, null, null);
        mv.visitCode();
        CodeContext directCtx = createFunctionCodeContext();
        reserveReceiverAndArgumentSlots(directCtx);
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            directCtx.allocateLocalVar(getDirectParameterType(funcDef, entry.getValue()));
        }
        directCtx.setTypeAnalyzer(createFunctionTypeAnalyzer());
        directCtx.enableEnvFreeMode(funcDef.getLocalVariables().size());
        directCtx.setExpectedReturnType(Object.class);
        emitLocalPoolLocal(mv, directCtx);
        if (!requiresIsolatedEnvironment(funcDef.getBody())) {
            directCtx.setEnvironmentLocalSlot(1);
        } else {
            emitDirectChildEnvironment(mv, directCtx);
        }
        emitDirectParameterBinding(mv, directCtx);
        emitDirectFunctionBody(mv, directCtx);
        mv.visitMaxs(0, directCtx.getLocalVarIndex() + 1);
        mv.visitEnd();
        return directCtx;
    }

    private void emitCallDirectBridge(MethodVisitor mv, CodeContext funcCtx) {
        // 直连函数的通用入口只负责协议转换，函数体统一落在 callDirect，避免生成两份业务字节码。
        reserveReceiverAndArgumentSlots(funcCtx);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getEnvironment", "()" + Environment.TYPE.getDescriptor(), false);
        emitDirectBridgeArguments(mv);
        mv.visitMethodInsn(INVOKEVIRTUAL, className, "callDirect", getDirectCallDescriptor(funcDef), false);
        emitDirectBridgeReturn(mv, funcCtx);
        mv.visitInsn(RETURN);
    }

    private void emitDirectBridgeArguments(MethodVisitor mv) {
        int argIndex = 0;
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            Type type = getDirectParameterType(funcDef, entry.getValue());
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(argIndex);
            if (type == Type.I) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsInt", "(" + I + ")" + I, false);
            } else if (type == Type.Z) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsBoolean", "(" + I + ")" + Z, false);
            } else if (type == Type.J) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsLong", "(" + I + ")" + J, false);
            } else if (type == Type.D) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsDouble", "(" + I + ")" + D, false);
            } else if (type == Type.F) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsFloat", "(" + I + ")" + F, false);
            } else {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArgBoxed", "(" + I + ")" + OBJECT, false);
            }
            argIndex++;
        }
    }

    private void emitDirectBridgeReturn(MethodVisitor mv, CodeContext funcCtx) {
        Type returnType = getDirectReturnType(funcDef);
        int returnSlot = funcCtx.allocateLocalVar(returnType);
        Instructions.emitStoreLocal(mv, returnType, returnSlot);
        mv.visitVarInsn(ALOAD, 1);
        Instructions.emitLoadLocal(mv, returnType, returnSlot);
        if (returnType.isPrimitive()) {
            Instructions.emitSetReturnPrimitive(mv, returnType);
            return;
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnRef", "(" + OBJECT + ")V", false);
    }

    private void reserveReceiverAndArgumentSlots(CodeContext funcCtx) {
        funcCtx.allocateLocalVar(Type.OBJECT);  // slot 0: this
        funcCtx.allocateLocalVar(Type.OBJECT);  // slot 1: FunctionContext 或 Environment 参数
    }

    private TypeAnalyzer createFunctionTypeAnalyzer() {
        // 对函数体进行类型分析
        TypeAnalyzer typeAnalyzer = new TypeAnalyzer();
        // 从参数类型注解初始化变量类型
        typeAnalyzer.initFromParameterTypes(funcDef.getParameterTypes());
        typeAnalyzer.analyzeNode(funcDef.getBody());
        // Lambda 函数：位置 >= 自身局部变量数的变量是从父作用域捕获的，必须用引用类型
        if (funcDef instanceof LambdaFunctionDefinition) {
            int ownLocalCount = funcDef.getLocalVariables().size();
            for (int pos : new HashSet<>(typeAnalyzer.getVariableTypes().keySet())) {
                if (pos >= ownLocalCount) {
                    typeAnalyzer.markCaptured(pos);
                }
            }
        }
        return typeAnalyzer;
    }

    private void emitContextPoolLocal(MethodVisitor mv, CodeContext funcCtx) {
        // 从 FunctionContext 获取 pool 并存入局部变量（避免重复 ThreadLocal.get()）
        mv.visitVarInsn(ALOAD, 1);  // load FunctionContext
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getPool", "()" + FunctionContextPool.TYPE.getDescriptor(), false);
        storePoolLocal(mv, funcCtx);
    }

    private void emitLocalPoolLocal(MethodVisitor mv, CodeContext funcCtx) {
        // callDirect 没有 FunctionContext，只能取线程本地 pool 来复用临时参数数组。
        mv.visitMethodInsn(INVOKESTATIC, FunctionContextPool.TYPE.getPath(), "local", "()" + FunctionContextPool.TYPE.getDescriptor(), false);
        storePoolLocal(mv, funcCtx);
    }

    private void storePoolLocal(MethodVisitor mv, CodeContext funcCtx) {
        int poolSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, poolSlot);
        funcCtx.setPoolLocalSlot(poolSlot);
    }

    /**
     * 判断函数体是否必须拥有独立 Environment。
     * 只有会观察 Environment 身份、切换 target 或延后执行的节点需要隔离；普通表达式递归检查子节点。
     */
    private boolean requiresIsolatedEnvironment(ParseResult node) {
        return requiresIsolatedEnvironment(node, Collections.newSetFromMap(new IdentityHashMap<>()));
    }

    private boolean requiresIsolatedEnvironment(ParseResult node, Set<Object> visited) {
        if (node == null || !visited.add(node)) return false;
        if (node instanceof EnvironmentBoundaryExpression) return true;
        final boolean[] isolated = {false};
        node.forEachChild(child -> {
            if (!isolated[0] && requiresIsolatedEnvironment(child, visited)) {
                isolated[0] = true;
            }
        });
        return isolated[0];
    }

    private void emitDirectChildEnvironment(MethodVisitor mv, CodeContext funcCtx) {
        mv.visitTypeInsn(NEW, Environment.TYPE.getPath());
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKESPECIAL, Environment.TYPE.getPath(), "<init>", "(" + Environment.TYPE + I + ")V", false);
        storeGeneratedEnvironment(mv, funcCtx);
    }

    /**
     * 复用 FunctionContext 里的调用方 Environment，并同步 RuntimeScriptBase.environment 字段。
     */
    private void emitCallerEnvironment(MethodVisitor mv, CodeContext funcCtx) {
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getEnvironment", "()" + Environment.TYPE.getDescriptor(), false);
        storeGeneratedEnvironment(mv, funcCtx);
    }

    /**
     * 保存栈顶 Environment，并同步 CodeContext 与 RuntimeScriptBase.environment。
     */
    private int storeGeneratedEnvironment(MethodVisitor mv, CodeContext funcCtx) {
        int envSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, envSlot);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, envSlot);
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());
        funcCtx.setEnvironmentLocalSlot(envSlot);
        return envSlot;
    }

    private void emitDirectParameterBinding(MethodVisitor mv, CodeContext funcCtx) {
        Map<Integer, Class<?>> parameterTypes = funcDef.getParameterTypes();
        int argSlot = 2;
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            int varPosition = entry.getValue();
            Class<?> declaredType = parameterTypes.get(varPosition);
            boolean splitCaptureCell = canSplitCaptureCell(varPosition);
            Type directType = getDirectParameterType(funcDef, varPosition);
            Type type = splitCaptureCell ? directType : funcCtx.isLocalCapturedByChild(varPosition) ? Type.OBJECT : declaredType != null ? Type.fromClass(declaredType) : Type.OBJECT;
            int jvmSlot = funcCtx.allocateLocalVar(type);
            funcCtx.mapVarToJvmSlot(varPosition, jvmSlot);
            if (splitCaptureCell) {
                Instructions.emitLoadLocal(mv, directType, argSlot);
                Instructions.emitStoreLocal(mv, type, jvmSlot);
                int cellSlot = funcCtx.allocateLocalVar(Type.OBJECT);
                funcCtx.mapVarToCaptureCellSlot(varPosition, cellSlot);
                Instructions.emitLoadLocal(mv, directType, argSlot);
                if (directType.isPrimitive()) {
                    Instructions.emitBoxing(mv, directType);
                }
                emitNewCaptureCell(mv);
                mv.visitVarInsn(ASTORE, cellSlot);
                argSlot += getJvmSlotSize(directType);
                continue;
            }
            if (funcCtx.isLocalCapturedByChild(varPosition)) {
                Instructions.emitLoadLocal(mv, directType, argSlot);
                if (directType.isPrimitive()) {
                    Instructions.emitBoxing(mv, directType);
                }
                emitNewCaptureCell(mv);
                mv.visitVarInsn(ASTORE, jvmSlot);
                argSlot += getJvmSlotSize(directType);
                continue;
            }
            if (directType.isPrimitive()) {
                Instructions.emitLoadLocal(mv, directType, argSlot);
                Instructions.emitStoreLocal(mv, directType, jvmSlot);
                argSlot += getJvmSlotSize(directType);
                continue;
            }
            mv.visitVarInsn(ALOAD, argSlot);
            if (type == Type.I) {
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
                mv.visitVarInsn(ISTORE, jvmSlot);
            } else if (type == Type.Z) {
                Instructions.emitUnboxBooleanCompatible(mv);
                mv.visitVarInsn(ISTORE, jvmSlot);
            } else if (type == Type.J) {
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "longValue", "()J", false);
                mv.visitVarInsn(LSTORE, jvmSlot);
            } else if (type == Type.D) {
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "doubleValue", "()D", false);
                mv.visitVarInsn(DSTORE, jvmSlot);
            } else if (type == Type.F) {
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "floatValue", "()F", false);
                mv.visitVarInsn(FSTORE, jvmSlot);
            } else {
                mv.visitVarInsn(ASTORE, jvmSlot);
            }
            argSlot++;
        }
        emitEnvFreeLocalDefaults(mv, funcCtx);
    }

    private void emitEnvFreeLocalDefaults(MethodVisitor mv, CodeContext funcCtx) {
        Set<Integer> parameterSlots = new HashSet<>(funcDef.getParameters().values());
        // 为非参数的局部变量分配 JVM 槽位并生成默认值初始化
        // 必须在方法入口处初始化所有局部变量，否则当首次赋值出现在分支内部时，
        // 另一条分支路径上该槽位仍为 top，JVM 验证器会拒绝后续的 ALOAD/ILOAD
        for (int pos = 0; pos < funcDef.getLocalVariables().size(); pos++) {
            if (parameterSlots.contains(pos) || funcCtx.hasJvmSlot(pos)) {
                continue;
            }
            emitEnvFreeLocalDefault(mv, funcCtx, pos);
        }
    }

    private void emitEnvFreeLocalDefault(MethodVisitor mv, CodeContext funcCtx, int pos) {
        if (funcCtx.isLocalCapturedByChild(pos)) {
            int jvmSlot = funcCtx.allocateLocalVar(Type.OBJECT);
            funcCtx.mapVarToJvmSlot(pos, jvmSlot);
            mv.visitInsn(ACONST_NULL);
            emitNewCaptureCell(mv);
            mv.visitVarInsn(ASTORE, jvmSlot);
            return;
        }
        Type varType = funcCtx.getVariableType(pos);
        if (varType == null || !varType.isPrimitive()) varType = Type.OBJECT;
        int jvmSlot = funcCtx.allocateLocalVar(varType);
        funcCtx.mapVarToJvmSlot(pos, jvmSlot);
        if (varType == Type.I || varType == Type.Z) {
            mv.visitInsn(ICONST_0);
        } else if (varType == Type.J) {
            mv.visitInsn(LCONST_0);
        } else if (varType == Type.D) {
            mv.visitInsn(DCONST_0);
        } else if (varType == Type.F) {
            mv.visitInsn(FCONST_0);
        } else {
            mv.visitInsn(ACONST_NULL);
        }
        Instructions.emitStoreLocal(mv, varType, jvmSlot);
    }

    private void emitNewCaptureCell(MethodVisitor mv) {
        mv.visitTypeInsn(NEW, CaptureCell.TYPE.getPath());
        mv.visitInsn(DUP_X1);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKESPECIAL, CaptureCell.TYPE.getPath(), "<init>", "(" + OBJECT + ")V", false);
    }

    private void emitDirectFunctionBody(MethodVisitor mv, CodeContext funcCtx) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Type directReturnType = getDirectReturnType(funcDef);
        mv.visitTryCatchBlock(start, end, handler, FluxonRuntimeError.class.getName().replace('.', '/'));
        mv.visitLabel(start);
        Type returnType = emitDirectReturnValue(mv, funcCtx);
        if (directReturnType.isPrimitive()) {
            // primitive 表达式函数直接返回原始值，避免 callDirect 内装箱、调用点再拆箱。
            if (returnType.isPrimitive()) {
                FunctionCallHandlers.emitPrimitiveConversion(returnType, directReturnType, mv);
            } else {
                FunctionCallHandlers.emitUnbox(directReturnType, mv);
            }
            mv.visitLabel(end);
            mv.visitInsn(returnOpcode(directReturnType));
        } else {
            if (returnType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
            } else if (returnType.isPrimitive()) {
                Instructions.emitBoxing(mv, returnType);
            }
            mv.visitLabel(end);
            mv.visitInsn(ARETURN);
        }
        mv.visitLabel(handler);
        emitRuntimeErrorHandler(mv, funcCtx);
    }

    private Type emitDirectReturnValue(MethodVisitor mv, CodeContext funcCtx) {
        ParseResult body = funcDef.getBody();
        if (body instanceof Block) {
            ParseResult[] statements = ((Block) body).getStatements();
            for (int i = 0; i < statements.length - 1; i++) {
                ParseResult statement = statements[i];
                Instructions.emitLineNumber(statement, mv);
                Type type = generator.generateStatementBytecode((Statement) statement, funcCtx, mv);
                if (type != Type.VOID) {
                    mv.visitInsn((type == Type.J || type == Type.D) ? POP2 : POP);
                }
            }
            ExpressionStatement last = (ExpressionStatement) statements[statements.length - 1];
            Instructions.emitLineNumber(last, mv);
            return generator.generateExpressionBytecode((Expression) last.getExpression(), funcCtx, mv);
        }
        Instructions.emitLineNumber(body, mv);
        return generator.generateExpressionBytecode((Expression) body, funcCtx, mv);
    }

    public static String getDirectCallDescriptor(FunctionDefinition definition) {
        StringBuilder descriptor = new StringBuilder("(");
        descriptor.append(Environment.TYPE);
        for (Map.Entry<String, Integer> entry : definition.getParameters().entrySet()) {
            descriptor.append(getDirectParameterType(definition, entry.getValue()));
        }
        descriptor.append(")");
        descriptor.append(getDirectReturnType(definition));
        return descriptor.toString();
    }

    public static Type getDirectReturnType(FunctionDefinition definition) {
        TypeAnalyzer analyzer = new TypeAnalyzer();
        analyzer.initFromParameterTypes(definition.getParameterTypes());
        analyzer.analyzeNode(definition.getBody());
        ParseResult returnNode = getDirectReturnNode(definition.getBody());
        if (returnNode == null) return Type.VOID;
        Type returnType = analyzer.inferType(returnNode);
        if (returnType.isPrimitive()) return returnType;
        return Type.OBJECT;
    }

    public static boolean canUseDirectReturnBody(ParseResult body) {
        return getDirectReturnNode(body) != null && !containsReturnStatement(body);
    }

    public static ParseResult getDirectReturnNode(ParseResult body) {
        if (body instanceof Expression) return body;
        if (!(body instanceof Block)) return null;
        ParseResult[] statements = ((Block) body).getStatements();
        if (statements.length == 0) return null;
        ParseResult last = statements[statements.length - 1];
        if (!(last instanceof ExpressionStatement)) return null;
        return ((ExpressionStatement) last).getExpression();
    }

    private static boolean containsReturnStatement(ParseResult node) {
        if (node instanceof ReturnStatement) return true;
        if (!(node instanceof Block)) return false;
        for (ParseResult statement : ((Block) node).getStatements()) {
            if (containsReturnStatement(statement)) return true;
        }
        return false;
    }

    private static Set<Integer> collectAssignedLocalPositions(ParseResult body) {
        Set<Integer> positions = new HashSet<>();
        collectAssignedLocalPositions(body, Collections.newSetFromMap(new IdentityHashMap<>()), positions);
        return positions;
    }

    /**
     * 只读捕获变量可以拆成本地热读槽和闭包 cell；出现赋值时必须保守回到单 cell。
     */
    private static void collectAssignedLocalPositions(Object value, Set<Object> visited, Set<Integer> positions) {
        if (!(value instanceof ParseResult) || !visited.add(value)) return;
        ParseResult node = (ParseResult) value;
        if (node instanceof AssignExpression) {
            int position = ((AssignExpression) node).getPosition();
            if (position >= 0) {
                positions.add(position);
            }
        }
        node.forEachChild(child -> collectAssignedLocalPositions(child, visited, positions));
    }

    public static Type getDirectParameterType(FunctionDefinition definition, int varPosition) {
        Class<?> declaredType = definition.getParameterTypes().get(varPosition);
        if (declaredType == null) {
            return Type.OBJECT;
        }
        Type type = Type.fromClass(declaredType);
        if (type.isPrimitive()) {
            return type;
        }
        return Type.OBJECT;
    }

    public static int getJvmSlotSize(Type type) {
        if (type == Type.J || type == Type.D) {
            return 2;
        }
        return 1;
    }

    public static int returnOpcode(Type type) {
        if (type == Type.VOID) return RETURN;
        if (type == Type.J) return LRETURN;
        if (type == Type.F) return FRETURN;
        if (type == Type.D) return DRETURN;
        if (type.isPrimitive()) return IRETURN;
        return ARETURN;
    }

    /**
     * 创建子 Environment 并存入局部变量和 this.environment 字段
     * 隔离 target 字段，防止多线程共享根 Environment 时 ContextCall（::）的 target 互相覆盖
     *
     * @return 子 Environment 的 JVM 局部变量槽位
     */
    private int emitChildEnvironment(MethodVisitor mv, CodeContext funcCtx, int localVarCount) {
        mv.visitTypeInsn(NEW, Environment.TYPE.getPath());
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getEnvironment", "()" + Environment.TYPE.getDescriptor(), false);
        mv.visitLdcInsn(localVarCount);
        mv.visitMethodInsn(INVOKESPECIAL, Environment.TYPE.getPath(), "<init>", "(" + Environment.TYPE + I + ")V", false);
        return storeGeneratedEnvironment(mv, funcCtx);
    }

    private void emitParameterBinding(MethodVisitor mv, CodeContext funcCtx) {
        Map<Integer, Class<?>> parameterTypes = funcDef.getParameterTypes();
        int envSlot = emitChildEnvironment(mv, funcCtx, funcDef.getLocalVariables().size());
        // Lambda 闭包捕获偏移
        if (funcDef instanceof LambdaFunctionDefinition) {
            int captureOffset = ((LambdaFunctionDefinition) funcDef).getCaptureOffset();
            if (captureOffset > 0) {
                mv.visitVarInsn(ALOAD, envSlot);
                mv.visitLdcInsn(captureOffset);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setCaptureOffset", "(" + I + ")V", false);
                mv.visitVarInsn(ALOAD, envSlot);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getCaptureFrame", "()" + CaptureFrame.TYPE, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setCaptureFrame", "(" + CaptureFrame.TYPE + ")V", false);
            }
        }
        // 内联绑定每个参数（编译时确定类型，无运行时分支）
        int argIndex = 0;
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            String name = entry.getKey();
            int slot = entry.getValue();
            Class<?> declaredType = parameterTypes.get(argIndex);
            Type type = declaredType != null ? Type.fromClass(declaredType) : Type.OBJECT;
            // env.setLocalXxx(slot, context.getXxx(argIndex))
            mv.visitVarInsn(ALOAD, envSlot);
            mv.visitLdcInsn(slot);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(argIndex);
            if (type == Type.I || type == Type.Z) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsInt", "(" + I + ")" + I, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalInt", "(" + I + I + ")V", false);
            } else if (type == Type.J) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsLong", "(" + I + ")" + J, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalLong", "(" + I + J + ")V", false);
            } else if (type == Type.D) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsDouble", "(" + I + ")" + D, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalDouble", "(" + I + D + ")V", false);
            } else if (type == Type.F) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsFloat", "(" + I + ")" + F, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalFloat", "(" + I + F + ")V", false);
            } else {
                // 无类型声明时使用 getArgBoxed，根据 argTypes 自动选择 refs 或 primitives
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArgBoxed", "(" + I + ")" + OBJECT, false);
                mv.visitMethodInsn(INVOKEVIRTUAL, Environment.TYPE.getPath(), "setLocalRef", "(" + I + OBJECT + ")V", false);
            }
            argIndex++;
        }
    }

    /**
     * 判断此函数是否可以使用 env-free 模式
     * 资格条件由 FunctionDefinition 统一维护，保持解释执行和编译执行一致。
     */
    private boolean canUseEnvFreeMode() {
        if (funcDef instanceof LambdaFunctionDefinition && ((LambdaFunctionDefinition) funcDef).getCaptureOffset() > 0) {
            // 编译路径的嵌套捕获需要完整 cell 转发，未完成前不能让捕获型 Lambda 走半套 env-free。
            return false;
        }
        return funcDef.canUseEnvFreeLocals();
    }

    /**
     * Env-free 模式的参数绑定：将参数直接存入 JVM 局部变量，跳过 Environment 变量存储
     * 创建轻量级子 Environment（localVariables=0）仅用于隔离 target 字段
     */
    private void emitParameterBindingEnvFree(MethodVisitor mv, CodeContext funcCtx) {
        Map<Integer, Class<?>> parameterTypes = funcDef.getParameterTypes();
        if (!requiresIsolatedEnvironment(funcDef.getBody())) {
            // 纯表达式 env-free 调用没有 target 隔离需求，复用调用方环境可省掉每次回调的子 Environment 分配。
            emitCallerEnvironment(mv, funcCtx);
        } else {
            emitChildEnvironment(mv, funcCtx, 0);
        }
        // 从 FunctionContext 读取参数，直接存入 JVM 局部变量
        int argIndex = 0;
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            int varPosition = entry.getValue();
            Class<?> declaredType = parameterTypes.get(argIndex);
            Type type = funcCtx.isLocalCapturedByChild(varPosition) ? Type.OBJECT : declaredType != null ? Type.fromClass(declaredType) : Type.OBJECT;
            int jvmSlot = funcCtx.allocateLocalVar(type);
            funcCtx.mapVarToJvmSlot(varPosition, jvmSlot);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(argIndex);
            if (funcCtx.isLocalCapturedByChild(varPosition)) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArgBoxed", "(" + I + ")" + OBJECT, false);
                emitNewCaptureCell(mv);
                mv.visitVarInsn(ASTORE, jvmSlot);
                argIndex++;
                continue;
            }
            if (type == Type.I || type == Type.Z) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsInt", "(" + I + ")" + I, false);
                mv.visitVarInsn(ISTORE, jvmSlot);
            } else if (type == Type.J) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsLong", "(" + I + ")" + J, false);
                mv.visitVarInsn(LSTORE, jvmSlot);
            } else if (type == Type.D) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsDouble", "(" + I + ")" + D, false);
                mv.visitVarInsn(DSTORE, jvmSlot);
            } else if (type == Type.F) {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getAsFloat", "(" + I + ")" + F, false);
                mv.visitVarInsn(FSTORE, jvmSlot);
            } else {
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getArgBoxed", "(" + I + ")" + OBJECT, false);
                mv.visitVarInsn(ASTORE, jvmSlot);
            }
            argIndex++;
        }
        emitEnvFreeLocalDefaults(mv, funcCtx);
    }

    private boolean canSplitCaptureCell(int position) {
        return funcDef.hasVariablesCapturedByChildren()
                && funcDef.isLocalCapturedByChild(position)
                && !assignedLocalPositions.contains(position);
    }

    private void emitFunctionBody(MethodVisitor mv, CodeContext funcCtx) {
        // 设置 try-catch 块捕获运行时错误
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Label functionExit = new Label();
        funcCtx.setFunctionExitLabel(functionExit);
        mv.visitTryCatchBlock(start, end, handler, FluxonRuntimeError.class.getName().replace('.', '/'));
        mv.visitLabel(start);
        // 空函数体不会生成实际指令，保留一条 no-op 避免异常表出现空区间。
        mv.visitInsn(NOP);
        // 根据函数体类型生成字节码
        Type returnType;
        if (funcDef.getBody() instanceof Statement) {
            Instructions.emitLineNumber(funcDef.getBody(), mv);
            returnType = generator.generateStatementBytecode((Statement) funcDef.getBody(), funcCtx, mv);
        } else {
            Instructions.emitLineNumber(funcDef.getBody(), mv);
            returnType = generator.generateExpressionBytecode((Expression) funcDef.getBody(), funcCtx, mv);
        }
        // 若有返回值则写入 context
        if (returnType != VOID) {
            if (returnType.isPrimitive()) {
                // 使用类型化的 setReturnXxx 避免装箱
                mv.visitVarInsn(ALOAD, 1);
                if (returnType == Type.D || returnType == Type.J) {
                    // wide 类型: value(2 slots), ctx -> ctx, value
                    mv.visitInsn(DUP_X2);
                    mv.visitInsn(POP);
                } else {
                    mv.visitInsn(SWAP);
                }
                Instructions.emitSetReturnPrimitive(mv, returnType);
            } else {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnRef", "(" + OBJECT + ")V", false);
            }
        }
        // 正常返回路径
        mv.visitLabel(end);
        mv.visitLabel(functionExit);
        mv.visitInsn(RETURN);
        // 异常处理：附加源码位置信息后重新抛出
        mv.visitLabel(handler);
        emitRuntimeErrorHandler(mv, funcCtx);
    }

    private void emitRuntimeErrorHandler(MethodVisitor mv, CodeContext funcCtx) {
        int exceptionSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, exceptionSlot);
        mv.visitVarInsn(ALOAD, exceptionSlot);
        loadSourceMetadata(mv);
        mv.visitLdcInsn(externalName(className));
        mv.visitMethodInsn(INVOKESTATIC, RuntimeScriptBase.TYPE.getPath(), "attachRuntimeError", "(" + FluxonRuntimeError.TYPE + STRING + STRING + STRING + ")" + FluxonRuntimeError.TYPE, false);
        mv.visitInsn(ATHROW);
    }

    private void emitStaticInit(List<LambdaFunctionDefinition> ownedLambdas, CodeContext funcCtx) {
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        // 初始化参数位置映射表: Map<String, Integer>
        Instructions.emitVariablePositionMap(mv, funcDef.getParameters());
        mv.visitFieldInsn(PUTSTATIC, className, "parameters", MAP.getDescriptor());
        // 初始化注解列表
        emitAnnotationsInit(mv);
        // 初始化所有 Lambda 实例字段
        for (LambdaFunctionDefinition lambdaDef : ownedLambdas) {
            emitLambdaInitialization(mv, lambdaDef, className);
        }
        // 初始化编译期优化相关的静态数组
        emitCompiledFunctionInits(mv, funcCtx, className);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void emitAnnotationsInit(MethodVisitor mv) {
        List<Annotation> annotations = funcDef.getAnnotations();
        if (annotations.isEmpty()) {
            // 无注解时使用空列表
            mv.visitMethodInsn(INVOKESTATIC, COLLECTIONS.getPath(), "emptyList", "()" + LIST, false);
        } else {
            // 创建注解数组并逐个初始化
            mv.visitIntInsn(BIPUSH, annotations.size());
            mv.visitTypeInsn(ANEWARRAY, ANNOTATION.getPath());
            for (int i = 0; i < annotations.size(); i++) {
                Annotation annotation = annotations.get(i);
                mv.visitInsn(DUP);           // 复制数组引用
                mv.visitIntInsn(BIPUSH, i);  // 数组索引
                Instructions.emitAnnotation(mv, annotation);
                mv.visitInsn(AASTORE);       // 存入数组
            }
            // 转换为不可变列表
            mv.visitMethodInsn(INVOKESTATIC, ARRAYS.getPath(), "asList", "([" + OBJECT + ")" + LIST, false);
        }
        mv.visitFieldInsn(PUTSTATIC, className, "annotations", LIST.getDescriptor());
    }

    public String getParentClassName() {
        return parentClassName;
    }
}
