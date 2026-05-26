package org.tabooproject.fluxon.runtime;

/**
 * FunctionContext 的返回值槽
 * 原始类型以 long 位模式保存，避免同步函数热路径装箱。
 *
 * @author sky
 */
final class FunctionReturnSlot {

    private long primitive;
    private Object ref;
    private Type type;

    void setInt(int v) {
        primitive = v;
        type = Type.I;
    }

    void setLong(long v) {
        primitive = v;
        type = Type.J;
    }

    void setDouble(double v) {
        primitive = Double.doubleToRawLongBits(v);
        type = Type.D;
    }

    void setFloat(float v) {
        primitive = Float.floatToRawIntBits(v);
        type = Type.F;
    }

    void setBool(boolean v) {
        primitive = v ? 1 : 0;
        type = Type.Z;
    }

    void setRef(Object v) {
        ref = v;
        type = Type.OBJECT;
    }

    long getPrimitive() {
        return primitive;
    }

    Object getRef() {
        if (type != null && type.isPrimitive()) {
            return Type.box(primitive, type);
        }
        return ref;
    }

    Type getType() {
        return type;
    }

    boolean isPrimitive() {
        return type != null && type.isPrimitive();
    }

    Object boxPrimitive() {
        if (type == Type.I || type == Type.Z) return (int) primitive;
        if (type == Type.J) return primitive;
        if (type == Type.D) return Double.longBitsToDouble(primitive);
        if (type == Type.F) return Float.intBitsToFloat((int) primitive);
        return null;
    }

    void clear() {
        ref = null;
        primitive = 0L;
        type = null;
    }
}
