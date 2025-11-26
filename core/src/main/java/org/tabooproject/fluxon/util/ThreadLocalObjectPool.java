package org.tabooproject.fluxon.util;

/**
 * 基于租约的线程本地对象池基类，封装了对象/Lease 的复用逻辑。
 *
 * @param <T> 池化对象类型
 * @param <L> 租约类型
 */
public abstract class ThreadLocalObjectPool<T, L extends ThreadLocalObjectPool.Lease<T>> {

    private final Object[] pool;
    private final Object[] leasePool;
    private int size;
    private int leaseSize;

    protected ThreadLocalObjectPool(int maxPoolSize) {
        this.pool = new Object[maxPoolSize];
        this.leasePool = new Object[maxPoolSize];
    }

    /**
     * 借用对象，委托子类提供上下文参数并返回租约。
     */
    @SuppressWarnings("unchecked")
    protected L borrowInternal() {
        T value;
        if (size > 0) {
            value = (T) pool[--size];
            pool[size] = null;
        } else {
            value = create();
        }
        onBorrow(value);
        L lease;
        if (leaseSize > 0) {
            lease = (L) leasePool[--leaseSize];
            leasePool[leaseSize] = null;
        } else {
            lease = newLease();
        }
        lease.reset(this, value);
        return lease;
    }

    private void releaseInternal(T value) {
        onRelease(value);
        if (size < pool.length) {
            pool[size++] = value;
        }
    }

    private void recycleLease(Lease<?> lease) {
        if (leaseSize < leasePool.length) {
            leasePool[leaseSize++] = lease;
        }
    }

    protected abstract T create();

    protected abstract void onBorrow(T value);

    protected abstract void onRelease(T value);

    protected abstract L newLease();

    /**
     * 租约，负责归还对象并回收到池。
     */
    public static class Lease<T> implements AutoCloseable {

        private ThreadLocalObjectPool<T, ?> owner;
        private T value;
        private Thread ownerThread;
        private boolean detached;

        protected void reset(ThreadLocalObjectPool<T, ?> owner, T value) {
            this.owner = owner;
            this.value = value;
            this.ownerThread = Thread.currentThread();
            this.detached = false;
        }

        public T get() {
            return value;
        }

        /**
         * 拿走对象，不归还池。
         */
        public T detach() {
            T current = value;
            this.value = null;
            this.detached = true;
            return current;
        }

        @Override
        public void close() {
            ThreadLocalObjectPool<T, ?> currentOwner = owner;
            T currentValue = value;
            boolean currentDetached = detached;
            if (currentOwner == null) {
                return;
            }
            if (!currentDetached && ownerThread != Thread.currentThread()) {
                value = null;
                owner = null;
                ownerThread = null;
                currentOwner.recycleLease(this);
                return;
            }
            if (!currentDetached && currentValue != null) {
                currentOwner.releaseInternal(currentValue);
            }
            value = null;
            owner = null;
            ownerThread = null;
            detached = false;
            currentOwner.recycleLease(this);
        }
    }
}
