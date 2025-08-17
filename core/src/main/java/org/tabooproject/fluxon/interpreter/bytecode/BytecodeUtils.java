package org.tabooproject.fluxon.interpreter.bytecode;

import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.parser.VariablePosition;
import org.tabooproject.fluxon.runtime.Type;

import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;

public class BytecodeUtils {

    /**
     * 生成创建和填充 Map<String, VariablePosition> 的字节码
     */
    public static void generateVariablePositionMap(MethodVisitor mv, Map<String, VariablePosition> variables) {
        // 创建 HashMap 实例
        mv.visitTypeInsn(NEW, HASH_MAP.getPath());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, HASH_MAP.getPath(), "<init>", "()V", false);

        // 填充 Map
        for (Map.Entry<String, VariablePosition> entry : variables.entrySet()) {
            mv.visitInsn(DUP);               // 复制 Map 引用
            mv.visitLdcInsn(entry.getKey()); // 键

            // 创建 VariablePosition 实例
            mv.visitTypeInsn(NEW, VariablePosition.TYPE.getPath());
            mv.visitInsn(DUP);
            VariablePosition position = entry.getValue();
            mv.visitLdcInsn(position.getLevel()); // level 参数
            mv.visitLdcInsn(position.getIndex()); // index 参数
            mv.visitMethodInsn(INVOKESPECIAL, VariablePosition.TYPE.getPath(), "<init>", "(II)V", false);

            // 调用 put 方法
            mv.visitMethodInsn(INVOKEINTERFACE, MAP.getPath(), "put", "(" + Type.OBJECT + Type.OBJECT + ")" + Type.OBJECT, true);
            // 丢弃 put 方法的返回值
            mv.visitInsn(POP);
        }
    }

    private static final Type MAP = new Type(Map.class);
    private static final Type HASH_MAP = new Type(HashMap.class);
}
