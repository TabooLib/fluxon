package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.SourceExcerpt;
import org.tabooproject.fluxon.parser.SourceTrace;
import org.tabooproject.fluxon.parser.expression.Expression;
import org.tabooproject.fluxon.parser.statement.Statement;
import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.java.Optional;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;

import org.tabooproject.fluxon.runtime.collection.ImmutableMap;
import org.tabooproject.fluxon.runtime.collection.SingleEntryMap;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;
import static org.tabooproject.fluxon.runtime.Type.*;
import static org.tabooproject.fluxon.runtime.Type.INT;
import static org.tabooproject.fluxon.runtime.Type.OBJECT;

public class Instructions {

    private static final Type MAP = new Type(Map.class);
    private static final Type STRING_BUILDER = new Type(StringBuilder.class);
    private static final Type LINKED_HASH_MAP = new Type(LinkedHashMap.class);
    private static final Type IMMUTABLE_MAP = new Type(ImmutableMap.class);
    private static final Type SINGLE_ENTRY_MAP = new Type(SingleEntryMap.class);

    // region 类型工具

    /**
     * 生成 instanceof 类型检查的字节码
     * <p>
     * 输入栈：[obj]
     * 输出栈：[int] (0 表示 false, 1 表示 true)
     * <p>
     * 处理逻辑：
     * - 如果 obj 为 null，返回 0 (false)
     * - 否则使用 INSTANCEOF 字节码指令进行类型检查
     *
     * @param mv          方法访问器
     * @param targetClass 要检查的目标类型
     */
    public static void emitInstanceofCheck(MethodVisitor mv, Class<?> targetClass) {
        // 创建标签用于 null 检查和结果处理
        Label nullLabel = new Label();
        Label endLabel = new Label();
        // 复制栈顶值用于 null 检查
        mv.visitInsn(DUP);
        // 检查是否为 null
        mv.visitJumpInsn(IFNULL, nullLabel);
        // 非 null 情况：使用 INSTANCEOF 指令
        String internalName = getInternalName(targetClass);
        mv.visitTypeInsn(INSTANCEOF, internalName);
        mv.visitJumpInsn(GOTO, endLabel);
        // null 情况：弹出栈顶值并压入 0 (false)
        mv.visitLabel(nullLabel);
        mv.visitInsn(POP);
        mv.visitInsn(ICONST_0);
        // 结束标签
        mv.visitLabel(endLabel);
        // 栈上现在是 int (0 或 1)
    }

    // endregion

    // region 环境加载

    /**
     * 加载 environment 到栈顶
     * 根据 CodeContext 配置，从局部变量或字段加载
     *
     * @param mv  方法访问器
     * @param ctx 代码上下文
     */
    public static void loadEnvironment(MethodVisitor mv, CodeContext ctx) {
        if (ctx.useLocalEnvironment()) {
            // 从局部变量加载
            mv.visitVarInsn(ALOAD, ctx.getEnvironmentLocalSlot());
        } else {
            // 从字段加载（保持向后兼容）
            mv.visitVarInsn(ALOAD, 0);  // this
            mv.visitFieldInsn(GETFIELD, ctx.getClassName(), "environment", Environment.TYPE.getDescriptor());
        }
    }

    /**
     * 加载 FunctionContextPool 到栈顶
     * 从 CodeContext 中记录的局部变量槽位加载
     *
     * @param mv  方法访问器
     * @param ctx 代码上下文
     */
    public static void loadPool(MethodVisitor mv, CodeContext ctx) {
        int poolSlot = ctx.getPoolLocalSlot();
        if (poolSlot < 0) {
            throw new IllegalStateException("Pool local slot not set in CodeContext");
        }
        mv.visitVarInsn(ALOAD, poolSlot);
    }

    // endregion

    // region 参数处理

    /**
     * 生成安全的参数访问代码
     * 如果参数不存在，使用默认值；如果参数存在，进行类型转换
     */
    public static void emitSafeParameterAccess(MethodVisitor mv, int paramIndex, Class<?> paramType, Parameter parameter) {
        if (parameter.isAnnotationPresent(Optional.class)) {
            // 可选参数：检查数组长度，如果不存在则使用默认值
            Label hasParam = new Label();
            Label endLabel = new Label();
            // if (args.length > paramIndex)
            mv.visitVarInsn(ALOAD, 3); // args 数组
            mv.visitInsn(ARRAYLENGTH);
            mv.visitIntInsn(BIPUSH, paramIndex + 1);
            mv.visitJumpInsn(IF_ICMPGE, hasParam);
            // 参数不存在，使用默认值
            emitDefaultValue(mv, paramType);
            mv.visitJumpInsn(GOTO, endLabel);
            // 参数存在，获取并转换
            mv.visitLabel(hasParam);
            mv.visitVarInsn(ALOAD, 3); // args 数组
            mv.visitIntInsn(BIPUSH, paramIndex);
            mv.visitInsn(AALOAD);
            emitTypeConversion(mv, paramType);
            mv.visitLabel(endLabel);
        } else {
            // 必需参数：直接获取并转换
            mv.visitVarInsn(ALOAD, 3); // args 数组
            mv.visitIntInsn(BIPUSH, paramIndex);
            mv.visitInsn(AALOAD);
            emitTypeConversion(mv, paramType);
        }
    }

    /**
     * 生成默认值
     */
    public static void emitDefaultValue(MethodVisitor mv, Class<?> type) {
        if (type == boolean.class) {
            mv.visitInsn(ICONST_0); // false
        } else if (type == byte.class || type == short.class || type == int.class || type == char.class) {
            mv.visitInsn(ICONST_0);
        } else if (type == long.class) {
            mv.visitInsn(LCONST_0);
        } else if (type == float.class) {
            mv.visitInsn(FCONST_0);
        } else if (type == double.class) {
            mv.visitInsn(DCONST_0);
        } else {
            // 对象类型，推送 null
            mv.visitInsn(ACONST_NULL);
        }
    }

    /**
     * 生成类型转换和拆箱代码
     */
    public static void emitTypeConversion(MethodVisitor mv, Class<?> targetType) {
        // 对象类型，直接类型转换
        if (!targetType.isPrimitive()) {
            mv.visitTypeInsn(CHECKCAST, getInternalName(targetType));
            return;
        }
        String wrapperClass = Primitives.getWrapperClassName(targetType);
        if (wrapperClass == null) {
            throw new IllegalArgumentException("Unknown primitive type: " + targetType);
        }
        String unboxingMethod = Primitives.getUnboxingMethodName(targetType);
        String descriptor = "()" + org.objectweb.asm.Type.getDescriptor(targetType);
        // boolean 和 char 直接从包装类拆箱
        if (targetType == boolean.class || targetType == char.class) {
            mv.visitTypeInsn(CHECKCAST, wrapperClass);
            mv.visitMethodInsn(INVOKEVIRTUAL, wrapperClass, unboxingMethod, descriptor, false);
        } else {
            // 其他数字类型通过 Number 拆箱
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", unboxingMethod, descriptor, false);
        }
    }

    /**
     * 生成基本类型装箱代码
     */
    public static void emitBoxing(MethodVisitor mv, Class<?> primitiveType) {
        String wrapperClass = Primitives.getWrapperClassName(primitiveType);
        if (wrapperClass == null) {
            return; // 不是基本类型，无需装箱
        }
        // 获取基本类型的描述符
        String primitiveDesc = org.objectweb.asm.Type.getDescriptor(primitiveType);
        String wrapperDesc = "L" + wrapperClass + ";";
        mv.visitMethodInsn(INVOKESTATIC, wrapperClass, "valueOf", "(" + primitiveDesc + ")" + wrapperDesc, false);
    }

    // endregion

    // region Map 生成

    /**
     * 生成创建和填充 Map<String, Integer> 的字节码
     * <p>
     * 优化策略：
     * - size == 0: 使用 ImmutableMap.empty()
     * - size == 1: 使用 SingleEntryMap（保持泛型类型）
     * - size 2-4: 使用 ImmutableMap.of(...)
     * - size > 4: 回退到 LinkedHashMap
     */
    public static void emitVariablePositionMap(MethodVisitor mv, Map<String, Integer> variables) {
        int size = variables.size();
        if (size == 0) {
            // ImmutableMap.empty()
            mv.visitMethodInsn(INVOKESTATIC, IMMUTABLE_MAP.getPath(), "empty", "()" + IMMUTABLE_MAP, false);
        } else if (size == 1) {
            // SingleEntryMap 保持泛型类型兼容性
            emitSingleEntryMap(mv, variables);
        } else if (size <= 4) {
            // ImmutableMap.of(...) 用于 2-4 个键值对
            emitImmutableMap(mv, variables);
        } else {
            // LinkedHashMap 用于超过 4 个键值对
            emitLinkedHashMap(mv, variables);
        }
    }

    /**
     * 生成 SingleEntryMap 构造（单个键值对）
     */
    public static void emitSingleEntryMap(MethodVisitor mv, Map<String, Integer> variables) {
        Map.Entry<String, Integer> entry = variables.entrySet().iterator().next();
        mv.visitTypeInsn(NEW, SINGLE_ENTRY_MAP.getPath());
        mv.visitInsn(DUP);
        mv.visitLdcInsn(entry.getKey());   // 键 (String)
        mv.visitLdcInsn(entry.getValue()); // 值 (int)
        mv.visitMethodInsn(INVOKESTATIC, INT.getPath(), "valueOf", "(I)" + INT, false);
        mv.visitMethodInsn(INVOKESPECIAL, SINGLE_ENTRY_MAP.getPath(), "<init>", "(" + OBJECT + OBJECT + ")V", false);
    }

    /**
     * 生成 ImmutableMap.of(...) 调用（2-4 个键值对）
     */
    public static void emitImmutableMap(MethodVisitor mv, Map<String, Integer> variables) {
        int size = variables.size();
        // 将键值对按顺序压入栈
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(variables.entrySet());
        for (int i = 0; i < size; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            mv.visitLdcInsn(entry.getKey());   // 键 (String)
            mv.visitLdcInsn(entry.getValue()); // 值 (int)
            mv.visitMethodInsn(INVOKESTATIC, INT.getPath(), "valueOf", "(I)" + INT, false);
        }
        // 根据大小调用对应的 ImmutableMap.of(...) 方法
        String descriptor;
        switch (size) {
            case 2:
                descriptor = "(" + OBJECT + OBJECT + OBJECT + OBJECT + ")" + IMMUTABLE_MAP;
                break;
            case 3:
                descriptor = "(" + OBJECT + OBJECT + OBJECT + OBJECT + OBJECT + OBJECT + ")" + IMMUTABLE_MAP;
                break;
            case 4:
                descriptor = "(" + OBJECT + OBJECT + OBJECT + OBJECT + OBJECT + OBJECT + OBJECT + OBJECT + ")" + IMMUTABLE_MAP;
                break;
            default:
                throw new IllegalStateException("Unexpected size for ImmutableMap: " + size);
        }
        mv.visitMethodInsn(INVOKESTATIC, IMMUTABLE_MAP.getPath(), "of", descriptor, false);
    }

    /**
     * 生成 LinkedHashMap 创建和填充（超过 4 个键值对时使用）
     */
    public static void emitLinkedHashMap(MethodVisitor mv, Map<String, Integer> variables) {
        // 创建 LinkedHashMap 实例
        mv.visitTypeInsn(NEW, LINKED_HASH_MAP.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, LINKED_HASH_MAP.getPath(), "<init>", "()V", false);
        // 填充 Map
        for (Map.Entry<String, Integer> entry : variables.entrySet()) {
            mv.visitInsn(DUP);                 // 复制 Map 引用
            mv.visitLdcInsn(entry.getKey());   // 键
            mv.visitLdcInsn(entry.getValue()); // 值
            // 装箱
            mv.visitMethodInsn(INVOKESTATIC, INT.getPath(), "valueOf", "(I)" + INT, false);
            // 调用 put 方法
            mv.visitMethodInsn(INVOKEINTERFACE, MAP.getPath(), "put", "(" + OBJECT + OBJECT + ")" + OBJECT, true);
            // 丢弃 put 方法的返回值
            mv.visitInsn(POP);
        }
    }

    // endregion

    // region Class 对象操作

    /**
     * 加载 Class 对象到栈顶（自动处理基本类型）
     *
     * @param mv   方法访问器
     * @param type 要加载的类型
     */
    public static void emitLoadClass(MethodVisitor mv, Class<?> type) {
        if (type.isPrimitive()) {
            // 基本类型：使用包装类的 TYPE 字段
            mv.visitFieldInsn(GETSTATIC, Primitives.getWrapperClassName(type), "TYPE", CLASS.getDescriptor());
        } else {
            // 引用类型：使用 LDC 指令直接加载 Class 常量
            mv.visitLdcInsn(getType(type));
        }
    }

    /**
     * 生成 Class[] 数组
     *
     * @param mv    方法访问器
     * @param types 类型数组
     */
    public static void emitClassArray(MethodVisitor mv, Class<?>[] types) {
        mv.visitLdcInsn(types.length);
        mv.visitTypeInsn(ANEWARRAY, CLASS.getPath());
        for (int i = 0; i < types.length; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            emitLoadClass(mv, types[i]);
            mv.visitInsn(AASTORE);
        }
    }

    // endregion

    // region 注解生成

    /**
     * 生成注解的字节码
     */
    public static void emitAnnotation(MethodVisitor mv, Annotation annotation) {
        // 创建 Annotation 实例
        mv.visitTypeInsn(NEW, Annotation.TYPE.getPath());
        mv.visitInsn(DUP);
        // 推送注解名称
        mv.visitLdcInsn(annotation.getName());
        // 处理属性
        Map<String, Object> attributes = annotation.getAttributes();
        if (attributes.isEmpty()) {
            // 无属性，使用单参数构造函数
            mv.visitMethodInsn(INVOKESPECIAL, Annotation.TYPE.getPath(), "<init>", "(" + STRING + ")V", false);
        } else {
            // 有属性，创建属性 Map
            mv.visitTypeInsn(NEW, "java/util/HashMap");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            // 填充属性
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                mv.visitInsn(DUP);               // 复制 Map 引用
                mv.visitLdcInsn(entry.getKey()); // 键
                emitPushValue(mv, entry.getValue()); // 值
                // 调用 put 方法
                mv.visitMethodInsn(INVOKEINTERFACE, MAP.getPath(), "put", "(" + OBJECT + OBJECT + ")" + OBJECT, true);
                mv.visitInsn(POP); // 丢弃返回值
            }
            // 调用双参数构造函数
            mv.visitMethodInsn(INVOKESPECIAL, Annotation.TYPE.getPath(), "<init>", "(" + STRING + MAP + ")V", false);
        }
    }

    /**
     * 生成将对象值推送到栈上的字节码
     */
    public static void emitPushValue(MethodVisitor mv, Object value) {
        if (value == null) {
            mv.visitInsn(ACONST_NULL);
        } else if (value instanceof String) {
            mv.visitLdcInsn(value);
        } else if (value instanceof Boolean) {
            mv.visitInsn((Boolean) value ? ICONST_1 : ICONST_0);
            emitBoxing(mv, boolean.class);
        } else if (value instanceof Integer) {
            int intValue = (Integer) value;
            if (intValue >= -1 && intValue <= 5) {
                mv.visitInsn(ICONST_0 + intValue);
            } else if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
                mv.visitIntInsn(BIPUSH, intValue);
            } else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
                mv.visitIntInsn(SIPUSH, intValue);
            } else {
                mv.visitLdcInsn(intValue);
            }
            emitBoxing(mv, int.class);
        } else if (value instanceof Long) {
            mv.visitLdcInsn(value);
            emitBoxing(mv, long.class);
        } else if (value instanceof Double) {
            mv.visitLdcInsn(value);
            emitBoxing(mv, double.class);
        } else if (value instanceof Float) {
            mv.visitLdcInsn(value);
            emitBoxing(mv, float.class);
        } else {
            // 其他类型直接使用 LDC
            mv.visitLdcInsn(value);
        }
    }

    // endregion

    // region 异常生成

    /**
     * 生成抛出 IllegalArgumentException 的字节码
     * throw new IllegalArgumentException(prefix + dynamicValue)
     *
     * @param mv          方法访问器
     * @param prefix      静态前缀（如 "Unknown method: "）
     * @param dynamicSlot 动态部分所在的局部变量槽位
     */
    public static void emitThrowIllegalArgument(MethodVisitor mv, String prefix, int dynamicSlot) {
        mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
        mv.visitInsn(DUP);
        mv.visitTypeInsn(NEW, STRING_BUILDER.getPath());
        mv.visitInsn(DUP);
        mv.visitLdcInsn(prefix);
        mv.visitMethodInsn(INVOKESPECIAL, STRING_BUILDER.getPath(), "<init>", "(" + STRING + ")V", false);
        mv.visitVarInsn(ALOAD, dynamicSlot);
        mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.getPath(), "append", "(" + STRING + ")" + STRING_BUILDER, false);
        mv.visitMethodInsn(INVOKEVIRTUAL, STRING_BUILDER.getPath(), "toString", "()" + STRING, false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(" + STRING + ")V", false);
        mv.visitInsn(ATHROW);
    }

    /**
     * 生成抛出异常的字节码（静态消息）
     *
     * @param mv             方法访问器
     * @param exceptionClass 异常类内部名称
     * @param message        静态消息
     */
    public static void emitThrowException(MethodVisitor mv, String exceptionClass, String message) {
        mv.visitTypeInsn(NEW, exceptionClass);
        mv.visitInsn(DUP);
        mv.visitLdcInsn(message);
        mv.visitMethodInsn(INVOKESPECIAL, exceptionClass, "<init>", "(" + STRING + ")V", false);
        mv.visitInsn(ATHROW);
    }

    // endregion

    // region 方法调用

    /**
     * 生成方法调用（自动区分接口/类）
     *
     * @param mv     方法访问器
     * @param method 要调用的方法
     */
    public static void emitMethodCall(MethodVisitor mv, Method method) {
        String owner = getInternalName(method.getDeclaringClass());
        String methodDesc = getMethodDescriptor(method);
        boolean isInterface = method.getDeclaringClass().isInterface();
        if (isInterface) {
            mv.visitMethodInsn(INVOKEINTERFACE, owner, method.getName(), methodDesc, true);
        } else {
            mv.visitMethodInsn(INVOKEVIRTUAL, owner, method.getName(), methodDesc, false);
        }
    }

    /**
     * 生成带返回值处理的方法调用（void→null，基本类型→装箱）
     *
     * @param mv     方法访问器
     * @param method 要调用的方法
     */
    public static void emitMethodCallWithReturn(MethodVisitor mv, Method method) {
        emitMethodCall(mv, method);
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            mv.visitInsn(ACONST_NULL);
        } else if (returnType.isPrimitive()) {
            emitBoxing(mv, returnType);
        }
    }

    /**
     * 生成返回指令，处理原始类型拆箱
     *
     * @param mv                 方法访问器
     * @param expectedReturnType 期望的返回类型（Java Class）
     * @param actualType         实际栈上的类型
     */
    public static void emitReturn(MethodVisitor mv, Class<?> expectedReturnType, Type actualType) {
        if (expectedReturnType == null) {
            mv.visitInsn(ARETURN);
            return;
        }
        if (expectedReturnType == void.class) {
            if (actualType != Type.VOID) {
                mv.visitInsn(POP);
            }
            mv.visitInsn(RETURN);
        } else if (expectedReturnType.isPrimitive()) {
            if (!actualType.isPrimitive()) {
                emitTypeConversion(mv, expectedReturnType);
            }
            if (expectedReturnType == int.class || expectedReturnType == boolean.class || expectedReturnType == byte.class || expectedReturnType == short.class || expectedReturnType == char.class) {
                mv.visitInsn(IRETURN);
            } else if (expectedReturnType == long.class) {
                mv.visitInsn(LRETURN);
            } else if (expectedReturnType == float.class) {
                mv.visitInsn(FRETURN);
            } else if (expectedReturnType == double.class) {
                mv.visitInsn(DRETURN);
            }
        } else {
            if (actualType == Type.VOID) {
                mv.visitInsn(ACONST_NULL);
            } else if (expectedReturnType != Object.class) {
                // 栈上是 Object，需要转换为期望的引用类型
                mv.visitTypeInsn(CHECKCAST, expectedReturnType.getName().replace('.', '/'));
            }
            mv.visitInsn(ARETURN);
        }
    }

    /**
     * 加载局部变量并装箱为 Object
     *
     * @param mv        方法访问器
     * @param slot      局部变量槽位
     * @param paramType 参数类型
     */
    public static void loadAndBoxParameter(MethodVisitor mv, int slot, Class<?> paramType) {
        if (paramType == long.class) {
            mv.visitVarInsn(LLOAD, slot);
            emitBoxing(mv, long.class);
        } else if (paramType == double.class) {
            mv.visitVarInsn(DLOAD, slot);
            emitBoxing(mv, double.class);
        } else if (paramType == float.class) {
            mv.visitVarInsn(FLOAD, slot);
            emitBoxing(mv, float.class);
        } else if (paramType.isPrimitive()) {
            mv.visitVarInsn(ILOAD, slot);
            emitBoxing(mv, paramType);
        } else {
            mv.visitVarInsn(ALOAD, slot);
        }
    }

    /**
     * 创建子环境并绑定方法参数
     * 生成: Environment childEnv = Intrinsics.bindMethodParameters(this.environment, names, args);
     *
     * @param mv             方法访问器
     * @param ctx            代码上下文
     * @param className      当前类名
     * @param paramNames     参数名列表
     * @param paramTypes     参数类型数组
     * @param paramStartSlot 参数起始槽位（通常为 1）
     * @param localVarCount  局部变量数量
     */
    public static void emitChildEnvironmentWithParams(MethodVisitor mv, CodeContext ctx, String className, List<String> paramNames, Class<?>[] paramTypes, int paramStartSlot, int localVarCount) {
        int paramCount = Math.min(paramNames.size(), paramTypes.length);
        // 参数1: this.environment
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, className, "environment", Environment.TYPE.getDescriptor());
        // 参数2: new String[] { "name1", "name2", ... }
        mv.visitLdcInsn(paramCount);
        mv.visitTypeInsn(ANEWARRAY, STRING.getPath());
        for (int i = 0; i < paramCount; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            mv.visitLdcInsn(paramNames.get(i));
            mv.visitInsn(AASTORE);
        }
        // 参数3: new Object[] { arg1, arg2, ... }
        mv.visitLdcInsn(paramCount);
        mv.visitTypeInsn(ANEWARRAY, OBJECT.getPath());
        for (int i = 0; i < paramCount; i++) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            loadAndBoxParameter(mv, paramStartSlot + i, paramTypes[i]);
            mv.visitInsn(AASTORE);
        }
        // 参数4: 局部变量数量
        mv.visitLdcInsn(localVarCount);
        // 调用 Intrinsics.bindMethodParameters
        mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "bindMethodParameters", "(" + Environment.TYPE + "[" + STRING + "[" + OBJECT + I + ")" + Environment.TYPE, false);
        // 存入局部变量
        int envSlot = ctx.allocateLocalVar(Type.OBJECT);
        mv.visitVarInsn(ASTORE, envSlot);
        ctx.setEnvironmentLocalSlot(envSlot);
    }

    /**
     * 生成方法体字节码
     *
     * @param generator 字节码生成器
     * @param body      方法体（Statement 或 Expression）
     * @param ctx       代码上下文
     * @param mv        方法访问器
     * @return 返回值类型
     */
    public static Type emitMethodBody(BytecodeGenerator generator, ParseResult body, CodeContext ctx, MethodVisitor mv) {
        emitLineNumber(body, mv);
        if (body instanceof Statement) {
            return generator.generateStatementBytecode((Statement) body, ctx, mv);
        } else {
            return generator.generateExpressionBytecode((Expression) body, ctx, mv);
        }
    }

    // endregion

    // region 源码追踪

    /**
     * 根据 SourceTrace 向字节码生成行号信息，方便运行时错误定位。
     */
    public static void emitLineNumber(ParseResult node, MethodVisitor mv) {
        if (node == null) {
            return;
        }
        SourceExcerpt excerpt = SourceTrace.get(node);
        if (excerpt == null) {
            return;
        }
        Label label = new Label();
        mv.visitLabel(label);
        mv.visitLineNumber(excerpt.getLine(), label);
    }

    // endregion
}
