package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
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
     * 生成类型转换和拆箱代码
     */
    public static void generateTypeConversion(MethodVisitor mv, Class<?> targetType) {
        if (targetType == boolean.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
        } else if (targetType == byte.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "byteValue", "()B", false);
        } else if (targetType == short.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "shortValue", "()S", false);
        } else if (targetType == int.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
        } else if (targetType == long.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
        } else if (targetType == float.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
        } else if (targetType == double.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
        } else if (targetType == char.class) {
            mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
        } else {
            // 对象类型，直接类型转换
            mv.visitTypeInsn(CHECKCAST, org.objectweb.asm.Type.getInternalName(targetType));
        }
    }

    /**
     * 生成基本类型装箱代码
     */
    public static void generateBoxing(MethodVisitor mv, Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
        } else if (primitiveType == byte.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
        } else if (primitiveType == short.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
        } else if (primitiveType == int.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
        } else if (primitiveType == long.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
        } else if (primitiveType == float.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
        } else if (primitiveType == double.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
        } else if (primitiveType == char.class) {
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
        }
    }

    private static final Type MAP = new Type(Map.class);
    private static final Type LINKED_HASH_MAP = new Type(LinkedHashMap.class);
}
