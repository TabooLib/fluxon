package org.tabooproject.fluxon.inst;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 注入规格。
 * 包含目标类/方法信息和注入类型。
 */
public class InjectionSpec {

    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private final String id;
    private final String className;        // 内部格式，使用 '/' 分隔
    private final String methodName;
    private final String methodDescriptor; // null 表示匹配任意重载
    private final InjectionType type;
    private final long registrationTime;

    /**
     * 创建注入规格（自动生成 ID）。
     */
    public InjectionSpec(String className, String methodName, String methodDescriptor, InjectionType type) {
        this("inj_" + ID_COUNTER.incrementAndGet(), className, methodName, methodDescriptor, type);
    }

    /**
     * 创建注入规格（指定 ID）。
     */
    public InjectionSpec(String id, String className, String methodName, String methodDescriptor, InjectionType type) {
        this.id = id;
        this.className = className.replace('.', '/');
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.type = type;
        this.registrationTime = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public InjectionType getType() {
        return type;
    }

    public long getRegistrationTime() {
        return registrationTime;
    }

    /**
     * 检查此规格是否匹配给定方法。
     */
    public boolean matchesMethod(String name, String descriptor) {
        if (!methodName.equals(name)) {
            return false;
        }
        return methodDescriptor == null || methodDescriptor.equals(descriptor);
    }

    /**
     * 返回目标字符串表示。
     */
    public String getTarget() {
        String dotClassName = className.replace('/', '.');
        if (methodDescriptor != null) {
            return dotClassName + "::" + methodName + methodDescriptor;
        }
        return dotClassName + "::" + methodName;
    }

    @Override
    public String toString() {
        return "InjectionSpec{id='" + id + "', target='" + getTarget() + "', type=" + type + '}';
    }
}
