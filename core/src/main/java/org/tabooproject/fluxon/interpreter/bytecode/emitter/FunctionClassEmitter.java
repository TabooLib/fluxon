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
import org.tabooproject.fluxon.parser.expression.BinaryExpression;
import org.tabooproject.fluxon.parser.expression.ElvisExpression;
import org.tabooproject.fluxon.parser.expression.Expression;
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
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.*;
import org.tabooproject.fluxon.runtime.error.FluxonRuntimeError;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

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
    }

    @Override
    public EmitResult emit() {
        List<LambdaFunctionDefinition> lambdaDefinitions = new ArrayList<>();
        CodeContext funcCtx = new CodeContext(className, RuntimeScriptBase.TYPE.getPath());
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
        emitFunctionInterfaceMethods(lambdaDefinitions, funcCtx);
        if (canUseDirectCallMethod()) {
            emitDirectCallMethod();
        }
        // 为此函数类的 lambda 创建静态字段
        List<LambdaFunctionDefinition> ownedLambdas = getOwnedLambdas(className, lambdaDefinitions);
        for (LambdaFunctionDefinition lambdaDef : ownedLambdas) {
            emitLambdaFieldDeclaration(lambdaDef);
        }
        // 声明编译期优化相关的静态字段
        emitCompiledFunctionFields(funcCtx);
        // 生成静态初始化块
        emitStaticInit(ownedLambdas, funcCtx);
        // 生成 clone 方法
        emitCloneMethod();
        return new EmitResult(endClass(), lambdaDefinitions);
    }

    private void emitFunctionInterfaceMethods(List<LambdaFunctionDefinition> lambdaDefinitions, CodeContext funcCtx) {
        emitGetNameMethod();
        emitGetNamespaceMethod();
        emitGetSignatureMethod();
        emitIsAsyncMethod();
        emitIsPrimarySyncMethod();
        emitGetAnnotationsMethod();
        emitCallMethod(lambdaDefinitions, funcCtx);
    }

    /**
     * 判断此函数是否可以生成直接调用方法。
     * 直接调用只覆盖同步表达式函数，显式 return 的块函数仍保留 FunctionContext 返回协议。
     */
    private boolean canUseDirectCallMethod() {
        if (!canUseEnvFreeMode()) return false;
        if (funcDef.isAsync() || funcDef.isPrimarySync()) return false;
        return funcDef.getBody().getType() != ParseResult.ResultType.STATEMENT;
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

    private void emitCallMethod(List<LambdaFunctionDefinition> lambdaDefinitions, CodeContext funcCtx) {
        // 生成 Function.call(FunctionContext) 方法（void 返回）
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "(" + FunctionContext.TYPE + ")V", null, null);
        mv.visitCode();
        // 初始化代码上下文，预留 slot 0 (this) 和 slot 1 (FunctionContext 参数)
        funcCtx.allocateLocalVar(Type.OBJECT);  // slot 0: this
        funcCtx.allocateLocalVar(Type.OBJECT);  // slot 1: FunctionContext
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
        funcCtx.setTypeAnalyzer(typeAnalyzer);
        // 从 FunctionContext 获取 pool 并存入局部变量（避免重复 ThreadLocal.get()）
        mv.visitVarInsn(ALOAD, 1);  // load FunctionContext
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "getPool", "()" + FunctionContextPool.TYPE.getDescriptor(), false);
        int poolSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, poolSlot);
        funcCtx.setPoolLocalSlot(poolSlot);
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

    private void emitDirectCallMethod() {
        String descriptor = getDirectCallDescriptor(funcDef);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "callDirect", descriptor, null, null);
        mv.visitCode();
        CodeContext directCtx = new CodeContext(className, RuntimeScriptBase.TYPE.getPath());
        directCtx.addDefinitions(generator.getDefinitions());
        for (Definition def : generator.getDefinitions()) {
            if (def instanceof FunctionDefinition) {
                FunctionDefinition fd = (FunctionDefinition) def;
                if (fd.isRegisterToRoot()) {
                    directCtx.registerUserFunction(fd.getName(), parentClassName);
                }
            }
        }
        directCtx.allocateLocalVar(Type.OBJECT);
        directCtx.allocateLocalVar(Type.OBJECT);
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            directCtx.allocateLocalVar(getDirectParameterType(funcDef, entry.getValue()));
        }
        TypeAnalyzer typeAnalyzer = new TypeAnalyzer();
        typeAnalyzer.initFromParameterTypes(funcDef.getParameterTypes());
        typeAnalyzer.analyzeNode(funcDef.getBody());
        directCtx.setTypeAnalyzer(typeAnalyzer);
        directCtx.enableEnvFreeMode(funcDef.getLocalVariables().size());
        directCtx.setExpectedReturnType(Object.class);
        mv.visitMethodInsn(INVOKESTATIC, FunctionContextPool.TYPE.getPath(), "local", "()" + FunctionContextPool.TYPE.getDescriptor(), false);
        int poolSlot = directCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, poolSlot);
        directCtx.setPoolLocalSlot(poolSlot);
        if (canReuseCallerEnvironmentForDirectCall(funcDef.getBody())) {
            directCtx.setEnvironmentLocalSlot(1);
        } else {
            emitDirectChildEnvironment(mv, directCtx);
        }
        emitDirectParameterBinding(mv, directCtx);
        emitDirectFunctionBody(mv, directCtx);
        mv.visitMaxs(0, directCtx.getLocalVarIndex() + 1);
        mv.visitEnd();
    }

    /**
     * 纯表达式函数复用调用方 Environment，避免每次 callDirect 创建子 Environment。
     * 出现函数调用、上下文调用等可观察 Environment 身份或 target 的节点时保留隔离环境。
     */
    private boolean canReuseCallerEnvironmentForDirectCall(ParseResult node) {
        if (node == null) return true;
        if (node instanceof BinaryExpression) {
            BinaryExpression binary = (BinaryExpression) node;
            return canReuseCallerEnvironmentForDirectCall(binary.getLeft())
                    && canReuseCallerEnvironmentForDirectCall(binary.getRight());
        }
        if (node instanceof LogicalExpression) {
            LogicalExpression logical = (LogicalExpression) node;
            return canReuseCallerEnvironmentForDirectCall(logical.getLeft())
                    && canReuseCallerEnvironmentForDirectCall(logical.getRight());
        }
        if (node instanceof UnaryExpression) {
            return canReuseCallerEnvironmentForDirectCall(((UnaryExpression) node).getRight());
        }
        if (node instanceof GroupingExpression) {
            return canReuseCallerEnvironmentForDirectCall(((GroupingExpression) node).getExpression());
        }
        if (node instanceof IfExpression) {
            IfExpression ifExpression = (IfExpression) node;
            return canReuseCallerEnvironmentForDirectCall(ifExpression.getCondition())
                    && canReuseCallerEnvironmentForDirectCall(ifExpression.getThenBranch())
                    && canReuseCallerEnvironmentForDirectCall(ifExpression.getElseBranch());
        }
        if (node instanceof TernaryExpression) {
            TernaryExpression ternary = (TernaryExpression) node;
            return canReuseCallerEnvironmentForDirectCall(ternary.getCondition())
                    && canReuseCallerEnvironmentForDirectCall(ternary.getTrueExpr())
                    && canReuseCallerEnvironmentForDirectCall(ternary.getFalseExpr());
        }
        if (node instanceof ElvisExpression) {
            ElvisExpression elvis = (ElvisExpression) node;
            return canReuseCallerEnvironmentForDirectCall(elvis.getCondition())
                    && canReuseCallerEnvironmentForDirectCall(elvis.getAlternative());
        }
        if (node instanceof ListExpression) {
            for (ParseResult element : ((ListExpression) node).getElements()) {
                if (!canReuseCallerEnvironmentForDirectCall(element)) return false;
            }
            return true;
        }
        if (node instanceof MapExpression) {
            for (MapExpression.MapEntry entry : ((MapExpression) node).getEntries()) {
                if (!canReuseCallerEnvironmentForDirectCall(entry.getKey())) return false;
                if (!canReuseCallerEnvironmentForDirectCall(entry.getValue())) return false;
            }
            return true;
        }
        if (node instanceof RangeExpression) {
            RangeExpression range = (RangeExpression) node;
            return canReuseCallerEnvironmentForDirectCall(range.getStart())
                    && canReuseCallerEnvironmentForDirectCall(range.getEnd());
        }
        if (node instanceof IndexAccessExpression) {
            IndexAccessExpression index = (IndexAccessExpression) node;
            if (!canReuseCallerEnvironmentForDirectCall(index.getTarget())) return false;
            for (ParseResult item : index.getIndices()) {
                if (!canReuseCallerEnvironmentForDirectCall(item)) return false;
            }
            return true;
        }
        if (node instanceof ReferenceExpression || node instanceof Identifier) {
            return true;
        }
        return node.getClass().getSimpleName().endsWith("Literal");
    }

    private void emitDirectChildEnvironment(MethodVisitor mv, CodeContext funcCtx) {
        mv.visitTypeInsn(NEW, Environment.TYPE.getPath());
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKESPECIAL, Environment.TYPE.getPath(), "<init>", "(" + Environment.TYPE + I + ")V", false);
        int envSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, envSlot);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, envSlot);
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());
        funcCtx.setEnvironmentLocalSlot(envSlot);
    }

    private void emitDirectParameterBinding(MethodVisitor mv, CodeContext funcCtx) {
        Map<Integer, Class<?>> parameterTypes = funcDef.getParameterTypes();
        int argSlot = 2;
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            int varPosition = entry.getValue();
            Class<?> declaredType = parameterTypes.get(varPosition);
            Type type = declaredType != null ? Type.fromClass(declaredType) : Type.OBJECT;
            Type directType = getDirectParameterType(funcDef, varPosition);
            int jvmSlot = funcCtx.allocateLocalVar(type);
            funcCtx.mapVarToJvmSlot(varPosition, jvmSlot);
            if (directType.isPrimitive()) {
                emitLoadDirectParameter(mv, directType, argSlot);
                emitStoreDirectParameter(mv, directType, jvmSlot);
                argSlot += getJvmSlotSize(directType);
                continue;
            }
            mv.visitVarInsn(ALOAD, argSlot);
            if (type == Type.I) {
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
                mv.visitVarInsn(ISTORE, jvmSlot);
            } else if (type == Type.Z) {
                // 兼容旧 FunctionContext 协议：布尔既可能是 Boolean，也可能是 int bit。
                Label numberLabel = new Label();
                Label storeLabel = new Label();
                mv.visitInsn(DUP);
                mv.visitTypeInsn(INSTANCEOF, "java/lang/Boolean");
                mv.visitJumpInsn(IFEQ, numberLabel);
                mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                mv.visitJumpInsn(GOTO, storeLabel);
                mv.visitLabel(numberLabel);
                mv.visitTypeInsn(CHECKCAST, Type.NUMBER.getPath());
                mv.visitMethodInsn(INVOKEVIRTUAL, Type.NUMBER.getPath(), "intValue", "()I", false);
                mv.visitLabel(storeLabel);
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

    private static void emitLoadDirectParameter(MethodVisitor mv, Type type, int slot) {
        if (type == Type.J) {
            mv.visitVarInsn(LLOAD, slot);
        } else if (type == Type.D) {
            mv.visitVarInsn(DLOAD, slot);
        } else if (type == Type.F) {
            mv.visitVarInsn(FLOAD, slot);
        } else {
            mv.visitVarInsn(ILOAD, slot);
        }
    }

    private static void emitStoreDirectParameter(MethodVisitor mv, Type type, int slot) {
        if (type == Type.J) {
            mv.visitVarInsn(LSTORE, slot);
        } else if (type == Type.D) {
            mv.visitVarInsn(DSTORE, slot);
        } else if (type == Type.F) {
            mv.visitVarInsn(FSTORE, slot);
        } else {
            mv.visitVarInsn(ISTORE, slot);
        }
    }

    private void emitEnvFreeLocalDefaults(MethodVisitor mv, CodeContext funcCtx) {
        for (int pos = funcDef.getParameters().size(); pos < funcDef.getLocalVariables().size(); pos++) {
            Type varType = funcCtx.getVariableType(pos);
            if (varType == null || !varType.isPrimitive()) varType = Type.OBJECT;
            int jvmSlot = funcCtx.allocateLocalVar(varType);
            funcCtx.mapVarToJvmSlot(pos, jvmSlot);
            if (varType == Type.I || varType == Type.Z) {
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, jvmSlot);
            } else if (varType == Type.J) {
                mv.visitInsn(LCONST_0);
                mv.visitVarInsn(LSTORE, jvmSlot);
            } else if (varType == Type.D) {
                mv.visitInsn(DCONST_0);
                mv.visitVarInsn(DSTORE, jvmSlot);
            } else if (varType == Type.F) {
                mv.visitInsn(FCONST_0);
                mv.visitVarInsn(FSTORE, jvmSlot);
            } else {
                mv.visitInsn(ACONST_NULL);
                mv.visitVarInsn(ASTORE, jvmSlot);
            }
        }
    }

    private void emitDirectFunctionBody(MethodVisitor mv, CodeContext funcCtx) {
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
        Type directReturnType = getDirectReturnType(funcDef);
        mv.visitTryCatchBlock(start, end, handler, FluxonRuntimeError.class.getName().replace('.', '/'));
        mv.visitLabel(start);
        Instructions.emitLineNumber(funcDef.getBody(), mv);
        Type returnType = generator.generateExpressionBytecode((Expression) funcDef.getBody(), funcCtx, mv);
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
        int exceptionSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, exceptionSlot);
        mv.visitVarInsn(ALOAD, exceptionSlot);
        loadSourceMetadata(mv);
        mv.visitLdcInsn(externalName(className));
        mv.visitMethodInsn(INVOKESTATIC, RuntimeScriptBase.TYPE.getPath(), "attachRuntimeError", "(" + FluxonRuntimeError.TYPE + STRING + STRING + STRING + ")" + FluxonRuntimeError.TYPE, false);
        mv.visitInsn(ATHROW);
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
        Type returnType = analyzer.inferType(definition.getBody());
        if (returnType.isPrimitive()) return returnType;
        return Type.OBJECT;
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
        int envSlot = funcCtx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, envSlot);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, envSlot);
        mv.visitFieldInsn(PUTFIELD, className, "environment", Environment.TYPE.getDescriptor());
        funcCtx.setEnvironmentLocalSlot(envSlot);
        return envSlot;
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
     * 资格条件：非 Lambda，且局部变量未被子 Lambda 捕获
     */
    private boolean canUseEnvFreeMode() {
        if (funcDef instanceof LambdaFunctionDefinition) return false;
        return !funcDef.hasVariablesCapturedByChildren();
    }

    /**
     * Env-free 模式的参数绑定：将参数直接存入 JVM 局部变量，跳过 Environment 变量存储
     * 创建轻量级子 Environment（localVariables=0）仅用于隔离 target 字段
     */
    private void emitParameterBindingEnvFree(MethodVisitor mv, CodeContext funcCtx) {
        Map<Integer, Class<?>> parameterTypes = funcDef.getParameterTypes();
        emitChildEnvironment(mv, funcCtx, 0);
        // 从 FunctionContext 读取参数，直接存入 JVM 局部变量
        int argIndex = 0;
        for (Map.Entry<String, Integer> entry : funcDef.getParameters().entrySet()) {
            int varPosition = entry.getValue();
            Class<?> declaredType = parameterTypes.get(argIndex);
            Type type = declaredType != null ? Type.fromClass(declaredType) : Type.OBJECT;
            int jvmSlot = funcCtx.allocateLocalVar(type);
            funcCtx.mapVarToJvmSlot(varPosition, jvmSlot);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(argIndex);
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
        // 为非参数的局部变量分配 JVM 槽位并生成默认值初始化
        // 必须在方法入口处初始化所有局部变量，否则当首次赋值出现在分支内部时，
        // 另一条分支路径上该槽位仍为 top，JVM 验证器会拒绝后续的 ALOAD/ILOAD
        for (int pos = funcDef.getParameters().size(); pos < funcDef.getLocalVariables().size(); pos++) {
            Type varType = funcCtx.getVariableType(pos);
            if (varType == null || !varType.isPrimitive()) varType = Type.OBJECT;
            int jvmSlot = funcCtx.allocateLocalVar(varType);
            funcCtx.mapVarToJvmSlot(pos, jvmSlot);
            if (varType == Type.I || varType == Type.Z) {
                mv.visitInsn(ICONST_0);
                mv.visitVarInsn(ISTORE, jvmSlot);
            } else if (varType == Type.J) {
                mv.visitInsn(LCONST_0);
                mv.visitVarInsn(LSTORE, jvmSlot);
            } else if (varType == Type.D) {
                mv.visitInsn(DCONST_0);
                mv.visitVarInsn(DSTORE, jvmSlot);
            } else if (varType == Type.F) {
                mv.visitInsn(FCONST_0);
                mv.visitVarInsn(FSTORE, jvmSlot);
            } else {
                mv.visitInsn(ACONST_NULL);
                mv.visitVarInsn(ASTORE, jvmSlot);
            }
        }
    }

    private void emitFunctionBody(MethodVisitor mv, CodeContext funcCtx) {
        // 设置 try-catch 块捕获运行时错误
        Label start = new Label();
        Label end = new Label();
        Label handler = new Label();
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
                emitSetReturnPrimitive(returnType, mv);
            } else {
                mv.visitVarInsn(ALOAD, 1);
                mv.visitInsn(SWAP);
                mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), "setReturnRef", "(" + OBJECT + ")V", false);
            }
        }
        // 正常返回路径
        mv.visitLabel(end);
        mv.visitInsn(RETURN);
        // 异常处理：附加源码位置信息后重新抛出
        mv.visitLabel(handler);
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

    /**
     * 生成类型化的 setReturnXxx 调用，避免原始类型装箱
     */
    public static void emitSetReturnPrimitive(Type type, MethodVisitor mv) {
        String method;
        String desc;
        if (type == Type.I) {
            method = "setReturnInt";
            desc = "(I)V";
        } else if (type == Type.Z) {
            method = "setReturnBool";
            desc = "(Z)V";
        } else if (type == Type.J) {
            method = "setReturnLong";
            desc = "(J)V";
        } else if (type == Type.D) {
            method = "setReturnDouble";
            desc = "(D)V";
        } else if (type == Type.F) {
            method = "setReturnFloat";
            desc = "(F)V";
        } else {
            return;
        }
        mv.visitMethodInsn(INVOKEVIRTUAL, FunctionContext.TYPE.getPath(), method, desc, false);
    }

    public String getParentClassName() {
        return parentClassName;
    }
}
