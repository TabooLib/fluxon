package org.tabooproject.fluxon.interpreter.bytecode.emitter;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.tabooproject.fluxon.interpreter.bytecode.Instructions;
import org.tabooproject.fluxon.interpreter.bytecode.Primitives;
import org.tabooproject.fluxon.runtime.stdlib.Intrinsics;
import org.tabooproject.fluxon.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

import static org.objectweb.asm.Opcodes.*;
import static org.tabooproject.fluxon.runtime.Type.*;

/**
 * 方法分发策略
 * 根据方法数量自动选择最优分发方式（Switch/IfElse/Overload）
 */
@SuppressWarnings("BooleanMethodIsAlwaysInverted")
public class DispatchStrategy {

    private final Map<String, List<NamedMethod>> allMethods;
    private final List<NamedMethod> uniqueMethods;
    private final Map<String, List<NamedMethod>> overloadedMethods;

    /**
     * 构造分发策略
     *
     * @param exportMethods 导出方法数组
     */
    public DispatchStrategy(Method[] exportMethods) {
        this.allMethods = buildMethodsMap(exportMethods);
        this.uniqueMethods = new ArrayList<>();
        this.overloadedMethods = new LinkedHashMap<>();
        // 分离唯一方法和重载方法
        for (Map.Entry<String, List<NamedMethod>> entry : allMethods.entrySet()) {
            List<NamedMethod> methodList = entry.getValue();
            if (methodList.size() > 1) {
                overloadedMethods.put(entry.getKey(), methodList);
            } else {
                uniqueMethods.addAll(methodList);
            }
        }
    }

    /**
     * 生成分发逻辑
     *
     * @param mv            方法访问器
     * @param methodHandler 单个方法的处理回调
     */
    public void emit(MethodVisitor mv, MethodHandler methodHandler) {
        Label defaultLabel = new Label();
        Label endLabel = new Label();
        // 1. 处理重载方法（需要参数类型匹配）
        if (!overloadedMethods.isEmpty()) {
            emitOverloadDispatch(mv, defaultLabel);
        }
        // 2. 处理唯一方法（选择 switch 或 if-else）
        if (uniqueMethods.size() <= 3) {
            emitIfElseDispatch(mv, defaultLabel);
        } else {
            emitSwitchDispatch(mv, defaultLabel);
        }
        // 3. 生成各方法的处理代码
        for (List<NamedMethod> methodList : allMethods.values()) {
            for (NamedMethod namedMethod : methodList) {
                mv.visitLabel(namedMethod.getLabel());
                methodHandler.handle(mv, namedMethod.getMethod());
                mv.visitJumpInsn(GOTO, endLabel);
            }
        }
        // 4. 默认：抛出异常
        mv.visitLabel(defaultLabel);
        Instructions.emitThrowIllegalArgument(mv, "Unknown method: ", 1);
        mv.visitLabel(endLabel);
    }

    /**
     * 检查是否有导出方法
     */
    public boolean hasExportMethods() {
        return !allMethods.isEmpty();
    }

    // ========== 内部分发方法 ==========

    /**
     * 生成方法名相等性检查并跳转的字节码
     */
    private void emitNameEqualsJump(MethodVisitor mv, String name, Label targetLabel) {
        mv.visitVarInsn(ALOAD, 1);
        mv.visitLdcInsn(name);
        mv.visitMethodInsn(INVOKEVIRTUAL, STRING.getPath(), "equals", "(" + OBJECT + ")Z", false);
        mv.visitJumpInsn(IFNE, targetLabel);
    }

    /**
     * 生成重载方法的分发逻辑
     */
    private void emitOverloadDispatch(MethodVisitor mv, Label defaultLabel) {
        for (Map.Entry<String, List<NamedMethod>> entry : overloadedMethods.entrySet()) {
            Label nextGroup = new Label();
            // 判断方法名
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(entry.getKey());
            mv.visitMethodInsn(INVOKEVIRTUAL, STRING.getPath(), "equals", "(" + OBJECT + ")Z", false);
            mv.visitJumpInsn(IFEQ, nextGroup);
            // 按参数数量和类型特异性排序
            List<NamedMethod> methodList = entry.getValue();
            methodList.sort(Comparator
                    .comparingInt((NamedMethod m) -> m.getMethod().getParameterCount())
                    .thenComparing((NamedMethod m) -> -calculateMethodSpecificity(m.getMethod())));
            // 逐个检查参数匹配
            for (NamedMethod namedMethod : methodList) {
                Label nextMethod = new Label();
                Class<?>[] paramTypes = namedMethod.getMethod().getParameterTypes();
                // 长度检查
                mv.visitVarInsn(ALOAD, 3);
                mv.visitInsn(ARRAYLENGTH);
                mv.visitIntInsn(BIPUSH, paramTypes.length);
                mv.visitJumpInsn(IF_ICMPNE, nextMethod);
                // 参数类型兼容性检查
                for (int i = 0; i < paramTypes.length; i++) {
                    Instructions.emitLoadClass(mv, paramTypes[i]);
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitIntInsn(BIPUSH, i);
                    mv.visitInsn(AALOAD);
                    mv.visitMethodInsn(INVOKESTATIC, Intrinsics.TYPE.getPath(), "isCompatibleType", "(" + CLASS + OBJECT + ")Z", false);
                    mv.visitJumpInsn(IFEQ, nextMethod);
                }
                mv.visitJumpInsn(GOTO, namedMethod.getLabel());
                mv.visitLabel(nextMethod);
            }
            mv.visitLabel(nextGroup);
        }
    }

    /**
     * 生成 If-Else 链分发（用于方法数量较少的情况）
     */
    private void emitIfElseDispatch(MethodVisitor mv, Label defaultLabel) {
        for (NamedMethod method : uniqueMethods) {
            emitNameEqualsJump(mv, method.getName(), method.getLabel());
        }
        mv.visitJumpInsn(GOTO, defaultLabel);
    }

    /**
     * 生成 Switch 分发（基于 hashCode，用于方法数量较多的情况）
     */
    private void emitSwitchDispatch(MethodVisitor mv, Label defaultLabel) {
        // 按哈希码分组
        Map<Integer, List<NamedMethod>> hashGroups = new LinkedHashMap<>();
        for (NamedMethod method : uniqueMethods) {
            int hash = method.getName().hashCode();
            hashGroups.computeIfAbsent(hash, k -> new ArrayList<>()).add(method);
        }
        // 生成 switch 语句
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, STRING.getPath(), "hashCode", "()I", false);
        // 确保键值按升序排列
        List<Integer> sortedHashes = new ArrayList<>(hashGroups.keySet());
        sortedHashes.sort(Integer::compareTo);
        Label[] switchLabels = new Label[sortedHashes.size()];
        int[] switchKeys = new int[sortedHashes.size()];
        for (int i = 0; i < sortedHashes.size(); i++) {
            switchLabels[i] = new Label();
            switchKeys[i] = sortedHashes.get(i);
        }
        mv.visitLookupSwitchInsn(defaultLabel, switchKeys, switchLabels);
        // 为每个哈希码分支生成代码
        for (int i = 0; i < sortedHashes.size(); i++) {
            mv.visitLabel(switchLabels[i]);
            List<NamedMethod> methods = hashGroups.get(sortedHashes.get(i));
            for (NamedMethod method : methods) {
                emitNameEqualsJump(mv, method.getName(), method.getLabel());
            }
            mv.visitJumpInsn(GOTO, defaultLabel);
        }
    }

    // ========== 工具方法 ==========

    /**
     * 构建方法映射表（方法名称 → 同名的多个方法）
     */
    private static Map<String, List<NamedMethod>> buildMethodsMap(Method[] exportMethods) {
        Map<String, List<NamedMethod>> methodsMap = new LinkedHashMap<>();
        Set<String> originalNames = new HashSet<>();
        for (Method method : exportMethods) {
            originalNames.add(method.getName());
        }
        for (Method method : exportMethods) {
            String name = StringUtils.transformMethodName(method.getName());
            // 如果转换后的名称与其他原始方法名冲突，则使用原始名称
            if (originalNames.contains(name) && !name.equals(method.getName())) {
                name = method.getName();
            }
            NamedMethod namedMethod = new NamedMethod(name, method);
            methodsMap.computeIfAbsent(name, k -> new ArrayList<>()).add(namedMethod);
        }
        return methodsMap;
    }

    /**
     * 计算方法参数类型的总特异性分数
     */
    private static int calculateMethodSpecificity(Method method) {
        int score = 0;
        for (Class<?> paramType : method.getParameterTypes()) {
            score += Primitives.getTypeSpecificity(paramType);
        }
        return score;
    }
}
