package org.tabooproject.fluxon.runtime;

import org.tabooproject.fluxon.lexer.TokenType;
import org.tabooproject.fluxon.interpreter.bytecode.Primitives;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fluxon 运算符重载注册表
 * 复合赋值先尝试命中这里，未命中再回退到内置 Operations。
 *
 * @author sky
 */
public final class OperatorOverloadRegistry {

    static final Map<TokenType, List<Entry>> ENTRIES = new EnumMap<>(TokenType.class);

    OperatorOverloadRegistry() {
    }

    public static synchronized void register(TokenType operator, Class<?> target, Method method, boolean returnsTarget) {
        Class<?>[] params = method.getParameterTypes();
        if (params.length != 2) {
            throw new IllegalStateException("@FluxonOperator 方法必须有两个参数: " + method);
        }
        if (!params[0].isAssignableFrom(target)) {
            throw new IllegalStateException("@FluxonOperator 第一个参数必须为 target 类型 (" + target.getName() + "): " + method);
        }
        try {
            MethodHandle handle = MethodHandles.lookup().unreflect(method);
            DirectBinding binding = DirectBinding.ofMethod(method.getDeclaringClass(), method);
            ENTRIES.computeIfAbsent(operator, k -> new ArrayList<>()).add(new Entry(target, params[1], handle, binding, returnsTarget));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("无法访问 @FluxonOperator 方法: " + method, e);
        }
    }

    public static boolean has(TokenType operator, Type leftType, Type rightType) {
        List<Entry> entries = ENTRIES.get(operator);
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        Class<?> leftClass = Primitives.boxToClass(leftType.getSource());
        Class<?> rightClass = Primitives.boxToClass(rightType.getSource());
        for (Entry entry : entries) {
            if (entry.matches(leftClass, rightClass)) {
                return true;
            }
        }
        return false;
    }

    public static Entry resolve(TokenType operator, Type leftType, Type rightType) {
        List<Entry> entries = ENTRIES.get(operator);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        Class<?> leftClass = Primitives.boxToClass(leftType.getSource());
        Class<?> rightClass = Primitives.boxToClass(rightType.getSource());
        return resolveBest(entries, leftClass, rightClass);
    }

    public static Result invoke(TokenType operator, Object left, Object right) {
        Entry entry = resolve(operator, left, right);
        if (entry == null) {
            return Result.missing();
        }
        return Result.found(entry.invoke(left, right));
    }

    static Entry resolve(TokenType operator, Object left, Object right) {
        if (left == null) {
            return null;
        }
        List<Entry> entries = ENTRIES.get(operator);
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        Class<?> leftClass = left.getClass();
        Class<?> rightClass = right == null ? Object.class : right.getClass();
        return resolveBest(entries, leftClass, rightClass);
    }

    /**
     * 统一选择最具体的运算符重载，供编译期类型和运行时对象两条路径复用。
     */
    static Entry resolveBest(List<Entry> entries, Class<?> leftClass, Class<?> rightClass) {
        Entry best = null;
        int bestScore = Integer.MIN_VALUE;
        for (Entry entry : entries) {
            if (!entry.matches(leftClass, rightClass)) {
                continue;
            }
            int score = entry.score(leftClass, rightClass);
            if (score > bestScore) {
                best = entry;
                bestScore = score;
            }
        }
        return best;
    }

    public static final class Result {

        public final boolean found;
        public final Object value;

        Result(boolean found, Object value) {
            this.found = found;
            this.value = value;
        }

        static Result found(Object value) {
            return new Result(true, value);
        }

        static Result missing() {
            return new Result(false, null);
        }
    }

    public static final class Entry {

        final Class<?> target;
        final Class<?> right;
        final MethodHandle handle;
        final DirectBinding binding;
        final boolean returnsTarget;

        Entry(Class<?> target, Class<?> right, MethodHandle handle, DirectBinding binding, boolean returnsTarget) {
            this.target = target;
            this.right = right;
            this.handle = handle;
            this.binding = binding;
            this.returnsTarget = returnsTarget;
        }

        boolean matches(Class<?> leftClass, Class<?> rightClass) {
            return target.isAssignableFrom(leftClass) && (right == Object.class || right.isAssignableFrom(rightClass));
        }

        int score(Class<?> leftClass, Class<?> rightClass) {
            int score = target == leftClass ? 100 : 10;
            if (right == rightClass) {
                score += 100;
            } else if (right != Object.class && right.isAssignableFrom(rightClass)) {
                score += 10;
            }
            return score;
        }

        Object invoke(Object left, Object rightValue) {
            try {
                Object result = handle.invoke(left, rightValue);
                return returnsTarget ? left : result;
            } catch (RuntimeException | Error e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        public Class<?> getTarget() {
            return target;
        }

        public Class<?> getRight() {
            return right;
        }

        public DirectBinding getBinding() {
            return binding;
        }
    }
}
