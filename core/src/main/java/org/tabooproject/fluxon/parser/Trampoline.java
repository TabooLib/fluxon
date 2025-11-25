package org.tabooproject.fluxon.parser;

import java.util.function.Supplier;

public interface Trampoline<T> {

    /**
     * 获取最终结果；只有完成态才可调用。
     */
    T get();

    /**
     * 是否已经完成（可直接 get）；未完成则需要继续 next()。
     */
    default boolean isComplete() {
        return true;
    }

    /**
     * 取得下一步的 Trampoline，驱动状态机前进。
     */
    default Trampoline<T> next() {
        throw new UnsupportedOperationException("Trampoline is already complete");
    }

    /**
     * 构造完成态。
     */
    static <T> Trampoline<T> done(T value) {
        return () -> value;
    }

    /**
     * 构造未完成态：延迟 supplier，直到外部循环调用 next()。
     */
    static <T> Trampoline<T> more(Supplier<Trampoline<T>> supplier) {
        return new Trampoline<T>() {
            @Override
            public boolean isComplete() {
                return false;
            }

            @Override
            public T get() {
                throw new IllegalStateException("Trampoline has not finished");
            }

            @Override
            public Trampoline<T> next() {
                return supplier.get();
            }
        };
    }

    /**
     * 驱动 trampoline：循环调用 next 直到完成，再返回结果。
     */
    static <T> T run(Trampoline<T> trampoline) {
        Trampoline<T> current = trampoline;
        while (!current.isComplete()) {
            current = current.next();
        }
        return current.get();
    }

    interface Continuation<R> {

        /**
         * 继续执行解析链，将已有部分结果 value 交给后续 trampoline。
         */
        Trampoline<R> apply(R value);
    }
}
