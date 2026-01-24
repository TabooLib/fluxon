package org.tabooproject.fluxon.interpreter.destructure;

import org.tabooproject.fluxon.runtime.Environment;
import org.tabooproject.fluxon.runtime.Type;
import org.tabooproject.fluxon.runtime.collection.SingleEntryMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.IntFunction;

/**
 * 解构器注册表
 * 管理所有注册的解构器并提供统一的解构接口
 */
public class DestructuringRegistry {

    // 单例实例
    private static final DestructuringRegistry INSTANCE = new DestructuringRegistry();

    // 解构器列表
    private final List<Destructurer> destructurers = new ArrayList<>();

    // 默认解构器 - 用于处理其他解构器都不支持的情况
    private final Destructurer defaultDestructurer = new SingleValueDestructurer();

    /**
     * 获取单例实例
     *
     * @return DestructuringRegistry 实例
     */
    public static DestructuringRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 私有构造函数
     * 初始化内置解构器
     */
    private DestructuringRegistry() {
        // 注册内置解构器
        registerBuiltInDestructurers();
        // 通过 SPI 加载外部解构器
        registerExternalDestructurers();
    }

    /**
     * 注册内置解构器
     */
    private void registerBuiltInDestructurers() {
        // 注册顺序很重要，越具体的解构器应该越靠前
        destructurers.add(new MapDestructurer());
        destructurers.add(new MapEntryDestructurer());
        destructurers.add(new ListDestructurer());
        destructurers.add(new ArrayDestructurer());
    }

    /**
     * 通过 SPI 加载外部解构器
     */
    private void registerExternalDestructurers() {
        // 使用 Java SPI 机制加载外部扩展的解构器
        ServiceLoader<Destructurer> loader = ServiceLoader.load(Destructurer.class);
        for (Destructurer destructurer : loader) {
            destructurers.add(destructurer);
        }
    }

    /**
     * 注册自定义解构器
     *
     * @param destructurer 要注册的解构器
     */
    public void registerDestructurer(Destructurer destructurer) {
        if (destructurer != null && !destructurers.contains(destructurer)) {
            // 新解构器添加到列表开头，使其优先级高于已有的解构器
            destructurers.add(0, destructurer);
        }
    }

    /**
     * 执行解构
     *
     * @param environment 目标环境
     * @param variables 变量名列表
     * @param element 要解构的元素
     */
    public void destructure(Environment environment, Map<String, Integer> variables, Object element) {
        destructure(environment, variables, element, null);
    }

    /**
     * 执行解构（带类型信息）
     *
     * @param environment 目标环境
     * @param variables 变量名列表
     * @param element 要解构的元素
     * @param typeProvider 类型提供器，根据位置返回类型（可为 null）
     */
    public void destructure(Environment environment, Map<String, Integer> variables, Object element, IntFunction<Type> typeProvider) {
        if (variables.isEmpty()) {
            return;
        }
        // 单变量情况特殊处理 - 利用 SingleEntryMap 直接访问避免迭代器开销
        if (variables.size() == 1) {
            if (variables instanceof SingleEntryMap) {
                SingleEntryMap<String, Integer> single = (SingleEntryMap<String, Integer>) variables;
                int pos = single.getValue();
                Type type = typeProvider != null ? typeProvider.apply(pos) : null;
                assignVariable(environment, single.getKey(), element, pos, type);
            } else {
                Map.Entry<String, Integer> entry = variables.entrySet().iterator().next();
                int pos = entry.getValue();
                Type type = typeProvider != null ? typeProvider.apply(pos) : null;
                assignVariable(environment, entry.getKey(), element, pos, type);
            }
            return;
        }
        // 查找合适的解构器
        for (Destructurer destructurer : destructurers) {
            if (destructurer.supports(element)) {
                destructurer.destructure(environment, variables, element);
                return;
            }
        }
        // 如果没有找到合适的解构器，使用默认解构器
        defaultDestructurer.destructure(environment, variables, element);
    }

    /**
     * 根据索引赋值变量（局部变量走 setLocalRef，根变量走 setRootVariable）
     */
    static void assignVariable(Environment environment, String name, Object value, int index) {
        assignVariable(environment, name, value, index, null);
    }

    /**
     * 根据索引和类型赋值变量
     * 如果类型是 primitive 且 value 是 Number，存到 primitive 槽位
     */
    static void assignVariable(Environment environment, String name, Object value, int index, Type type) {
        if (index >= 0) {
            if (type != null && type.isPrimitive() && value instanceof Number) {
                Number num = (Number) value;
                if (type == Type.I || type == Type.Z) {
                    environment.setLocalInt(index, num.intValue());
                } else if (type == Type.J) {
                    environment.setLocalLong(index, num.longValue());
                } else if (type == Type.F) {
                    environment.setLocalFloat(index, num.floatValue());
                } else if (type == Type.D) {
                    environment.setLocalDouble(index, num.doubleValue());
                } else {
                    environment.setLocalRef(index, value);
                }
            } else {
                environment.setLocalRef(index, value);
            }
        } else {
            environment.setRootVariable(name, value);
        }
    }
}