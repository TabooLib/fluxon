package org.tabooproject.fluxon.interpreter.bytecode;

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.definition.Annotation;
import org.tabooproject.fluxon.parser.ParseResult;
import org.tabooproject.fluxon.parser.SourceExcerpt;
import org.tabooproject.fluxon.parser.SourceTrace;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.java.Optional;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;

public class BytecodeUtils {

    /**
     * 生成创建和填充 Map<String, Integer> 的字节码
     */
    public static void generateVariablePositionMap(MethodVisitor mv, LinkedHashMap<String, Integer> variables) {
        // 创建 HashMap 实例
        mv.visitTypeInsn(NEW, LINKED_HASH_MAP.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, LINKED_HASH_MAP.getPath(), "<init>", "()V", false);

        // 填充 Map
        for (Map.Entry<String, Integer> entry : variables.entrySet()) {
            mv.visitInsn(DUP);                 // 复制 Map 引用
            mv.visitLdcInsn(entry.getKey());   // 键
            mv.visitLdcInsn(entry.getValue()); // 值
            // 装箱
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            // 调用 put 方法
            mv.visitMethodInsn(INVOKEINTERFACE, MAP.getPath(), "put", "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT, true);
            // 丢弃 put 方法的返回值
            mv.visitInsn(POP);
        }
    }

    /**
     * 生成安全的参数访问代码
     * 如果参数不存在，使用默认值；如果参数存在，进行类型转换
     */
    public static void generateSafeParameterAccess(MethodVisitor mv, int paramIndex, Class<?> paramType, java.lang.reflect.Parameter parameter) {
        boolean isOptional = parameter.isAnnotationPresent(Optional.class);

        if (isOptional) {
            // 可选参数：检查数组长度，如果不存在则使用默认值
            Label hasParam = new Label();
            Label endLabel = new Label();

            // if (args.length > paramIndex)
            mv.visitVarInsn(ALOAD, 3); // args 数组
            mv.visitInsn(ARRAYLENGTH);
            mv.visitIntInsn(BIPUSH, paramIndex + 1);
            mv.visitJumpInsn(IF_ICMPGE, hasParam);

            // 参数不存在，使用默认值
            generateDefaultValue(mv, paramType);
            mv.visitJumpInsn(GOTO, endLabel);

            // 参数存在，获取并转换
            mv.visitLabel(hasParam);
            mv.visitVarInsn(ALOAD, 3); // args 数组
            mv.visitIntInsn(BIPUSH, paramIndex);
            mv.visitInsn(AALOAD);
            generateTypeConversion(mv, paramType);

            mv.visitLabel(endLabel);
        } else {
            // 必需参数：直接获取并转换
            mv.visitVarInsn(ALOAD, 3); // args 数组
            mv.visitIntInsn(BIPUSH, paramIndex);
            mv.visitInsn(AALOAD);
            generateTypeConversion(mv, paramType);
        }
    }

    /**
     * 生成默认值
     */
    public static void generateDefaultValue(MethodVisitor mv, Class<?> type) {
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
     * 获取基本类型对应的包装类内部名称
     */
    @Nullable
    public static String getWrapperClassName(Class<?> primitiveType) {
        if (primitiveType == int.class) return "java/lang/Integer";
        if (primitiveType == long.class) return "java/lang/Long";
        if (primitiveType == double.class) return "java/lang/Double";
        if (primitiveType == float.class) return "java/lang/Float";
        if (primitiveType == boolean.class) return "java/lang/Boolean";
        if (primitiveType == byte.class) return "java/lang/Byte";
        if (primitiveType == short.class) return "java/lang/Short";
        if (primitiveType == char.class) return "java/lang/Character";
        if (primitiveType == void.class) return "java/lang/Void";
        return null;
    }

    /**
     * 获取基本类型对应的拆箱方法名
     */
    @Nullable
    private static String getUnboxingMethodName(Class<?> primitiveType) {
        if (primitiveType == boolean.class) return "booleanValue";
        if (primitiveType == byte.class) return "byteValue";
        if (primitiveType == short.class) return "shortValue";
        if (primitiveType == int.class) return "intValue";
        if (primitiveType == long.class) return "longValue";
        if (primitiveType == float.class) return "floatValue";
        if (primitiveType == double.class) return "doubleValue";
        if (primitiveType == char.class) return "charValue";
        return null;
    }

    /**
     * 生成类型转换和拆箱代码
     */
    public static void generateTypeConversion(MethodVisitor mv, Class<?> targetType) {
        // 对象类型，直接类型转换
        if (!targetType.isPrimitive()) {
            mv.visitTypeInsn(CHECKCAST, org.objectweb.asm.Type.getInternalName(targetType));
            return;
        }

        String wrapperClass = getWrapperClassName(targetType);
        if (wrapperClass == null) {
            throw new IllegalArgumentException("Unknown primitive type: " + targetType);
        }

        String unboxingMethod = getUnboxingMethodName(targetType);
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
    public static void generateBoxing(MethodVisitor mv, Class<?> primitiveType) {
        String wrapperClass = getWrapperClassName(primitiveType);
        if (wrapperClass == null) {
            return; // 不是基本类型，无需装箱
        }
        // 获取基本类型的描述符
        String primitiveDesc = org.objectweb.asm.Type.getDescriptor(primitiveType);
        String wrapperDesc = "L" + wrapperClass + ";";
        mv.visitMethodInsn(INVOKESTATIC, wrapperClass, "valueOf", "(" + primitiveDesc + ")" + wrapperDesc, false);
    }

    /**
     * 生成注解的字节码
     */
    public static void generateAnnotation(MethodVisitor mv, Annotation annotation) {
        // 创建 Annotation 实例
        mv.visitTypeInsn(NEW, Annotation.TYPE.getPath());
        mv.visitInsn(DUP);
        // 推送注解名称
        mv.visitLdcInsn(annotation.getName());
        // 处理属性
        Map<String, Object> attributes = annotation.getAttributes();
        if (attributes.isEmpty()) {
            // 无属性，使用单参数构造函数
            mv.visitMethodInsn(INVOKESPECIAL, Annotation.TYPE.getPath(), "<init>", "(Ljava/lang/String;)V", false);
        } else {
            // 有属性，创建属性 Map
            mv.visitTypeInsn(NEW, "java/util/HashMap");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            // 填充属性
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                mv.visitInsn(DUP);               // 复制 Map 引用
                mv.visitLdcInsn(entry.getKey()); // 键
                generatePushValue(mv, entry.getValue()); // 值
                // 调用 put 方法
                mv.visitMethodInsn(INVOKEINTERFACE, MAP.getPath(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitInsn(POP); // 丢弃返回值
            }
            // 调用双参数构造函数
            mv.visitMethodInsn(INVOKESPECIAL, Annotation.TYPE.getPath(), "<init>", "(Ljava/lang/String;Ljava/util/Map;)V", false);
        }
    }

    /**
     * 生成将对象值推送到栈上的字节码
     */
    private static void generatePushValue(MethodVisitor mv, Object value) {
        if (value == null) {
            mv.visitInsn(ACONST_NULL);
        } else if (value instanceof String) {
            mv.visitLdcInsn(value);
        } else if (value instanceof Boolean) {
            mv.visitInsn((Boolean) value ? ICONST_1 : ICONST_0);
            generateBoxing(mv, boolean.class);
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
            generateBoxing(mv, int.class);
        } else if (value instanceof Long) {
            mv.visitLdcInsn(value);
            generateBoxing(mv, long.class);
        } else if (value instanceof Double) {
            mv.visitLdcInsn(value);
            generateBoxing(mv, double.class);
        } else if (value instanceof Float) {
            mv.visitLdcInsn(value);
            generateBoxing(mv, float.class);
        } else {
            // 其他类型直接使用 LDC
            mv.visitLdcInsn(value);
        }
    }

    /**
     * 根据 SourceTrace 向字节码发射行号信息，方便运行时错误定位。
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

    /**
     * 计算类型的特异性分数（继承深度）
     * 分数越高表示类型越具体，用于重载解析时的优先级排序
     * <p>
     * 分数规则：
     * - null/Object: 0
     * - 接口: 1（最通用的引用类型）
     * - 具体类: 10 + 继承深度（确保比接口更具体）
     * - 基本类型: 100（最具体）
     *
     * @param type 要计算特异性的类型
     * @return 特异性分数
     */
    public static int getTypeSpecificity(Class<?> type) {
        if (type == null || type == Object.class) {
            return 0;
        }
        if (type.isPrimitive()) {
            return 100;
        }
        if (type.isInterface()) {
            return 1;
        }
        // 计算到 Object 的继承深度，基础分 10 确保比接口高
        int depth = 10;
        Class<?> current = type;
        while (current != null && current != Object.class) {
            depth++;
            current = current.getSuperclass();
        }
        return depth;
    }

    private static final Type MAP = new Type(Map.class);
    private static final Type LINKED_HASH_MAP = new Type(LinkedHashMap.class);
}
