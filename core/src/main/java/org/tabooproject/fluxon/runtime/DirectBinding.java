package org.tabooproject.fluxon.runtime;

import java.lang.reflect.Method;

/**
 * 直接绑定信息
 * 将 Fluxon 函数映射到 JVM 静态方法，编译器可直接生成 INVOKESTATIC 跳过函数调用框架
 *
 * @author sky
 */
public final class DirectBinding {

    final String owner;
    final String method;
    final String descriptor;

    public DirectBinding(String owner, String method, String descriptor) {
        this.owner = owner;
        this.method = method;
        this.descriptor = descriptor;
    }

    /**
     * 从 Class 和方法名自动生成绑定，根据函数签名推导 JVM descriptor
     */
    public static DirectBinding of(Class<?> clazz, String methodName, FunctionSignature signature) {
        String owner = clazz.getName().replace('.', '/');
        String descriptor = buildDescriptor(signature);
        return new DirectBinding(owner, methodName, descriptor);
    }

    /**
     * 从 Java Method 反射信息直接生成绑定
     * descriptor 精确匹配 Java 方法签名（包括 boolean/int 的区分）
     */
    public static DirectBinding ofMethod(Class<?> clazz, Method method) {
        String owner = clazz.getName().replace('.', '/');
        String descriptor = org.objectweb.asm.Type.getMethodDescriptor(method);
        return new DirectBinding(owner, method.getName(), descriptor);
    }

    public String getOwner() {
        return owner;
    }

    public String getMethod() {
        return method;
    }

    public String getDescriptor() {
        return descriptor;
    }

    /**
     * 修正返回类型
     * TYPE_MAP 将 Integer/Long/... 统一映射到 primitive Type（I/J/...），
     * 但 JVM descriptor 保留了精确的方法签名。当 Java 方法返回包装类型（如 Integer）时，
     * JVM 栈上是引用类型，不能按 primitive 处理（否则 VerifyError）。
     */
    public Type reconcileReturnType(Type signatureReturn) {
        if (!signatureReturn.isPrimitive()) return signatureReturn;
        // 从 descriptor 提取 JVM 返回类型：取 ')' 之后的部分
        char jvmReturn = descriptor.charAt(descriptor.indexOf(')') + 1);
        // JVM 返回引用类型（L...;）或数组（[）但 Fluxon 签名说 primitive → 栈上实际是 boxed 对象
        if (jvmReturn == 'L' || jvmReturn == '[') {
            return Type.OBJECT;
        }
        return signatureReturn;
    }

    /**
     * 用 JVM descriptor 修正参数类型
     * 当 TYPE_MAP 将包装类映射为 primitive（如 Integer→I）但 JVM 方法实际期望引用类型时，
     * 将对应位置的参数类型修正为 OBJECT，避免错误的拆箱。
     *
     * @param signatureTypes FunctionSignature 中的参数类型
     * @param skipParams     跳过前 N 个 descriptor 参数（扩展函数跳过 target）
     * @return 修正后的参数类型数组
     */
    public Type[] reconcileParamTypes(Type[] signatureTypes, int skipParams) {
        // 快速检查：如果没有 primitive 参数类型，无需修正
        boolean hasPrimitive = false;
        for (Type t : signatureTypes) {
            if (t.isPrimitive()) { hasPrimitive = true; break; }
        }
        if (!hasPrimitive) return signatureTypes;
        // 解析 descriptor 中的参数类型字符
        char[] jvmParamChars = parseDescriptorParamChars();
        if (jvmParamChars.length <= skipParams) return signatureTypes;
        Type[] result = null;
        for (int i = 0; i < signatureTypes.length; i++) {
            int descIdx = i + skipParams;
            if (descIdx >= jvmParamChars.length) break;
            if (signatureTypes[i].isPrimitive()) {
                char jvmChar = jvmParamChars[descIdx];
                if (jvmChar == 'L' || jvmChar == '[') {
                    // JVM 期望引用类型但签名说 primitive → 修正为 OBJECT
                    if (result == null) {
                        result = signatureTypes.clone();
                    }
                    result[i] = Type.OBJECT;
                }
            }
        }
        return result != null ? result : signatureTypes;
    }

    /**
     * 解析 JVM 方法 descriptor 的参数类型首字符
     * 返回每个参数类型的首字符数组（如 'I'=int, 'L'=object, '['=array）
     */
    private char[] parseDescriptorParamChars() {
        char[] chars = new char[16];
        int count = 0;
        int i = 1; // 跳过 '('
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            if (count >= chars.length) {
                char[] newChars = new char[chars.length * 2];
                System.arraycopy(chars, 0, newChars, 0, chars.length);
                chars = newChars;
            }
            chars[count++] = descriptor.charAt(i);
            i = skipDescriptorType(i);
        }
        char[] result = new char[count];
        System.arraycopy(chars, 0, result, 0, count);
        return result;
    }

    /**
     * 解析 descriptor 中每个参数的 JVM internal name
     * 引用类型返回具体类名（如 "java/lang/String"），primitive 和 java/lang/Object 返回 null。
     * 编译器用此信息在栈上类型为 Object 但 descriptor 期望具体子类时生成 CHECKCAST。
     *
     * @param skipParams 跳过前 N 个参数（扩展函数跳过 target）
     * @param paramCount 需要的参数数量（用户参数个数）
     * @return internal name 数组，全部为 null 时返回 null
     */
    public String[] getDescriptorParamCastTargets(int skipParams, int paramCount) {
        String[] targets = null;
        int paramIdx = 0;
        int descIdx = 0;
        int i = 1; // 跳过 '('
        while (i < descriptor.length() && descriptor.charAt(i) != ')') {
            char c = descriptor.charAt(i);
            if (c == 'L' && descIdx >= skipParams && paramIdx < paramCount) {
                int semi = descriptor.indexOf(';', i);
                String internalName = descriptor.substring(i + 1, semi);
                // java/lang/Object 不需要 CHECKCAST
                if (!internalName.equals("java/lang/Object")) {
                    if (targets == null) targets = new String[paramCount];
                    targets[paramIdx] = internalName;
                }
            }
            i = skipDescriptorType(i);
            if (descIdx >= skipParams) paramIdx++;
            descIdx++;
        }
        return targets;
    }

    /**
     * 跳过 descriptor 中位置 i 处的一个完整类型描述符，返回下一个类型的起始位置
     */
    private int skipDescriptorType(int i) {
        char c = descriptor.charAt(i);
        if (c == 'L') {
            return descriptor.indexOf(';', i) + 1;
        } else if (c == '[') {
            while (i < descriptor.length() && descriptor.charAt(i) == '[') i++;
            if (i < descriptor.length() && descriptor.charAt(i) == 'L') {
                return descriptor.indexOf(';', i) + 1;
            }
            return i + 1;
        }
        return i + 1;
    }

    private static String buildDescriptor(FunctionSignature signature) {
        StringBuilder sb = new StringBuilder("(");
        for (Type paramType : signature.getParameterTypes()) {
            sb.append(toJvmType(paramType));
        }
        sb.append(")");
        sb.append(toJvmType(signature.getReturnType()));
        return sb.toString();
    }

    private static String toJvmType(Type type) {
        if (type == Type.I || type == Type.Z) return "I";
        if (type == Type.J) return "J";
        if (type == Type.D) return "D";
        if (type == Type.F) return "F";
        if (type == Type.VOID) return "V";
        return "Ljava/lang/Object;";
    }

    @Override
    public String toString() {
        return owner + "." + method + descriptor;
    }
}
