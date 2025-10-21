package org.tabooproject.fluxon.interpreter.error;

/**
 * 索引访问异常
 * 当尝试访问或设置无效索引时抛出
 */
public class IndexAccessException extends RuntimeException {

    private final Object target;
    private final Object index;
    private final IndexErrorType errorType;

    public IndexAccessException(String message, Object target, Object index, IndexErrorType errorType) {
        super(message);
        this.target = target;
        this.index = index;
        this.errorType = errorType;
    }

    public IndexAccessException(String message, Object target, Object index, IndexErrorType errorType, Throwable cause) {
        super(message, cause);
        this.target = target;
        this.index = index;
        this.errorType = errorType;
    }

    public Object getTarget() {
        return target;
    }

    public Object getIndex() {
        return index;
    }

    public IndexErrorType getErrorType() {
        return errorType;
    }

    /**
     * 索引错误类型
     */
    public enum IndexErrorType {
        /** 索引越界 */
        OUT_OF_BOUNDS,
        /** 目标类型不支持索引访问 */
        UNSUPPORTED_TYPE,
        /** 目标类型不支持索引设置 */
        UNSUPPORTED_SET_TYPE,
        /** 目标为 null */
        NULL_TARGET
    }

    /**
     * 创建索引越界异常
     */
    public static IndexAccessException outOfBounds(Object target, Object index, int size) {
        String message = String.format("Index out of bounds: index=%s, size=%d", index, size);
        return new IndexAccessException(message, target, index, IndexErrorType.OUT_OF_BOUNDS);
    }

    /**
     * 创建不支持索引访问的类型异常
     */
    public static IndexAccessException unsupportedType(Object target, Object index) {
        String typeName = target == null ? "null" : target.getClass().getName();
        String message = String.format("Cannot index type: %s", typeName);
        return new IndexAccessException(message, target, index, IndexErrorType.UNSUPPORTED_TYPE);
    }

    /**
     * 创建不支持索引设置的类型异常
     */
    public static IndexAccessException unsupportedSetType(Object target, Object index) {
        String typeName = target == null ? "null" : target.getClass().getName();
        String message = String.format("Cannot set index on type: %s", typeName);
        return new IndexAccessException(message, target, index, IndexErrorType.UNSUPPORTED_SET_TYPE);
    }

    /**
     * 创建目标为 null 的异常
     */
    public static IndexAccessException nullTarget(Object index) {
        return new IndexAccessException("Cannot index null target", null, index, IndexErrorType.NULL_TARGET);
    }
}

